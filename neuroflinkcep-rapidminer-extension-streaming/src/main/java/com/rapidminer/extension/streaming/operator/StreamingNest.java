/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator;

import static com.rapidminer.extension.streaming.PluginInitStreaming.getPluginLoader;
import static com.rapidminer.extension.streaming.connection.StreamingConnectionHelper.createStreamConnectionSelector;
import static com.rapidminer.extension.streaming.deploy.StreamRunnerConstants.RM_CONF_CLUSTER_HOST;
import static com.rapidminer.extension.streaming.deploy.StreamRunnerConstants.RM_CONF_CLUSTER_PORT;
import static com.rapidminer.extension.streaming.deploy.StreamRunnerConstants.RM_CONF_CLUSTER_REMOTE_DASHBOARD;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.apache.commons.lang3.RandomStringUtils;

import com.google.common.collect.Maps;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.util.ConnectionInformationSelector;
import com.rapidminer.extension.streaming.connection.StreamConnectionHandler;
import com.rapidminer.extension.streaming.connection.StreamingConnectionHelper;
import com.rapidminer.extension.streaming.connection.optimizer.OptimizerConnectionHandler;
import com.rapidminer.extension.streaming.deploy.StreamRunner;
import com.rapidminer.extension.streaming.deploy.StreamRunnerException;
import com.rapidminer.extension.streaming.deploy.StreamRunnerFactory;
import com.rapidminer.extension.streaming.deploy.StreamRunnerType;
import com.rapidminer.extension.streaming.deploy.management.DateTimeUtil;
import com.rapidminer.extension.streaming.deploy.management.api.Status;
import com.rapidminer.extension.streaming.deploy.management.api.Type;
import com.rapidminer.extension.streaming.deploy.management.db.Job;
import com.rapidminer.extension.streaming.deploy.management.db.ManagementDAO;
import com.rapidminer.extension.streaming.deploy.management.db.StreamingEndpoint;
import com.rapidminer.extension.streaming.deploy.management.db.Workflow;
import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.optimizer.connection.OptimizerConnection;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.PortPairExtender;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.ClassLoaderSwapper;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.container.Pair;


/**
 * Nest operator that wraps a stream workflow, this is the main entry-point for a stream workflow
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamingNest extends OperatorChain {

	private static final Logger LOGGER = LogService.getRoot();

	private static final String PARAMETER_JOB_NAME = "job_name";

	public static final String PARAMETER_OPTIMIZATION_WORKFLOW_ID = "optimization_workflow_id";

	private static final StreamRunnerFactory RUNNER_FACTORY = new StreamRunnerFactory();

	private final ConnectionInformationSelector connectionSelector = createStreamConnectionSelector(this, "connection");

	private final PortPairExtender inputPortPairExtender = new PortPairExtender(
		"in", getInputPorts(), getSubprocess(0).getInnerSources());

	private final PortPairExtender outputPortPairExtender = new PortPairExtender(
		"out", getSubprocess(0).getInnerSinks(), getOutputPorts());

	private Workflow workflow;

	private StreamGraph graph;

	private ConnectionConfiguration connConfig;

	private String optimizationWorkflowId = null;

	public StreamingNest(OperatorDescription description) {
		super(description, "Streaming");
		inputPortPairExtender.start();
		outputPortPairExtender.start();

		getTransformer().addRule(() -> {
			updateOptimizationWorkflowID();
		});

		getTransformer().addRule(inputPortPairExtender.makePassThroughRule());
		getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
		getTransformer().addRule(outputPortPairExtender.makePassThroughRule());
	}

	@Override
	public void doWork() throws OperatorException {
		inputPortPairExtender.passDataThrough();
		updateOptimizationWorkflowID();
		graph = new StreamGraph(getParameterAsString(PARAMETER_JOB_NAME));
		LOGGER.fine("Processing NEST for: " + graph.getName());

		ConnectionInformation connectionInformation = connectionSelector.getConnection();
		connConfig = connectionInformation.getConfiguration();
		// If we have a connection to an INFORE Optimizer Service this is a benchmarking request and we just serialize
		// the graph and upload this file to the FileServer of the Optimizer Service
		if (connConfig.getType().equals(OptimizerConnectionHandler.TYPE)) {
			OptimizerConnection optimizerConnection =
				OptimizerConnectionHandler.createConnection(connectionInformation, this);
			try {
				String response = optimizerConnection.submitGraphFile(fillGraphForBenchmarking(),getParameterAsString(PARAMETER_JOB_NAME));
			} catch (IOException e) {
				throw new UserError(this, "optimizer.benchmarking.file_upload_fail", e.getMessage());
			}
			// return early, because we don't upload and deploy the graph to any streaming cluster
			return;
		}

		// TODO: this won't really be happening here, but for now it will
		String workflowName = graph.getName() + "-" + RandomStringUtils.randomAlphanumeric(5);

		String workflowID = optimizationWorkflowId != null ? optimizationWorkflowId: UUID.randomUUID().toString();
		String processLocation = Objects.toString(getProcess().getProcessLocation(), "");
		workflow = new Workflow(
			workflowID,
			workflowName,
			processLocation,
			DateTimeUtil.getTimestamp(),
			Maps.newHashMap());
		if (optimizationWorkflowId == null) {
			// We only create the new entry in the ManagementDB if this is not part of an optimization workflow.
			ManagementDAO.addOrUpdate(workflow);
		}

		// Certain libraries depend on the thread's context loader, so we set the plugin's loader to be that
		try (ClassLoaderSwapper cls = ClassLoaderSwapper.withContextClassLoader(getPluginLoader())) {
			// Execute children operators
			super.doWork();
			outputPortPairExtender.passDataThrough();

			// Sanity check
			if (graph.getSources().isEmpty()){
				throw new UserError(this, "streaming_nest.empty_graph");
			}

			// Based on the connection create appropriate configuration, stream-runner, etc.
			String connType = connConfig.getType();
			StreamConnectionHandler handler = StreamingConnectionHelper.CONNECTION_HANDLER_MAP.get(connType);
			StreamRunnerType runnerType = handler.getRunnerType();
			Properties runnerConfig = handler.buildClusterConfiguration(connConfig);
			StreamRunner runner = RUNNER_FACTORY.createRunner(runnerType, runnerConfig);

			// Start execution asynchronously and periodically check for stop
			CompletableFuture<String> future = supplyAsync(() -> executeGraph(runner), newSingleThreadExecutor());
			try {
				while (!future.isDone()) {
					checkForStop();
					Thread.sleep(1000);
				}

				// Save job into "DB"
				Job job = createJob(workflow, future.get(), runnerConfig, getJobType(runnerType));
				ManagementDAO.addOrUpdate(workflow.getId(), job);
			} catch (ProcessStoppedException pse) {
				LOGGER.warning("Process stopped for streaming job execution: '" + graph.getName() + "'");
				future.cancel(true);
				runner.abort();
				throw pse;
			} catch (InterruptedException | ExecutionException ee) {
				LOGGER.warning("Error while executing streaming job '" + graph.getName() + "': " + ee.getMessage());
				throw new UserError(this, ee, "stream_connection.unsuccessful");
			}
		} catch (StreamRunnerException e) {
			LOGGER.warning("Error while executing streaming job '" + graph.getName() + "': " + e.getMessage());
			throw new UserError(this, e, "stream_connection.unsuccessful");
		}
	}

	private void updateOptimizationWorkflowID(){
		String optimizationWorkflowIdParam = null;
		try {
			optimizationWorkflowIdParam = getParameterAsString(PARAMETER_OPTIMIZATION_WORKFLOW_ID);
		} catch (UndefinedParameterError e) {
			if (getLogger() != null){
				getLogger().info("Exception: " + e.getMessage());
			}
		}
		if (optimizationWorkflowIdParam != null && !optimizationWorkflowIdParam.isEmpty()){
			this.optimizationWorkflowId = optimizationWorkflowIdParam;
		} else {
			this.optimizationWorkflowId = null;
		}
	}

	private StreamGraph fillGraphForBenchmarking() throws OperatorException {
		List<Operator> operators = getSubprocess(0).getAllInnerOperators();
		StreamGraph graph = null;

		for (Operator operator : operators) {
			if (operator instanceof StreamOperator) {
				StreamOperator streamOperator = (StreamOperator) operator;
				Pair<StreamGraph, List<StreamDataContainer>> inputs = streamOperator.getStreamDataInputs();
				graph = inputs.getFirst();

				List<StreamProducer> streamProducers = streamOperator.addToGraph(graph, inputs.getSecond());
				streamOperator.deliverStreamDataOutputs(graph, streamProducers);
			} else {
				operator.doWork();
			}
		}
		if (graph == null) {
			throw new UserError(this, "streaming_nest.empty_graph");
		}
		return graph;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> params = super.getParameterTypes();

		ParameterType jobName = new ParameterTypeString(PARAMETER_JOB_NAME, "Name of the stream job.", false);
		params.add(jobName);

		ParameterType optimizationWorkflowIdParam = new ParameterTypeString(PARAMETER_OPTIMIZATION_WORKFLOW_ID,"",
				true, true);
		optimizationWorkflowIdParam.setHidden(true);
		params.add(optimizationWorkflowIdParam);

		return params;
	}

	public PortPairExtender getInputPortPairExtender() {
		return inputPortPairExtender;
	}

	public PortPairExtender getOutputPortPairExtender() {
		return outputPortPairExtender;
	}

	/**
	 * @return graph object for this workflow
	 */
	public StreamGraph getGraph() {
		return graph;
	}

	/**
	 * @return parent workflow object representation in the "DB"
	 */
	public Workflow getWorkflow() {
		return workflow;
	}

	public void registerOptimizationWorkflowId(String optimizationWorkflowId){
		this.optimizationWorkflowId = optimizationWorkflowId;
	}

	/**
	 * Initiates execution of the workflow using the given StreamRunner implementation
	 * @param runner
	 */
	private String executeGraph(StreamRunner runner) {
		try {
			LOGGER.fine("Starting execution using: " + runner.getClass().getName());
			return runner.execute(graph);
		} catch (StreamRunnerException ex) {
			throw new CompletionException(ex);
		}
	}

	/**
	 * Creates instance that holds the details of the job that was dispatched
	 *
	 * @param workflow
	 * @param remoteId ID that was provided by the streaming platform to identify the job
	 * @param config
	 * @param jobType
	 * @return newly created instance
	 */
	private Job createJob(Workflow workflow, String remoteId, Properties config, Type jobType) {
		// Get job-ID and save job to "DB"
		String workflowId = workflow.getId();
		String uniqueId = UUID.randomUUID().toString();
		String remoteDashUrl = config.getProperty(RM_CONF_CLUSTER_REMOTE_DASHBOARD);
		StreamingEndpoint endpoint = new StreamingEndpoint(
			config.getProperty(RM_CONF_CLUSTER_HOST),
			config.getProperty(RM_CONF_CLUSTER_PORT));

		return new Job(workflowId, uniqueId, remoteId, endpoint, graph.getName(), jobType, true, Status.Unknown, remoteDashUrl);
	}

	/**
	 * @param runnerType
	 * @return JobType equivalent for the StreamRunnerType
	 */
	private Type getJobType(StreamRunnerType runnerType) {
		switch (runnerType) {
			case SPARK:
				return Type.Spark;
			case FLINK:
				return Type.Flink;
			default:
				throw new IllegalArgumentException("Unknown stream-runner-type");
		}
	}

}