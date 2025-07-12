/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator.demokritos;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.rapidminer.extension.streaming.connection.StreamingConnectionHelper.createFlinkSelector;
import static com.rapidminer.extension.streaming.connection.StreamingConnectionHelper.createKafkaSelector;
import static com.rapidminer.extension.streaming.deploy.StreamRunnerConstants.RM_CONF_CLUSTER_HOST;
import static com.rapidminer.extension.streaming.deploy.StreamRunnerConstants.RM_CONF_CLUSTER_PORT;
import static com.rapidminer.extension.streaming.deploy.StreamRunnerConstants.RM_CONF_CLUSTER_REMOTE_DASHBOARD;
import static com.rapidminer.extension.streaming.operator.demokritos.DemokritosCEFConstants.*;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
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
import com.rapidminer.extension.streaming.operator.AbstractStreamTransformOperator;
import com.rapidminer.extension.streaming.operator.StreamingNest;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;
import com.rapidminer.extension.streaming.utility.graph.sink.KafkaSink;
import com.rapidminer.extension.streaming.utility.graph.source.KafkaSource;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.container.Pair;


/**
 * Operator to deploy an CEF job and involve that in the workflow
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamCEFOperator extends AbstractStreamTransformOperator {

	private final ConnectionInformationSelector kafkaConn = createKafkaSelector(this, "kafka-connection");

	private final ConnectionInformationSelector flinkConn = createFlinkSelector(this, "flink-connection");

	private KafkaClient kafkaClient;

	private FlinkRestClient flinkClient;

	private Properties kafkaConfig;

	private Properties flinkConfig;

	/**
	 * Constructor
	 *
	 * @param description
	 */
	public StreamCEFOperator(OperatorDescription description) {
		super(description);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Overwriting the {@link #doWork()} method from {@link AbstractStreamTransformOperator} to insert
	 * {@link #configureAndDeployCEF(String)}
	 */
	@Override
	public void doWork() throws OperatorException {
		Pair<StreamGraph, List<StreamDataContainer>> inputs = getStreamDataInputs();
		StreamGraph graph = inputs.getFirst();
		logProcessing(graph.getName());

		List<StreamProducer> streamProducers = addToGraph(graph, inputs.getSecond());

		configureAndDeployCEF(graph.getName());

		deliverStreamDataOutputs(graph, streamProducers);
	}

	@Override
	protected StreamProducer createStreamProducer(StreamGraph graph, StreamDataContainer inData) throws UserError {
		// Setup cluster connection configurations + clients for them
		ConnectionConfiguration kafkaConnConfig = kafkaConn.getConnection().getConfiguration();
		ConnectionConfiguration flinkConnConfig = flinkConn.getConnection().getConfiguration();
		kafkaConfig = KafkaConnectionHandler.getINSTANCE().buildClusterConfiguration(kafkaConnConfig);
		flinkConfig = FlinkConnectionHandler.getINSTANCE().buildClusterConfiguration(flinkConnConfig);

		kafkaClient = new KafkaClient(kafkaConfig);
		flinkClient = new FlinkRestClient(
			flinkConfig.getProperty(RM_CONF_CLUSTER_HOST),
			flinkConfig.getProperty(RM_CONF_CLUSTER_PORT));

		// Extend graph with CEF parts
		return extendGraph(inData);
	}

	private void configureAndDeployCEF(String graphName) throws OperatorException {
		// Setup Kafka topics for CEF
		setupKafkaTopics();

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
		List<ParameterType> types = newArrayList();

		types.add(new ParameterTypeDouble(PARAMETER_THRESHOLD, "Threshold", 0.0d, 1.0, false));
		types.add(new ParameterTypeInt(PARAMETER_MAX_SPREAD, "Max spread", 0, Integer.MAX_VALUE, false));
		types.add(new ParameterTypeInt(PARAMETER_HORIZON, "Horizon", 0, Integer.MAX_VALUE, false));
		types.add(new ParameterTypeFile(PARAMETER_PATTERNS, "Pattern file", "*", false));
		types.add(new ParameterTypeFile(PARAMETER_DECLARATIONS, "Declaration file", "*", true));
		types.add(new ParameterTypeInt(PARAMETER_K, "Forecast Frequency", 1, Integer.MAX_VALUE,8000));
		types.add(new ParameterTypeInt(PARAMETER_PARALLELISM, "Job parallelism", 1, Integer.MAX_VALUE, 1));
		types.add(
			new ParameterTypeCategory(
				PARAMETER_OFFSET_RESET,
				"Offset for reading input data",
				new String[] { "earliest", "latest" },
				0,
				true));
		types.add(new ParameterTypeString(PARAMETER_INPUT_TOPIC, "Input topic", false));
		types.add(new ParameterTypeString(PARAMETER_OUTPUT_TOPIC, "Output topic", false));
		types.add(new ParameterTypeString(PARAMETER_CONFIG_TOPIC, "Config topic", "NCSR_CEF_Config"));
		types.add(new ParameterTypeString(PARAMETER_TIMESTAMP_KEY, "Timestamp key", "timestampKey"));
		types.add(
			new ParameterTypeCategory(
				PARAMETER_DOMAIN_STREAM,
				"Domain stream",
				new String[] { "json", "bio", "maritime" },
				0));

		// For the fat-JAR
		types.add(new ParameterTypeFile(PARAMETER_JOB_JAR, "Path to the job fat-JAR", "jar", false));

		return types;
	}

	/**
	 * Extends the graph with CEF specific logic (data producer + Kafka source for forecast consumption)
	 *
	 * @return node representing the incoming prediction data for the downstream
	 */
	private StreamProducer extendGraph(StreamDataContainer inData) throws UserError {
		StreamProducer lastNode = inData.getLastNode();
		StreamGraph graph = inData.getStreamGraph();

		// Produce training data for CEF
		new KafkaSink.Builder(graph)
			.withTopic(getParameterAsString(PARAMETER_INPUT_TOPIC))
			.withParent(lastNode)
			.withConfiguration(kafkaConfig)
			.build();

		// Consume predictions
		KafkaSource forecastSource = new KafkaSource.Builder(graph)
			.withTopic(getParameterAsString(PARAMETER_OUTPUT_TOPIC))
			.withConfiguration(kafkaConfig)
			.build();

		graph.registerSource(forecastSource);
		return forecastSource;
	}

	/**
	 * Creates topics for the CEF. There is a pre-check before the actual creation for early failing.
	 */
	private void setupKafkaTopics() throws OperatorException {
		LOGGER.fine("Setting up topics");
		Set<String> topics = newHashSet(getParameter(PARAMETER_INPUT_TOPIC), getParameter(PARAMETER_OUTPUT_TOPIC));

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
	 * Deploys the JAR given as parameter to the Flink cluster
	 * @return remote-ID for the job
	 */
	private String deployJob() {
		LOGGER.fine("Deploying job");
		try {
			// Parallelism will eventually become a parameter itself
			int parallelism = getParameterAsInt(PARAMETER_PARALLELISM);

			// RUN IT IN A TASK and then can abort it later
			return flinkClient.uploadAndSubmit(
				getParameterAsFile(PARAMETER_JOB_JAR).getPath(),
				buildJobArgList(),
				parallelism,
				CEF_JOB_MAIN_CLASS);
		} catch (IOException e) {
			throw new CompletionException(new UserError(this, e, "flink_io"));
		} catch (UserError ue) {
			throw new CompletionException(ue);
		}
	}

	/**
	 * @return list of parameters for the Flink job
	 */
	private List<String> buildJobArgList() throws UserError {
		return newArrayList(
			CEF_FLINK_JOB_ARG_ONLINE_FORECASTING,
			CEF_FLINK_JOB_ARG_THRESHOLD, getParameter(PARAMETER_THRESHOLD),
			CEF_FLINK_JOB_ARG_MAX_SPREAD, getParameter(PARAMETER_MAX_SPREAD),
			CEF_FLINK_JOB_ARG_HORIZON, getParameter(PARAMETER_HORIZON),
			CEF_FLINK_JOB_ARG_PATTERNS, getFileContent(getParameterAsFile(PARAMETER_PATTERNS)),
			CEF_FLINK_JOB_ARG_DECLARATIONS, getFileContent(getParameterAsFile(PARAMETER_DECLARATIONS)),
			CEF_FLINK_JOB_ARG_K, getParameter(PARAMETER_K),
			CEF_FLINK_JOB_ARG_PARALLELISM, getParameter(PARAMETER_PARALLELISM),
			CEF_FLINK_JOB_ARG_OFFSET_RESET, getParameter(PARAMETER_OFFSET_RESET),
			CEF_FLINK_JOB_ARG_INPUT_TOPIC, getParameter(PARAMETER_INPUT_TOPIC),
			CEF_FLINK_JOB_ARG_OUTPUT_TOPIC, getParameter(PARAMETER_OUTPUT_TOPIC),
			CEF_FLINK_JOB_ARG_CONFIG_TOPIC, getParameter(PARAMETER_CONFIG_TOPIC),
			CEF_FLINK_JOB_ARG_DOMAIN_STREAM, getParameter(PARAMETER_DOMAIN_STREAM),
			CEF_FLINK_JOB_ARG_TIMESTAMP_KEY, getParameter(PARAMETER_TIMESTAMP_KEY),
			CEF_FLINK_JOB_ARG_BOOTSTRAPS, kafkaConfig.getProperty(BOOTSTRAP_SERVERS_CONFIG)
		);
	}

	/**
	 * If the src is not null, reads the file's content and returns it, otherwise it returns an EMPTY string
	 * @param src file to read
	 * @return see above
	 * @throws UserError
	 */
	private String getFileContent(File src) throws UserError {
		if (src == null) {
			return StringUtils.EMPTY;
		} else {
			try {
				return StringUtils.trimToEmpty(FileUtils.readFileToString(src, Charset.defaultCharset()));
			} catch (IOException e) {
				throw new UserError(this, e, 302);
			}
		}
	}

	/**
	 * Creates instance that holds the details of the job that was dispatched
	 *
	 * @param workflow
	 * @param remoteId ID that was provided by the streaming platform to identify the job
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

		return new Job(workflowId, uniqueId, remoteId, endpoint, getName(), Type.Flink, true, Status.Unknown, remoteDashUrl);
	}

}