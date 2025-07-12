/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.flink.translate;

import java.util.Properties;

import com.rapidminer.extension.streaming.utility.graph.source.KafkaSource;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.json.JSONObject;


/**
 * Flink specific translator for KafkaSource
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class FlinkKafkaSourceTranslator {

	private final StreamExecutionEnvironment executionEnv;

	public FlinkKafkaSourceTranslator(StreamExecutionEnvironment executionEnv) {
		this.executionEnv = executionEnv;
	}

	/**
	 * Sources Kafka for a data stream
	 *
	 * @param source configurations for the source
	 * @return new stream
	 */
	public DataStream<JSONObject> translate(KafkaSource source) {
		Properties config = source.getConfig();
		String topic = source.getTopic();

		FlinkKafkaConsumer<String> consumer = new FlinkKafkaConsumer<>(topic, new SimpleStringSchema(), config);

		// Start reading earliest messages if set
		if (source.startFromEarliest()) {
			consumer.setStartFromEarliest();
		}

		return executionEnv
			.addSource(consumer)
			.map(JSONObject::new);
	}

}