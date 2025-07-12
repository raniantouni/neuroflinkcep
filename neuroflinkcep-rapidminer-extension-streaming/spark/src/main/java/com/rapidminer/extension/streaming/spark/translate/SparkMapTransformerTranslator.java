/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.spark.translate;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.json.JSONObject;

import com.rapidminer.extension.streaming.utility.graph.transform.MapTransformer;


/**
 * Spark specific translator for Map
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class SparkMapTransformerTranslator {

	private final Dataset<JSONObject> stream;

	public SparkMapTransformerTranslator(Dataset<JSONObject> stream) {
		this.stream = stream;
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @param mapTransformer configurations for the transformation
	 * @return transformed stream
	 */
	public Dataset<JSONObject> translate(MapTransformer mapTransformer) {
		String key = mapTransformer.getKey();
		String newValue = mapTransformer.getNewValue();

		return stream
			.map((MapFunction<JSONObject, JSONObject>) obj -> obj.put(key, newValue), Encoders.kryo(JSONObject.class));
	}

}