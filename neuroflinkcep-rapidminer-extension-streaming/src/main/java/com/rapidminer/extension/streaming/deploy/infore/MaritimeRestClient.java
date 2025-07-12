/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.infore;

import static com.rapidminer.extension.streaming.deploy.infore.MaritimeConstants.API_JOB;
import static com.rapidminer.extension.streaming.deploy.infore.MaritimeConstants.API_LOGIN;
import static com.rapidminer.extension.streaming.deploy.infore.MaritimeConstants.HEADER_AUTHORIZATION_KEY;
import static com.rapidminer.extension.streaming.deploy.infore.MaritimeConstants.HEADER_AUTHORIZATION_VALUE_PATTERN;
import static com.rapidminer.extension.streaming.utility.JsonUtil.toJson;
import static java.lang.String.format;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import com.rapidminer.extension.streaming.deploy.RestClient;
import com.rapidminer.extension.streaming.deploy.management.JobSummary;
import com.rapidminer.extension.streaming.deploy.management.api.Status;
import com.rapidminer.extension.streaming.utility.JsonUtil;
import com.rapidminer.extension.streaming.utility.api.infore.maritime.JobRequest;
import com.rapidminer.extension.streaming.utility.api.infore.maritime.JobResponse;
import com.rapidminer.extension.streaming.utility.api.infore.maritime.JwtRequest;
import com.rapidminer.extension.streaming.utility.api.infore.maritime.JwtResponse;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.LogService;


/**
 * REST client for communicating with a Maritime cluster
 *
 * @author Mate Torok
 * @since 0.6.0
 */
public class MaritimeRestClient {

	private static final Logger LOGGER = LogService.getRoot();

	private final RestClient restClient;

	private final JwtRequest loginRequest;

	/**
	 * Constructor for creating a Maritime cluster client for the given host and port (https)
	 *  @param username
	 * @param password
	 * @param host
	 * @param port
	 * @param allowSelfSignedCert
	 */
	public MaritimeRestClient(String username, String password, String host, String port, Boolean allowSelfSignedCert) {
		this.restClient = new RestClient(true, allowSelfSignedCert, host, port);
		this.loginRequest = new JwtRequest(username, password);
	}

	/**
	 * Starts a job
	 * @param jobRequest job parameters
	 * @return job-id
	 * @throws OperatorException if something goes wrong (e.g.: wrong credentials)
	 */
	public String startJob(JobRequest jobRequest) throws OperatorException {
		LOGGER.fine("Starting job");

		// 1. login
		login();

		// 2. initiate job
		try {
			HttpEntity resp = restClient.post(
				API_JOB,
				new StringEntity(toJson(jobRequest), ContentType.APPLICATION_JSON),
				"Could not initiate AKKA job");
			return JsonUtil.fromString(restClient.getResponseBody(resp), JobResponse.class).getJobId();
		} catch (IOException e) {
			throw new OperatorException("Error while starting the Maritime job", e);
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

		try {
			// 1. login
			login();

			// 2. delete job
			restClient.delete(API_JOB + "/" + id, "Could not remove AKKA job");

			return new JobSummary(id, null, Status.Finished);
		} catch (IOException | OperatorException e) {
			LOGGER.log(Level.SEVERE, "Could not terminate job", e);
			return new JobSummary(id, null, Status.Unknown);
		}
	}

	/**
	 * This method logs the user in and sets up the underlying REST client for further, authenticated communication
	 * @throws OperatorException if anything goes wrong during authentication
	 */
	private void login() throws OperatorException {
		try {
			HttpEntity authResp = restClient.post(
				API_LOGIN,
				new StringEntity(toJson(loginRequest), ContentType.APPLICATION_JSON),
				"Could not login into AKKA cluster");

			String jwt = JsonUtil.fromString(restClient.getResponseBody(authResp), JwtResponse.class).getJwt();

			// Set auth header for the underlying client
			restClient.setHeader(HEADER_AUTHORIZATION_KEY, format(HEADER_AUTHORIZATION_VALUE_PATTERN, jwt));
		} catch (IOException e) {
			throw new OperatorException("Error while logging into the Maritime cluster", e);
		}
	}

}