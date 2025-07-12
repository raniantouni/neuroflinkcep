/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.flink.translate;

import java.util.Map;

import org.apache.flink.api.common.functions.JoinFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.json.JSONObject;

import com.rapidminer.extension.streaming.utility.graph.transform.JoinTransformer;


/**
 * Flink specific translator for Join
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class FlinkJoinTransformerTranslator {

	private final DataStream<JSONObject> leftStream;

	private final DataStream<JSONObject> rightStream;

	public FlinkJoinTransformerTranslator(DataStream<JSONObject> leftStream, DataStream<JSONObject> rightStream) {
		this.leftStream = leftStream;
		this.rightStream = rightStream;
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @param joinTransformer configurations for the transformation
	 * @return transformed stream
	 */
	public DataStream<JSONObject> translate(JoinTransformer joinTransformer) {
		String leftKey = joinTransformer.getLeftKey();
		String rightKey = joinTransformer.getRightKey();
		long windowLength = joinTransformer.getWindowLength();

		return leftStream
			.join(rightStream)
			.where(obj -> obj.get(leftKey))
			.equalTo(obj -> obj.get(rightKey))
			.window(TumblingProcessingTimeWindows.of(Time.seconds(windowLength)))
			.apply(new FlinkJoinTransformerFunction());
	}

	/**
	 * Join functionality
	 */
	private static class FlinkJoinTransformerFunction implements JoinFunction<JSONObject, JSONObject, JSONObject> {

		private static final long serialVersionUID = 1L;

		@Override
		public JSONObject join(JSONObject left, JSONObject right) throws Exception {
			JSONObject newObj = new JSONObject(left.toMap());
			Map<String, Object> rightData = right.toMap();

			for (Map.Entry<String, Object> entry : rightData.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();

				// if key exists --> "joined_KEY"
				key = newObj.has(key) ? "joined_" + key : key;

				newObj.put(key, value);
			}

			return newObj;
		}

	}

}