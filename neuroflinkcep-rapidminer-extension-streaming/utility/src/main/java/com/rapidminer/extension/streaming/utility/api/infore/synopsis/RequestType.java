/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.api.infore.synopsis;

import com.fasterxml.jackson.annotation.JsonValue;


/**
 * Available synopsis request types for SDE
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public enum RequestType {

	ADD(1),
	REMOVE(2),
	ESTIMATE(3),
	ADD_WITH_RANDOM_PARTITIONING(4),
	CONTINUOUS(5),
	ADVANCED_ESTIMATE(6),
	// TODO: make use of this later (maybe?)
	UPDATE_SYNOPSIS(7);

	private final int id;

	RequestType(int id) {
		this.id = id;
	}

	@JsonValue
	public int getId() {
		return id;
	}

}