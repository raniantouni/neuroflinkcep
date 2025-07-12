package com.rapidminer.extension.streaming.utility.api.infore.maritime;/*
 * Copyright (C) 2016-2022 RapidMiner GmbH
 */

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * @author Fabian Temme
 * @since 0.6.2
 */
public abstract class KafkaConfiguration {

	@JsonProperty("out")
	private final TopicConfiguration out;

	KafkaConfiguration() {
		this(null);
	}

	public KafkaConfiguration(TopicConfiguration out) {
		this.out = out;
	}
}
