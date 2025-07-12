/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.management.db;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rapidminer.extension.streaming.deploy.management.api.Type;
import com.rapidminer.extension.streaming.deploy.management.api.Status;
import com.rapidminer.extension.streaming.deploy.management.db.infore.InforeOptimizerStreamingEndpoint;


/**
 * "DB" representation of a job
 *
 * @author Mate Torok
 * @since 0.2.0
 */
public class Job {

	@JsonProperty
	private String workflowId;

	@JsonProperty
	private String remoteId;

	@JsonProperty
	private String uniqueId;

	@JsonProperty
	private StreamingEndpoint endpoint;

	@JsonProperty
	private String name;

	@JsonProperty
	private Type type;

	@JsonProperty
	private boolean monitorable;

	@JsonProperty
	private Status state;

	@JsonProperty
	private String remoteDashboardURL;

	Job() {
		this(null, null, null, null, null, null, false, null, null);
	}

	public Job(Job other) {
		this(other.workflowId,
			other.uniqueId,
			other.remoteId,
			other.endpoint,
			other.name,
			other.type,
			other.monitorable,
			other.state,
			other.remoteDashboardURL);
	}

	public Job(String workflowId,
			   String uniqueId,
			   String remoteId,
			   StreamingEndpoint endpoint,
			   String name,
			   Type type,
			   boolean monitorable,
			   Status state,
			   String remoteDashboardURL) {
		this.workflowId = workflowId;
		this.uniqueId = uniqueId;
		this.remoteId = remoteId;
		this.endpoint = endpoint;
		this.name = name;
		this.type = type;
		this.monitorable = monitorable;
		this.state = state;
		this.remoteDashboardURL = remoteDashboardURL;
	}

	public String getWorkflowId() {
		return workflowId;
	}

	public String getRemoteId() {
		return remoteId;
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public StreamingEndpoint getEndpoint() {
		return endpoint;
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	public boolean isMonitorable() {
		return monitorable;
	}

	public Status getState() {
		return state;
	}

	public String getRemoteDashboardURL() {
		return remoteDashboardURL;
	}

}