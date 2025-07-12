/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.api.infore.maritime;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Maritime specific configuration for Kafka topics
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class TopicConfiguration {

	@JsonProperty("brokers")
	private final String brokers;

	@JsonProperty("topic")
	private final String topic;

	TopicConfiguration() {
		this(null, null);
	}

	public TopicConfiguration(String brokers, String topic) {
		this.brokers = brokers;
		this.topic = topic;
	}

}