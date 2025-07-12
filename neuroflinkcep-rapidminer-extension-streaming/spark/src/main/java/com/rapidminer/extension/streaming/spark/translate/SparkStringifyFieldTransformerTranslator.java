/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.spark.translate;

import java.util.Objects;
import java.util.Set;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.json.JSONObject;

import com.rapidminer.extension.streaming.utility.graph.transform.StringifyFieldTransformer;


/**
 * Spark specific translator for StringifyField
 *
 * @author Mate Torok
 * @since
 */
public class SparkStringifyFieldTransformerTranslator {

	private final Dataset<JSONObject> stream;

	public SparkStringifyFieldTransformerTranslator(Dataset<JSONObject> stream) {
		this.stream = stream;
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @param transformer configurations for the transformation
	 * @return transformed stream
	 */
	public Dataset<JSONObject> translate(StringifyFieldTransformer transformer) {
		Set<String> keys = transformer.getKeys();

		return stream
			.map(
				(MapFunction<JSONObject, JSONObject>) obj -> {
					// Iterate through the fields to be stringified
					for (String key : keys) {
						// If the field does actually exist
						if (obj.has(key)) {
							String newValue = Objects.toString(obj.get(key));
							obj.put(key, newValue);
						}
					}
					return obj;
				},
				Encoders.kryo(JSONObject.class));
	}

}