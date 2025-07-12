/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.flink.translate;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.json.JSONObject;

import com.rapidminer.extension.streaming.utility.graph.transform.AggregateTransformer;


/**
 * Flink specific translator for Aggregate
 */
public class FlinkAggregateTransformerTranslator {

	private final DataStream<JSONObject> stream;

	public FlinkAggregateTransformerTranslator(DataStream<JSONObject> stream) {
		this.stream = stream;
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @param aggregateTransformer configurations for the transformation
	 * @return transformed stream
	 */
	public DataStream<JSONObject> translate(AggregateTransformer aggregateTransformer) {
		String key = aggregateTransformer.getKey();
		String valueKey = aggregateTransformer.getValueKey();
		long window = aggregateTransformer.getWindowLength();
		AggregateTransformer.Function function = aggregateTransformer.getFunction();

		return stream
			.keyBy(obj -> obj.getString(key))
			.window(TumblingProcessingTimeWindows.of(Time.seconds(window)))
			.process(new FlinkAggregateProcessFunction(key, valueKey, function));
	}

	/**
	 * Aggregation functionality
	 */
	private class FlinkAggregateProcessFunction extends ProcessWindowFunction<JSONObject, JSONObject, String, TimeWindow> {

		private static final long serialVersionUID = 1L;

		private final String partitionKey;

		private final String valueKey;

		private final AggregateTransformer.Function function;

		public FlinkAggregateProcessFunction(String partitionKey, String valueKey, AggregateTransformer.Function function) {
			this.partitionKey = partitionKey;
			this.valueKey = valueKey;
			this.function = function;
		}

		@Override
		public void process(String key, Context context, Iterable<JSONObject> elements, Collector<JSONObject> out) {
			String resultKey = String.format("%s-%s", function.toString(), valueKey);
			double min = Double.MAX_VALUE;
			double max = Double.MIN_VALUE;
			double count = 0.0;
			double sum = 0.0;
			double avg = Double.NaN;
			double result = Double.NaN;

			for (JSONObject element : elements) {
				if (!element.has(valueKey)) {
					continue;
				}

				double value = Double.valueOf(element.getString(valueKey));
				sum += value;
				count++;
				min = min > value ? value : min;
				max = max < value ? value : max;
			}
			avg = count > 0.0 ? sum / count : avg;

			switch (function) {
				case AVG:
					result = avg;
					break;
				case MAX:
					result = max;
					break;
				case MIN:
					result = min;
					break;
				case SUM:
					result = sum;
					break;
				case COUNT:
					result = count;
					break;
			}

			out.collect(new JSONObject().put(partitionKey, key).put(resultKey, result));
		}

	}

}