/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.spark.translate;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.streaming.DataStreamWriter;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.json.JSONObject;

import com.rapidminer.extension.streaming.utility.graph.sink.KafkaSink;

import scala.Tuple2;


/**
 * Spark specific translator for KafkaSink
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class SparkKafkaSinkTranslator {

	private final Dataset<JSONObject> stream;

	public SparkKafkaSinkTranslator(Dataset<JSONObject> stream) {
		this.stream = stream;
	}

	/**
	 * Sinks the stream
	 *
	 * @param sink configurations for the transformation
	 */
	public StreamingQuery translate(KafkaSink sink) {
		Properties config = sink.getConfig();
		String topic = sink.getTopic();
		String keyField = sink.getKey();

		DataStreamWriter<Row> writer = stream
			.map(mapToRecords(keyField), Encoders.tuple(Encoders.STRING(), Encoders.STRING()))
			.toDF("key", "value")
			.writeStream()
			.format("kafka")
			.option("topic", topic);

		// Add configurations prefixed with "kafka." (Spark requires it so)
		config.forEach((key, value) -> writer.option("kafka." + key, (String) value));

		return writer.start();
	}

	/**
	 * Maps the JSON object to a Kafka "record" using the key optionally (or null by default)
	 *
	 * @param keyField
	 * @return the mapper function
	 */
	private MapFunction<JSONObject, Tuple2<String, String>> mapToRecords(String keyField) {
		String key = StringUtils.trimToNull(keyField);
		if (key == null) {
			return (MapFunction<JSONObject, Tuple2<String, String>>) obj -> new Tuple2<>(null, obj.toString());
		} else {
			return (MapFunction<JSONObject, Tuple2<String, String>>) obj -> new Tuple2<>(obj.getString(key), obj.toString());
		}
	}

}