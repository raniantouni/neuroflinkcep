/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.api.infore.maritime;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonValue;


/**
 * Available Maritime job types
 *
 * @author Mate Torok
 * @since 0.6.0
 */
public enum JobType {

	EVENTS("events"),

	FUSION("fusion");

	private final String text;

	JobType(String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return text;
	}

	@JsonValue
	public String getText() {
		return text;
	}

	/**
	 * String to enum
	 * @param text representation of an enum
	 * @return enum for the parameter
	 */
	public static JobType fromString(String text) {
		for (JobType jobType : JobType.values()) {
			if (text.equals(jobType.text)) {
				return jobType;
			}
		}
		throw new IllegalArgumentException(
			String.format(
				"Provided string (%s) does not match with any known Maritime job-type. Allowed types are: %s",
				text,
				Arrays.toString(JobType.values())));
	}

}