/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.flink.translate;

import static com.rapidminer.extension.streaming.utility.JsonUtil.toJson;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.annotation.Nullable;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONObject;

import com.rapidminer.extension.streaming.utility.api.infore.synopsis.DataPoint;
import com.rapidminer.extension.streaming.utility.graph.infore.synopsis.SynopsisDataProducer;


/**
 * Flink specific translator for synopsis data producer
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class FlinkSynopsisDataProducerTranslator {

	private final DataStream<JSONObject> parentStream;

	public FlinkSynopsisDataProducerTranslator(DataStream<JSONObject> parentStream) {
		this.parentStream = parentStream;
	}

	/**
	 * Transforms the events into synopsis data points and then sinks them (to Kafka)
	 *
	 * @param producer
	 */
	public void translate(SynopsisDataProducer producer) {
		String topic = producer.getTopic();
		String dataSetKey = producer.getDataSetKey();
		String streamId = producer.getStreamIdKey();
		Properties config = producer.getConfig();
		FlinkKafkaProducer.Semantic producerSemantic = FlinkKafkaProducer.Semantic.EXACTLY_ONCE;

		SinkFunction<DataPoint> sdeSinker = new FlinkKafkaProducer<>(
			topic,
			new SynopsisDataSerializer(topic),
			config,
			producerSemantic);

		parentStream
			.map(event -> new DataPoint(dataSetKey, event.getString(streamId), event.toMap()))
			.addSink(sdeSinker);
	}

	/**
	 * Logic for synopsis data serialization:
	 * <ul>
	 *     <li>topic</li>
	 *     <li>key: SDEDataPoint::getRecordKey</li>
	 *     <li>value: SDEDataPoint::getRecordValue</li>
	 * </ul>
	 */
	private static class SynopsisDataSerializer implements KafkaSerializationSchema<DataPoint> {

		private final String topic;

		public SynopsisDataSerializer(String topic) {
			this.topic = topic;
		}

		@Override
		public ProducerRecord<byte[], byte[]> serialize(DataPoint element, @Nullable Long timestamp) {
			return new ProducerRecord<>(
				topic,
				element.getDataSetKey().getBytes(StandardCharsets.UTF_8),
				toJson(element).getBytes(StandardCharsets.UTF_8));
		}

	}

}