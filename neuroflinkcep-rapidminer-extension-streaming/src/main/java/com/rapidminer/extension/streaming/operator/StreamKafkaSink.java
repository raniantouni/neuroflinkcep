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
import com.rapidminer.extension.streaming.utility.graph.sink.KafkaSink;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeLinkButton;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.container.Pair;


/**
 * Kafka sink operator for stream graphs
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamKafkaSink extends AbstractKafkaOperator implements StreamOperator {

	public static final String PARAMETER_RECORD_KEY = "record_key";

	private final InputPort input = getInputPorts().createPort("input stream", StreamDataContainer.class);

	public StreamKafkaSink(OperatorDescription description) {
		super(description);

		addTopicNameMetaDataCheck();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		types.add(kafkaTopics());

		ParameterType updateLink =
			new ParameterTypeLinkButton(PARAMETER_BUTTON_UPDATE_TOPICS, "update topic list from Kafka server",
				updateAction);
		types.add(updateLink);

		ParameterType key =
			new ParameterTypeString(PARAMETER_RECORD_KEY, "Name of the field to use for record key.", true);
		types.add(key);

		return types;
	}

	@Override
	public Pair<StreamGraph, List<StreamDataContainer>> getStreamDataInputs() throws UserError {
		String kafkaTopic = getParameterAsString(PARAMETER_TOPIC);
		performTopicNameCheck(kafkaTopic);

		StreamDataContainer inData = input.getData(StreamDataContainer.class);
		return new Pair<>(inData.getStreamGraph(), Collections.singletonList(inData));
	}

	@Override
	public List<StreamProducer> addToGraph(StreamGraph graph, List<StreamDataContainer> streamDataInputs) throws UserError {
		ConnectionConfiguration connConfig = connectionSelector.getConnection().getConfiguration();
		Properties clusterConfig = KafkaConnectionHandler.getINSTANCE().buildClusterConfiguration(connConfig);

		StreamDataContainer inData = streamDataInputs.get(0);
		new KafkaSink.Builder(graph)
			.withConfiguration(clusterConfig)
			.withTopic(getParameterAsString(PARAMETER_TOPIC))
			.withKey(getParameterAsString(PARAMETER_RECORD_KEY))
			.withParent(inData.getLastNode())
			.build();
		return new ArrayList<>();
	}

	@Override
	public void deliverStreamDataOutputs(StreamGraph graph, List<StreamProducer> streamProducers) throws UserError {
		// nothing to do for the StreamKafkaSink
	}

	@Override
	public void logProcessing(String graphName) {
		LOGGER.fine("Processing " + getName() + " for: " + graphName);
	}

	@Override
	public void doWork() throws OperatorException {
		doWorkDefault();
	}

}
