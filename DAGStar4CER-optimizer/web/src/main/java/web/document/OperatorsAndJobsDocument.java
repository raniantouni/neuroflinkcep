package web.document;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import web.repository.RepositoryDocument;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Document(indexName = "operators-jobs", createIndex = false)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
public class OperatorsAndJobsDocument implements RepositoryDocument<OperatorsAndJobsDocument>, Serializable {
    @EqualsAndHashCode.Include
    @org.springframework.data.annotation.Id
    private String id;

    @Field(name = "@timestamp", type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Instant timestamp;

    @Field(name = "platform", type = FieldType.Keyword) //Optimizer platform (e.g flink/spark/akka)
    private String platform;

    @Field(name = "platform_id", type = FieldType.Keyword) //Optimizer platform ID (e.g site1_flink2)
    private String platform_id;

    @Field(name = "hostname", type = FieldType.Keyword)
    private String hostname;

    @Field(name = "job_id", type = FieldType.Keyword)
    private String jobId;

    @Field(name = "job_name", type = FieldType.Keyword)
    private String jobName;

    @Field(name = "metric_key", type = FieldType.Keyword)
    private String metric_key;

    @Field(name = "metric_type", type = FieldType.Keyword)
    private String metric_type;

    @Field(name = "metric_value_number", type = FieldType.Double)
    private Double metric_value_number;

    @Field(name = "operator_type", type = FieldType.Keyword)
    private String operator_type;

    @Field(name = "operator_workflow_name", type = FieldType.Keyword)
    private String operator_workflow_name;

    @Field(name = "status", type = FieldType.Keyword)
    private String status;

    @Field(name = "site", type = FieldType.Keyword)
    private String site;

    @Field(name = "job_sites", type = FieldType.Keyword)
    private String job_sites;

    @Field(name = "job_platforms", type = FieldType.Keyword)
    private String job_platforms;

    @Field(name = "start_time", type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant start_time;

    @Field(name = "duration", type = FieldType.Integer)
    private int duration;

    @Field(name = "flink_ip", type = FieldType.Keyword)
    private String flink_ip;

    @Field(name = "job_url", type = FieldType.Keyword)
    private String job_url;

    @Field(name = "platform_demo_name", type = FieldType.Keyword)
    private String platformDemoName;

    @Field(name = "site_demo_name", type = FieldType.Keyword)
    private String siteDemoName;

    @Field(name = "job_parallelism", type = FieldType.Integer)
    private int job_parallelism;

    @Field(name = "total_task_slots", type = FieldType.Long)
    private long totalTaskSlots;

    @Field(name = "avail_task_slots", type = FieldType.Long)
    private long availableTaskSlots;

    @Field(name = "site_avail_task_slots", type = FieldType.Long)
    private long siteAvailTaskSlots;

    @Field(name = "site_total_task_slots", type = FieldType.Long)
    private long siteTotalTaskSlots;

    @Field(name = "site_slot_utilization", type = FieldType.Float)
    private Double siteTaskSlotUtilization;

    @Field(name = "site_slot_utilization_times_100", type = FieldType.Float)
    private Double siteTaskSlotUtilization100;

    @Field(name = "job_memory_utilization_percentage", type = FieldType.Float)
    private Double jobMemoryUtilization;

    @Field(name = "job_cpu_utilization_percentage", type = FieldType.Float)
    private Double jobCpuUtilization;

    @Field(name = "platform_memory_utilization_percentage", type = FieldType.Float)
    private Double platformMemoryUtilization;

    @Field(name = "platform_cpu_utilization_percentage", type = FieldType.Float)
    private Double platformCPUUtilization;

    @Field(name = "slots_utilization", type = FieldType.Double)
    private Double slots_utilization;

    @Field(name = "slots_utilization_times_100", type = FieldType.Double)
    private Double slots_utilization100;

    @Field(name = "expected_mem_utilization_percentage", type = FieldType.Float)
    private Double expected_mem_utilization_percentage;

    @Field(name = "avg_records_out_per_second", type = FieldType.Float)
    private Double jobAvgRecordsOutPerSecond;

    @Field(name = "avg_records_in_per_second", type = FieldType.Float)
    private Double jobAvgRecordsInPerSecond;

    @Field(name = "ingestion_rate", type = FieldType.Integer)
    private int ingestionRate;

    @Field(name = "dropped_records", type = FieldType.Integer)
    private int droppedRecords;

    @Field(name = "job_records_in", type = FieldType.Integer)
    private int jobRecordsIn;

    @Field(name = "job_records_out", type = FieldType.Integer)
    private int jobRecordsOut;

    @Field(name = "late_records", type = FieldType.Integer)
    private int lateRecords;

    @Field(name = "throughput", type = FieldType.Integer)
    private int throughput;

    public OperatorsAndJobsDocument() {
    }

    public OperatorsAndJobsDocument(OperatorStatsDocument opDoc, FlinkJobDocument jobDoc) {
        this.timestamp = Instant.now();
        this.id = opDoc.id() + "_" + jobDoc.id();
        this.jobId = opDoc.getJob_id();
        this.site = jobDoc.getSite();
        this.platform = opDoc.getPlatform();
        switch (this.site) {
            case "site1":
                this.siteDemoName = "brehmen";
                this.platformDemoName = "brehm_flink";
                break;
            case "site3":
                this.siteDemoName = "barcelona";
                this.platformDemoName = "barc_flink";
                break;
            case "site2":
                this.siteDemoName = "athens";
                this.platformDemoName = "ath_flink";
                break;
            default:
                throw new IllegalStateException("Unknown site");
        }
        this.platform_id = opDoc.getPlatform_id();
        this.hostname = opDoc.getHostname();
        this.metric_key = opDoc.getMetric_key();
        this.metric_type = opDoc.getMetric_type();
        this.metric_value_number = opDoc.getMetric_value_number();
        this.operator_type = opDoc.getOperator_type();
        this.operator_workflow_name = opDoc.getOperator_workflow_name();
        this.status = jobDoc.getStatus();
        this.start_time = jobDoc.getStart_time();
        this.duration = jobDoc.getDuration();
        this.jobName = jobDoc.getJobName();
        this.flink_ip = jobDoc.getFlink_ip();
        this.job_url = jobDoc.getJob_url();
        this.job_parallelism = jobDoc.getJob_parallelism();
        double mul = 1.0 + 0.00001 * random.nextInt(10000);
        this.platformMemoryUtilization = 10 * mul * Math.min(jobDoc.getPlatformMemoryUtilization(), 100.0) * mul;
        double mul1 = 0.9 + 0.00001 * random.nextInt(10000);
        this.platformCPUUtilization = mul1 * Math.min(3 * jobDoc.getPlatformCPUUtilization(), 800.0) / 8.0;
    }

    public void setSlotPair(int availForJob, int totalForJob, int siteAvailSlots, int siteTotalSlots) {
        this.availableTaskSlots = availForJob;
        this.totalTaskSlots = totalForJob;
        this.slots_utilization = (totalTaskSlots - availableTaskSlots) / (double) totalTaskSlots;
        this.slots_utilization100 = slots_utilization * 100;

        this.siteAvailTaskSlots = siteAvailSlots;
        this.siteTotalTaskSlots = siteTotalSlots;
        this.siteTaskSlotUtilization = (siteTotalTaskSlots - siteAvailTaskSlots) / (double) siteTotalTaskSlots;
        this.siteTaskSlotUtilization100 = siteTaskSlotUtilization * 100;//Math.max(0.0, siteTaskSlotUtilization * 100 - 50);
    }

    public void setJob_sites(Set<String> job_sites) {
        Set<String> sites = new HashSet<>();
        Set<String> platforms = new HashSet<>();
        job_sites.forEach(site -> {
            switch (site) {
                case "site1":
                    sites.add("brehmen");
                    platforms.add("brehm_flink");
                    platforms.add("brehm_kafka");
                    break;
                case "site3":
                    sites.add("barcelona");
                    platforms.add("barc_flink");
                    platforms.add("barc_kafka");
                    break;
                case "site2":
                    sites.add("athens");
                    platforms.add("ath_flink");
                    platforms.add("ath_kafka");
                    break;
                default:
                    throw new IllegalStateException("Unknown site");
            }
        });

        this.job_sites = sites.stream().sorted().collect(Collectors.toList()).toString().replace("[", "").replace("]", "");
        this.job_platforms = platforms.stream().sorted().collect(Collectors.toList()).toString().replace("[", "").replace("]", "");
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public OperatorsAndJobsDocument getObject() {
        return this;
    }
}
