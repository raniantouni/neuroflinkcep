package com.rapidminer.extension.streaming.utility.graph.crexdata;
import com.rapidminer.extension.streaming.utility.graph.*;
import com.rapidminer.extension.streaming.utility.graph.sink.KafkaSink;
import com.rapidminer.extension.streaming.utility.graph.source.KafkaSource;

import java.util.Properties;


public class JsonDataProducer implements StreamSource, StreamProducer {


    private final String jsonData;
    private StreamConsumer child;
    private KafkaSink sink;
    /**
     * Constructor for JsonDataProducer.
     *
     * @param jsonData the JSON data this producer generates
     */
    public JsonDataProducer(String jsonData) {
        super();
        this.jsonData = jsonData;
    }
    public JsonDataProducer() {
        this.jsonData = ""; // Default value or null
        this.child = null;
        this.sink = null;
    }

    JsonDataProducer(JsonDataProducer.Builder builder) {
        this.jsonData = builder.jsonData;
    }

    public StreamConsumer getChild() {
        return child;
    }


    public String getJsonData() {
        return jsonData;
    }

    @Override
    public void registerChild(StreamConsumer child) {
        this.child = child;
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public void accept(StreamGraphNodeVisitor visitor) {
        visitor.visit(this);
    }

    public static class Builder {

        private final StreamGraph graph;
        private String jsonData;
        private KafkaSink sink;
        private Properties clusterConfig;
        private String topic;
        public Builder(StreamGraph graph) {
            this.graph = graph;
        }

        public JsonDataProducer build() {
//            new KafkaSink.Builder(this.graph)
//                    .withConfiguration(clusterConfig)
//                    .withTopic(topic)
//                    .withKey(getParameterAsString(PARAMETER_RECORD_KEY))
//                    .withParent(inData.getLastNode())
//                    .build();
            return new JsonDataProducer(this);
        }

        public JsonDataProducer.Builder withJsonData(String jsonData) {
            this.jsonData = jsonData;
            return this;
        }


    }

}
