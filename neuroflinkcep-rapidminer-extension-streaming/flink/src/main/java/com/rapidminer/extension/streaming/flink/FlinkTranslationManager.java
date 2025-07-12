/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.flink;

import static java.util.Objects.isNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.rapidminer.extension.streaming.utility.graph.crexdata.JsonDataProducer;
import com.rapidminer.extension.streaming.utility.graph.transform.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.rapidminer.extension.streaming.flink.translate.*;
import com.rapidminer.extension.streaming.utility.graph.StreamConsumer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraphNode;
import com.rapidminer.extension.streaming.utility.graph.StreamGraphNodeVisitor;
import com.rapidminer.extension.streaming.utility.graph.infore.synopsis.SynopsisDataProducer;
import com.rapidminer.extension.streaming.utility.graph.infore.synopsis.SynopsisEstimateQuery;
import com.rapidminer.extension.streaming.utility.graph.sink.KafkaSink;
import com.rapidminer.extension.streaming.utility.graph.source.KafkaSource;
import com.rapidminer.extension.streaming.utility.graph.source.SpringFinancialSource;


/**
 * Flink specific StreamGraph visitor which basically builds the platform dependent stream(s) for the workflow
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class FlinkTranslationManager implements StreamGraphNodeVisitor {

	private static final Logger LOGGER = LoggerFactory.getLogger(FlinkTranslationManager.class);

	private final StreamExecutionEnvironment executionEnv;

	private final Map<Pair<Long, Long>, DataStream<JSONObject>> parentStreams = Maps.newHashMap();

	public FlinkTranslationManager(StreamExecutionEnvironment executionEnv) {
		this.executionEnv = executionEnv;
	}

	@Override
	public void initialize() {
		// nothing to do
	}

	@Override
	public void visit(KafkaSink sink) {
		long id = sink.getId();
		LOGGER.info("Translating 'KafkaSink' node with id: {}", id);

		DataStream<JSONObject> parentStream = getParentStream(sink.getParent(), sink);
		FlinkKafkaSinkTranslator translator = new FlinkKafkaSinkTranslator(parentStream);
		translator.translate(sink);
	}

	@Override
	public void visit(KafkaSource source) {
		long id = source.getId();
		LOGGER.info("Translating 'KafkaSource' node with id: {}", id);

		FlinkKafkaSourceTranslator translator = new FlinkKafkaSourceTranslator(executionEnv);
		DataStream<JSONObject> stream = translator.translate(source);

		StreamConsumer child = source.getChild();
		putParentStream(source, child, stream);
		child.accept(this);
	}
	@Override
	public void visit(JsonDataProducer jsonDataProducer)
	{
		long id = jsonDataProducer.getId();
		LOGGER.info("Translating 'JsonProducer' node with id: {}", id);
		FlinkJsonProducerTranslator translator = new FlinkJsonProducerTranslator(executionEnv);
		DataStream<JSONObject> stream = translator.translate(jsonDataProducer);

		StreamConsumer child = jsonDataProducer.getChild();
		putParentStream(jsonDataProducer, child, stream);
		child.accept(this);
	}


	@Override
	public void visit(MapTransformer mapTransformer) {
		long id = mapTransformer.getId();
		LOGGER.info("Translating 'MapTransformer' node with id: {}", id);

		DataStream<JSONObject> parentStream = getParentStream(mapTransformer.getParent(), mapTransformer);
		FlinkMapTransformerTranslator translator = new FlinkMapTransformerTranslator(parentStream);
		DataStream<JSONObject> stream = translator.translate(mapTransformer);

		StreamConsumer child = mapTransformer.getChild();
		putParentStream(mapTransformer, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(TimestampTransformer timestampTransformer) {
		long id = timestampTransformer.getId();
		LOGGER.info("Translating 'TimestampTransformer' node with id: {}", id);

		DataStream<JSONObject> parentStream = getParentStream(timestampTransformer.getParent(), timestampTransformer);
		FlinkTimestampTransformerTranslator translator = new FlinkTimestampTransformerTranslator(parentStream);
		DataStream<JSONObject> stream = translator.translate(timestampTransformer);

		StreamConsumer child = timestampTransformer.getChild();
		putParentStream(timestampTransformer, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(SelectTransformer selectTransformer) {
		long id = selectTransformer.getId();
		LOGGER.info("Translating 'SelectTransformer' node with id: {}", id);

		DataStream<JSONObject> parentStream = getParentStream(selectTransformer.getParent(), selectTransformer);
		FlinkSelectTransformerTranslator translator = new FlinkSelectTransformerTranslator(parentStream);
		DataStream<JSONObject> stream = translator.translate(selectTransformer);

		StreamConsumer child = selectTransformer.getChild();
		putParentStream(selectTransformer, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(JoinTransformer joinTransformer) {
		long id = joinTransformer.getId();
		LOGGER.info("Translating 'JoinTransformer' node with id: {}", id);

		DataStream<JSONObject> leftStream = getParentStream(joinTransformer.getLeftParent(), joinTransformer);
		DataStream<JSONObject> rightStream = getParentStream(joinTransformer.getRightParent(), joinTransformer);

		// if any of the input streams is still missing, we won't go any further, next call will take care of this anyway
		if (isNull(leftStream) || isNull(rightStream)) {
			return;
		}

		FlinkJoinTransformerTranslator translator = new FlinkJoinTransformerTranslator(leftStream, rightStream);
		DataStream<JSONObject> stream = translator.translate(joinTransformer);

		StreamConsumer child = joinTransformer.getChild();
		putParentStream(joinTransformer, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(CEP cep) {
		long id = cep.getId();
		LOGGER.info("Translating 'CEPTransformer' node with id: {}", id);
		DataStream<JSONObject> parentStream = getParentStream(cep.getParent(), cep);
		FlinkCEPTranslator translator = new FlinkCEPTranslator(parentStream);
		DataStream<JSONObject> stream = translator.translate(cep);

		StreamConsumer child = cep.getChild();
		putParentStream(cep, child, stream);
		child.accept(this);
	}
	@Override
	public void visit(FilterTransformer filterTransformer) {
		long id = filterTransformer.getId();
		LOGGER.info("Translating 'FilterTransformer' node with id: {}", id);

		DataStream<JSONObject> parentStream = getParentStream(filterTransformer.getParent(), filterTransformer);
		FlinkFilterTransformerTranslator translator = new FlinkFilterTransformerTranslator(parentStream);
		DataStream<JSONObject> stream = translator.translate(filterTransformer);

		StreamConsumer child = filterTransformer.getChild();
		putParentStream(filterTransformer, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(AggregateTransformer aggregateTransformer) {
		long id = aggregateTransformer.getId();
		LOGGER.info("Translating 'AggregateTransformer' node with id: {}", id);

		DataStream<JSONObject> parentStream = getParentStream(aggregateTransformer.getParent(), aggregateTransformer);
		FlinkAggregateTransformerTranslator translator = new FlinkAggregateTransformerTranslator(parentStream);
		DataStream<JSONObject> stream = translator.translate(aggregateTransformer);

		StreamConsumer child = aggregateTransformer.getChild();
		putParentStream(aggregateTransformer, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(DuplicateStreamTransformer duplicate) {
		long id = duplicate.getId();
		LOGGER.info("Translating 'DuplicateStreamTransformer' node with id: {}", id);

		DataStream<JSONObject> parentStream = getParentStream(duplicate.getParent(), duplicate);
		FlinkDuplicateStreamTransformerTranslator translator = new FlinkDuplicateStreamTransformerTranslator(parentStream);
		Tuple2<DataStream<JSONObject>, DataStream<JSONObject>> streams = translator.translate();

		// when duplicating streams, the order does not matter
		StreamConsumer leftChild = duplicate.getLeftChild();
		StreamConsumer rightChild = duplicate.getRightChild();
		putParentStream(duplicate, leftChild, streams.f0);
		putParentStream(duplicate, rightChild, streams.f1);
		leftChild.accept(this);
		rightChild.accept(this);
	}

	@Override
	public void visit(ConnectTransformer connectTransformer) {
		long id = connectTransformer.getId();
		LOGGER.info("Translating 'ConnectTransformer' node with id: {}", id);

		DataStream<JSONObject> controlStream = getParentStream(connectTransformer.getControlParent(), connectTransformer);
		DataStream<JSONObject> dataStream = getParentStream(connectTransformer.getDataParent(), connectTransformer);

		// if any of the input streams is still missing, we won't go any further, next call will take care of this anyway
		if (isNull(controlStream) || isNull(dataStream)) {
			return;
		}

		FlinkConnectTransformerTranslator translator = new FlinkConnectTransformerTranslator(controlStream, dataStream);
		DataStream<JSONObject> stream = translator.translate(connectTransformer);

		StreamConsumer child = connectTransformer.getChild();
		putParentStream(connectTransformer, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(ParseFieldTransformer parseFieldTransformer) {
		long id = parseFieldTransformer.getId();
		LOGGER.info("Translating 'ParseFieldTransformer' node with id: {}", id);

		DataStream<JSONObject> parentStream = getParentStream(parseFieldTransformer.getParent(), parseFieldTransformer);
		FlinkParseFieldTransformerTranslator translator = new FlinkParseFieldTransformerTranslator(parentStream);
		DataStream<JSONObject> stream = translator.translate(parseFieldTransformer);

		StreamConsumer child = parseFieldTransformer.getChild();
		putParentStream(parseFieldTransformer, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(StringifyFieldTransformer stringifyFieldTransformer) {
		long id = stringifyFieldTransformer.getId();
		LOGGER.info("Translating 'StringifyFieldTransformer' node with id: {}", id);

		DataStream<JSONObject> parentStream = getParentStream(stringifyFieldTransformer.getParent(), stringifyFieldTransformer);
		FlinkStringifyFieldTransformerTranslator translator = new FlinkStringifyFieldTransformerTranslator(parentStream);
		DataStream<JSONObject> stream = translator.translate(stringifyFieldTransformer);

		StreamConsumer child = stringifyFieldTransformer.getChild();
		putParentStream(stringifyFieldTransformer, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(SynopsisEstimateQuery synopsisEstimateQuery) {
		long id = synopsisEstimateQuery.getId();
		LOGGER.info("Translating 'SynopsisEstimateQuery' node with id: {}", id);

		FlinkSynopsisEstimateQueryTranslator translator = new FlinkSynopsisEstimateQueryTranslator(executionEnv);
		translator.translate(synopsisEstimateQuery);
	}

	@Override
	public void visit(SynopsisDataProducer synopsisDataProducer) {
		long id = synopsisDataProducer.getId();
		LOGGER.info("Translating 'SynopsisDataProducer' node with id: {}", id);

		DataStream<JSONObject> parentStream = getParentStream(synopsisDataProducer.getParent(), synopsisDataProducer);
		FlinkSynopsisDataProducerTranslator translator = new FlinkSynopsisDataProducerTranslator(parentStream);
		translator.translate(synopsisDataProducer);
	}

	@Override
	public void visit(SpringFinancialSource source) {
		long id = source.getId();
		LOGGER.info("Translating 'SpringFinancialSource' node with id: {}", id);
		FlinkSpringFinancialSourceTranslator translator = new FlinkSpringFinancialSourceTranslator(executionEnv);
		DataStream<JSONObject> stream = translator.translate(source);
		StreamConsumer child = source.getChild();
		putParentStream(source, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(UnionTransformer unionTransformer) {
		long id = unionTransformer.getId();
		LOGGER.info("Translating 'UnionTransformer' node with id: {}", id);

		List<DataStream<JSONObject>> inputStreams = unionTransformer.getParents()
			.stream()
			.map(parent -> getParentStream(parent, unionTransformer))
			.collect(Collectors.toList());

		// if any of the input streams is still missing, we won't go any further, next call will take care of this anyway
		if (inputStreams.stream().anyMatch(Objects::isNull)) {
			return;
		}

		FlinkUnionStreamTransformerTranslator translator = new FlinkUnionStreamTransformerTranslator(inputStreams);
		DataStream<JSONObject> stream = translator.translate();

		StreamConsumer child = unionTransformer.getChild();
		putParentStream(unionTransformer, child, stream);
		child.accept(this);
	}

	@Override
	public void visit(RapidMinerModelApplierTransformer rmModelApplierTransformer) {
		long id = rmModelApplierTransformer.getId();
		LOGGER.info("Translating 'RapidMinerModelApplierTransformer' node with id: {}", id);

		DataStream<JSONObject> parentStream = getParentStream(rmModelApplierTransformer.getParent(), rmModelApplierTransformer);
		FlinkRapidMinerModelApplierTranslator translator = new FlinkRapidMinerModelApplierTranslator(parentStream);
		DataStream<JSONObject> stream = translator.translate(rmModelApplierTransformer);

		StreamConsumer child = rmModelApplierTransformer.getChild();
		putParentStream(rmModelApplierTransformer, child, stream);
		child.accept(this);
	}

	/**
	 * @param parent
	 * @param child
	 * @return parent stream or null if no stream is available for the parameter
	 */
	private DataStream<JSONObject> getParentStream(StreamGraphNode parent, StreamGraphNode child) {
		return parentStreams.get(Pair.of(parent.getId(), child.getId()));
	}

	/**
	 * Places the correct stream for the child into the data structure: parentStreams
	 *
	 * @param parent
	 * @param child
	 * @param stream
	 */
	private void putParentStream(StreamGraphNode parent, StreamGraphNode child, DataStream<JSONObject> stream) {
		parentStreams.put(Pair.of(parent.getId(), child.getId()), stream);
	}

}