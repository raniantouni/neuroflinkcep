/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.flink.translate;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.json.JSONObject;

import com.rapidminer.extension.streaming.utility.graph.transform.TimestampTransformer;


/**
 * Flink specific translator for Timestamp
 *
 * @author Mate Torok
 * @since 0.5.0
 */
public class FlinkTimestampTransformerTranslator {

	private final DataStream<JSONObject> stream;

	public FlinkTimestampTransformerTranslator(DataStream<JSONObject> stream) {
		this.stream = stream;
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @param timestampTransformer configurations for the transformation
	 * @return transformed stream
	 */
	public DataStream<JSONObject> translate(TimestampTransformer timestampTransformer) {
		String tsKey = timestampTransformer.getKey();
		String tsFormat = timestampTransformer.getFormat();
		boolean useUnixTime = timestampTransformer.isUseUnixTime();

		if (useUnixTime) {
			return stream.map(obj -> obj.put(tsKey, Instant.now().getEpochSecond()));
		} else {
			return stream.map(obj -> obj.put(tsKey, new SimpleDateFormat(tsFormat).format(new Date())));
		}
	}

}