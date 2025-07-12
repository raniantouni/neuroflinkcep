/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.management;

import com.rapidminer.extension.streaming.deploy.management.api.Status;


/**
 * Summary/state of a job (remote platform)
 *
 * @author Mate Torok
 * @since 0.2.0
 */
public class JobSummary {

	private String id;

	private String name;

	private Status state;

	public JobSummary(String id, String name, Status state) {
		this.id = id;
		this.name = name;
		this.state = state;
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

}