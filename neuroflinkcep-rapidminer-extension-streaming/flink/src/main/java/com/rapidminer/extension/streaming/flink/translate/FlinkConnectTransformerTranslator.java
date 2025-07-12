/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.flink.translate;

import java.util.Map;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.co.RichCoFlatMapFunction;
import org.apache.flink.util.Collector;
import org.json.JSONObject;

import com.google.common.collect.Maps;
import com.rapidminer.extension.streaming.utility.graph.transform.ConnectTransformer;


/**
 * Flink specific translator for Connect
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class FlinkConnectTransformerTranslator {

	private final DataStream<JSONObject> controlStream;

	private final DataStream<JSONObject> dataStream;

	public FlinkConnectTransformerTranslator(DataStream<JSONObject> controlStream, DataStream<JSONObject> dataStream) {
		this.controlStream = controlStream;
		this.dataStream = dataStream;
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @param connectTransformer configurations for the transformation
	 * @return transformed stream
	 */
	public DataStream<JSONObject> translate(ConnectTransformer connectTransformer) {
		String controlKey = connectTransformer.getControlKey();
		String dataKey = connectTransformer.getDataKey();

		DataStream<JSONObject> keyedDataStream = dataStream.keyBy(obj -> obj.getString(dataKey));

		return controlStream
			.keyBy(obj -> obj.getString(controlKey))
			.connect(keyedDataStream)
			.flatMap(new FlinkConnectFlatMapFunction());
	}

	/**
	 * Connect functionality
	 */
	private static class FlinkConnectFlatMapFunction extends RichCoFlatMapFunction<JSONObject, JSONObject, JSONObject> {

		private ValueState<JSONObject> controlState;

		@Override
		public void open(Configuration config) {
			controlState = getRuntimeContext().getState(new ValueStateDescriptor<>("controlState", JSONObject.class));
		}

		@Override
		public void flatMap1(JSONObject controlValue, Collector<JSONObject> out) throws Exception {
			controlState.update(controlValue);
		}

		@Override
		public void flatMap2(JSONObject dataValue, Collector<JSONObject> out) throws Exception {
			JSONObject newObj = new JSONObject(dataValue.toMap());
			JSONObject controlObj = controlState.value();
			Map<String, Object> controlMap = controlObj == null ? Maps.newHashMap() : controlObj.toMap();

			// Only put entries into newObj that are new keys
			for (Map.Entry<String, Object> entry : controlMap.entrySet()) {
				String key = entry.getKey();
				if (!newObj.has(key)) {
					newObj.put(key, entry.getValue());
				}
			}

			out.collect(newObj);
		}

	}

}