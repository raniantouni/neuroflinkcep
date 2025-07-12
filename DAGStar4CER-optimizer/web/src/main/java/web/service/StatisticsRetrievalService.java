package web.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerStats;
import com.spotify.docker.client.messages.ExecCreation;
import core.parser.dictionary.INFOREDictionary;
import lombok.extern.java.Log;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import web.document.*;
import web.interceptor.LoggingRequestInterceptor;
import web.repository.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Log
public class StatisticsRetrievalService {

    private final Object lock = new Object();
    // ***
    private Map<String, String> siteToContainerName;
    private Map<String, String> clusterIPs;
    private Map<String, Integer> siteTotalTaskSlots;
    private Map<String, String> dashboardIPs;
    //Clients and wrappers
    // REQUIRES EXTERNAL SYNCHRONIZATION!!!
    private DockerClient dockerClient;
    private Runtime runtime;
    // Flink-JMX stats for all operator
    private Map<String, Map<String, Double>> JMXUnpackedStatsPerFlinkOperator;     //<operator,<metric_key,metric_value>>
    //Symmetric map of latencies between sites
    private Map<String, Map<String, Integer>> siteToSiteLatencyMS;  //<site1,<siteX,latency>>
    //Container name mapped to container ID
    private Map<String, String> containerIdForContainerName;
    //Container name mapped to container IP
    private Map<String, String> containerIPForContainerName;       //<site_name,siteIP>
    //Flink JMX site-specific stats
    private Map<String, Map<String, Double>> JMXFlinkSiteMetricsForSiteName;  //<site_name,<metric_key#[Value|Rate|Count],metric_value>>
    //Flink JMX platform-specific stats
    private Map<String, Map<String, Map<String, Double>>> JMXFlinkPlatformMetricsForSiteAndPlatformName;  //<site_name,<platform_name,<metric_key#[Value|Rate|Count],metric_value>>>
    //Docker stats
    private Map<String, Map<String, Double>> dockerStatsForContainerName;
    //Flink task slots  - <site_name,<[available,used,remaining],value>>
    private Map<String, Map<String, Integer>> taskSlotInfo;
    private List<FlinkJobDocument> flinkJobDocumentList;
    private List<OperatorStatsDocument> operatorStatsDocument;
    //e.g <map1,streaming:map>
    private Map<String, String> operatorClassForOperatorName;
    @Autowired
    private OperatorStatsRepository operatorStatsRepository;
    @Autowired
    private SiteLatencyRepository siteLatencyRepository;
    @Autowired
    private HostStatsRepository hostStatsRepository;
    @Autowired
    private DockerStatsRepository dockerStatsRepository;
    @Autowired
    private FlinkJobStatsRepository flinkJobStatsRepository;
    @Autowired
    private OperatorsAndJobsRepository operatorsAndJobsRepository;
    @Autowired
    @Qualifier("elasticsearchClient")
    private RestHighLevelClient ELKClient;
    private RestTemplate restTemplate;

    @PostConstruct
    private void init() throws DockerCertificateException {
        this.restTemplate = new RestTemplateBuilder()
                .errorHandler(new DefaultResponseErrorHandler())
                .interceptors(Collections.singletonList(new LoggingRequestInterceptor(false)))
                .requestFactory(() -> new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
                .build();
        this.runtime = Runtime.getRuntime();
        this.JMXUnpackedStatsPerFlinkOperator = new HashMap<>();
        this.siteToSiteLatencyMS = new HashMap<>();
        this.containerIdForContainerName = new HashMap<>();
        this.JMXFlinkSiteMetricsForSiteName = new HashMap<>();
        this.containerIPForContainerName = new HashMap<>();
        this.dockerStatsForContainerName = new HashMap<>();
        this.taskSlotInfo = new HashMap<>();
        this.JMXFlinkPlatformMetricsForSiteAndPlatformName = new HashMap<>();
        this.dockerClient = DefaultDockerClient.fromEnv().build();

        this.siteToContainerName = new LinkedHashMap<>();
        this.clusterIPs = new LinkedHashMap<>();
        this.siteTotalTaskSlots = new LinkedHashMap<>();
        this.dashboardIPs = new LinkedHashMap<>();
        this.siteToContainerName.put("site1", "infore_optimizer_taskmanager-1_1");
        this.siteToContainerName.put("site2", "infore_optimizer_taskmanager-2_1");
        this.siteToContainerName.put("site3", "infore_optimizer_taskmanager-3_1");
        this.clusterIPs.put("site1", "http://192.168.1.200:8081/jobs");
        this.clusterIPs.put("site2", "http://192.168.1.200:18081/jobs");
        this.clusterIPs.put("site3", "http://192.168.1.200:28081/jobs");
        this.dashboardIPs.put("site1", "http://2.84.153.237:8081");
        this.dashboardIPs.put("site2", "http://2.84.153.237:18081");
        this.dashboardIPs.put("site3", "http://2.84.153.237:28081");
        this.siteTotalTaskSlots.put("site1", 4);
        this.siteTotalTaskSlots.put("site2", 2);
        this.siteTotalTaskSlots.put("site3", 1);
        this.operatorClassForOperatorName = new HashMap<>();

        this.flinkJobDocumentList = new ArrayList<>();
        this.operatorStatsDocument = new ArrayList<>();
    }

    @PreDestroy
    private void shutdown() {
        this.dockerClient.close();
        log.info("Service shutdown.");
    }

    @Scheduled(cron = "${jobs.cronSchedule:-}")
    private void fetchOperatorStats() {
        Instant start = Instant.now();
        Map<String, Map<String, Double>> detailsPerStatAndOperatorNew = new HashMap<>();
        int hits = 0;
        List<OperatorStatsDocument> docs = new ArrayList<>();
        try {
            operatorStatsRepository.findAll().forEach(statsEntry -> {
                String op = statsEntry.getOperator_workflow_name();
                String key = statsEntry.getMetric_key() + "#" + statsEntry.getMetric_type();
                this.operatorClassForOperatorName.putIfAbsent(op, statsEntry.getOperator_type());
                detailsPerStatAndOperatorNew.putIfAbsent(op, new HashMap<>());
                detailsPerStatAndOperatorNew.get(op).put(key, statsEntry.getMetric_value_number());
                docs.add(statsEntry);
            });
            synchronized (this.lock) {
                this.operatorStatsDocument.addAll(docs);
            }
            this.JMXUnpackedStatsPerFlinkOperator.putAll(detailsPerStatAndOperatorNew);
            log.info(String.format("Updated stats for a total of %d operators and %d hits in %d ms.",
                    this.JMXUnpackedStatsPerFlinkOperator.size(), hits, Duration.between(start, Instant.now()).toMillis()));
        } catch (ElasticsearchStatusException e) {
            log.severe(e.getMessage());
        }
    }

    @Scheduled(cron = "${jobs.cronSchedule:-}")
    private void fetchHostStats() {
        Instant start = Instant.now();
        Map<String, Map<String, Double>> metricsPerSiteNew = new HashMap<>();
        Map<String, Map<String, Map<String, Double>>> metricsPerPlatformNew = new HashMap<>();
        int hits = 0;
        try {
            hostStatsRepository.findAll().forEach(hostStatsEntry -> {
                String site_name = hostStatsEntry.getSite();
                String site_metric_key = hostStatsEntry.getMetric_key();
                Double site_metric_value = hostStatsEntry.getMetric_value();
                String platform_name = hostStatsEntry.getPlatform();
                String key2 = hostStatsEntry.getMetric_key();

                metricsPerSiteNew.putIfAbsent(site_name, new HashMap<>());
                metricsPerPlatformNew.putIfAbsent(site_name, new HashMap<>());
                metricsPerPlatformNew.get(site_name).putIfAbsent(platform_name, new HashMap<>());

                metricsPerSiteNew.get(site_name).put(site_metric_key, site_metric_value);
                metricsPerPlatformNew.get(site_name).get(platform_name).put(key2, hostStatsEntry.getMetric_value());
            });
            this.JMXFlinkSiteMetricsForSiteName.putAll(metricsPerSiteNew);
            this.JMXFlinkPlatformMetricsForSiteAndPlatformName.putAll(metricsPerPlatformNew);
            log.info(String.format("Updated stats for a total of %d hosts and %d hits  in %d ms.",
                    this.JMXFlinkSiteMetricsForSiteName.size(), hits, Duration.between(start, Instant.now()).toMillis()));
        } catch (ElasticsearchStatusException e) {
            log.severe(e.getMessage());
        }
    }

    @Scheduled(cron = "${jobs.cronSchedule:-}")
    private void joinOperatorsAndFlinkJobs() {
        Instant start = Instant.now();
        Map<String, Map<String, Set<OperatorsAndJobsDocument>>> recordsPerJobNameAndSite = new HashMap<>();
        Map<String, Double> MemUtilForDocId = new HashMap<>();
        Map<String, Double> CpuUtilForDocId = new HashMap<>();

        Map<String, Double> recordsOutPSForJobNameSum = new HashMap<>();
        Map<String, Integer> recordsOutPSForJobNameCnt = new HashMap<>();
        Map<String, Integer> recordsOutForJobName = new HashMap<>();
        Map<String, Integer> lateRecordsDroppedForJobName = new HashMap<>();
        Map<String, Double> recordsInPSForJobNameSum = new HashMap<>();
        Map<String, Integer> recordsInPSForJobNameCnt = new HashMap<>();
        Map<String, Integer> recordsInForJobName = new HashMap<>();
        Map<String, Integer> throughputForJobName = new HashMap<>();
        Map<String, Integer> ingestionRateForJobName = new HashMap<>();
        Map<String, Set<String>> visitedKafkaOperators = new HashMap<>();
        Map<String, Map<String, Integer>> availSlotsPerJob = new HashMap<>();//job,site,slots
        Map<String, Map<String, Integer>> totalSlotsPerJob = new HashMap<>();//job,site,slots
        Map<String, Integer> availSlotsPerSite = new HashMap<>();
        Map<String, Integer> totalSlotsPerSite = new HashMap<>();

        int cntRecordsIn = 0, cntRecordsOut = 0;
        List<FlinkJobDocument> flinkJobDocumentListTemp;
        List<OperatorStatsDocument> operatorStatsDocumentTemp;
        Map<String, Set<String>> visitedJobNamesForSiteAndSlots = new HashMap<>();
        synchronized (this.lock) {
            flinkJobDocumentListTemp = new ArrayList<>(this.flinkJobDocumentList);
            operatorStatsDocumentTemp = new ArrayList<>(this.operatorStatsDocument);
        }
        for (FlinkJobDocument flinkJobDocument : flinkJobDocumentListTemp) {
            if (Duration.between(flinkJobDocument.getTimestamp(), start).getSeconds() > 50) {
                continue;
            }
            visitedJobNamesForSiteAndSlots.putIfAbsent(flinkJobDocument.getSite(), new HashSet<>());
            availSlotsPerSite.putIfAbsent(flinkJobDocument.getSite(), flinkJobDocument.getTotalTaskSlots());
            totalSlotsPerSite.putIfAbsent(flinkJobDocument.getSite(), flinkJobDocument.getTotalTaskSlots());

            if (!visitedJobNamesForSiteAndSlots.get(flinkJobDocument.getSite()).contains(flinkJobDocument.getJobName()) &&
                    flinkJobDocument.getStatus().equalsIgnoreCase("running")) {
                int new_slots = availSlotsPerSite.get(flinkJobDocument.getSite()) - flinkJobDocument.getJob_parallelism();
                availSlotsPerSite.put(flinkJobDocument.getSite(), new_slots);
                visitedJobNamesForSiteAndSlots.get(flinkJobDocument.getSite()).add(flinkJobDocument.getJobName());
            }

            totalSlotsPerJob.put(flinkJobDocument.getJobName(), new HashMap<>());
            totalSlotsPerJob.get(flinkJobDocument.getJobName()).put(flinkJobDocument.getSite(), flinkJobDocument.getTotalTaskSlots());
            availSlotsPerJob.put(flinkJobDocument.getJobName(), new HashMap<>());
            availSlotsPerJob.get(flinkJobDocument.getJobName()).put(flinkJobDocument.getSite(), flinkJobDocument.getAvailableTaskSlots());

            ingestionRateForJobName.put(flinkJobDocument.getJobName(), 0);
            throughputForJobName.put(flinkJobDocument.getJobName(), Integer.MAX_VALUE);
            visitedKafkaOperators.put(flinkJobDocument.getJobName(), new HashSet<>());
            for (OperatorStatsDocument operatorStatsDocument : operatorStatsDocumentTemp) {
                if (flinkJobDocument.getJobId().equals(operatorStatsDocument.getJob_id())) {
                    OperatorsAndJobsDocument rec = new OperatorsAndJobsDocument(operatorStatsDocument, flinkJobDocument);
                    MemUtilForDocId.put(rec.id(), flinkJobDocument.getPlatformMemoryUtilization());
                    CpuUtilForDocId.put(rec.id(), flinkJobDocument.getPlatformCPUUtilization());
                    switch (operatorStatsDocument.getMetric_key()) {
                        case "numRecordsOutPerSecond":
                            if (!operatorStatsDocument.getMetric_type().equalsIgnoreCase("rate")) {
                                continue;
                            }
                            int recsOutPerSecond = operatorStatsDocument.getMetric_value_number().intValue();
                            recordsOutPSForJobNameSum.putIfAbsent(rec.getJobName(), 0.0);
                            recordsOutPSForJobNameCnt.putIfAbsent(rec.getJobName(), 0);
                            recordsOutPSForJobNameSum.put(rec.getJobName(), recordsOutPSForJobNameSum.get(rec.getJobName()) + recsOutPerSecond);
                            recordsOutPSForJobNameCnt.put(rec.getJobName(), recordsOutPSForJobNameCnt.get(rec.getJobName()) + 1);
                            if (recsOutPerSecond > 0) {
                                throughputForJobName.put(rec.getJobName(), Math.min(throughputForJobName.get(rec.getJobName()), recsOutPerSecond));
                            }

                            String hash = operatorStatsDocument.getOperator_workflow_name() + "_" + operatorStatsDocument.getJob_id();
                            if (operatorStatsDocument.getOperator_type().equals("streaming:kafka_source") &&
                                    !visitedKafkaOperators.get(rec.getJobName()).contains(hash)) {
                                ingestionRateForJobName.put(rec.getJobName(), ingestionRateForJobName.get(rec.getJobName()) + recsOutPerSecond);
                                visitedKafkaOperators.get(rec.getJobName()).add(hash);
                            }
                            break;
                        case "numRecordsInPerSecond": {
                            if (!operatorStatsDocument.getMetric_type().equalsIgnoreCase("rate")) {
                                continue;
                            }
                            int recsInPerSecond = operatorStatsDocument.getMetric_value_number().intValue();
                            recordsInPSForJobNameSum.putIfAbsent(rec.getJobName(), 0.0);
                            recordsInPSForJobNameCnt.putIfAbsent(rec.getJobName(), 0);
                            recordsInPSForJobNameSum.put(rec.getJobName(), recordsInPSForJobNameSum.get(rec.getJobName()) + recsInPerSecond);
                            recordsInPSForJobNameCnt.put(rec.getJobName(), recordsInPSForJobNameCnt.get(rec.getJobName()) + 1);
                            break;
                        }
                        case "numLateRecordsDropped": {
                            int droppedRecords = operatorStatsDocument.getMetric_value_number().intValue();
                            lateRecordsDroppedForJobName.putIfAbsent(rec.getJobName(), 0);
                            lateRecordsDroppedForJobName.put(rec.getJobName(), lateRecordsDroppedForJobName.get(rec.getJobName()) + droppedRecords);
                            break;
                        }
                        case "numRecordsOut": {
                            if (!operatorStatsDocument.getMetric_type().equalsIgnoreCase("count")) {
                                continue;
                            }
                            int recordsOut = operatorStatsDocument.getMetric_value_number().intValue();
                            recordsOutForJobName.putIfAbsent(rec.getJobName(), 0);
                            recordsOutForJobName.put(rec.getJobName(), recordsOut);
                            cntRecordsOut++;
                            break;
                        }
                        case "numRecordsIn": {
                            if (!operatorStatsDocument.getMetric_type().equalsIgnoreCase("count")) {
                                continue;
                            }
                            int recordsIn = operatorStatsDocument.getMetric_value_number().intValue();
                            recordsInForJobName.putIfAbsent(rec.getJobName(), 0);
                            recordsInForJobName.put(rec.getJobName(), recordsIn);
                            cntRecordsIn++;
                            break;
                        }
                    }
                    recordsPerJobNameAndSite.putIfAbsent(rec.getJobName(), new HashMap<>());
                    recordsPerJobNameAndSite.get(rec.getJobName()).putIfAbsent(rec.getSite(), new HashSet<>());
                    recordsPerJobNameAndSite.get(rec.getJobName()).get(rec.getSite()).add(rec);
                }
            }
        }

        Map<String, Double> cpuPerJob = new HashMap<>();
        Map<String, Double> memPerJob = new HashMap<>();
        for (String jobName : recordsPerJobNameAndSite.keySet()) {
            double mem = 0.0, cpu = 0.0;
            int availSlots = availSlotsPerJob.get(jobName).values().stream().reduce(Integer::sum).orElse(0);
            int totalSlots = totalSlotsPerJob.get(jobName).values().stream().reduce(Integer::sum).orElse(0);
            for (String site : recordsPerJobNameAndSite.get(jobName).keySet()) {
                double utilization = 0;
                double platformUtilMem = 0;
                double platformUtilCPU = 0;
                for (OperatorsAndJobsDocument doc : recordsPerJobNameAndSite.get(jobName).get(site)) {
                    doc.setSlotPair(availSlots, totalSlots, availSlotsPerSite.get(site), totalSlotsPerSite.get(site));
                    utilization = Math.max(utilization, doc.getSlots_utilization());
                    platformUtilMem = Math.max(platformUtilMem, MemUtilForDocId.get(doc.id()));
                    platformUtilCPU = Math.max(platformUtilCPU, CpuUtilForDocId.get(doc.id()));
                }
                mem += Math.min(platformUtilMem * utilization, 100.0);
                cpu += Math.min(3 * platformUtilCPU * utilization, 800.0);
            }
            memPerJob.put(jobName, 100 * mem);
            cpuPerJob.put(jobName, cpu / 800.0);
        }

        List<OperatorsAndJobsDocument> docs = new ArrayList<>();
        for (String jobName : recordsPerJobNameAndSite.keySet()) {
            double avgRecordsOutPS = 0.0, avgRecordsInPS = 0.0;
            int recordsOut = 0, recordsIn = 0, ingestionRate = 0, lateRecords = 0, throughput = 0;
            boolean moreStats = false;
            if (recordsOutPSForJobNameSum.containsKey(jobName) && recordsOutPSForJobNameSum.get(jobName) != null && recordsOutPSForJobNameCnt.get(jobName) != null) {
                avgRecordsOutPS = recordsOutPSForJobNameSum.get(jobName) / recordsOutPSForJobNameCnt.get(jobName);
                avgRecordsInPS = recordsInPSForJobNameSum.get(jobName) / recordsInPSForJobNameCnt.get(jobName);
                recordsOut = (int) (recordsOutForJobName.getOrDefault(jobName, 0) / (double) cntRecordsOut);
                recordsIn = (int) (recordsInForJobName.getOrDefault(jobName, 0) / (double) cntRecordsIn);
                ingestionRate = ingestionRateForJobName.getOrDefault(jobName, 0);
                throughput = throughputForJobName.getOrDefault(jobName, 0);
                if (throughput == Integer.MAX_VALUE) {
                    throughput = 0;
                }
                lateRecords = lateRecordsDroppedForJobName.getOrDefault(jobName, 0);
                moreStats = true;
            }
            for (String site : recordsPerJobNameAndSite.get(jobName).keySet()) {
                for (OperatorsAndJobsDocument doc : recordsPerJobNameAndSite.get(jobName).get(site)) {
                    doc.setJob_sites(recordsPerJobNameAndSite.get(jobName).keySet());
                    doc.setJobCpuUtilization(cpuPerJob.get(jobName) * 20);
                    doc.setJobMemoryUtilization(memPerJob.get(jobName) * 0.5);
                    if (moreStats) {
                        doc.setJobAvgRecordsInPerSecond(avgRecordsInPS);
                        doc.setJobAvgRecordsOutPerSecond(avgRecordsOutPS);
                        doc.setJobRecordsOut(recordsOut);
                        doc.setJobRecordsIn(recordsIn);
                        doc.setIngestionRate(ingestionRate);
                        doc.setThroughput(throughput);
                        doc.setLateRecords(lateRecords);
                    }
                    docs.add(doc);
                }
            }
        }
        this.operatorsAndJobsRepository.saveAll(docs);
        log.info(String.format("Joined jobs and operators with %d records in %d ms.",
                docs.size(), Duration.between(start, Instant.now()).toMillis()));
    }

    //FIXME
   // @Scheduled(cron = "${jobs.cronSchedule:-}")
    private void computeDockerStats() throws IOException, InterruptedException, DockerException {
        Instant start = Instant.now();
        List<DockerStatsDocument> statsDocuments = new ArrayList<>();
        for (Map.Entry<String, String> cname_cid : this.containerIdForContainerName.entrySet()) {
            // Container ID, IP and name
            String containerName = cname_cid.getKey();
            String containerID = cname_cid.getValue();
            String containerIP = this.containerIPForContainerName.get(containerName);

            // Docker stats for this container via a docker client
            final ContainerStats stats;
            synchronized (dockerClient) {
                stats = dockerClient.stats(containerID);
            }
            DockerStatsDocument doc = new DockerStatsDocument(containerID, containerName, containerIP, stats);

            // Extra stats via the cmd line
            double cpu_percentage = -1.0;
            double memory_percentage = -1.0;
            Process proc2 = runtime.exec(String.format("docker stats %s", containerID));
            try (BufferedReader stdInput2 = new BufferedReader(new InputStreamReader(proc2.getInputStream()))) {
                String header = "", stats_data = "";
                for (int i = 0; i < 10; i++) {
                    header = stdInput2.readLine();
                    stats_data = stdInput2.readLine();
                }
                proc2.destroyForcibly();
                if (stats_data == null || stats_data.isBlank()) {
                    log.warning("Failed to get stats for site " + containerName + " with IP " + containerIP);
                } else {
                    List<String> tokens = Arrays.stream(stats_data.split(" "))
                            .filter(s1 -> !s1.isBlank())
                            .filter(s1 -> !s1.equals("/"))
                            .collect(Collectors.toList());
                    try {
                        String container_name = tokens.get(1);
                        cpu_percentage = Double.parseDouble(tokens.get(2).split("%")[0]);
                        memory_percentage = Double.parseDouble(tokens.get(5).split("%")[0]);
                        doc.enrich(cpu_percentage, memory_percentage);
                    } catch (Exception e) {
                        log.warning(e.getMessage());
                    }
                }
            }

            //Save to list
            statsDocuments.add(doc);
            this.dockerStatsForContainerName.putIfAbsent(containerName, new HashMap<>());
            if (memory_percentage >= 0 && cpu_percentage >= 0) {
                this.dockerStatsForContainerName.get(containerName).put("direct_cpu_percentage", cpu_percentage);
                this.dockerStatsForContainerName.get(containerName).put("direct_memory_percentage", memory_percentage);
            }
        }
        dockerStatsRepository.saveAll(statsDocuments);
        log.info(String.format("computeDockerStats took %d ms", Duration.between(start, Instant.now()).toMillis()));
    }

   // @Scheduled(cron = "${jobs.cronSchedule:-}")
    private void computePingForAllPlatforms() throws IOException, InterruptedException, DockerException {
        Instant start = Instant.now();

        //Run a low-level cmd to retrieve the network
        Process proc1 = runtime.exec("docker network inspect infore_optimizer_docker_optimizer_network");

        String bufferResult;
        try (
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc1.getInputStream()));
                BufferedReader stdError = new BufferedReader(new InputStreamReader(proc1.getErrorStream()))
        ) {
            // Read the output from the command
            String s;
            StringBuilder buffer = new StringBuilder();
            while ((s = stdInput.readLine()) != null) {
                buffer.append(s);
            }
            int e1 = proc1.waitFor();
            if (e1 != 0) {
                proc1.destroyForcibly();
                log.severe(String.format("Docker attach failed with error [%s]", stdError.lines().collect(Collectors.joining())));
                return;
            }
            proc1.destroyForcibly();
            bufferResult = buffer.toString();
        }
        if (bufferResult.isEmpty()) {
            return;
        }

        JsonArray jsonObject = JsonParser.parseString(bufferResult).getAsJsonArray();
        JsonObject root = jsonObject.get(0).getAsJsonObject();
        JsonElement containers_obj = root.getAsJsonObject("Containers");
        Set<Map.Entry<String, JsonElement>> containers = JsonParser.parseString(containers_obj.toString()).getAsJsonObject().entrySet();
        Map<String, String> siteIPs = new HashMap<>();
        Map<String, String> siteNameToContainerID = new HashMap<>();
        for (Map.Entry<String, JsonElement> container : containers) {
            String containerID = container.getKey();
            Set<Map.Entry<String, JsonElement>> map = JsonParser.parseString(container.getValue().toString()).getAsJsonObject().entrySet();
            String site = "";
            String ip = "";
            for (Map.Entry<String, JsonElement> entry : map) {
                String key = entry.getKey();
                String value = entry.getValue().toString();
                if (key.equals("IPv4Address")) {
                    ip = value.substring(0, value.indexOf("/")).replace("\"", "");
                }
                if (key.equals("Name")) {
                    site = value.replace("\"", "");
                }
            }
            if (site.isEmpty()) {
                throw new IllegalStateException("Empty name");
            }
            siteIPs.put(site, ip);
            siteNameToContainerID.put(site, containerID);
        }

        //<site1,<site2,latency_ms>>
        Map<String, Map<String, Integer>> latencyMap = new HashMap<>();
        for (Map.Entry<String, String> site_ip_A : siteIPs.entrySet()) {
            String srcSite = site_ip_A.getKey();
            String srcIp = site_ip_A.getValue();
            Map<String, Integer> curMap = new HashMap<>();
            for (Map.Entry<String, String> site_ip_B : siteIPs.entrySet()) {
                String destIp = site_ip_B.getValue();
                String destSite = site_ip_B.getKey();

                if (latencyMap.containsKey(destSite) && latencyMap.get(destSite).containsKey(srcSite)) {
                    curMap.put(destSite, latencyMap.get(destSite).get(srcSite));
                    continue;
                }

                final ExecCreation execCreation;
                final LogStream output;
                synchronized (dockerClient) {
                    execCreation = dockerClient.execCreate(siteNameToContainerID.get(destSite),
                            new String[]{"ping", destSite, "-c", "1"},
                            DockerClient.ExecCreateParam.attachStdout(),
                            DockerClient.ExecCreateParam.attachStderr());
                    output = dockerClient.execStart(execCreation.id());
                }
                final String execOutput = output.readFully();
                output.close();

                String token1 = execOutput.split("=")[3];
                String token2 = token1.split("ms")[0];
                int latency = (int) Math.ceil(Double.parseDouble(token2));
                curMap.put(destSite, latency);
                output.close();
                log.fine(String.format("Site [%s] -> Site [%s] latency: %d", srcSite, destSite, latency));
            }
            latencyMap.put(srcSite, curMap);
        }

        this.siteToSiteLatencyMS.putAll(latencyMap);
        this.containerIdForContainerName.putAll(siteNameToContainerID);
        this.containerIPForContainerName.putAll(siteIPs);

        //Update elasticsearch index with fresh values
        List<SiteLatencyDocument> list = new ArrayList<>();
        for (String src_site : this.siteToSiteLatencyMS.keySet()) {
            String src_ip = this.containerIPForContainerName.get(src_site);
            for (String dst_site : this.siteToSiteLatencyMS.get(src_site).keySet()) {
                String dst_ip = this.containerIPForContainerName.get(dst_site);
                int latency = this.siteToSiteLatencyMS.get(src_site).get(dst_site);
                list.add(new SiteLatencyDocument(src_site, src_ip, dst_site, dst_ip, latency));
            }
        }

        //ELK may not be ready..
        if (this.ELKClient.indices().exists(new GetIndexRequest("site-latency"), RequestOptions.DEFAULT)) {
            this.siteLatencyRepository.saveAll(list);
            log.info(String.format("Updated latency stats for %d sites in %d ms.",
                    this.containerIPForContainerName.size(), Duration.between(start, Instant.now()).toMillis()));
        } else {
            log.info(String.format("Updated LOCALLY latency stats for %d sites in %d ms.",
                    this.containerIPForContainerName.size(), Duration.between(start, Instant.now()).toMillis()));
        }
    }

   // @Scheduled(cron = "${jobs.cronSchedule:-}")
    private void computeHttpStats() {
        Instant start = Instant.now();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.TEXT_PLAIN));
        headers.setCacheControl(CacheControl.noCache());

        List<FlinkJobDocument> documents = new ArrayList<>();
        for (Map.Entry<String, String> entry : clusterIPs.entrySet()) {
            String siteIP = entry.getValue();
            String site = entry.getKey();
            String platform = "flink";
            String platform_id = site + "_flink1";
            ResponseEntity<String> dr = null;
            try {
                dr = this.restTemplate.exchange(siteIP, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            } catch (Exception e) {
                log.warning(e.getMessage());
            }
            if (dr == null || dr.getStatusCode() != HttpStatus.OK) {
                continue;
            }
            String body = dr.getBody();
            if (body == null) {
                continue;
            }
            JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("jobs").forEach(job -> {
                JsonObject jobObj = job.getAsJsonObject();
                String jobId = jobObj.get("id").getAsString();
                String status = jobObj.get("status").getAsString();
                documents.add(new FlinkJobDocument(site, platform, platform_id, jobId, status, siteIP));
            });
        }

        Map<String, AtomicInteger> usedSlotsPerSite = new HashMap<>();
        for (FlinkJobDocument doc : documents) {
            String site = doc.getSite();
            usedSlotsPerSite.putIfAbsent(site, new AtomicInteger(0));
            String siteIP = doc.getFlink_ip() + "/" + doc.getJobId();
            ResponseEntity<String> dr = null;
            try {
                dr = this.restTemplate.exchange(siteIP, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            } catch (Exception e) {
                log.warning(e.getMessage());
            }
            if (dr == null || dr.getStatusCode() != HttpStatus.OK) {
                continue;
            }
            String body = dr.getBody();
            if (body == null) {
                continue;
            }

            JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
            int start_time = obj.get("start-time").getAsInt();
            int end_time = obj.get("end-time").getAsInt();
            int duration = obj.get("duration").getAsInt();
            String jobname = obj.get("name").getAsString();
            AtomicInteger jobParallelism = new AtomicInteger(-1);
            obj.get("vertices").getAsJsonArray().forEach(vtx -> {
                JsonObject vtxObj = vtx.getAsJsonObject();
                int job_par = vtxObj.get("parallelism").getAsInt();
                usedSlotsPerSite.get(site).addAndGet(job_par);
                jobParallelism.set(job_par);
            });
            doc.setStart_time(Instant.ofEpochMilli(start_time));
            doc.setDuration(duration);
            doc.setJobName(jobname);
            doc.setJob_parallelism(jobParallelism.get());
            doc.setTotalTaskSlots(siteTotalTaskSlots.get(site));
            doc.setAvailableTaskSlots(doc.getTotalTaskSlots() - doc.getJob_parallelism());
            doc.setJob_url(String.format("%s/#/job/%s", dashboardIPs.get(site), doc.getJobId()));

            Map<String, Double> siteStats = this.dockerStatsForContainerName.get(siteToContainerName.get(site));
            double memory = 0.0, cpu = 0.0;
            if (siteStats != null && !siteStats.isEmpty()) {
                memory = siteStats.get("direct_memory_percentage");
                cpu = siteStats.get("direct_cpu_percentage");
            }
            doc.setPlatformMemoryUtilization(memory);
            doc.setPlatformCPUUtilization(cpu);
        }

        for (String site : siteTotalTaskSlots.keySet()) {
            if (!usedSlotsPerSite.containsKey(site) || !siteTotalTaskSlots.containsKey(site)) {
                continue;
            }
            int total_task_slots = siteTotalTaskSlots.get(site);
            int used_task_slots = usedSlotsPerSite.get(site).get();
            int available_task_slots = total_task_slots - used_task_slots;
            this.taskSlotInfo.putIfAbsent(site, new HashMap<>());
            this.taskSlotInfo.get(site).put("total", total_task_slots);
            this.taskSlotInfo.get(site).put("used", used_task_slots);
            this.taskSlotInfo.get(site).put("available", available_task_slots);
        }

        this.flinkJobStatsRepository.saveAll(documents);
        synchronized (this.lock) {
            this.flinkJobDocumentList.addAll(documents);
        }
        log.info(String.format("Updated %d flink jobs in %d ms.", documents.size(), Duration.between(start, Instant.now()).toMillis()));
    }

    /**
     * Adjusts dictionary values based on polled stats.
     */
    public INFOREDictionary adjustDictionaryWithModel1(INFOREDictionary dictionary) {
        Instant start = Instant.now();
        double ss_A = 1.0, ss_B = 1.0, ss_MUL = 0.0001;
        double ps_A = 100.0;
        double pm_A = 100.0;
        double sm_A = 1000.0;

        //Adjust input rates
        for (Map.Entry<String, Map<String, Double>> entry : this.JMXUnpackedStatsPerFlinkOperator.entrySet()) {
            //Retrieve data
            String operatorClassKey = this.operatorClassForOperatorName.get(entry.getKey());
            Map<String, Double> metrics = entry.getValue();

            //Update operator input rate
            Double op_rate = metrics.get("numRecordsOutPerSecond#Rate");
            if (op_rate != null) {
                int new_val = Math.toIntExact(Math.round(op_rate));
                Integer old_val = dictionary.updateInputRateForOperator(operatorClassKey, new_val);
                log.fine(String.format("Operator %s changed input rate from %d to %d.", operatorClassKey, old_val, new_val));
            }

            //Update site static cost by weighting CPU and Mem utilization
            for (String site : this.JMXFlinkSiteMetricsForSiteName.keySet()) {

                //Translate to container name
                String containerName = this.siteToContainerName.get(site);

                //Site static cost
                Map<String, Double> containerStats = this.dockerStatsForContainerName.get(containerName);
                Double cpu_percentage = containerStats == null
                        ? Double.valueOf(0.1)
                        : containerStats.getOrDefault("direct_cpu_percentage", 0.1);

                Double mem_percentage = containerStats == null
                        ? Double.valueOf(0.1)
                        : containerStats.getOrDefault("direct_memory_percentage", 0.1);
                Double total_mem_mb = this.JMXFlinkSiteMetricsForSiteName.get(site).getOrDefault("JVM.Memory.Direct.MemoryUsed", 128_000_000.0);
                int siteStaticCost = (int) (ss_MUL * (ss_A * mem_percentage * total_mem_mb.intValue() + ss_B * cpu_percentage));
                dictionary.updateSiteStaticCostForOperator(operatorClassKey, site, siteStaticCost);

                // Site migration cost is
                for (String other_site : this.JMXFlinkSiteMetricsForSiteName.keySet()) {
                    String other_container_name = this.siteToContainerName.get(other_site);
                    int mig_cost = this.siteToSiteLatencyMS.containsKey(containerName)
                            ? (int) sm_A * this.siteToSiteLatencyMS.get(containerName).get(other_container_name)
                            : 10;
                    dictionary.updateSiteMigrationCostForOperator(operatorClassKey, site, other_site, mig_cost);
                }

                //Iterate over all platforms in this site
                Set<String> allPlatforms = this.JMXFlinkPlatformMetricsForSiteAndPlatformName.get(site).keySet();
                for (String platform : allPlatforms) {

                    //Platform static cost
                    Map<String, Integer> stats = this.taskSlotInfo.get(site);
                    int platform_static_cost = stats == null || stats.isEmpty()
                            ? 100
                            : (int) (ps_A * (stats.getOrDefault("used", 1) / (double) stats.getOrDefault("total", 2)));
                    dictionary.updatePlatformStaticCostForOperator(operatorClassKey, platform, platform_static_cost);

                    //Platform migration cost
                    for (String other_platform : allPlatforms) {
                        int platform_migration_cost = this.JMXFlinkPlatformMetricsForSiteAndPlatformName != null && this.JMXFlinkPlatformMetricsForSiteAndPlatformName.containsKey(site)
                                ? (int) pm_A * this.JMXFlinkPlatformMetricsForSiteAndPlatformName.get(site).get(platform).get("buffers.outputQueueLength").intValue()
                                : 100;
                        dictionary.updatePlatformMigrationCostForOperator(operatorClassKey, platform, other_platform, platform_migration_cost);
                    }
                }

            }
        }

        //Keep track of dictionary live updates
        log.info(String.format("Updated dictionary with live stats in %d ms.", Duration.between(start, Instant.now()).toMillis()));

        //Return the same object, no copies.
        return dictionary;
    }

}
