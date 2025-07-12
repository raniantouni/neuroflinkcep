/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator.marinetraffic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.utility.api.infore.maritime.FusionKafkaConfiguration;
import com.rapidminer.extension.streaming.utility.api.infore.maritime.JobType;
import com.rapidminer.extension.streaming.utility.api.infore.maritime.KafkaConfiguration;
import com.rapidminer.extension.streaming.utility.api.infore.maritime.TopicConfiguration;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPortExtender;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.container.Pair;


/**
 * Operator for interfacing with Marine Traffic's Akka cluster
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamMaritimeTrackFusionOperator extends AbstractMaritimeOperator {

	private final InputPortExtender inputExtender = new InputPortExtender("input stream", getInputPorts()) {

		@Override
		protected Precondition makePrecondition(InputPort port) {
			return new SimplePrecondition(port, new MetaData(StreamDataContainer.class),false);
		}
	};

	public static final String PARAMETER_INPUT_TOPICS = "input_topics";


	public StreamMaritimeTrackFusionOperator(OperatorDescription description) {
		super(description);
		inputExtender.start();
		getTransformer().addPassThroughRule(inputExtender.getManagedPorts().get(0), output);
	}

	@Override
	protected List<ParameterType> createInputTopicParameterTypes() {
		return Collections.singletonList(new ParameterTypeEnumeration(PARAMETER_INPUT_TOPICS,
			"Topic names used for the provided input streams.",
			new ParameterTypeString("topic_name",
				"Topic name used for the provided input stream.",
				true),false));
	}


	@Override
	protected List<String> getTopicNames(int desiredSize) throws UndefinedParameterError {
		Set<String> topics = new LinkedHashSet<>(
			ParameterTypeEnumeration.transformString2List(getParameterAsString(PARAMETER_INPUT_TOPICS)));
		for (int i = topics.size(); i < desiredSize; i++) {
			String topicName;
			do {
				topicName = "input topic " + (i + 1);
				i++;
			} while (topics.contains(topicName) || i > 100_000);
			if (i >= 100_000) {
				// throw error
			}
			topics.add(topicName);
		}
		return new ArrayList<>(topics);
	}

	@Override
	protected JobType getJobType() {
		return JobType.FUSION;
	}

	@Override
	protected KafkaConfiguration getKafkaConfiguration(String brokers, String outTopic, int numberOfInputs) throws UndefinedParameterError {
		List<TopicConfiguration> topics = new ArrayList<>();
		for (String topicName : getTopicNames(numberOfInputs)) {
			topics.add(new TopicConfiguration(brokers, topicName));
		}
		return new FusionKafkaConfiguration(topics, new TopicConfiguration(brokers, outTopic));
	}


	@Override
	public Pair<StreamGraph, List<StreamDataContainer>> getStreamDataInputs() throws UserError {
		List<StreamDataContainer> inputStreams = inputExtender.getData(StreamDataContainer.class, true);
		if (inputStreams.isEmpty()) {
			// This should not happen, throw user error
		}
		return new Pair<>(inputStreams.get(0).getStreamGraph(), inputStreams);
	}

}