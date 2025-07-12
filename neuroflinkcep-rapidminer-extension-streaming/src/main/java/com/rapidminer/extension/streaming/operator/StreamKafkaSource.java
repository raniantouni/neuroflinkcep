/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.extension.kafka_connector.connections.KafkaConnectionHandler;
import com.rapidminer.extension.kafka_connector.operator.AbstractKafkaOperator;
import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;
import com.rapidminer.extension.streaming.utility.graph.source.KafkaSource;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeLinkButton;
import com.rapidminer.tools.container.Pair;


/**
 * Kafka source for stream graphs
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamKafkaSource extends AbstractKafkaOperator implements StreamOperator {

	public static final String PARAMETER_START_FROM_EARLIEST = "start_from_earliest";

	private final OutputPort output = getOutputPorts().createPort("output stream");

	public StreamKafkaSource(OperatorDescription description) {
		super(description);
		addTopicNameMetaDataCheck();

		getTransformer().addGenerationRule(output, StreamDataContainer.class);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType updateLink = new ParameterTypeLinkButton(PARAMETER_BUTTON_UPDATE_TOPICS, "update topic list from Kafka server", updateAction);
		types.add(updateLink);

		ParameterType earliest = new ParameterTypeBoolean(PARAMETER_START_FROM_EARLIEST, "Start from earliest event", false);
		types.add(earliest);

		return types;
	}

	@Override
	public Pair<StreamGraph, List<StreamDataContainer>> getStreamDataInputs() throws UserError {
		String kafkaTopic = getParameterAsString(PARAMETER_TOPIC);
		performTopicNameCheck(kafkaTopic);
		StreamGraph graph = ((StreamingNest) getExecutionUnit().getEnclosingOperator()).getGraph();
		return new Pair<>(graph,new ArrayList<>());
	}

	@Override
	public List<StreamProducer> addToGraph(StreamGraph graph, List<StreamDataContainer> streamDataInputs) throws UserError {
		ConnectionConfiguration connConfig = connectionSelector.getConnection().getConfiguration();
		Properties clusterConfig = KafkaConnectionHandler.getINSTANCE().buildClusterConfiguration(connConfig);

		KafkaSource source = new KafkaSource.Builder(graph)
			.withConfiguration(clusterConfig)
			.withTopic(getParameterAsString(PARAMETER_TOPIC))
			.withStartFromEarliest(getParameterAsBoolean(PARAMETER_START_FROM_EARLIEST))
			.build();

		graph.registerSource(source);
		return Collections.singletonList(source);
	}

	@Override
	public void logProcessing(String graphName) {
		LOGGER.fine("Processing " + getName() + " for: " + graphName);
	}

	@Override
	public void deliverStreamDataOutputs(StreamGraph graph, List<StreamProducer> streamProducers) throws UserError {
		StreamDataContainer outData = new StreamDataContainer(graph, streamProducers.get(0));
		output.deliver(outData);
	}

	@Override
	public void doWork() throws OperatorException {
		doWorkDefault();
	}

}