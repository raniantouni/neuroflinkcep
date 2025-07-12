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

@Document(indexName = "site-latency", createIndex = false)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
public class SiteLatencyDocument implements Serializable, RepositoryDocument<SiteLatencyDocument> {
    @EqualsAndHashCode.Include
    @org.springframework.data.annotation.Id
    private String id;

    @Field(name = "@timestamp", type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Instant timestamp;

    @Field(name = "source_site", type = FieldType.Text)
    private String src_site;

    @Field(name = "source_ip", type = FieldType.Ip)
    private String src_ip;

    @Field(name = "dst_site", type = FieldType.Text)
    private String dst_site;

    @Field(name = "dst_ip", type = FieldType.Ip)
    private String dst_ip;

    @Field(name = "latency_ms", type = FieldType.Integer)
    private int latency_ms;

    public SiteLatencyDocument() {
    }

    public SiteLatencyDocument(String src_site, String src_ip, String dst_site, String dst_ip, int latency_ms) {
        this.timestamp = Instant.now();
        this.src_site = src_site;
        this.src_ip = src_ip;
        this.dst_site = dst_site;
        this.dst_ip = dst_ip;
        this.latency_ms = latency_ms;
        this.id = String.format("%s#%s#%s", src_site, dst_site, timestamp.toString());
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public SiteLatencyDocument getObject() {
        return this;
    }
}
