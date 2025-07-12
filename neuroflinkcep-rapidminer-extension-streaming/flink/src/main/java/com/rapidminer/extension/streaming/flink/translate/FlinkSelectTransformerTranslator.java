/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.flink.translate;

import java.util.Set;

import com.rapidminer.extension.streaming.utility.graph.transform.SelectTransformer;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.json.JSONObject;


/**
 * Flink specific translator for Select
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class FlinkSelectTransformerTranslator {

	private final DataStream<JSONObject> stream;

	public FlinkSelectTransformerTranslator(DataStream<JSONObject> stream) {
		this.stream = stream;
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @param transformer configurations for the transformation
	 * @return transformed stream
	 */
	public DataStream<JSONObject> translate(SelectTransformer transformer) {
		Set<String> keys = transformer.getKeys();
		boolean flatten = transformer.flatten();

		return stream.map(obj -> {
			JSONObject newObj = new JSONObject();
			for (String key : keys) {
				Object field = obj.get(key);

				// If it is a nested JSON object and flattening is turned on
				if (field instanceof JSONObject && flatten) {
					((JSONObject)field).toMap().forEach((nK, nV) -> newObj.put(nK, nV));
				} else {
					newObj.put(key, field);
				}
			}
			return newObj;
		});
	}

}