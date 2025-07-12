package com.rapidminer.extension.streaming.flink.translate;

import com.rapidminer.extension.streaming.utility.graph.crexdata.JsonDataProducer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import org.json.JSONObject;

public class FlinkJsonProducerTranslator {

    private final StreamExecutionEnvironment executionEnv;

    public FlinkJsonProducerTranslator(StreamExecutionEnvironment executionEnv) {
        this.executionEnv = executionEnv;
    }


    public DataStream<JSONObject> translate(JsonDataProducer jsonDataProducer) {
        String jsonData = jsonDataProducer.getJsonData();
        JSONObject jsonObject = new JSONObject(jsonData);

        return executionEnv
                .fromElements(jsonObject);
    }
}
