/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.api.infore.onlineml;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Request for communicating with the OML
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class Request implements Serializable {

	/**
	 * Types of requests that can be sent to OML
	 */
	public enum Type {Create}

	@JsonProperty("request")
	private final Type type;

	@JsonProperty("id")
	private final int mlPipelineId;

	@JsonProperty("learner")
	private final ModelConfiguration learner;

	@JsonProperty("preProcessors")
	private final List<ModelConfiguration> preProcessors;

	@JsonProperty("trainingConfiguration")
	private final Map<String, Object> trainingConfiguration;

	public Request(
		Type type,
		int mlPipelineId,
		ModelConfiguration learner,
		List<ModelConfiguration> preProcessors,
		Map<String, Object> trainingConfiguration) {
		this.type = type;
		this.mlPipelineId = mlPipelineId;
		this.learner = learner;
		this.preProcessors = preProcessors;
		this.trainingConfiguration = trainingConfiguration;
	}

}