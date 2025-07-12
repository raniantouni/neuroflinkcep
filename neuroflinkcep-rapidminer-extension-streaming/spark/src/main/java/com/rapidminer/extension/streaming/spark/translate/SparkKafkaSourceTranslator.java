/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.spark.translate;

import java.util.Properties;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.DataStreamReader;
import org.json.JSONObject;

import com.rapidminer.extension.streaming.utility.graph.source.KafkaSource;


/**
 * Spark specific translator for KafkaSource
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class SparkKafkaSourceTranslator {

	private final SparkSession session;

	public SparkKafkaSourceTranslator(SparkSession session) {
		this.session = session;
	}

	/**
	 * Sources Kafka for a data stream
	 *
	 * @param source configurations for the source
	 * @return new stream
	 */
	public Dataset<JSONObject> translate(KafkaSource source) {
		Properties config = source.getConfig();
		String topic = source.getTopic();

		DataStreamReader reader = session
			.readStream()
			.format("kafka")
			.option("subscribe", topic)
			.option("startingOffsets", source.startFromEarliest() ? "earliest" : "latest");

		// Add configurations prefixed with "kafka." (Spark requires it so)
		config.forEach((key, value) -> reader.option("kafka." + (String) key, (String) value));

		return reader
			.load()
			.select("value")
			.as(Encoders.BINARY())
			.map((MapFunction<byte[], JSONObject>) obj -> new JSONObject(new String(obj)), Encoders.kryo(JSONObject.class));
	}

}