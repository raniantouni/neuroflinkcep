/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.management.api;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * DTO for Job details
 *
 * @author Mate Torok
 * @since 0.2.0
 */
public class JobDTO {

	@JsonProperty
	private String id;

	@JsonProperty
	private String name;

	@JsonProperty
	private Type type;

	@JsonProperty
	private Status state;

	@JsonProperty
	private String remoteDashboardURL;

	@JsonProperty
	private String optimizerResponseInfo;

	JobDTO() {
		this(null, null, null, null, null, null);
	}

	public JobDTO(String id, String name, Type type, Status state, String remoteDashboardURL,
				  String optimizerResponseInfo) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.state = state;
		this.remoteDashboardURL = remoteDashboardURL;
		this.optimizerResponseInfo = optimizerResponseInfo;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	public Status getState() {
		return state;
	}

	public String getRemoteDashboardURL() {
		return remoteDashboardURL;
	}

	public String getOptimizerResponseInfo() {
		return optimizerResponseInfo;
	}
}