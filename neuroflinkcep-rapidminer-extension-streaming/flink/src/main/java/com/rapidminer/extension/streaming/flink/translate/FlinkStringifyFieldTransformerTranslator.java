/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.flink.translate;

import java.util.Objects;
import java.util.Set;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.json.JSONObject;

import com.rapidminer.extension.streaming.utility.graph.transform.StringifyFieldTransformer;


/**
 * Flink specific translator for StringifyField
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class FlinkStringifyFieldTransformerTranslator {

	private final DataStream<JSONObject> stream;

	public FlinkStringifyFieldTransformerTranslator(DataStream<JSONObject> stream) {
		this.stream = stream;
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @param transformer configurations for the transformation
	 * @return transformed stream
	 */
	public DataStream<JSONObject> translate(StringifyFieldTransformer transformer) {
		Set<String> keys = transformer.getKeys();

		return stream.map(obj -> {
			// Iterate through the fields to be stringified
			for (String key : keys) {
				// If the field does actually exist
				if (obj.has(key)) {
					String newValue = Objects.toString(obj.get(key));
					obj.put(key, newValue);
				}
			}
			return obj;
		});
	}

}