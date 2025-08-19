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

import java.util.Map;


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
		Boolean neuroFlinkMode = filterTransformer.getNeuroFlinkMode();
		Map<String, Map<String, String>> parsedPredicates = filterTransformer.getParsedPredicates();

		if (neuroFlinkMode) {
			return stream.filter(buildPredicate(parsedPredicates));
		}

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
	public FilterFunction<JSONObject> buildPredicate(Map<String, Map<String, String>> predicates) {
		return obj -> {
			for (Map.Entry<String, Map<String, String>> attributeEntry : predicates.entrySet()) {
				String key = attributeEntry.getKey();
				Map<String, String> conditions = attributeEntry.getValue();

				for (Map.Entry<String, String> condEntry : conditions.entrySet()) {
					String condition = condEntry.getKey().toLowerCase();  // case-insensitive
					String value = condEntry.getValue();

					if (!evaluateCondition(obj, key, condition, value)) {
						return false; // If any condition fails, reject the object
					}
				}
			}
			return true; // All conditions passed
		};
	}
	private static boolean evaluateCondition(JSONObject obj, String key, String condition, String value) {
		if (!obj.has(key)) return false;

		switch (condition) {
			case "equal":
				return StringUtils.equals(obj.get(key).toString(), value);
			case "not equal":
				return !StringUtils.equals(obj.get(key).toString(), value);
			case "greater than":
				return obj.getDouble(key) > Double.parseDouble(value);
			case "greater than or equal":
				return obj.getDouble(key) >= Double.parseDouble(value);
			case "less than":
				return obj.getDouble(key) < Double.parseDouble(value);
			case "less than or equal":
				return obj.getDouble(key) <= Double.parseDouble(value);
			default:
				throw new IllegalArgumentException("Unsupported condition: " + condition);
		}
	}


}