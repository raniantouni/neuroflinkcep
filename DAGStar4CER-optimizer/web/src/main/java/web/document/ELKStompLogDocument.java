package web.document;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Getter
@Setter
@Document(indexName = "stomp-logs", createIndex = false)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Data
public class ELKStompLogDocument implements Serializable {
    @EqualsAndHashCode.Include
    @org.springframework.data.annotation.Id
    private String id;

    @Field(name = "command", type = FieldType.Keyword)
    private String command;

    @Field(name = "payload", type = FieldType.Text)
    private String payload;

    @Field(name = "topic", type = FieldType.Keyword)
    private String topic;

    @Field(name = "user", type = FieldType.Keyword)
    private String user;

    @Field(name = "simpSessionId", type = FieldType.Keyword)
    private String simpSessionId;

    @Field(name = "receipt", type = FieldType.Keyword)
    private String receipt;

    @Field(name = "source", type = FieldType.Keyword)
    private String source;

    @Field(name = "@timestamp", type = FieldType.Date, format = DateFormat.custom, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Instant timestamp;


    public ELKStompLogDocument(StompHeaderAccessor accessor, Message<?> message, String source) {
        this.command = accessor.getCommand().toString();
        if (accessor.getUser() != null) {
            this.user = accessor.getUser().getName();
        } else {
            this.user = "UNKNOWN";
        }
        this.simpSessionId = accessor.getSessionId();
        this.topic = accessor.getDestination();
        this.receipt = accessor.getReceipt();
        this.source = source;
        this.payload = new String((byte[]) message.getPayload(), StandardCharsets.UTF_8);
        this.timestamp = Instant.now();
        this.id = String.format("%s_%d", this.timestamp, this.payload.hashCode());
    }
}
