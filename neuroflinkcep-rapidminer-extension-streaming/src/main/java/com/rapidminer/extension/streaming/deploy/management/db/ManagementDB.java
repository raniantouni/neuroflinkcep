/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.management.db;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Maps;


/**
 * Representing the "data-base" for storing workflow information for management
 *
 * @author Mate Torok
 * @since 0.2.0
 */
class ManagementDB {

	@JsonProperty
	private Map<String, Workflow> workflows = Maps.newHashMap();

	/**
	 * @return list of workflows (deep copies)
	 */
	List<Workflow> getWorkflows() {
		return workflows.values()
			.stream()
			.map(Workflow::new)
			.collect(Collectors.toList());
	}

	/**
	 * @param id
	 * @return deep copy of the workflow that has the parameter as ID or null
	 */
	Workflow getWorkflow(String id) {
		Workflow workflow = workflows.get(id);
		return workflow == null ? null : new Workflow(workflow);
	}

	/**
	 * Removes the workflow that has the parameter as ID
	 * @param id
	 */
	void removeWorkflow(String id) {
		workflows.remove(id);
	}

	/**
	 * Puts the deep copy of the parameter into the local store
	 * @param workflow
	 */
	void addOrUpdate(Workflow workflow) {
		workflows.put(workflow.getId(), new Workflow(workflow));
	}

	/**
	 * Puts the deep copy of the parameter (job) into the workflow that has the parameter (workflowId) as ID
	 * @param workflowId
	 * @param job
	 */
	void addOrUpdate(String workflowId, Job job) {
		Workflow workflow = workflows.get(workflowId);

		if (workflow != null) {
			workflow.getJobs().put(job.getUniqueId(), new Job(job));
		}
	}

	void removeJob(String workflowId, Job job){
		Workflow workflow = workflows.get(workflowId);

		if (workflow != null) {
			workflow.getJobs().remove(job.getUniqueId());
		}
	}

}