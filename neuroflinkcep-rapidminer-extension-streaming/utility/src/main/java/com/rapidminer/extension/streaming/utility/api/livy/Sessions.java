/*
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.api.livy;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Apache-Livy sessions
 *
 * @author Mate Torok
 * @since 0.2.0
 */
public class Sessions {

	@JsonProperty("sessions")
	private List<Batch> sessions;

	public List<Batch> getSessions() {
		return sessions;
	}

}