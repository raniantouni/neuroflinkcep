/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.flink.translate;

import static com.rapidminer.extension.streaming.utility.JsonUtil.toJson;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;
import javax.annotation.Nullable;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.rapidminer.extension.streaming.utility.api.infore.synopsis.Request;
import com.rapidminer.extension.streaming.utility.api.infore.synopsis.RequestType;
import com.rapidminer.extension.streaming.utility.graph.infore.synopsis.SynopsisEstimateQuery;
import com.rapidminer.extension.streaming.utility.api.infore.synopsis.SynopsisType;


/**
 * Flink specific implementation for synopsis estimate query emitter translation
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class FlinkSynopsisEstimateQueryTranslator {

	private static final FlinkKafkaProducer.Semantic PRODUCER_SEMANTIC = FlinkKafkaProducer.Semantic.EXACTLY_ONCE;

	private final StreamExecutionEnvironment executionEnvironment;

	public FlinkSynopsisEstimateQueryTranslator(StreamExecutionEnvironment executionEnv) {
		this.executionEnvironment = executionEnv;
	}

	/**
	 * Creates a periodic synopsis estimate emitter
	 *
	 * @param query
	 */
	public void translate(SynopsisEstimateQuery query) {
		String[] parameters = query.getParameters();
		String dataSetKey = query.getDataSetKey();
		String streamIdKey = query.getStreamIdKey();
		String topic = query.getTopic();
		long frequency = query.getFrequency();
		long parallelism = query.getParallelism();
		long uId = query.getUId();
		SynopsisType synopsisType = query.getSynopsis();
		Properties config = query.getConfig();

		// Request to be emitted
		Request request = new Request(
			dataSetKey,
			query.getRequestType(),
			synopsisType,
			uId,
			streamIdKey,
			parameters,
			parallelism);

		// Request producer source
		SourceFunction<Request> requestSource = new PeriodicEstimateQuerySource(frequency, request);

		// Request sinker
		SinkFunction<Request> requestSinker = new FlinkKafkaProducer<>(
			topic,
			new PeriodicRequestSerializer(topic),
			config,
			PRODUCER_SEMANTIC);

		executionEnvironment
			.addSource(requestSource)
			.addSink(requestSinker);
	}

	/**
	 * Flink specific source implementation for periodic event emission
	 */
	private static class PeriodicEstimateQuerySource implements SourceFunction<Request> {

		private final Request request;

		private final long frequency;

		private volatile boolean isRunning = true;

		public PeriodicEstimateQuerySource(long frequency, Request request) {
			this.frequency = frequency;
			this.request = request;
		}

		@Override
		public void run(SourceContext<Request> ctx) {
			while (isRunning) {
				try {
					Thread.sleep(Duration.ofSeconds(frequency).toMillis());
					ctx.collect(request);
				} catch (InterruptedException ie) {
					// nothing to do here
				}
			}
		}

		@Override
		public void cancel() {
			isRunning = false;
		}

	}

	/**
	 * Logic for request serialization:
	 * <ul>
	 *     <li>topic</li>
	 *     <li>key: SDERequest::getRecordKey</li>
	 *     <li>value: SDERequest::getRecordValue</li>
	 * </ul>
	 */
	private static class PeriodicRequestSerializer implements KafkaSerializationSchema<Request> {

		private final String topic;

		public PeriodicRequestSerializer(String topic) {
			this.topic = topic;
		}

		@Override
		public ProducerRecord<byte[], byte[]> serialize(Request element, @Nullable Long timestamp) {
			return new ProducerRecord<>(
				topic,
				element.getDataSetKey().getBytes(StandardCharsets.UTF_8),
				toJson(element).getBytes(StandardCharsets.UTF_8));
		}

	}

}