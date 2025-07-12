package web.document;

import com.spotify.docker.client.messages.ContainerStats;
import com.spotify.docker.client.messages.NetworkStats;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import web.repository.RepositoryDocument;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Objects;

@Document(indexName = "docker-stats", createIndex = false)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
public class DockerStatsDocument implements RepositoryDocument<DockerStatsDocument>, Serializable {

    @EqualsAndHashCode.Include
    @org.springframework.data.annotation.Id
    private String id;

    @Field(name = "@timestamp", type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Instant timestamp;

    @Field(name = "containerId", type = FieldType.Keyword)
    private String containerId;

    @Field(name = "containerName", type = FieldType.Keyword)
    private String containerName;

    @Field(name = "containerIp", type = FieldType.Keyword)
    private String containerIp;

    @Field(name = "usermode_cpu_usage", type = FieldType.Long)
    private Long usermode_cpu_usage;

    @Field(name = "kernelmode_cpu_usage", type = FieldType.Long)
    private Long kernelmode_cpu_usage;

    @Field(name = "usermode_cpu_usage_fraction", type = FieldType.Double)
    private Double usermode_cpu_usage_fraction;

    @Field(name = "kernelmode_cpu_usage_fraction", type = FieldType.Double)
    private Double kernelmode_cpu_usage_fraction;

    @Field(name = "system_cpu_usage", type = FieldType.Long)
    private Long system_cpu_usage;

    @Field(name = "total_cpu_usage", type = FieldType.Long)
    private Long total_cpu_usage;

    @Field(name = "mem_usage", type = FieldType.Long)
    private Long mem_usage;

    @Field(name = "max_mem_usage", type = FieldType.Long)
    private Long max_mem_usage;

    @Field(name = "mem_limit", type = FieldType.Long)
    private Long mem_limit;

    @Field(name = "net_rx_bytes", type = FieldType.Long)
    private Long net_rx_bytes;

    @Field(name = "net_tx_bytes", type = FieldType.Long)
    private Long net_tx_bytes;

    @Field(name = "direct_cpu_percentage", type = FieldType.Double)
    private Double direct_cpu_percentage;

    @Field(name = "direct_memory_percentage", type = FieldType.Double)
    private Double direct_memory_percentage;

    @Field(name = "raw_data", type = FieldType.Keyword)
    private String raw_data;

    public DockerStatsDocument(String containerID, String containerName, String containerIP, ContainerStats stats) {
        this.id = containerID + "_" + System.nanoTime();
        this.timestamp = Instant.now();
        this.containerId = containerID;
        this.containerName = containerName;
        this.containerIp = containerIP;
        this.usermode_cpu_usage = stats.cpuStats().cpuUsage().usageInUsermode();
        this.total_cpu_usage = stats.cpuStats().cpuUsage().totalUsage();
        this.kernelmode_cpu_usage = stats.cpuStats().cpuUsage().usageInKernelmode();
        this.system_cpu_usage = stats.cpuStats().systemCpuUsage();
        this.usermode_cpu_usage_fraction = stats.cpuStats().cpuUsage().usageInUsermode() / (double) total_cpu_usage;
        this.kernelmode_cpu_usage_fraction = stats.cpuStats().cpuUsage().usageInKernelmode() / (double) total_cpu_usage;
        this.mem_usage = stats.memoryStats().usage();
        this.max_mem_usage = stats.memoryStats().maxUsage();
        this.mem_limit = stats.memoryStats().limit();
        if (stats.networks() != null) {
            NetworkStats net = Objects.requireNonNull(stats.networks()).values().stream().findFirst().orElse(null);
            if (net != null) {
                this.net_tx_bytes = net.txBytes();
                this.net_rx_bytes = net.rxBytes();
            }
        }
        this.raw_data = gson.toJson(stats);
    }


    @Override
    public String id() {
        return this.id;
    }

    public DockerStatsDocument getObject() {
        return this;
    }

    public void enrich(double cpu_percentage, double memory_percentage) {
        this.direct_cpu_percentage = cpu_percentage;
        this.direct_memory_percentage = memory_percentage;
    }
}
