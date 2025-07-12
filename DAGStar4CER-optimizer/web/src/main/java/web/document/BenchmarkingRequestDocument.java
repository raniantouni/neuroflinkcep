package web.document;

import core.parser.benchmarking.BenchmarkingRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import web.repository.RepositoryDocument;

import java.io.Serializable;
import java.time.Instant;

@Document(indexName = "benchmarking_request", createIndex = false)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
public class BenchmarkingRequestDocument implements RepositoryDocument<BenchmarkingRequest>, Serializable {
    @EqualsAndHashCode.Include
    @org.springframework.data.annotation.Id
    private String id;

    @Field(name = "@timestamp", type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Instant timestamp;

    @Field(name = "request", type = FieldType.Text)
    private String request;

    public BenchmarkingRequestDocument(BenchmarkingRequest benchmarkingRequest) {
        this.timestamp = Instant.now();
        this.request = gson.toJson(benchmarkingRequest);
    }

    @Override
    public String id() {
        return this.id;
    }

    public BenchmarkingRequest getObject() {
        return gson.fromJson(this.request, BenchmarkingRequest.class);
    }
}

