/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator.demokritos;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.rapidminer.extension.streaming.connection.StreamingConnectionHelper.createKafkaSelector;
import static com.rapidminer.extension.streaming.operator.demokritos.DemokritosCEFConstants.MLSTM_CONFIGURATION_TOPIC;
import static java.lang.String.format;

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.kafka.clients.admin.NewTopic;
import org.json.JSONObject;

import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.util.ConnectionInformationSelector;
import com.rapidminer.extension.kafka_connector.connections.KafkaConnectionHandler;
import com.rapidminer.extension.streaming.deploy.KafkaClient;
import com.rapidminer.extension.streaming.deploy.flink.FlinkRestClient;
import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.operator.AbstractStreamTransformOperator;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;
import com.rapidminer.extension.streaming.utility.graph.sink.KafkaSink;
import com.rapidminer.extension.streaming.utility.graph.source.KafkaSource;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.container.Pair;


/**
 * Operator to involve a running 'MLSTM' (early time series classification module) in a streaming workflow
 *
 * @author Mate Torok
 * @since 0.6.0
 */
public class StreamMLSTMOperator extends AbstractStreamTransformOperator {

	private final ConnectionInformationSelector kafkaConn = createKafkaSelector(this, "kafka-connection");

	private Properties kafkaConfig;

	private String pipelineName;
	private String mlstmIn;
	private String mlstmOut;


	/**
	 * Constructor
	 *
	 * @param description
	 */
	public StreamMLSTMOperator(OperatorDescription description) {
		super(description);
		getTransformer().addPassThroughRule(input, output);
	}



	/**
	 * {@inheritDoc}
	 * <p>
	 * Overwriting the {@link #doWork()} method from {@link AbstractStreamTransformOperator} to insert
	 * {@link #setupPipeline(Properties, String, String, String)}
	 */
	@Override
	public void doWork() throws OperatorException {
		Pair<StreamGraph, List<StreamDataContainer>> inputs = getStreamDataInputs();
		StreamGraph graph = inputs.getFirst();
		logProcessing(graph.getName());

		List<StreamProducer> streamProducers = addToGraph(graph, inputs.getSecond());

		setupPipeline(kafkaConfig, pipelineName, mlstmIn, mlstmOut);

		deliverStreamDataOutputs(graph, streamProducers);
	}

	@Override
	protected StreamProducer createStreamProducer(StreamGraph graph, StreamDataContainer inData) throws UserError {
		// Setup Kafka connection configuration + client
		ConnectionConfiguration kafkaConnConfig = kafkaConn.getConnection().getConfiguration();
		kafkaConfig = KafkaConnectionHandler.getINSTANCE().buildClusterConfiguration(kafkaConnConfig);

		// Setup topics and extend graph with MLSTM parts
		pipelineName = "MLSTM-" + RandomStringUtils.randomAlphanumeric(10);
		mlstmIn = "MLSTM-IN-" + RandomStringUtils.randomAlphanumeric(10);
		mlstmOut = "MLSTM-OUT-" + RandomStringUtils.randomAlphanumeric(10);
		LOGGER.info(format("MLSTM pipeline: %s, topics (in: %s, out: %s)", pipelineName, mlstmIn, mlstmOut));
		
		return extendGraph(kafkaConfig, mlstmIn, mlstmOut);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		return newArrayList();
	}

	/**
	 * Extends the graph with MLSTM specific logic
	 *
	 * @return node representing the output of MLSTM (~downstream)
	 * @param kafkaConfig
	 * @param mlstmIn
	 * @param mlstmOut
	 */
	private StreamProducer extendGraph(Properties kafkaConfig, String mlstmIn, String mlstmOut) throws UserError {
		StreamDataContainer inData = input.getData(StreamDataContainer.class);
		StreamProducer lastNode = inData.getLastNode();
		StreamGraph graph = inData.getStreamGraph();

		// Produce input data for MLSTM
		new KafkaSink.Builder(graph)
			.withTopic(mlstmIn)
			.withParent(lastNode)
			.withConfiguration(kafkaConfig)
			.build();

		// Consume output data of MLSTM
		KafkaSource output = new KafkaSource.Builder(graph)
			.withTopic(mlstmOut)
			.withConfiguration(kafkaConfig)
			.build();

		graph.registerSource(output);
		return output;
	}

	/**
	 * Sets up the MLSTM pipeline (creates topics, sends configuration event: START)
	 * @param kafkaConfig for the KafkaClient
	 * @param pipelineName name of the pipeline to setup
	 * @param mlstmIn name of the input topic
	 * @param mlstmOut name of the output topic
	 */
	private void setupPipeline(Properties kafkaConfig,
							   String pipelineName,
							   String mlstmIn,
							   String mlstmOut) throws OperatorException {
		LOGGER.fine("Setting up pipeline");
		KafkaClient kafkaClient = new KafkaClient(kafkaConfig);

		// Create them now (1-1: partitions and replication factor, these will eventually become parameters themselves)
		Collection<NewTopic> newTopics = newHashSet(mlstmIn, mlstmOut)
			.stream()
			.map(topic -> new NewTopic(topic, 1, (short) 1))
			.collect(Collectors.toList());
		kafkaClient.createTopics(newTopics);

		// Send START request
		JSONObject startRequest = new JSONObject();
		JSONObject startParams = new JSONObject();
		startParams.put("name", pipelineName);
		startParams.put("input_topic", mlstmIn);
		startParams.put("output_topic", mlstmOut);
		startRequest.put("start", startParams);
		kafkaClient.send(MLSTM_CONFIGURATION_TOPIC, null, startRequest.toString());
	}

}