/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.flink.translate;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.json.JSONObject;

import com.rapidminer.extension.streaming.utility.graph.transform.FilterTransformer;


/**
 * Flink specific translator for Filter
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class FlinkFilterTransformerTranslator {

	private final DataStream<JSONObject> stream;

	public FlinkFilterTransformerTranslator(DataStream<JSONObject> stream) {
		this.stream = stream;
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @param filterTransformer configurations for the transformation
	 * @return transformed stream
	 */
	public DataStream<JSONObject> translate(FilterTransformer filterTransformer) {
		FilterFunction<JSONObject> function = null;

		String key = filterTransformer.getKey();
		String value = filterTransformer.getValue();

		switch (filterTransformer.getOperator()) {
			case STARTS_WITH:
				function = obj -> StringUtils.startsWith(obj.get(key).toString(), value);
				break;
			case EQUAL:
				function = obj -> StringUtils.equals(obj.get(key).toString(), value);
				break;
			case NOT_EQUAL:
				function = obj -> !StringUtils.equals(obj.get(key).toString(), value);
				break;
			case GREATER_THAN:
				function = obj -> Double.valueOf(value).compareTo(obj.getDouble(key)) < 0;
				break;
			case GREATER_THAN_OR_EQUAL:
				function = obj -> Double.valueOf(value).compareTo(obj.getDouble(key)) <= 0;
				break;
			case LESS_THAN:
				function = obj -> Double.valueOf(value).compareTo(obj.getDouble(key)) > 0;
				break;
			case LESS_THAN_OR_EQUAL:
				function = obj -> Double.valueOf(value).compareTo(obj.getDouble(key)) >= 0;
				break;
		}

		return stream.filter(function);
	}

}