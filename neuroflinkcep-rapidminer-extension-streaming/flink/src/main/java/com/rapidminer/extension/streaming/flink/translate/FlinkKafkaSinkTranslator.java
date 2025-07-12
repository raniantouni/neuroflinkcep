/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.flink.translate;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONObject;

import com.rapidminer.extension.streaming.utility.graph.sink.KafkaSink;


/**
 * Flink specific translator for KafkaSink
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class FlinkKafkaSinkTranslator {

	private static final FlinkKafkaProducer.Semantic PRODUCER_SEMANTIC = FlinkKafkaProducer.Semantic.EXACTLY_ONCE;

	private final DataStream<JSONObject> stream;

	public FlinkKafkaSinkTranslator(DataStream<JSONObject> stream) {
		this.stream = stream;
	}

	/**
	 * Sinks the stream
	 *
	 * @param sink configurations for the transformation
	 */
	public void translate(KafkaSink sink) {
		String topic = sink.getTopic();
		String keyField = sink.getKey();
		Properties config = sink.getConfig();

		SinkFunction<JSONObject> sinker = new FlinkKafkaProducer<>(
			topic,
			new KeyValueSerializer(keyField, topic),
			config,
			PRODUCER_SEMANTIC);

		stream
			.addSink(sinker);
	}

	/**
	 * Logic for event serialization:
	 * <ul>
	 *     <li>topic</li>
	 *     <li>key: null</li>
	 *     <li>value: data</li>
	 * </ul>
	 */
	private static class KeyValueSerializer implements KafkaSerializationSchema<JSONObject> {

		private final String topic;

		private final String keyField;

		public KeyValueSerializer(String keyField, String topic) {
			this.topic = topic;
			this.keyField = StringUtils.trimToNull(keyField);
		}

		@Override
		public ProducerRecord<byte[], byte[]> serialize(JSONObject element, @Nullable Long timestamp) {
			byte[] key = keyField == null ? null : element.getString(keyField).getBytes(StandardCharsets.UTF_8);
			byte[] value = element.toString().getBytes(StandardCharsets.UTF_8);

			return new ProducerRecord<>(topic, key, value);
		}

	}

}