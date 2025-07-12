/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.api.infore.onlineml;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Class for storing pre-processors and learner properties of a Request
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class ModelConfiguration implements Serializable {

	@JsonProperty("name")
	private final String name;

	@JsonProperty("hyperParameters")
	private final Map<String, Object> hyperParameters;

	@JsonProperty("parameters")
	private final Map<String, Object> parameters;

	public ModelConfiguration(String name, Map<String, Object> hyperParameters, Map<String, Object> parameters) {
		this.name = name;
		this.hyperParameters = ensureParameters(hyperParameters);
		this.parameters = ensureParameters(parameters);
	}

	/**
	 * Ensures the integrity of the configuration
	 *
	 * @param input
	 * @return cleaned up input
	 */
	private Map<String, Object> ensureParameters(Map<String, Object> input) {
		if (input == null) {
			return null;
		}

		// Remove potential empty entry
		input.remove("");

		return input.isEmpty() ? null : input;
	}

}
