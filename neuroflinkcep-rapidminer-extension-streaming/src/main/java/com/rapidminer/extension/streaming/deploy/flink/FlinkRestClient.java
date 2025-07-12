/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.flink;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.flink.runtime.jobgraph.JobStatus;
import org.apache.flink.runtime.messages.webmonitor.MultipleJobsDetails;
import org.apache.flink.runtime.webmonitor.handlers.JarRunRequestBody;
import org.apache.flink.runtime.webmonitor.handlers.JarRunResponseBody;
import org.apache.flink.runtime.webmonitor.handlers.JarUploadResponseBody;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.rapidminer.extension.streaming.deploy.RestClient;
import com.rapidminer.extension.streaming.deploy.management.api.Status;
import com.rapidminer.extension.streaming.deploy.management.JobSummary;
import com.rapidminer.tools.LogService;


/**
 * REST client for communicating with a Flink cluster (JAR upload, job submission, etc.)
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class FlinkRestClient {

	private static final Logger LOGGER = LogService.getRoot();

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final ContentType JAR_CONTENT_TYPE = ContentType.create("application/x-java-archive");

	private static final String URL_JOBS_OVERVIEW = "jobs/overview";

	private static final String URL_UPLOAD = "jars/upload";

	private static final String URL_RUN_TEMPLATE = "jars/%s/run";

	private static final String URL_JOB_STOP = "jobs/%s";

	private final ConcurrentMap<String, HttpUriRequest> activeRequests = Maps.newConcurrentMap();

	private final RestClient restClient;

	/**
	 * Constructor for creating a Flink cluster client for the given host and port
	 *
	 * @param host
	 * @param port
	 */
	public FlinkRestClient(String host, String port) {
		this.restClient = new RestClient(host, port);
	}

	/**
	 * Uploads a JAR to the Flink cluster and initiates (submits) the job contained by that JAR
	 *
	 * @param jarPath     file system path to the fat-JAR
	 * @param args        list of arguments, e.g.: <b>(--parallelism,8,--clusterAddr,localhost:9092)</b>
	 * @param parallelism of the Flink job
	 * @param mainClass entry point for the Flink job
	 * @return jobId of the Flink job
	 * @throws IOException
	 */
	public String uploadAndSubmit(String jarPath, List<String> args, int parallelism, String mainClass) throws IOException {
		LOGGER.fine("Uploading & Submitting job for JAR: " + jarPath);

		// Upload JAR
		ImmutablePair<HttpUriRequest, Future<HttpEntity>> uploadDetails = uploadJarAsync(jarPath);
		HttpUriRequest uploadReq = uploadDetails.getLeft();
		Future<HttpEntity> uploadResp = uploadDetails.getRight();
		String uuid = UUID.randomUUID().toString();

		// Register task
		activeRequests.put(uuid, uploadReq);

		// Wait for result
		String jarId;
		try {
			jarId =
				new File(
					MAPPER
					.readValue(uploadResp.get().getContent(), JarUploadResponseBody.class)
					.getFilename()).getName();
		} catch (ExecutionException | InterruptedException e) {
			throw new IOException("Error while uploading JAR", e.getCause());
		} finally {
			// Remove registered task
			activeRequests.remove(uuid);
		}

		// Submit job (sync)
		return submitJob(jarId, args, parallelism, mainClass);
	}

	/**
	 * Queries the remote endpoint for job states
	 * @return see above
	 */
	public List<JobSummary> getStates() {
		LOGGER.fine("Retrieving job states");
		try {
			HttpEntity response = restClient.get(URL_JOBS_OVERVIEW, "Unsuccessful overview query");
			return MAPPER
				.readValue(response.getContent(), MultipleJobsDetails.class)
				.getJobs()
				.stream()
				.map(job -> new JobSummary(job.getJobId().toString(), job.getJobName(), adaptStatus(job.getStatus())))
				.collect(Collectors.toList());
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Could not get states", e);
			return Lists.newArrayList();
		}
	}

	/**
	 * Tries to queries the remote endpoint for job states
	 * @return see above
	 */
	public boolean isReachable() {
		LOGGER.fine("Retrieving Spark state");
		try {
			HttpEntity response = restClient.get(URL_JOBS_OVERVIEW, "Unsuccessful overview query");
			return true;
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Could not connect to server", e);
			return false;
		}
	}

	/**
	 * Terminates a job
	 *
	 * @param id remote job-ID
	 * @return status of the job before stopping
	 */
	public JobSummary stopJob(String id) {
		LOGGER.fine("Stopping job: " + id);
		JobSummary summary = new JobSummary(id, null, Status.Unknown);
		try {
			summary = getStates()
				.stream()
				.filter(jobSummary -> StringUtils.equals(jobSummary.getId(), id))
				.findFirst()
				.orElse(summary);
			String url = String.format(URL_JOB_STOP, id);
			restClient.patch(url, "Unsuccessful job termination");
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Could not terminate job", e);
		}
		return summary;
	}

	/**
	 * Aborts all running requests (asynchronous ones: upload tasks)
	 */
	public void abort() {
		// Iterate through active requests and abort them one after another
		for (HttpUriRequest request : activeRequests.values()) {
			try {
				request.abort();
			} catch (Exception ex) {
				LOGGER.log(Level.WARNING, "Request could not be aborted, URI: " + request.getURI().toString(), ex);
			}
		}
	}

	/**
	 * Uploads a JAR to the Flink cluster
	 *
	 * @param jarPath
	 * @return pair of request - response future
	 */
	private ImmutablePair<HttpUriRequest, Future<HttpEntity>> uploadJarAsync(String jarPath) {
		LOGGER.fine("Uploading JAR: " + jarPath);

		// Build multi-part body with file
		File jarFile = new File(jarPath);
		HttpEntity entity = MultipartEntityBuilder
			.create()
			.addBinaryBody("jarFile", jarFile, JAR_CONTENT_TYPE, jarFile.getName())
			.build();

		// Send and process response (get JarId)
		return restClient.postAsync(URL_UPLOAD, entity, "Unsuccessful job upload");
	}

	/**
	 * Submits a job with the given parameters
	 *
	 * @param jarId
	 * @param args        list of arguments, e.g.: <b>(--parallelism,8,--clusterAddr,localhost:9092)</b>
	 * @param parallelism
	 * @param mainClass
	 * @return jobId for the submitted job returned by Flink
	 * @throws IOException
	 */
	private String submitJob(String jarId, List<String> args, int parallelism, String mainClass) throws IOException {
		LOGGER.fine("Submitting job for JAR: " + jarId);
		StringEntity requestEntity;
		JarRunRequestBody requestBody = new JarRunRequestBody(mainClass, null, args, parallelism, null, null, null);

		// Serializing request into JSON
		try {
			String requestStr = MAPPER.writerFor(JarRunRequestBody.class).writeValueAsString(requestBody);
			requestEntity = new StringEntity(requestStr);
		} catch (JsonProcessingException e) {
			LOGGER.log(Level.SEVERE, "Could not submit job for JAR: " + jarId, e);
			throw new IOException(e);
		}

		// Send and process response
		String url = String.format(URL_RUN_TEMPLATE, jarId);
		HttpEntity response = restClient.post(url, requestEntity, "Unsuccessful job submission");

		return MAPPER
			.readValue(response.getContent(), JarRunResponseBody.class)
			.getJobId()
			.toString();
	}

	/**
	 * @param status Flink specific job status
	 * @return our domain specific representation for status
	 */
	private Status adaptStatus(JobStatus status) {
		switch (status) {
			case FAILED:
				return Status.Failed;
			case RUNNING:
			case CANCELLING:
			case RESTARTING:
			case RECONCILING:
			case FAILING:
				return Status.Running;
			case CANCELED:
			case SUSPENDED:
			case FINISHED:
				return Status.Finished;
			case CREATED:
			default:
				return Status.Unknown;
		}
	}

}