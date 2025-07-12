package web.document;

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

@Document(indexName = "flink-http-stats", createIndex = false)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
public class FlinkJobDocument implements RepositoryDocument<FlinkJobDocument>, Serializable {
    @EqualsAndHashCode.Include
    @org.springframework.data.annotation.Id
    private String id;

    @Field(name = "@timestamp", type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Instant timestamp;

    @Field(name = "jobName", type = FieldType.Keyword)
    private String jobName;

    @Field(name = "jobId", type = FieldType.Keyword)
    private String jobId;

    @Field(name = "status", type = FieldType.Keyword)
    private String status;

    @Field(name = "site", type = FieldType.Keyword)
    private String site;

    @Field(name = "platform", type = FieldType.Keyword)
    private String platform;

    @Field(name = "platform_id", type = FieldType.Keyword)
    private String platform_id;

    @Field(name = "start_time", type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant start_time;

    @Field(name = "duration", type = FieldType.Integer)
    private int duration;

    @Field(name = "flink_ip", type = FieldType.Keyword)
    private String flink_ip;

    @Field(name = "job_url", type = FieldType.Keyword)
    private String job_url;

    @Field(name = "job_parallelism", type = FieldType.Integer)
    private int job_parallelism;

    @Field(name = "total_task_slots", type = FieldType.Integer)
    private int totalTaskSlots;

    @Field(name = "avail_task_slots", type = FieldType.Integer)
    private int availableTaskSlots;

    @Field(name = "platform_memory_utilization_percentage", type = FieldType.Double)
    private Double platformMemoryUtilization;

    @Field(name = "platform_cpu_utilization_percentage", type = FieldType.Double)
    private Double platformCPUUtilization;


    public FlinkJobDocument() {
    }

    public FlinkJobDocument(String site, String platform, String platform_id, String jobId, String status, String siteIP) {
        this.timestamp = Instant.now();
        this.site = site;
        this.platform = platform;
        this.platform_id = platform_id;
        this.jobId = jobId;
        this.status = status;
        this.flink_ip = siteIP;
        this.id = String.format("%s_%s_%d", jobId, platform_id, timestamp.toEpochMilli());
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public FlinkJobDocument getObject() {
        return this;
    }
}
