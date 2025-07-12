package com.rapidminer.extension.streaming.utility.api.infore.maritime;/*
 * Copyright (C) 2016-2022 RapidMiner GmbH
 */

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * @author Fabian Temme
 * @since
 */
public class FusionKafkaConfiguration extends KafkaConfiguration {

	@JsonProperty("in")
	private final List<TopicConfiguration> in;

	FusionKafkaConfiguration() {
		this(null, null);
	}

	public FusionKafkaConfiguration(List<TopicConfiguration> in, TopicConfiguration out) {
		super(out);
		this.in = in;
	}
}
