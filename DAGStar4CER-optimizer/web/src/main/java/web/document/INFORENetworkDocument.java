package web.document;

import core.parser.network.INFORENetwork;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import web.repository.RepositoryDocument;

import java.io.Serializable;
import java.util.Date;

@Document(indexName = "infore_network", createIndex = false)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
public class INFORENetworkDocument implements RepositoryDocument<INFORENetwork>, Serializable {
    @Id
    @EqualsAndHashCode.Include
    private String id;

    @Field(name = "network", type = FieldType.Text)
    private String network;

    @Field(name = "@timestamp", type = FieldType.Date, format = DateFormat.date_optional_time)
    private Date modifiedAt;

    public INFORENetworkDocument() {
    }

    public INFORENetworkDocument(INFORENetwork network) {
        this.id = network.getNetwork();
        this.network = gson.toJson(network);
        this.modifiedAt = new Date();
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public INFORENetwork getObject() {
        return gson.fromJson(this.network, INFORENetwork.class);
    }
}
