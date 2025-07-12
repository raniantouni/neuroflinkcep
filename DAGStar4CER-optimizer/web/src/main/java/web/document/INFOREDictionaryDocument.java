package web.document;

import core.parser.dictionary.INFOREDictionary;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import web.repository.RepositoryDocument;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

@Document(indexName = "infore_dictionary", createIndex = false)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
public class INFOREDictionaryDocument implements RepositoryDocument<INFOREDictionary>, Serializable {
    @Id
    @EqualsAndHashCode.Include
    private String id;

    @Field(name = "dictionary", type = FieldType.Text)
    private String dictionary;

    @Field(name = "@timestamp", type = FieldType.Date, format = DateFormat.date_optional_time)
    private Date modifiedAt;

    public INFOREDictionaryDocument() {
    }

    public INFOREDictionaryDocument(INFOREDictionary dictionary) {
        this.id = dictionary.getName();
        this.dictionary = gson.toJson(dictionary);
        this.modifiedAt = new Date();
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public INFOREDictionary getObject() {
        return gson.fromJson(dictionary, INFOREDictionary.class);
    }
}
