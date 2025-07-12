/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.management.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * DTO for Workflow details
 *
 * @author Mate Torok
 * @since 0.2.0
 */
public class WorkflowDTO {

	@JsonProperty
	private String id;

	@JsonProperty
	private String name;

	@JsonProperty
	private Status state;

	@JsonProperty
	private String processLocation;

	@JsonProperty
	private String startTime;

	@JsonProperty
	private List<JobDTO> jobs;

	public WorkflowDTO() {
		this(null, null, null, null, null, null);
	}

	public WorkflowDTO(String id,
					   String name,
					   Status state,
					   String processLocation,
					   String startTime,
					   List<JobDTO> jobs) {
		this.id = id;
		this.name = name;
		this.state = state;
		this.processLocation = processLocation;
		this.startTime = startTime;
		this.jobs = jobs;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Status getState() {
		return state;
	}

	public String getProcessLocation() {
		return processLocation;
	}

	public String getStartTime() {
		return startTime;
	}

	public List<JobDTO> getJobs() {
		return jobs;
	}

}