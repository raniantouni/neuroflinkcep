/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.api.infore.maritime;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maritime specific configuration for interfacing with Kafka
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class EventDetectionKafkaConfiguration extends KafkaConfiguration {

	@JsonProperty("in")
	private final TopicConfiguration in;


	EventDetectionKafkaConfiguration() {
		this(null, null);
	}

	public EventDetectionKafkaConfiguration(TopicConfiguration in, TopicConfiguration out) {
		super(out);
		this.in = in;
	}

}