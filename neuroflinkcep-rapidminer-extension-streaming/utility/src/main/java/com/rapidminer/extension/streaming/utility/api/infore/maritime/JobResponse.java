/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.api.infore.maritime;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * MarineTraffic (Akka) cluster job initiation response
 *
 * @author Mate Torok
 * @since 0.6.0
 */
public class JobResponse implements Serializable {

	@JsonProperty("id")
	private final String id;

	JobResponse() {
		this(null);
	}

	JobResponse(String id) {
		this.id = id;
	}

	public String getJobId() {
		return id;
	}

}