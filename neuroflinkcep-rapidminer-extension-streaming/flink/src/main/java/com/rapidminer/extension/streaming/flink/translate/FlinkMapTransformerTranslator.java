/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.flink.translate;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.json.JSONObject;

import com.rapidminer.extension.streaming.utility.graph.transform.MapTransformer;


/**
 * Flink specific translator for Map
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class FlinkMapTransformerTranslator {

	private final DataStream<JSONObject> stream;

	public FlinkMapTransformerTranslator(DataStream<JSONObject> stream) {
		this.stream = stream;
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @param mapTransformer configurations for the transformation
	 * @return transformed stream
	 */
	public DataStream<JSONObject> translate(MapTransformer mapTransformer) {
		String key = mapTransformer.getKey();
		String newValue = mapTransformer.getNewValue();

		return stream.map(obj -> obj.put(key, newValue));
	}

}