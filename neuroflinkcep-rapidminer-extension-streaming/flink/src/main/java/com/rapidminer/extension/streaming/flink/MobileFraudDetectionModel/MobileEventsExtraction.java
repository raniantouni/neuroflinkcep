package com.rapidminer.extension.streaming.flink.MobileFraudDetectionModel;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MobileEventsExtraction extends RichFlatMapFunction<JSONObject, JSONObject> {
    private transient MobileFraudDetectionModel model;
    private final boolean recognizeAggEvents;
    private final String modelPath;

    public MobileEventsExtraction(String modelPath, boolean recognizeAggEvents) {
        this.modelPath = modelPath;
        this.recognizeAggEvents = recognizeAggEvents;
    }

    @Override
    public void flatMap(JSONObject mobileRecord, Collector<JSONObject> collector) throws Exception {
        Map<String, String> simpleEvents = model.predict(mobileRecord);
        for (Map.Entry<String, String> entry : simpleEvents.entrySet()) {
            JSONObject simpleEvent = new JSONObject(mobileRecord.toString());
            if (entry.getValue().equals("1")) {
                simpleEvent.put("event", entry.getKey());
                collector.collect(simpleEvent);

            }
//            else simpleEvent.put("event", "UNKNOWN");
        }
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        this.model = new MobileFraudDetectionModel(modelPath);
        this.model.loadModel();
    }
}
