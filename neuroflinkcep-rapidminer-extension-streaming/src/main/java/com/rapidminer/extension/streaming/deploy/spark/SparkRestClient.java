/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.spark;

import static com.rapidminer.extension.streaming.utility.JsonUtil.toJson;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.rapidminer.extension.streaming.deploy.RestClient;
import com.rapidminer.extension.streaming.deploy.management.api.Status;
import com.rapidminer.extension.streaming.deploy.management.JobSummary;
import com.rapidminer.extension.streaming.utility.JsonUtil;
import com.rapidminer.extension.streaming.utility.api.livy.Batch;
import com.rapidminer.extension.streaming.utility.api.livy.Sessions;
import com.rapidminer.extension.streaming.utility.api.livy.SubmitRequest;
import com.rapidminer.tools.LogService;


/**
 * REST client for communicating with a Spark cluster (job submission, etc.).
 * Further info: <b>Apache Livy</b>
 *
 * @author Mate Torok
 * @since 0.2.0
 */
public class SparkRestClient {

	private static final Logger LOGGER = LogService.getRoot();

	private static final Set<String> LIVY_BLACKLISTED_PROPERTIES = Sets.newHashSet("spark.master");

	private static final String URL_LIVY_BATCHES = "batches";

	private static final String URL_LIVY_STOP_BATCH = "batches/%s";

	private final RestClient restClient;

	/**
	 * Constructor for creating a Spark (Apache Livy) cluster client for the given host and port
	 *
	 * @param host
	 * @param port
	 */
	public SparkRestClient(String host, String port) {
		this.restClient = new RestClient(host, port);
	}

	/**
	 * Submits a job via Apache-Livy REST API
	 * @param hdfsJarDst
	 * @param name
	 * @param config
	 * @param args
	 * @return job-ID
	 */
	public String submit(String hdfsJarDst, String name, Properties config, List<String> args) throws IOException {
		// Build configuration map without Apache-Livy "blacklisted" configs
		Map<String, String> conf = config.entrySet()
			.stream()
			.filter(entry -> !LIVY_BLACKLISTED_PROPERTIES.contains(entry.getKey()))
			.collect(Collectors.toMap(
				entry -> (String)entry.getKey(),
				entry -> (String)entry.getValue()
			));

		// Submit job
		SubmitRequest request = new SubmitRequest(hdfsJarDst, SparkConstants.SPARK_MAIN_CLASS, name, args, conf);
		HttpEntity resp = restClient.post(
			URL_LIVY_BATCHES,
			new StringEntity(toJson(request), ContentType.APPLICATION_JSON),
			"Could not initiate job via Apache-Livy");

		String responseStr = restClient.getResponseBody(resp);
		Batch response = JsonUtil.fromString(responseStr, Batch.class);

		LOGGER.info("Spark submission: " + responseStr);
		return response.getId();
	}

	/**
	 * Queries the remote endpoint for job states
	 * @return see above
	 */
	public List<JobSummary> getStates() {
		LOGGER.fine("Retrieving job states");
		try {
			HttpEntity response = restClient.get(URL_LIVY_BATCHES, "Unsuccessful overview query");
			return JsonUtil.fromString(response.getContent(), Sessions.class)
				.getSessions()
				.stream()
				.map(batch -> new JobSummary(batch.getId(), batch.getName(), getStatus(batch.getState())))
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
			HttpEntity response = restClient.get(URL_LIVY_BATCHES, "Unsuccessful overview query");
			return true;
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Could not connect to server", e);
			return false;
		}
	}

	/**
	 * Terminates job
	 * @param id
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
			String url = String.format(URL_LIVY_STOP_BATCH, id);
			restClient.delete(url, "Unsuccessful job termination");
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Could not terminate job", e);
		}
		return summary;
	}

	/**
	 * @param status Spark specific job status
	 * @return our domain specific representation for status
	 */
	private Status getStatus(String status) {
		switch (status) {
			case "error":
				return Status.Failed;
			case "running":
			case "waiting":
			case "cancelling":
				return Status.Running;
			case "cancelled":
				return Status.Finished;
			case "available":
			default:
				return Status.Unknown;
		}
	}

}