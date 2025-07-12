/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.spark.translate;

import java.util.Set;

import com.rapidminer.extension.streaming.utility.graph.transform.SelectTransformer;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.json.JSONObject;


/**
 * Spark specific translator for Select
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class SparkSelectTransformerTranslator {

	private final Dataset<JSONObject> stream;

	public SparkSelectTransformerTranslator(Dataset<JSONObject> stream) {
		this.stream = stream;
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @param transformer configurations for the transformation
	 * @return transformed stream
	 */
	public Dataset<JSONObject> translate(SelectTransformer transformer) {
		Set<String> keys = transformer.getKeys();
		boolean flatten = transformer.flatten();

		return stream
			.map(
				(MapFunction<JSONObject, JSONObject>) obj -> {
					JSONObject newObj = new JSONObject();
					for(String key : keys) {
						Object field = obj.get(key);

						// If it is a nested JSON object and flattening is turned on
						if (field instanceof JSONObject && flatten) {
							((JSONObject)field).toMap().forEach((nK, nV) -> newObj.put(nK, nV));
						} else {
							newObj.put(key, field);
						}
					}
					return newObj;
				},
				Encoders.kryo(JSONObject.class));
	}

}