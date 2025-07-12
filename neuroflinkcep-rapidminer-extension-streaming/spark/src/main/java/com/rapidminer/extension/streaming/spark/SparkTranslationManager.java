/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.spark;

import static java.util.Objects.isNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.rapidminer.extension.streaming.utility.graph.crexdata.JsonDataProducer;
import com.rapidminer.extension.streaming.utility.graph.transform.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.rapidminer.extension.streaming.spark.translate.*;
import com.rapidminer.extension.streaming.utility.graph.StreamConsumer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraphNode;
import com.rapidminer.extension.streaming.utility.graph.StreamGraphNodeVisitor;
import com.rapidminer.extension.streaming.utility.graph.infore.synopsis.SynopsisDataProducer;
import com.rapidminer.extension.streaming.utility.graph.infore.synopsis.SynopsisEstimateQuery;
import com.rapidminer.extension.streaming.utility.graph.sink.KafkaSink;
import com.rapidminer.extension.streaming.utility.graph.source.KafkaSource;
import com.rapidminer.extension.streaming.utility.graph.source.SpringFinancialSource;


/**
 * Spark specific StreamGraph visitor which basically builds the platform dependent stream(s) for the workflow
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class SparkTranslationManager implements StreamGraphNodeVisitor {

	private static final Logger LOGGER = LoggerFactory.getLogger(SparkTranslationManager.class);

	private final SparkSession session;

	private final Map<Pair<Long, Long>, Dataset<JSONObject>> parentStreams = Maps.newHashMap();

	private final List<StreamingQuery> streamingQueries = Lists.newArrayList();

	public SparkTranslationManager(SparkSession sparkSession) {
		session = sparkSession;
	}

	@Override
	public void initialize() {
		// nothing to do
	}

	@Override
	public void visit(KafkaSink sink) {
		LOGGER.info("Visiting KafkaSink with id: {}", sink.getId());

		Dataset<JSONObject> parentStream = getParentStream(sink.getParent(), sink);
		SparkKafkaSinkTranslator translator = new SparkKafkaSinkTranslator(parentStream);
		StreamingQuery streamingQuery = translator.translate(sink);

		streamingQueries.add(streamingQuery);
	}
	@Override
	public void visit(JsonDataProducer jsonDataProducer)
	{
		long id = jsonDataProducer.getId();
		LOGGER.info("Translating 'JsonProducer' node with id: {}", id);

	}
	@Override
	public void visit(KafkaSource source) {
		long id = source.getId();
		LOGGER.info("Visiting KafkaSource with id: {}", id);

		SparkKafkaSourceTranslator translator = new SparkKafkaSourceTranslator(session);
		Dataset<JSONObject> stream = translator.translate(source);

		StreamConsumer child = source.getChild();
		putParentStream(source, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(MapTransformer mapTransformer) {
		long id = mapTransformer.getId();
		LOGGER.info("Visiting MapTransformer with id: {}", id);

		Dataset<JSONObject> parentStream = getParentStream(mapTransformer.getParent(), mapTransformer);
		SparkMapTransformerTranslator translator = new SparkMapTransformerTranslator(parentStream);
		Dataset<JSONObject> stream = translator.translate(mapTransformer);

		StreamConsumer child = mapTransformer.getChild();
		putParentStream(mapTransformer, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(TimestampTransformer timestampTransformer) {
		long id = timestampTransformer.getId();
		LOGGER.info("Visiting TimestampTransformer with id: {}", id);

		Dataset<JSONObject> parentStream = getParentStream(timestampTransformer.getParent(), timestampTransformer);
		SparkTimestampTransformerTranslator translator = new SparkTimestampTransformerTranslator(parentStream);
		Dataset<JSONObject> stream = translator.translate(timestampTransformer);

		StreamConsumer child = timestampTransformer.getChild();
		putParentStream(timestampTransformer, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(SelectTransformer selectTransformer) {
		long id = selectTransformer.getId();
		LOGGER.info("Visiting SelectTransformer with id: {}", id);

		Dataset<JSONObject> parentStream = getParentStream(selectTransformer.getParent(), selectTransformer);
		SparkSelectTransformerTranslator translator = new SparkSelectTransformerTranslator(parentStream);
		Dataset<JSONObject> stream = translator.translate(selectTransformer);

		StreamConsumer child = selectTransformer.getChild();
		putParentStream(selectTransformer, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(JoinTransformer joinTransformer) {
		long id = joinTransformer.getId();
		LOGGER.info("Visiting JoinTransformer with id: {}", id);

		Dataset<JSONObject> leftStream = getParentStream(joinTransformer.getLeftParent(), joinTransformer);
		Dataset<JSONObject> rightStream = getParentStream(joinTransformer.getRightParent(), joinTransformer);

		// if any of the input streams is still missing, we won't go any further, next call will take care of this anyway
		if (isNull(leftStream) || isNull(rightStream)) {
			return;
		}
		SparkJoinTransformerTranslator translator = new SparkJoinTransformerTranslator(leftStream, rightStream);
		Dataset<JSONObject> stream = translator.translate(joinTransformer);

		StreamConsumer child = joinTransformer.getChild();
		putParentStream(joinTransformer, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(CEP cep) {

	}

	@Override
	public void visit(FilterTransformer filterTransformer) {
		long id = filterTransformer.getId();
		LOGGER.info("Visiting FilterTransformer with id: {}", id);

		Dataset<JSONObject> parentStream = getParentStream(filterTransformer.getParent(), filterTransformer);
		SparkFilterTransformerTranslator translator = new SparkFilterTransformerTranslator(parentStream);
		Dataset<JSONObject> stream = translator.translate(filterTransformer);

		StreamConsumer child = filterTransformer.getChild();
		putParentStream(filterTransformer, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(AggregateTransformer aggregateTransformer) {
		long id = aggregateTransformer.getId();
		LOGGER.info("Visiting AggregateTransformer with id: {}", id);

		Dataset<JSONObject> parentStream = getParentStream(aggregateTransformer.getParent(), aggregateTransformer);
		SparkAggregateTransformerTranslator translator = new SparkAggregateTransformerTranslator(parentStream);
		Dataset<JSONObject> stream = translator.translate(aggregateTransformer);

		StreamConsumer child = aggregateTransformer.getChild();
		putParentStream(aggregateTransformer, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(DuplicateStreamTransformer duplicate) {
		long id = duplicate.getId();
		LOGGER.info("Visiting DuplicateStreamTransformer with id: {}", id);

		Dataset<JSONObject> parentStream = getParentStream(duplicate.getParent(), duplicate);
		SparkDuplicateStreamTransformerTranslator translator = new SparkDuplicateStreamTransformerTranslator(parentStream);
		Pair<Dataset<JSONObject>, Dataset<JSONObject>> streams = translator.translate();

		// when duplicating streams, the order does not matter
		StreamConsumer leftChild = duplicate.getLeftChild();
		StreamConsumer rightChild = duplicate.getRightChild();
		putParentStream(duplicate, leftChild, streams.getLeft());
		putParentStream(duplicate, rightChild, streams.getRight());
		leftChild.accept(this);
		rightChild.accept(this);
	}

	@Override
	public void visit(ConnectTransformer connectTransformer) {
		long id = connectTransformer.getId();
		LOGGER.info("Visiting ConnectTransformer with id: {}", id);

		Dataset<JSONObject> controlStream = getParentStream(connectTransformer.getControlParent(), connectTransformer);
		Dataset<JSONObject> dataStream = getParentStream(connectTransformer.getDataParent(), connectTransformer);

		// if any of the input streams is still missing, we won't go any further, next call will take care of this anyway
		if (isNull(controlStream) || isNull(dataStream)) {
			return;
		}

		SparkConnectTransformerTranslator translator = new SparkConnectTransformerTranslator(controlStream, dataStream);
		Dataset<JSONObject> stream = translator.translate(connectTransformer);

		StreamConsumer child = connectTransformer.getChild();
		putParentStream(connectTransformer, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(ParseFieldTransformer parseFieldTransformer) {
		long id = parseFieldTransformer.getId();
		LOGGER.info("Visiting ParseFieldTransformer with id: {}", id);

		Dataset<JSONObject> parentStream = getParentStream(parseFieldTransformer.getParent(), parseFieldTransformer);
		SparkParseFieldTransformerTranslator translator = new SparkParseFieldTransformerTranslator(parentStream);
		Dataset<JSONObject> stream = translator.translate(parseFieldTransformer);

		StreamConsumer child = parseFieldTransformer.getChild();
		putParentStream(parseFieldTransformer, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(StringifyFieldTransformer stringifyFieldTransformer) {
		long id = stringifyFieldTransformer.getId();
		LOGGER.info("Visiting StringifyFieldTransformer with id: {}", id);

		Dataset<JSONObject> parentStream = getParentStream(stringifyFieldTransformer.getParent(), stringifyFieldTransformer);
		SparkStringifyFieldTransformerTranslator translator = new SparkStringifyFieldTransformerTranslator(parentStream);
		Dataset<JSONObject> stream = translator.translate(stringifyFieldTransformer);

		StreamConsumer child = stringifyFieldTransformer.getChild();
		putParentStream(stringifyFieldTransformer, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(SynopsisEstimateQuery query) {
		LOGGER.info("Visiting SynopsisEstimateQuery with id: {}", query.getId());

		SparkSynopsisEstimateQueryTranslator translator = new SparkSynopsisEstimateQueryTranslator(session);
		StreamingQuery streamingQuery = translator.translate(query);

		streamingQueries.add(streamingQuery);
	}

	@Override
	public void visit(SynopsisDataProducer producer) {
		LOGGER.info("Visiting SynopsisDataProducer with id: {}", producer.getId());

		Dataset<JSONObject> parentStream = getParentStream(producer.getParent(), producer);
		SparkSynopsisDataProducerTranslator translator = new SparkSynopsisDataProducerTranslator(parentStream);
		StreamingQuery streamingQuery = translator.translate(producer);

		streamingQueries.add(streamingQuery);
	}

	@Override
	public void visit(SpringFinancialSource springFinancialSource) {
		// TODO: under INF-62
	}

	@Override
	public void visit(UnionTransformer unionTransformer) {
		long id = unionTransformer.getId();
		LOGGER.info("Translating 'UnionTransformer' node with id: {}", id);

		List<Dataset<JSONObject>> inputStreams = unionTransformer.getParents()
			.stream()
			.map(parent -> getParentStream(parent, unionTransformer))
			.collect(Collectors.toList());

		// if any of the input streams is still missing, we won't go any further, next call will take care of this anyway
		if (inputStreams.stream().anyMatch(Objects::isNull)) {
			return;
		}

		SparkUnionStreamTransformerTranslator translator = new SparkUnionStreamTransformerTranslator(inputStreams);
		Dataset<JSONObject> stream = translator.translate();

		StreamConsumer child = unionTransformer.getChild();
		putParentStream(unionTransformer, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(RapidMinerModelApplierTransformer rmModelApplierTransformer) {
		long id = rmModelApplierTransformer.getId();
		LOGGER.info("Visiting RapidMinerModelApplierTransformer with id: {}", id);

		Dataset<JSONObject> parentStream = getParentStream(rmModelApplierTransformer.getParent(), rmModelApplierTransformer);
		SparkRapidMinerModelApplierTranslator translator = new SparkRapidMinerModelApplierTranslator(parentStream);
		Dataset<JSONObject> stream = translator.translate(rmModelApplierTransformer);

		StreamConsumer child = rmModelApplierTransformer.getChild();
		putParentStream(rmModelApplierTransformer, child, stream);
		child.accept(this);
	}

	/**
	 * @return collection of streams that were produced as a result for the workflow contained by a StreamGraph
	 */
	public List<StreamingQuery> getStreams() {
		return streamingQueries;
	}

	/**
	 * @param parent
	 * @param child
	 * @return parent stream or null if no stream is available for the parameter
	 */
	private Dataset<JSONObject> getParentStream(StreamGraphNode parent, StreamGraphNode child) {
		return parentStreams.get(Pair.of(parent.getId(), child.getId()));
	}

	/**
	 * Places the correct stream for the child into the data structure: parentStreams
	 *
	 * @param parent
	 * @param child
	 * @param stream
	 */
	private void putParentStream(StreamGraphNode parent, StreamGraphNode child, Dataset<JSONObject> stream) {
		parentStreams.put(Pair.of(parent.getId(), child.getId()), stream);
	}


}