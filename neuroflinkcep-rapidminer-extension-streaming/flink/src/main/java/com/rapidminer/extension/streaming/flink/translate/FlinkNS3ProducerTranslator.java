package com.rapidminer.extension.streaming.flink.translate;

import com.rapidminer.extension.streaming.utility.graph.crexdata.JsonDataProducer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.json.JSONObject;

public class FlinkNS3ProducerTranslator {

    private final StreamExecutionEnvironment executionEnv;

    public FlinkNS3ProducerTranslator(StreamExecutionEnvironment executionEnv) {
        this.executionEnv = executionEnv;
    }


    public DataStream<JSONObject> translate(JsonDataProducer jsonDataProducer) {
        String jsonData = jsonDataProducer.getJsonData();
        JSONObject jsonObject = new JSONObject(jsonData);

        return executionEnv
                .fromElements(jsonObject);
    }
}


