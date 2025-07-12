/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator.athena;

import static com.google.common.collect.Sets.newHashSet;
import static com.rapidminer.extension.streaming.connection.StreamingConnectionHelper.createFlinkSelector;
import static com.rapidminer.extension.streaming.connection.StreamingConnectionHelper.createKafkaSelector;
import static com.rapidminer.extension.streaming.deploy.StreamRunnerConstants.RM_CONF_CLUSTER_HOST;
import static com.rapidminer.extension.streaming.deploy.StreamRunnerConstants.RM_CONF_CLUSTER_PORT;
import static com.rapidminer.extension.streaming.deploy.StreamRunnerConstants.RM_CONF_CLUSTER_REMOTE_DASHBOARD;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_FORECAST_INPUT_TOPIC;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_FORECAST_OUTPUT_TOPIC;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_JOB_JAR;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_PMESSAGE_TOPIC;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_REQUEST_TOPIC;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_RESPONSE_TOPIC;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_TRAIN_TOPIC;
import static com.rapidminer.extension.streaming.utility.JsonUtil.toJson;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.NewTopic;

import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.util.ConnectionInformationSelector;
import com.rapidminer.extension.kafka_connector.connections.KafkaConnectionHandler;
import com.rapidminer.extension.streaming.connection.FlinkConnectionHandler;
import com.rapidminer.extension.streaming.deploy.KafkaClient;
import com.rapidminer.extension.streaming.deploy.flink.FlinkRestClient;
import com.rapidminer.extension.streaming.deploy.management.api.Status;
import com.rapidminer.extension.streaming.deploy.management.api.Type;
import com.rapidminer.extension.streaming.deploy.management.db.Job;
import com.rapidminer.extension.streaming.deploy.management.db.ManagementDAO;
import com.rapidminer.extension.streaming.deploy.management.db.StreamingEndpoint;
import com.rapidminer.extension.streaming.deploy.management.db.Workflow;
import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.operator.AbstractStream2to1Operator;
import com.rapidminer.extension.streaming.operator.AbstractStreamOperator;
import com.rapidminer.extension.streaming.operator.StreamingNest;
import com.rapidminer.extension.streaming.utility.api.infore.onlineml.ModelConfiguration;
import com.rapidminer.extension.streaming.utility.api.infore.onlineml.Request;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;
import com.rapidminer.extension.streaming.utility.graph.sink.KafkaSink;
import com.rapidminer.extension.streaming.utility.graph.source.KafkaSource;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.container.Pair;


/**
 * Operator to deploy an Athena-OML job and involve that in the workflow
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamAthenaOMLOperator extends AbstractStream2to1Operator {

	private final AthenaOMLParameterHelper paramHelper = new AthenaOMLParameterHelper(this);

	private final ConnectionInformationSelector kafkaConn = createKafkaSelector(this, "kafka-connection");

	private final ConnectionInformationSelector flinkConn = createFlinkSelector(this, "flink-connection");

	private KafkaClient kafkaClient;

	private FlinkRestClient flinkClient;

	private Properties kafkaConfig;

	private Properties flinkConfig;

	public StreamAthenaOMLOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	protected String getFirstInputName() {
		return "training input";
	}

	@Override
	protected String getSecondInputName() {
		return "input stream";
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Overwriting the {@link #doWork()} method from {@link AbstractStreamOperator} to insert the handling of the OML
	 * component
	 */
	@Override
	public void doWork() throws OperatorException {
		Pair<StreamGraph, List<StreamDataContainer>> inputs = getStreamDataInputs();
		StreamGraph graph = inputs.getFirst();
		logProcessing(graph.getName());

		List<StreamProducer> streamProducers = addToGraph(graph, inputs.getSecond());

		configureAndDeployOML(graph.getName());

		deliverStreamDataOutputs(graph, streamProducers);
	}

	@Override
	protected StreamProducer createProducer(StreamGraph graph, StreamDataContainer firstStreamData,
											StreamDataContainer secondStreamData) throws UserError {
		// Setup cluster connection configurations + clients for them
		ConnectionConfiguration kafkaConnConfig = kafkaConn.getConnection().getConfiguration();
		ConnectionConfiguration flinkConnConfig = flinkConn.getConnection().getConfiguration();
		kafkaConfig = KafkaConnectionHandler.getINSTANCE().buildClusterConfiguration(kafkaConnConfig);
		flinkConfig = FlinkConnectionHandler.getINSTANCE().buildClusterConfiguration(flinkConnConfig);

		kafkaClient = new KafkaClient(kafkaConfig);
		flinkClient = new FlinkRestClient(
			flinkConfig.getProperty(RM_CONF_CLUSTER_HOST),
			flinkConfig.getProperty(RM_CONF_CLUSTER_PORT));

		// Extend graph with OML parts
		return extendGraph(firstStreamData, secondStreamData);
	}

	private void configureAndDeployOML(String graphName) throws OperatorException {
		// Setup Kafka topics for OML
		setupKafkaTopics();

		// Send ML pipeline "Create" request (~ configure OML algorithm)
		sendCreateRequest();

		// Deploy job asynchronously and periodically check for stop
		CompletableFuture<String> future = supplyAsync(this::deployJob, newSingleThreadExecutor());
		try {
			while (!future.isDone()) {
				checkForStop();
				Thread.sleep(1000);
			}

			// Save job into "DB"
			String jobId = future.get();
			Workflow workflow = ((StreamingNest) getExecutionUnit().getEnclosingOperator()).getWorkflow();
			Job job = createJob(workflow, jobId);
			ManagementDAO.addOrUpdate(workflow.getId(), job);
		} catch (ProcessStoppedException pse) {
			LOGGER.warning("Process stopped for Flink job execution: '" + graphName + "'");
			future.cancel(true);
			flinkClient.abort();
			throw pse;
		} catch (InterruptedException | ExecutionException ee) {
			LOGGER.warning("Error while executing Flink job '" + graphName + "': " + ee.getMessage());
			throw new UserError(this, ee, "stream_connection.unsuccessful");
		}
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(paramHelper.buildParameterTypes());
		return types;
	}

	/**
	 * Extends the graph with Athena OML specific logic
	 *
	 * @return node representing the incoming prediction data for the downstream
	 */
	private StreamProducer extendGraph(StreamDataContainer trainInData, StreamDataContainer inData) throws UserError {
		StreamProducer trainLastNode = trainInData.getLastNode();
		StreamProducer lastNode = inData.getLastNode();
		StreamGraph graph = inData.getStreamGraph();

		new KafkaSink.Builder(graph)
			.withConfiguration(kafkaConfig)
			.withTopic(getParameterAsString(PARAMETER_TRAIN_TOPIC))
			.withParent(trainLastNode)
			.build();

		new KafkaSink.Builder(graph)
			.withConfiguration(kafkaConfig)
			.withTopic(getParameterAsString(PARAMETER_FORECAST_INPUT_TOPIC))
			.withParent(lastNode)
			.build();

		// Consume predictions
		KafkaSource forecastSource = new KafkaSource.Builder(graph)
			.withConfiguration(kafkaConfig)
			.withTopic(getParameterAsString(PARAMETER_FORECAST_OUTPUT_TOPIC))
			.build();

		graph.registerSource(forecastSource);
		return forecastSource;
	}

	/**
	 * Creates topics for the OML. There is a pre-check before the actual creation for early failing.
	 */
	private void setupKafkaTopics() throws OperatorException {
		LOGGER.fine("Setting up topics");
		Set<String> topics = newHashSet(
			getParameterAsString(PARAMETER_PMESSAGE_TOPIC),
			getParameterAsString(PARAMETER_REQUEST_TOPIC),
			getParameterAsString(PARAMETER_RESPONSE_TOPIC),
			getParameterAsString(PARAMETER_TRAIN_TOPIC),
			getParameterAsString(PARAMETER_FORECAST_INPUT_TOPIC),
			getParameterAsString(PARAMETER_FORECAST_OUTPUT_TOPIC)
		);

		// Check if any of the topics already exists (not atomic, just trying to help the user)
		Set<String> existingTopics = kafkaClient.checkTopics(topics);
		if (!existingTopics.isEmpty()) {
			throw new OperatorException("The following topics already exist: " + existingTopics);
		}

		// Create them now (1-1: partitions and replication factor, these will eventually become parameters themselves)
		Collection<NewTopic> newTopics = topics
			.stream()
			.map(topic -> new NewTopic(topic, 1, (short) 1))
			.collect(Collectors.toList());
		kafkaClient.createTopics(newTopics);
	}

	/**
	 * Sends the ML pipeline creation request to Kafka thus configuring the appropriate algorithm for OML
	 */
	private void sendCreateRequest() throws OperatorException {
		ModelConfiguration learner = paramHelper.getLearner();
		List<ModelConfiguration> preProcessors = paramHelper.getPreProcessors();
		Map<String, Object> trainingConfig = paramHelper.getTrainingConfiguration();

		// ML-PipelineId could be changed later on, for now: 1
		Request request = new Request(Request.Type.Create, 1, learner, preProcessors, trainingConfig);

		LOGGER.fine("Sending request to Kafka");
		KafkaClient kafkaClient = new KafkaClient(kafkaConfig);
		kafkaClient.send(getParameterAsString(PARAMETER_REQUEST_TOPIC), null, toJson(request));
	}

	/**
	 * Deploys the JAR given as parameter to the Flink cluster
	 *
	 * @return job-ID
	 */
	private String deployJob() {
		LOGGER.fine("Deploying job");

		try {
			// Start job (parallelism will eventually become a parameter itself)
			int parallelism = 1;
			String brokers = kafkaConfig.getProperty(BOOTSTRAP_SERVERS_CONFIG);

			// RUN IT IN A TASK and then can abort it later
			return flinkClient.uploadAndSubmit(
				getParameterAsFile(PARAMETER_JOB_JAR).getPath(),
				paramHelper.getJobArguments(parallelism, brokers),
				parallelism,
				null);
		} catch (IOException e) {
			throw new CompletionException(new UserError(this, e, "flink_io"));
		} catch (UserError ue) {
			throw new CompletionException(ue);
		}
	}

	/**
	 * Creates instance that holds the details of the job that was dispatched
	 *
	 * @param workflow
	 * @param remoteId
	 * 	ID that was provided by the streaming platform to identify the job
	 * @return newly created instance
	 */
	private Job createJob(Workflow workflow, String remoteId) {
		// Get job-ID and save job to "DB"
		String workflowId = workflow.getId();
		String uniqueId = UUID.randomUUID().toString();
		String remoteDashUrl = flinkConfig.getProperty(RM_CONF_CLUSTER_REMOTE_DASHBOARD);
		StreamingEndpoint endpoint = new StreamingEndpoint(
			flinkConfig.getProperty(RM_CONF_CLUSTER_HOST),
			flinkConfig.getProperty(RM_CONF_CLUSTER_PORT));

		return new Job(workflowId, uniqueId, remoteId, endpoint, getName(), Type.Flink, true, Status.Unknown,
			remoteDashUrl);
	}

}