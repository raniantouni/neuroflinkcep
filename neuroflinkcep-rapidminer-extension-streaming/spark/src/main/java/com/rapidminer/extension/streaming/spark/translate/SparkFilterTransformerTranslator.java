/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.spark.translate;

import org.apache.commons.lang3.StringUtils;
import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.sql.Dataset;
import org.json.JSONObject;

import com.rapidminer.extension.streaming.utility.graph.transform.FilterTransformer;


/**
 * Spark specific translator for Filter
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class SparkFilterTransformerTranslator {

	private final Dataset<JSONObject> stream;

	public SparkFilterTransformerTranslator(Dataset<JSONObject> stream) {
		this.stream = stream;
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @param filterTransformer configurations for the transformation
	 * @return transformed stream
	 */
	public Dataset<JSONObject> translate(FilterTransformer filterTransformer) {
		FilterFunction<JSONObject> function = null;

		String key = filterTransformer.getKey();
		String value = filterTransformer.getValue();

		switch (filterTransformer.getOperator()) {
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