/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.spark.translate;

import static com.rapidminer.extension.streaming.spark.translate.SparkSynopsisEstimateQueryTranslator.PeriodicEstimateQuerySource.SCHEMA;
import static com.rapidminer.extension.streaming.utility.JsonUtil.toJson;
import static java.util.Collections.singletonList;
import static org.apache.spark.sql.types.DataTypes.StringType;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.spark.rdd.RDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.catalyst.expressions.GenericInternalRow;
import org.apache.spark.sql.execution.streaming.LongOffset;
import org.apache.spark.sql.execution.streaming.Offset;
import org.apache.spark.sql.sources.DataSourceRegister;
import org.apache.spark.sql.sources.StreamSourceProvider;
import org.apache.spark.sql.streaming.DataStreamWriter;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.Trigger;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.unsafe.types.UTF8String;

import com.rapidminer.extension.streaming.utility.api.infore.synopsis.Request;
import com.rapidminer.extension.streaming.utility.api.infore.synopsis.RequestType;
import com.rapidminer.extension.streaming.utility.graph.infore.synopsis.SynopsisEstimateQuery;
import com.rapidminer.extension.streaming.utility.api.infore.synopsis.SynopsisType;

import scala.Option;
import scala.Tuple2;
import scala.collection.immutable.Map;
import scala.concurrent.duration.Duration;


/**
 * Spark specific implementation for synopsis estimate query emitter translation
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class SparkSynopsisEstimateQueryTranslator {

	private static final String RECORD_KEY = "RECORD_KEY";

	private static final String RECORD_VALUE = "RECORD_VALUE";

	private final SparkSession session;

	public SparkSynopsisEstimateQueryTranslator(SparkSession session) {
		this.session = session;
	}

	/**
	 * Creates a periodic synopsis estimate emitter
	 *
	 * @param query
	 */
	public StreamingQuery translate(SynopsisEstimateQuery query) {
		Properties config = query.getConfig();
		String[] parameters = query.getParameters();
		String dataSetKey = query.getDataSetKey();
		String streamId = query.getStreamIdKey();
		String topic = query.getTopic();
		String frequency = query.getFrequency() + " seconds";
		long parallelism = query.getParallelism();
		long uId = query.getUId();
		SynopsisType synopsisType = query.getSynopsis();

		// Request to be emitted
		Request request = new Request(
			dataSetKey,
			query.getRequestType(),
			synopsisType,
			uId,
			streamId,
			parameters,
			parallelism);

		String reqJson = toJson(request);

		DataStreamWriter<Row> writer = session
			.readStream()
			.format(PeriodicEstimateQuerySourceProvider.class.getName())
			.option(RECORD_KEY, request.getDataSetKey())
			.option(RECORD_VALUE, reqJson)
			.load()
			.writeStream()
			.format("kafka")
			.option("topic", topic);

		// Add configurations prefixed with "kafka." (Spark requires it so)
		config.forEach((key, value) -> writer.option("kafka." + key, (String) value));

		return writer
			.trigger(Trigger.ProcessingTime(Duration.apply(frequency)))
			.start();
	}

	/**
	 * Custom source to emit specific DataFrame
	 */
	public static class PeriodicEstimateQuerySource implements org.apache.spark.sql.execution.streaming.Source {

		static final StructType SCHEMA = new StructType(
			new StructField[]{
				new StructField("key", StringType, true, Metadata.empty()),
				new StructField("value", StringType, true, Metadata.empty())});

		private final SparkSession session;

		private final RDD<InternalRow> recordRDD;

		private final AtomicLong counter = new AtomicLong(0);

		PeriodicEstimateQuerySource(SparkSession session, String key, String value) {
			this.session = session;

			// Create record (RDD) that will be emitted periodically
			recordRDD = session
				.createDataset(singletonList(new Tuple2<>(key, value)), Encoders.tuple(Encoders.STRING(), Encoders.STRING()))
				.toJavaRDD()
				.map(tuple -> (InternalRow) new GenericInternalRow(
					new Object[]{
						UTF8String.fromString(tuple._1()),
						UTF8String.fromString(tuple._2())}))
				.rdd();
		}

		@Override
		public StructType schema() {
			return SCHEMA;
		}

		@Override
		public Option<Offset> getOffset() {
			return Option.apply(new LongOffset(counter.getAndAdd(1)));
		}

		@Override
		public Dataset<Row> getBatch(Option<Offset> start, Offset end) {
			return session.internalCreateDataFrame(recordRDD, SCHEMA, true);
		}

		@Override
		public void commit(Offset end) {
			// nothing to do
		}

		@Override
		public void stop() {
			// nothing to do
		}

	}

	/**
	 * Class for building the custom source
	 */
	public static class PeriodicEstimateQuerySourceProvider implements StreamSourceProvider, DataSourceRegister {

		@Override
		public String shortName() {
			return "sde_estimate";
		}

		@Override
		public Tuple2<String, StructType> sourceSchema(
			SQLContext sqlContext,
			Option<StructType> schema,
			String providerName,
			Map<String, String> parameters) {
			return new Tuple2<>(shortName(), SCHEMA);
		}

		@Override
		public org.apache.spark.sql.execution.streaming.Source createSource(
			SQLContext sqlContext,
			String metadataPath,
			Option<StructType> schema,
			String providerName,
			Map<String, String> parameters) {
			return new PeriodicEstimateQuerySource(
				sqlContext.sparkSession(),
				parameters.get(RECORD_KEY).get(),
				parameters.get(RECORD_VALUE).get());
		}

	}

}