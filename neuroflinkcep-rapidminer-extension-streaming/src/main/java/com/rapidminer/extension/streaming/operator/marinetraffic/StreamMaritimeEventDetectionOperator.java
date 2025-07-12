/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator.marinetraffic;

import static com.rapidminer.extension.streaming.connection.StreamingConnectionHelper.createKafkaSelector;
import static com.rapidminer.extension.streaming.connection.StreamingConnectionHelper.createSelector;
import static com.rapidminer.extension.streaming.connection.infore.MaritimeConnectionHandler.PARAMETER_ALLOW_SELF_SIGNED_CERT;
import static com.rapidminer.extension.streaming.connection.infore.MaritimeConnectionHandler.PARAMETER_HOST;
import static com.rapidminer.extension.streaming.connection.infore.MaritimeConnectionHandler.PARAMETER_PASSWORD;
import static com.rapidminer.extension.streaming.connection.infore.MaritimeConnectionHandler.PARAMETER_PORT;
import static com.rapidminer.extension.streaming.connection.infore.MaritimeConnectionHandler.PARAMETER_USERNAME;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.util.ConnectionInformationSelector;
import com.rapidminer.extension.kafka_connector.connections.KafkaConnectionHandler;
import com.rapidminer.extension.streaming.connection.infore.MaritimeConnectionHandler;
import com.rapidminer.extension.streaming.deploy.infore.MaritimeRestClient;
import com.rapidminer.extension.streaming.deploy.management.api.Status;
import com.rapidminer.extension.streaming.deploy.management.api.Type;
import com.rapidminer.extension.streaming.deploy.management.db.Job;
import com.rapidminer.extension.streaming.deploy.management.db.ManagementDAO;
import com.rapidminer.extension.streaming.deploy.management.db.StreamingEndpoint;
import com.rapidminer.extension.streaming.deploy.management.db.Workflow;
import com.rapidminer.extension.streaming.deploy.management.db.infore.MaritimeStreamingEndpoint;
import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.operator.AbstractStreamTransformOperator;
import com.rapidminer.extension.streaming.operator.StreamingNest;
import com.rapidminer.extension.streaming.utility.api.infore.maritime.EventDetectionKafkaConfiguration;
import com.rapidminer.extension.streaming.utility.api.infore.maritime.JobRequest;
import com.rapidminer.extension.streaming.utility.api.infore.maritime.JobType;
import com.rapidminer.extension.streaming.utility.api.infore.maritime.KafkaConfiguration;
import com.rapidminer.extension.streaming.utility.api.infore.maritime.TopicConfiguration;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;
import com.rapidminer.extension.streaming.utility.graph.sink.KafkaSink;
import com.rapidminer.extension.streaming.utility.graph.source.KafkaSource;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.container.Pair;


/**
 * Operator for interfacing with Marine Traffic's Akka cluster
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamMaritimeEventDetectionOperator extends AbstractMaritimeOperator {

	protected final InputPort input = getInputPorts().createPort("input stream", StreamDataContainer.class);

	public static final String PARAMETER_INPUT_TOPIC = "input_topic";

	public StreamMaritimeEventDetectionOperator(OperatorDescription description) {
		super(description);
		getTransformer().addPassThroughRule(input, output);
	}

	@Override
	public Pair<StreamGraph, List<StreamDataContainer>> getStreamDataInputs() throws UserError {
		StreamDataContainer inData = input.getData(StreamDataContainer.class);
		return new Pair<>(inData.getStreamGraph(), Collections.singletonList(inData));
	}

	@Override
	protected List<ParameterType> createInputTopicParameterTypes() {
		return Collections.singletonList(new ParameterTypeString(PARAMETER_INPUT_TOPIC, "Input topic", false));
	}

	@Override
	protected List<String> getTopicNames(int desiredSize) throws UndefinedParameterError {
		return Collections.singletonList(getParameterAsString(PARAMETER_INPUT_TOPIC));
	}

	@Override
	protected JobType getJobType() {
		return JobType.EVENTS;
	}

	@Override
	protected KafkaConfiguration getKafkaConfiguration(String brokers, String outTopic, int numberOfInputs) throws UndefinedParameterError {
		return new EventDetectionKafkaConfiguration(new TopicConfiguration(brokers,getParameterAsString(PARAMETER_INPUT_TOPIC)),
			new TopicConfiguration(brokers,outTopic));
	}


}