/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.management.db;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;
import com.rapidminer.extension.streaming.deploy.management.api.Status;
import com.rapidminer.extension.streaming.optimizer.connection.OptimizerConnection;


/**
 * "DB" representation of the workflow (comprising an arbitrary number of streaming-jobs)
 *
 * @author Mate Torok
 * @since 0.2.0
 */
public class Workflow {

	@JsonProperty
	private String id;

	@JsonProperty
	private String name;

	@JsonProperty
	private String processLocation;

	@JsonProperty
	private long startTime;

	@JsonProperty
	private Map<String,Job> jobs;

	Workflow() {
		this(null, null, null, -1, Maps.newHashMap());
	}

	public Workflow(Workflow workflow) {
		this(
			workflow.id,
			workflow.name,
			workflow.processLocation,
			workflow.startTime,
			workflow.jobs.entrySet()
				.stream()
				.map(Map.Entry::getValue)
				.collect(Collectors.toMap(
					Job::getUniqueId,
					Job::new
				))
		);
	}

	public Workflow(String id, String name, String processLocation, long startTime, Map<String,Job> jobs) {
		this.id = id;
		this.name = name;
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

	public String getProcessLocation() {
		return processLocation;
	}

	public long getStartTime() {
		return startTime;
	}

	public Status getState() {
		Set<Status> states = jobs.values().stream().map(Job::getState).collect(Collectors.toSet());
		boolean hasUnknown = states.contains(Status.Unknown);
		boolean hasRunning = states.contains(Status.Running);
		boolean hasStopping = states.contains(Status.Stopping);
		boolean hasFinished = states.contains(Status.Finished);
		boolean hasFailed = states.contains(Status.Failed);
		boolean hasNewPlanAvailable = states.contains(Status.NewPlanAvailable);
		boolean hasDeployingNewPlan = states.contains(Status.DeployingNewPlan);

		if (hasRunning) {
			return Status.Running;
		} else if (hasUnknown) {
			return Status.Unknown;
		} else if (hasStopping) {
			return Status.Stopping;
		} else if (hasNewPlanAvailable) {
			return Status.NewPlanAvailable;
		} else if (hasDeployingNewPlan) {
			return Status.DeployingNewPlan;
		} else if (hasFailed) {
			return Status.Failed;
		} else if (hasFinished) {
			return Status.Finished;
		}
		return Status.Unknown;
	}

	public Map<String,Job> getJobs() {
		return jobs;
	}
}