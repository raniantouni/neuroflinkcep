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

@Document(indexName = "host-stats-transform-1", createIndex = false)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
public class HostStatsDocument implements Serializable {

    @EqualsAndHashCode.Include
    @org.springframework.data.annotation.Id
    private String id;

    @Field(name = "@timestamp.max", type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Instant timestamp;

    @Field(name = "hostname", type = FieldType.Keyword)
    private String hostname;

    @Field(name = "metric_key", type = FieldType.Keyword)
    private String metric_key;

    @Field(name = "metric_value_number", type = FieldType.Double)
    private Double metric_value;

    @Field(name = "platform_id", type = FieldType.Keyword)
    private String platform_id;

    @Field(name = "platform", type = FieldType.Keyword)
    private String platform;

    @Field(name = "site", type = FieldType.Keyword)
    private String site;

    public HostStatsDocument() {
    }
}
