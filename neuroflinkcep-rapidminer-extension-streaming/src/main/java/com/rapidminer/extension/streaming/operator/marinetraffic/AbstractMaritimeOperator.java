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
import java.util.Collection;
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
import com.rapidminer.extension.streaming.operator.AbstractStreamOperator;
import com.rapidminer.extension.streaming.operator.AbstractStreamTransformOperator;
import com.rapidminer.extension.streaming.operator.StreamingNest;
import com.rapidminer.extension.streaming.utility.api.infore.maritime.JobRequest;
import com.rapidminer.extension.streaming.utility.api.infore.maritime.JobType;
import com.rapidminer.extension.streaming.utility.api.infore.maritime.KafkaConfiguration;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;
import com.rapidminer.extension.streaming.utility.graph.sink.KafkaSink;
import com.rapidminer.extension.streaming.utility.graph.source.KafkaSource;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.container.Pair;


/**
 * @author Fabian Temme
 * @since 0.6.2
 */
public abstract class AbstractMaritimeOperator extends AbstractStreamOperator {

	private final ConnectionInformationSelector maritimeConn = maritimeConnectionSelector(this);

	private final ConnectionInformationSelector kafkaConn = createKafkaSelector(this, "kafka-connection");

	protected final OutputPort output = getOutputPorts().createPort("output stream");

	public static final String PARAMETER_KEY_FIELD = "key_field";

	public static final String PARAMETER_OUTPUT_TOPIC = "output_topic";

	public AbstractMaritimeOperator(OperatorDescription description) {
		super(description);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Overwriting the {@link #doWork()} method from {@link AbstractStreamTransformOperator} to insert
	 * {@link #configureAkkaJob(int)}.
	 */
	@Override
	public void doWork() throws OperatorException {
		Pair<StreamGraph, List<StreamDataContainer>> inputs = getStreamDataInputs();
		StreamGraph graph = inputs.getFirst();
		logProcessing(graph.getName());

		List<StreamProducer> streamProducers = addToGraph(graph, inputs.getSecond());

		configureAkkaJob(inputs.getSecond().size());

		deliverStreamDataOutputs(graph, streamProducers);
	}

	@Override
	public List<StreamProducer> addToGraph(StreamGraph graph, List<StreamDataContainer> streamDataInputs) throws UserError {
		// Connection details
		ConnectionConfiguration kafkaConnConfig = kafkaConn.getConnection().getConfiguration();

		Properties kafkaConfig = KafkaConnectionHandler.getINSTANCE().buildClusterConfiguration(kafkaConnConfig);

		return Collections.singletonList(extendGraph(streamDataInputs, kafkaConfig));
	}

	@Override
	public void deliverStreamDataOutputs(StreamGraph graph, List<StreamProducer> streamProducers) throws UserError {
		StreamDataContainer outData = new StreamDataContainer(graph, streamProducers.get(0));
		output.deliver(outData);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new ArrayList<>(createInputTopicParameterTypes());
		types.add(new ParameterTypeString(PARAMETER_OUTPUT_TOPIC, "Output topic", false));
		types.add(new ParameterTypeString(PARAMETER_KEY_FIELD, "Name of key field to use for Kafka records", true));
		return types;
	}

	protected abstract List<ParameterType> createInputTopicParameterTypes();

	protected abstract List<String> getTopicNames(int desiredSize) throws UndefinedParameterError;

	protected abstract JobType getJobType();

	protected abstract KafkaConfiguration getKafkaConfiguration(String brokers, String outTopic, int numberOfInputs) throws UndefinedParameterError;

	/**
	 * Extends the graph with the necessary nodes to communicate with the MarineTraffic AKKA cluster
	 *
	 * @param kafkaConfig configuration of the Kafka connection
	 * @return last node
	 * @throws UserError in case of parameter errors
	 */
	private StreamProducer extendGraph(List<StreamDataContainer> streamDataInputs,
									   Properties kafkaConfig) throws UserError {
		StreamGraph graph = streamDataInputs.get(0).getStreamGraph();

		List<String> topicNames = getTopicNames(streamDataInputs.size());
		int i = 0;

		for (StreamDataContainer streamData: streamDataInputs){
			new KafkaSink.Builder(graph)
				.withConfiguration(kafkaConfig)
				.withTopic(topicNames.get(i))
				.withKey(getParameterAsString(PARAMETER_KEY_FIELD))
				.withParent(streamData.getLastNode())
				.build();
			i++;
		}

		// Consume results from MarineTraffic cluster
		KafkaSource resultSource = new KafkaSource.Builder(graph)
			.withConfiguration(kafkaConfig)
			.withTopic(getParameterAsString(PARAMETER_OUTPUT_TOPIC))
			.build();

		graph.registerSource(resultSource);
		return resultSource;
	}

	private void configureAkkaJob(int numberOfInputs) throws UserError {
		// Connection details
		ConnectionConfiguration kafkaConnConfig = kafkaConn.getConnection().getConfiguration();
		ConnectionConfiguration maritimeConnConfig = maritimeConn.getConnection().getConfiguration();

		Properties kafkaConfig = KafkaConnectionHandler.getINSTANCE().buildClusterConfiguration(kafkaConnConfig);
		Properties maritimeConfig =
			MaritimeConnectionHandler.getINSTANCE().buildClusterConfiguration(maritimeConnConfig);

		// REST initiate AKKA job
		String jobId = initiateJob(maritimeConfig, kafkaConfig, numberOfInputs);

		// Save job into workflow
		Workflow workflow = ((StreamingNest) getExecutionUnit().getEnclosingOperator()).getWorkflow();
		Job job = createJob(workflow, jobId);
		ManagementDAO.addOrUpdate(workflow.getId(), job);
	}

	/**
	 * This method initiates/deploys the AKKA cluster job on the MarineTraffic side via REST
	 *
	 * @param maritimeConfig configuration for the Maritime Akka cluster
	 * @param kafkaConfig configuration for the Kafka cluster
	 * @return job-id
	 * @throws UserError in case parameter errors
	 */
	private String initiateJob(Properties maritimeConfig, Properties kafkaConfig, int numberOfInputs) throws UserError {
		String brokers = kafkaConfig.getProperty(BOOTSTRAP_SERVERS_CONFIG);

		MaritimeRestClient restClient = new MaritimeRestClient(
			maritimeConfig.getProperty(PARAMETER_USERNAME),
			maritimeConfig.getProperty(PARAMETER_PASSWORD),
			maritimeConfig.getProperty(PARAMETER_HOST),
			maritimeConfig.getProperty(PARAMETER_PORT),
			Boolean.valueOf(maritimeConfig.getProperty(PARAMETER_ALLOW_SELF_SIGNED_CERT))
		);

		try {
			JobRequest req = new JobRequest(
				getJobType(),
				getKafkaConfiguration(brokers, getParameterAsString(PARAMETER_OUTPUT_TOPIC), numberOfInputs));

			String jobId = restClient.startJob(req);
			LOGGER.info("Maritime Event Detection job-id: " + jobId);
			return jobId;
		} catch (OperatorException oe) {
			throw new UserError(this, oe, "maritime.dispatch");
		}
	}





	/**
	 * Creates instance that holds the details of the job that was dispatched
	 *
	 * @param workflow parent object
	 * @param remoteId ID that was provided by the streaming platform to identify the job
	 * @return newly created instance
	 */
	private Job createJob(Workflow workflow, String remoteId) {
		// Get job-ID and save job to "DB"
		String workflowId = workflow.getId();
		String uniqueId = UUID.randomUUID().toString();
		StreamingEndpoint endpoint = new MaritimeStreamingEndpoint(maritimeConn.getConnectionLocation().getAbsoluteLocation());

		return new Job(workflowId, uniqueId, remoteId, endpoint, getName(), Type.Maritime, false, Status.Running, null);
	}

	/**
	 * @param op operator, to create the selector for
	 * @return Maritime connection selector
	 */
	private static ConnectionInformationSelector maritimeConnectionSelector(Operator op) {
		return createSelector(op, "maritime-connection", MaritimeConnectionHandler.getINSTANCE().getType());
	}
}
