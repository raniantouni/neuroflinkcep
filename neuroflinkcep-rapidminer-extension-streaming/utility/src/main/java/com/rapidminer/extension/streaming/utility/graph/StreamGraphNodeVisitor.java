/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.graph;

import com.rapidminer.extension.streaming.utility.graph.crexdata.JsonDataProducer;
import com.rapidminer.extension.streaming.utility.graph.infore.synopsis.SynopsisDataProducer;
import com.rapidminer.extension.streaming.utility.graph.infore.synopsis.SynopsisEstimateQuery;
import com.rapidminer.extension.streaming.utility.graph.sink.KafkaSink;
import com.rapidminer.extension.streaming.utility.graph.source.KafkaSource;
import com.rapidminer.extension.streaming.utility.graph.transform.*;
import com.rapidminer.extension.streaming.utility.graph.source.SpringFinancialSource;


/**
 * Functionality defined for graph visitors (e.g.: translation managers)
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public interface StreamGraphNodeVisitor {

	/**
	 * Initializes visitor
	 */
	void initialize();

	/**
	 * Visit Kafka sink
	 *
	 * @param kafkaSink
	 */
	void visit(KafkaSink kafkaSink);

	/**
	 * Visit Kafka source
	 *
	 * @param kafkaSource
	 */
	void visit(KafkaSource kafkaSource);


	void visit(JsonDataProducer synopsisDataProducer);
	/**
	 * Visit Map transformer
	 *
	 * @param mapTransformer
	 */
	void visit(MapTransformer mapTransformer);

	/**
	 * Visit TIMESTAMP transformer
	 *
	 * @param timestampTransformer
	 */
	void visit(TimestampTransformer timestampTransformer);

	/**
	 * Visit Select transformer
	 *
	 * @param selectTransformer
	 */
	void visit(SelectTransformer selectTransformer);

	/**
	 * Visit Join transformer
	 *
	 * @param joinTransformer
	 */
	void visit(JoinTransformer joinTransformer);

    void visit(CEP cep);

    /**
	 * Visit Filter transformer
	 *
	 * @param filterTransformer
	 */
	void visit(FilterTransformer filterTransformer);

	/**
	 * Visit Aggregate transformer
	 *
	 * @param aggregateTransformer
	 */
	void visit(AggregateTransformer aggregateTransformer);

	/**
	 * Visit Duplicate transformer
	 *
	 * @param duplicateStreamTransformer
	 */
	void visit(DuplicateStreamTransformer duplicateStreamTransformer);

	/**
	 * Visit Connect transformer
	 *
	 * @param connectTransformer
	 */
	void visit(ConnectTransformer connectTransformer);

	/**
	 * Visit ParseField transformer
	 *
	 * @param parseFieldTransformer
	 */
	void visit(ParseFieldTransformer parseFieldTransformer);

	/**
	 * Visit StringifyFieldTransformer transformer
	 *
	 * @param stringifyFieldTransformer
	 */
	void visit(StringifyFieldTransformer stringifyFieldTransformer);

	/**
	 * Visit Synopsis estimate query
	 *
	 * @param synopsisEstimateQuery
	 */
	void visit(SynopsisEstimateQuery synopsisEstimateQuery);

	/**
	 * Visit Synopsis data producer
	 *
	 * @param synopsisDataProducer
	 */
	void visit(SynopsisDataProducer synopsisDataProducer);

	/**
	 * Visit Spring Financial Server source
	 *
	 * @param springFinancialSource
	 */
	void visit(SpringFinancialSource springFinancialSource);

	/**
	 * Visit Union transformer
	 *
	 * @param unionTransformer
	 */
	void visit(UnionTransformer unionTransformer);

	/**
	 * Visit RapidMiner Model Applier transformer
	 *
	 * @param rmModelApplierTransformer
	 */
	void visit(RapidMinerModelApplierTransformer rmModelApplierTransformer);

}