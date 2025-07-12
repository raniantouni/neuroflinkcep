/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.management;

import static com.rapidminer.extension.streaming.connection.infore.MaritimeConnectionHandler.PARAMETER_ALLOW_SELF_SIGNED_CERT;
import static com.rapidminer.extension.streaming.connection.infore.MaritimeConnectionHandler.PARAMETER_HOST;
import static com.rapidminer.extension.streaming.connection.infore.MaritimeConnectionHandler.PARAMETER_PASSWORD;
import static com.rapidminer.extension.streaming.connection.infore.MaritimeConnectionHandler.PARAMETER_PORT;
import static com.rapidminer.extension.streaming.connection.infore.MaritimeConnectionHandler.PARAMETER_USERNAME;
import static java.util.concurrent.Executors.newFixedThreadPool;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.rapidminer.Process;
import com.rapidminer.RepositoryProcessLocation;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.extension.streaming.connection.infore.MaritimeConnectionHandler;
import com.rapidminer.extension.streaming.deploy.flink.FlinkRestClient;
import com.rapidminer.extension.streaming.deploy.infore.MaritimeRestClient;
import com.rapidminer.extension.streaming.deploy.management.api.JobDTO;
import com.rapidminer.extension.streaming.deploy.management.api.Status;
import com.rapidminer.extension.streaming.deploy.management.api.Type;
import com.rapidminer.extension.streaming.deploy.management.api.WorkflowDTO;
import com.rapidminer.extension.streaming.deploy.management.db.Job;
import com.rapidminer.extension.streaming.deploy.management.db.ManagementDAO;
import com.rapidminer.extension.streaming.deploy.management.db.StreamingEndpoint;
import com.rapidminer.extension.streaming.deploy.management.db.Workflow;
import com.rapidminer.extension.streaming.deploy.management.db.infore.InforeOptimizerStreamingEndpoint;
import com.rapidminer.extension.streaming.deploy.management.db.infore.MaritimeStreamingEndpoint;
import com.rapidminer.extension.streaming.deploy.spark.SparkRestClient;
import com.rapidminer.extension.streaming.operator.StreamingOptimizationOperator;
import com.rapidminer.extension.streaming.optimizer.OptimizationHelper;
import com.rapidminer.extension.streaming.optimizer.settings.OperationMode;
import com.rapidminer.extension.streaming.optimizer.settings.OptimizerResponse;
import com.rapidminer.extension.streaming.utility.JsonUtil;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.repository.ConnectionEntry;
import com.rapidminer.repository.ProcessEntry;
import com.rapidminer.repository.RepositoryException;
import com.rapidminer.repository.RepositoryLocation;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.XMLException;
import com.teamdev.jxbrowser.js.JsAccessible;


/**
 * Class handling requests coming from the UI (jxbrowser will register "this" object as a "window" object
 * accessible by javascript code).
 *
 * @author Mate Torok
 * @since 0.2.0
 */
@JsAccessible
public class DashboardAPIHandler {

	public static final String WINDOW_KEY = "rm_api";

	private static final Logger LOGGER = LogService.getRoot();

	private static final int UPDATE_THREADS = 10;

	private static final ExecutorService UPDATE_EXECUTOR = newFixedThreadPool(UPDATE_THREADS);

	/**
	 * Opens the RapidMiner process at the given location
	 *
	 * @param location of the process to be opened
	 */
	public void openProcess(String location) {
		LOGGER.fine("Dashboard API: Opening process: " + location);
		Process proc = getProcess(location);

		// Switch over to the process
		if (proc != null && RapidMinerGUI.getMainFrame().close()) {
			SwingUtilities.invokeLater(() -> RapidMinerGUI.getMainFrame().setOpenedProcess(proc));
		}
	}

	private Process getProcess(String location) {
		try {
			RepositoryLocation repoLoc =
				RepositoryLocation.getRepositoryLocationData(location, null, ProcessEntry.class);
			RepositoryProcessLocation procLoc = new RepositoryProcessLocation(repoLoc);
			return procLoc.load(null);
		} catch (UserError | XMLException | IOException ex) {
			LOGGER.log(Level.WARNING, "Could not open the process", ex);
		}
		return null;
	}

	/**
	 * This method does the following:
	 * <ol>
	 *     <li>Collects jobs that in the following states: RUNNING or UNKNOWN</li>
	 *     <li>Builds an endpoint to jobs maps using the above collected jobs</li>
	 *     <li>Asynchronously queries the endpoints for job states</li>
	 *     <li>Asynchronously updates the job states based on the result of the previous operation</li>
	 * </ol>
	 */
	public void update() {
		LOGGER.fine("Dashboard API: Update called");
		ArrayListMultimap<StreamingEndpoint, Job> endpointToJob = ArrayListMultimap.create();

		// Collect endpoints (for jobs that are not yet finished)
		ManagementDAO.getWorkflows()
			.stream()
			.flatMap(workflow -> workflow.getJobs().values().stream())
			.filter(job -> Status.Unknown == job.getState() || Status.Running == job.getState() || Status.NewPlanAvailable == job.getState())
			.filter(Job::isMonitorable)
			.forEach(job -> endpointToJob.put(job.getEndpoint(), job));

		// Fetch data for every endpoint and update the respective jobs
		for (StreamingEndpoint endpoint : endpointToJob.keySet()) {
			List<Job> jobs = endpointToJob.get(endpoint);
			Type type = jobs.iterator().next().getType();

			CompletableFuture
				.supplyAsync(() -> this.queryJobStatus(type, endpoint), UPDATE_EXECUTOR)
				.thenAcceptAsync(jobSummaries -> this.updateJobs(jobSummaries, jobs));
		}
	}

	/**
	 * This method removes the workflow marked by the parameter from the "DB"
	 * @param id of the workflow
	 */
	public void removeWorkflow(String id) {
		LOGGER.fine("Dashboard API: Removing workflow");
		ManagementDAO.removeWorkflow(id);
	}

	/**
	 * This method returns the workflows from the "DB"
	 * @return see above
	 */
	public String getWorkflows() {
		LOGGER.fine("Dashboard API: Getting workflows");
		List<WorkflowDTO> workflows = ManagementDAO.getWorkflows()
			.stream()
			.map(this::adapt)
			.collect(Collectors.toList());
		return JsonUtil.toJson(workflows);
	}

	/**
	 * This method takes the workflow from the "DB" and stops the RUNNING jobs 1 by 1, asynchronously.
	 * <ol>
	 *     <li>Fetches the workflow from the DB</li>
	 *     <li>Iterates through its jobs</li>
	 *     <li>Filters for every Running job and sets their state into Stopping</li>
	 *     <li>Asynchronously stops those jobs</li>
	 * </ol>
	 * @param id of the workflow
	 */
	public void stopWorkflow(String id) {
		LOGGER.fine("Dashboard API: Stopping workflow");
		Workflow workflow = ManagementDAO.getWorkflow(id);

		if (workflow != null) {
			String wId = workflow.getId();
			workflow.getJobs()
				.values()
				.stream()
				.filter(job -> Status.Running == job.getState() || Status.NewPlanAvailable == job.getState())
				.peek(job -> ManagementDAO.updateState(wId, job.getUniqueId(), job.getState(), Status.Stopping))
				.forEach(job -> CompletableFuture.runAsync(() -> this.doStopJob(wId, job), UPDATE_EXECUTOR));
		}
	}

	public void clearFinishedJobs(String id){
		LOGGER.fine("Dashboard API: Clear finished jobs");
		Workflow workflow = ManagementDAO.getWorkflow(id);

		if (workflow != null) {
			String wId = workflow.getId();
			workflow.getJobs()
				.values()
				.stream()
				.filter(job -> Status.Finished == job.getState() || Status.Failed == job.getState())
				.forEach(job -> removeJob(wId,job.getUniqueId()));
		}
	}

	/**
	 * This method stops the job marked by the parameters, asynchronously.
	 * <ol>
	 *     <li>Fetches the workflow from the DB</li>
	 *     <li>Fetches the job from the workflow</li>
	 *     <li>Sets its state into Stopping if it was running</li>
	 *     <li>Asynchronously stops it</li>
	 * </ol>
	 * @param wId
	 * @param jId
	 */
	public void stopJob(String wId, String jId) {
		LOGGER.fine("Dashboard API: Stopping job");
		Workflow workflow = ManagementDAO.getWorkflow(wId);
		Job job = workflow == null ? null : workflow.getJobs().get(jId);

		if (job != null && (Status.Running == job.getState() || Status.NewPlanAvailable == job.getState())) {
			ManagementDAO.updateState(wId, job.getUniqueId(), job.getState(), Status.Stopping);
			CompletableFuture.runAsync(() -> this.doStopJob(wId, job), UPDATE_EXECUTOR);
		}
	}

	/**
	 * This method removes the job marked by the parameter from the "DB"
	 * @param wId id of the workflow
	 * @param jId id of the job
	 */
	public void removeJob(String wId, String jId) {
		LOGGER.fine("Dashboard API: Removing job");
		Workflow workflow = ManagementDAO.getWorkflow(wId);
		Job job = workflow == null ? null : workflow.getJobs().get(jId);
		if (job != null && (job.getState() != Status.Finished || job.getState() != Status.Failed)){
			stopJob(wId, jId);
		}
		ManagementDAO.removeJob(wId, job);
	}

	public void openDeployedProcess(String wId, String jId) {
		LOGGER.fine("Dashboard API: Opening deployed process of the optimization workflow: ");
		Workflow workflow = ManagementDAO.getWorkflow(wId);
		Job optimizationJob = workflow == null ? null : workflow.getJobs().get(jId);
		if (optimizationJob != null && optimizationJob.getType().equals(Type.InforeOptimizer)) {
			InforeOptimizerStreamingEndpoint endpoint = (InforeOptimizerStreamingEndpoint) optimizationJob.getEndpoint();
			Process proc = openProcessAndAdaptToLatestPlan(workflow, optimizationJob.getUniqueId(), endpoint, false);
			// Switch over to the process
			if (proc != null && RapidMinerGUI.getMainFrame().close()) {
				SwingUtilities.invokeLater(() -> RapidMinerGUI.getMainFrame().setOpenedProcess(proc));
			}
		}
	}

	public void updateOptimizationJob(String wId, String jId) {
		LOGGER.fine("Dashboard API: Updating optimization workflow");
		Workflow workflow = ManagementDAO.getWorkflow(wId);
		Job optimizationJob = workflow == null ? null : workflow.getJobs().get(jId);
		if (optimizationJob != null && optimizationJob.getType().equals(Type.InforeOptimizer)) {
			String optJobId = optimizationJob.getUniqueId();
			// Stop all other running jobs
			workflow.getJobs()
				.values()
				.stream()
				.filter(job -> Status.Running == job.getState())
				.filter(job -> !job.getUniqueId().equals(optJobId))
				.peek(job -> ManagementDAO.updateState(wId, job.getUniqueId(), Status.Running, Status.Stopping))
				.forEach(job -> CompletableFuture.runAsync(() -> this.doStopJob(wId, job), UPDATE_EXECUTOR));

			InforeOptimizerStreamingEndpoint endpoint = (InforeOptimizerStreamingEndpoint) optimizationJob.getEndpoint();
			ManagementDAO.updateState(wId, optJobId, Status.NewPlanAvailable, Status.DeployingNewPlan);
			CompletableFuture.runAsync(() -> this.doOptimizationUpdate(workflow, optJobId, endpoint), UPDATE_EXECUTOR);
		}
	}

	private void doOptimizationUpdate(Workflow workflow, String jId, InforeOptimizerStreamingEndpoint endpoint) {
		try {
			Process process = openProcessAndAdaptToLatestPlan(workflow,jId,endpoint,true);
			if (process != null) {
				process.run(null, LogService.WARNING, Collections.emptyMap(),
					false);
				ManagementDAO.updateState(workflow.getId(), jId, Status.DeployingNewPlan, Status.Running);
			} else {
				LOGGER.warning("Original workflow couldn't be retrieved.");
				ManagementDAO.updateState(workflow.getId(), jId, Status.DeployingNewPlan, Status.Failed);
			}
		} catch (OperatorException e) {
			LOGGER.warning("Exception: " + e.getLocalizedMessage());
			ManagementDAO.updateState(workflow.getId(), jId, Status.DeployingNewPlan, Status.Failed);
			throw new RuntimeException(e);
		}
	}

	private Process openProcessAndAdaptToLatestPlan(Workflow workflow, String jId,
													InforeOptimizerStreamingEndpoint endpoint, boolean newPlan){
		if (newPlan && !endpoint.newPlanAvailable()){
			LOGGER.warning("No new plan available.");
			ManagementDAO.updateState(workflow.getId(), jId, Status.DeployingNewPlan, Status.Failed);
			return null;
		}
		if (!newPlan && !endpoint.deployedPlanAvailable()){
			LOGGER.warning("Deployed plan not available.");
			return null;
		}
		Process process = getProcess(workflow.getProcessLocation()); // open process
		if (process == null){
			LOGGER.warning("Original workflow couldn't be retrieved.");
			ManagementDAO.updateState(workflow.getId(), jId, Status.DeployingNewPlan, Status.Failed);
			return null;
		}
		StreamingOptimizationOperator operator =
			(StreamingOptimizationOperator) process.getOperator(endpoint.getStreamingOptimizationOperatorName());
		try {
			operator.initializedInnerPortsFromNetwork(endpoint.getNetwork());
			OptimizerResponse plan;
			if (newPlan){
				plan = endpoint.getNewPlanForDeployment();
			} else{
				plan = endpoint.getDeployedPlan();
			}
			if (plan == null){
				LOGGER.warning("Plan could not be retrieved from endpoint.");
				ManagementDAO.updateState(workflow.getId(), jId, Status.DeployingNewPlan, Status.Failed);
				return null;
			}
			OptimizationHelper.updateSubprocess(operator, plan, workflow.getId(), operator.getAvailableSites());
			operator.setParameter(StreamingOptimizationOperator.PARAMETER_OPERATION_MODE,
				OperationMode.OnlyDeploy.getDescription());
			return process;
		} catch (OperatorCreationException | IOException e) {
			LOGGER.warning("Exception: " + e.getLocalizedMessage());
			ManagementDAO.updateState(workflow.getId(), jId, Status.DeployingNewPlan, Status.Failed);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Stops the job and saves it with a new state into the "DB" (depending on its last known state)
	 * @param wId ID of the workflow that owns the job that is to be stopped
	 * @param job should be in the Stopping state already
	 */
	private void doStopJob(String wId, Job job) {
		Status status = Status.Finished;
		StreamingEndpoint endpoint = job.getEndpoint();
		String jobId = job.getRemoteId();

		switch (job.getType()) {
			case Flink:
				FlinkRestClient flinkClient = new FlinkRestClient(endpoint.getHost(), endpoint.getPort());
				status = flinkClient.stopJob(jobId).getState();
				break;
			case Spark:
				SparkRestClient sparkClient = new SparkRestClient(endpoint.getHost(), endpoint.getPort());
				status = sparkClient.stopJob(jobId).getState();
				break;
			case Maritime:
				try {
					// Retrieve Maritime connection information
					MaritimeStreamingEndpoint mse = (MaritimeStreamingEndpoint) endpoint;
					ConnectionInformationContainerIOObject connContainer =
						(ConnectionInformationContainerIOObject) RepositoryLocation
							.getRepositoryLocationData(mse.getConnectionLocation(), null, ConnectionEntry.class)
							.<ConnectionEntry>locateData()
							.retrieveData(null);

					ConnectionConfiguration maritimeConnConf =
						connContainer.getConnectionInformation().getConfiguration();
					Properties maritimeConfig =
						MaritimeConnectionHandler.getINSTANCE().buildClusterConfiguration(maritimeConnConf);

					// Stop Maritime job on Akka cluster
					MaritimeRestClient maritimeClient = new MaritimeRestClient(
						maritimeConfig.getProperty(PARAMETER_USERNAME),
						maritimeConfig.getProperty(PARAMETER_PASSWORD),
						maritimeConfig.getProperty(PARAMETER_HOST),
						maritimeConfig.getProperty(PARAMETER_PORT),
						Boolean.valueOf(maritimeConfig.getProperty(PARAMETER_ALLOW_SELF_SIGNED_CERT)));
					status = maritimeClient.stopJob(jobId).getState();
				} catch (UserError | RepositoryException ue) {
					LOGGER.log(Level.SEVERE, "Could not stop Maritime job with remote-id: " + jobId, ue);
					status = job.getState();
				}
				break;

			case InforeOptimizer:
				InforeOptimizerStreamingEndpoint optimizerStreamingEndpoint =
					(InforeOptimizerStreamingEndpoint) endpoint;
				try {
					status = optimizerStreamingEndpoint.getOptimizerConnection().cancelOptimizationRequest(
						optimizerStreamingEndpoint.getRequestId(),
						optimizerStreamingEndpoint.getTimeOutSessionConnect()).getState();
				} catch (ExecutionException | InterruptedException | TimeoutException e) {
					LOGGER.log(Level.SEVERE, "Could not stop INFORE Optimizer Request with request-id: " + jobId,
						optimizerStreamingEndpoint.getRequestId());
					status = job.getState();
				}
				break;
		}

		Status newState = Status.Running == status ? Status.Finished : status;
		ManagementDAO.updateState(wId, job.getUniqueId(), Status.Stopping, newState);
	}

	/**
	 * @param type
	 * @param endpoint
	 * @return list of summary objects containing the job states for the endpoint
	 */
	private List<JobSummary> queryJobStatus(Type type, StreamingEndpoint endpoint) {
		LOGGER.fine("Dashboard API: Querying jobs: " + endpoint.getHost() + ":" + endpoint.getPort());
		switch (type) {
			case Flink:
				return new FlinkRestClient(endpoint.getHost(), endpoint.getPort()).getStates();
			case Spark:
				return new SparkRestClient(endpoint.getHost(), endpoint.getPort()).getStates();
			case InforeOptimizer:
				InforeOptimizerStreamingEndpoint optimizerStreamingEndpoint =
					(InforeOptimizerStreamingEndpoint) endpoint;
				Status newStatus = optimizerStreamingEndpoint.isOptimizationFinished() ? Status.Finished :
					Status.Running;
				newStatus = optimizerStreamingEndpoint.newPlanAvailable() ? Status.NewPlanAvailable
					: newStatus;
				return Collections.singletonList(
					new JobSummary(
						optimizerStreamingEndpoint.getRequestId(),
						optimizerStreamingEndpoint.getStreamingOptimizationOperatorName(),
						newStatus
					)
				);
			default:
				return Lists.newArrayList();
		}
	}

	/**
	 * This method updates the "DB" states for the jobs (2nd parameter) based on the summaries (1st parameter)
	 * @param jobSummaries
	 * @param jobs
	 */
	private void updateJobs(List<JobSummary> jobSummaries, List<Job> jobs) {
		Map<String, JobSummary> remoteStates = jobSummaries
			.stream()
			.collect(Collectors.toMap(JobSummary::getId, j -> j));

		for (Job job : jobs) {
			String wId = job.getWorkflowId();
			JobSummary summary = remoteStates.get(job.getRemoteId());
			Status newState = summary == null ? Status.Unknown : summary.getState();

			ManagementDAO.updateState(wId, job.getUniqueId(), job.getState(), newState);
		}
	}

	/**
	 * @param workflow to be adapted
	 * @return the adapted DTO object
	 */
	private WorkflowDTO adapt(Workflow workflow) {
		List<JobDTO> jobs = workflow.getJobs()
			.values()
			.stream()
			.map(this::adapt)
			.collect(Collectors.toList());
		return new WorkflowDTO(
			workflow.getId(),
			workflow.getName(),
			workflow.getState(),
			workflow.getProcessLocation(),
			DateTimeUtil.getUTCDateTime(workflow.getStartTime()).toString(),
			jobs);
	}

	/**
	 * @param job to be adapted
	 * @return the adapted DTO object
	 */
	private JobDTO adapt(Job job) {
		String optimizerResponseInfo = null;
		if (job.getType() == Type.InforeOptimizer){
			optimizerResponseInfo = ((InforeOptimizerStreamingEndpoint) job.getEndpoint()).newPlanInfos();
		}
		return new JobDTO(job.getUniqueId(), job.getName(), job.getType(), job.getState(),
			job.getRemoteDashboardURL(), optimizerResponseInfo);
	}

}