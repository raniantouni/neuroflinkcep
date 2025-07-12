/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.api.infore.maritime;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * MarineTraffic (Akka) cluster request for job initiation
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class JobRequest implements Serializable {

	@JsonProperty("type")
	private final JobType type;

	@JsonProperty("kafka")
	private final KafkaConfiguration kafka;

	JobRequest() {
		this(null, null, null);
	}

	public JobRequest(JobType type, TopicConfiguration in, TopicConfiguration out) {
		this.type = type;
		this.kafka = new EventDetectionKafkaConfiguration(in, out);
	}

	public JobRequest(JobType type, KafkaConfiguration kafkaConfiguration) {
		this.type = type;
		this.kafka = kafkaConfiguration;
	}

}