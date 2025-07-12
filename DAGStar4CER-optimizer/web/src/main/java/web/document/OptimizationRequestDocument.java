package web.document;

import core.parser.workflow.OptimizationRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import web.repository.RepositoryDocument;

import java.io.Serializable;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Date;

@Document(indexName = "infore_request", createIndex = false)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
public class OptimizationRequestDocument implements RepositoryDocument<OptimizationRequest>, Serializable {

    //Guaranteed to be unique at all times
    @Id
    @EqualsAndHashCode.Include
    private String id;

    @Field(name = "request", type = FieldType.Text)
    private String request;

    @Field(name = "user", type = FieldType.Keyword)
    private String user;

    @Field(name = "@timestamp", type = FieldType.Date, format = DateFormat.date_optional_time)
    private Date modifiedAt;

    @Field(name = "isContinuous", type = FieldType.Boolean)
    private boolean isContinuous;

    @Field(name = "parameters", type = FieldType.Keyword)
    private String parameters;

    public OptimizationRequestDocument() {
    }

    public OptimizationRequestDocument(OptimizationRequest request, Principal principal) {
        this.request = gson.toJson(request);
        this.parameters = gson.toJson(request.getOptimizationParameters());
        this.modifiedAt = new Date();
        this.user = principal.getName();
        this.isContinuous = request.getOptimizationParameters().isContinuous();
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public OptimizationRequest getObject() {
        return gson.fromJson(this.request, OptimizationRequest.class);
    }

    public String getId() {
        return id;
    }

    public String getRequest() {
        return request;
    }

    public String getUser() {
        return user;
    }

    public boolean isContinuous() {
        return isContinuous;
    }
}
