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

@Document(indexName = "operator-stats-transform-1", createIndex = false )
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
public class OperatorStatsDocument implements Serializable, RepositoryDocument<OperatorStatsDocument> {

    @EqualsAndHashCode.Include
    @org.springframework.data.annotation.Id
    private String id;

    @Field(name = "@timestamp.max", type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Instant timestamp; //Example: 2021-03-11T10:29:43.256Z

    @Field(name = "platform", type = FieldType.Keyword) //Optimizer platform (e.g flink/spark/akka)
    private String platform;

    @Field(name = "platform_id", type = FieldType.Keyword) //Optimizer platform ID (e.g site1_flink2)
    private String platform_id;

    @Field(name = "hostname", type = FieldType.Keyword) //Optimizer site
    private String hostname;

    @Field(name = "metric_key", type = FieldType.Keyword)
    private String metric_key;

    @Field(name = "job_id", type = FieldType.Keyword)
    private String job_id;

    @Field(name = "job_name", type = FieldType.Keyword)
    private String job_name;

    @Field(name = "metric_type", type = FieldType.Keyword)
    private String metric_type;

    @Field(name = "metric_value_number", type = FieldType.Double)
    private Double metric_value_number;

    @Field(name = "operator_type", type = FieldType.Keyword)
    private String operator_type;

    @Field(name = "operator_workflow_name", type = FieldType.Keyword)
    private String operator_workflow_name;

    public OperatorStatsDocument() {
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public OperatorStatsDocument getObject() {
        return this;
    }
}
