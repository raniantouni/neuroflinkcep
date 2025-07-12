/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.spark.translate;

import static com.rapidminer.extension.streaming.utility.JsonUtil.toJson;

import java.util.Properties;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.streaming.DataStreamWriter;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.json.JSONObject;

import com.rapidminer.extension.streaming.utility.api.infore.synopsis.DataPoint;
import com.rapidminer.extension.streaming.utility.graph.infore.synopsis.SynopsisDataProducer;

import scala.Tuple2;


/**
 * Spark specific translator for synopsis data producer
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class SparkSynopsisDataProducerTranslator {

	private final Dataset<JSONObject> parentStream;

	public SparkSynopsisDataProducerTranslator(Dataset<JSONObject> parentStream) {
		this.parentStream = parentStream;
	}

	/**
	 * Transforms the events into synopsis data points and then sinks them (to Kafka)
	 *
	 * @param producer
	 * @return
	 */
	public StreamingQuery translate(SynopsisDataProducer producer) {
		Properties config = producer.getConfig();
		String topic = producer.getTopic();
		String dataSetKey = producer.getDataSetKey();
		String streamIdKey = producer.getStreamIdKey();

		DataStreamWriter<Row> writer = parentStream
			.map(getDataPointMapper(dataSetKey, streamIdKey), Encoders.tuple(Encoders.STRING(), Encoders.STRING()))
			.toDF("key", "value")
			.writeStream()
			.format("kafka")
			.option("topic", topic);

		// Add configurations prefixed with "kafka." (Spark requires it so)
		config.forEach((key, value) -> writer.option("kafka." + key, (String) value));

		return writer.start();
	}

	/**
	 * Object goal: SDEDataPoint
	 *
	 * @param setKey
	 * @param streamIdKey
	 * @return the mapper that transforms an object into the 'key' and 'value'
	 */
	private MapFunction<JSONObject, Tuple2<String, String>> getDataPointMapper(String setKey, String streamIdKey) {
		return (MapFunction<JSONObject, Tuple2<String, String>>) obj -> {
			DataPoint dataPoint = new DataPoint(setKey, obj.getString(streamIdKey), obj.toMap());
			return new Tuple2<>(dataPoint.getDataSetKey(), toJson(dataPoint));
		};
	}

}