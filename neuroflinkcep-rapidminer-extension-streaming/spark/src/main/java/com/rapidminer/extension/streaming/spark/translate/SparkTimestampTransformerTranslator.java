/**
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.spark.translate;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.json.JSONObject;

import com.rapidminer.extension.streaming.utility.graph.transform.TimestampTransformer;


/**
 * Spark specific translator for Timestamp
 *
 * @author Mate Torok
 * @since 0.5.0
 */
public class SparkTimestampTransformerTranslator {

	private final Dataset<JSONObject> stream;

	public SparkTimestampTransformerTranslator(Dataset<JSONObject> stream) {
		this.stream = stream;
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @param timestampTransformer configurations for the transformation
	 * @return transformed stream
	 */
	public Dataset<JSONObject> translate(TimestampTransformer timestampTransformer) {
		String tsKey = timestampTransformer.getKey();
		String tsFormat = timestampTransformer.getFormat();
		boolean useUnixTime = timestampTransformer.isUseUnixTime();

		if (useUnixTime) {
			return stream.map(
				(MapFunction<JSONObject, JSONObject>) obj -> obj.put(tsKey, Instant.now().getEpochSecond()),
				Encoders.kryo(JSONObject.class));
		} else {
			return stream.map(
				(MapFunction<JSONObject, JSONObject>) obj -> obj.put(tsKey, new SimpleDateFormat(tsFormat).format(new Date())),
				Encoders.kryo(JSONObject.class));
		}
	}

}