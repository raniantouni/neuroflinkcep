/*
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.api.livy;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Apache-Livy Batch
 *
 * @author Mate Torok
 * @since 0.2.0
 */
public class Batch {

	@JsonProperty("id")
	private final int id;

	@JsonProperty("name")
	private final String name;

	@JsonProperty("state")
	private final String state;

	Batch() {
		this(-1, null, null);
	}

	public Batch(int id, String name, String state) {
		this.id = id;
		this.name = name;
		this.state = state;
	}

	public String getId() {
		return String.valueOf(id);
	}

	public String getName() {
		return name;
	}

	public String getState() {
		return state;
	}

}