/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.flink.translate;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.json.JSONObject;


/**
 * Flink specific translator for Duplicate
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class FlinkDuplicateStreamTransformerTranslator {

	private final DataStream<JSONObject> stream;

	public FlinkDuplicateStreamTransformerTranslator(DataStream<JSONObject> stream) {
		this.stream = stream;
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @return duplicated stream(s)
	 */
	public Tuple2<DataStream<JSONObject>, DataStream<JSONObject>> translate() {
		return new Tuple2<>(stream, stream);
	}

}