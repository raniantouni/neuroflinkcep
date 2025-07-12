package com.rapidminer.extension.streaming.flink.RobotStatusPredictionModel;

import org.apache.flink.api.common.functions.OpenContext;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RobotBatchProcessor  extends KeyedProcessFunction<Integer, JSONObject, JSONObject> {
    RobotGoalPredictionModel goalPredictionModel;
    private transient ListState<JSONObject> robotState;

    public RobotBatchProcessor(RobotGoalPredictionModel goalPredictionModel) {
        this.goalPredictionModel = goalPredictionModel;
    }
    @Override
    public void processElement(JSONObject jsonObject, KeyedProcessFunction<Integer, JSONObject, JSONObject>.Context context, Collector<JSONObject> collector) throws Exception {
        int robotId = jsonObject.getInt("robotID");

        List<JSONObject> batch = new ArrayList<>();
        for (JSONObject obj : robotState.get()) {
            batch.add(obj);
        }
        batch.add(jsonObject);
        robotState.update(batch);
        if (batch.size() == 50) {
            Map<String, String> predictedClass = goalPredictionModel.predict(batch);
            JSONObject result = new JSONObject(jsonObject.toString());
            predictedClass.forEach(result::put);
            result.put("robotID", robotId);
            result.put("timestamp", Long.valueOf(jsonObject.getInt("current_time_step")) * 1000);
            collector.collect(result);
            robotState.clear();
        }
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        ListStateDescriptor<JSONObject> descriptor =
                new ListStateDescriptor<>("robotState", TypeInformation.of(new TypeHint<JSONObject>() {
                }));
        robotState = getRuntimeContext().getListState(descriptor);
        goalPredictionModel.loadModel();
    }

//    @Override
//    public void open() throws Exception {
//        ListStateDescriptor<JSONObject> descriptor =
//                new ListStateDescriptor<>("robotState", TypeInformation.of(new TypeHint<JSONObject>() {
//                }));
//        robotState = getRuntimeContext().getListState(descriptor);
//    }
}
