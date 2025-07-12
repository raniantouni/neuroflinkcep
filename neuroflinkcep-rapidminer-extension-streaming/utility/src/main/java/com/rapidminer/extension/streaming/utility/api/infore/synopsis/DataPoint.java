/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.api.infore.synopsis;

import java.io.Serializable;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents a data point for the SDE
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class DataPoint implements Serializable {

	@JsonProperty("dataSetkey")
	private final String dataSetKey;

	@JsonProperty("streamID")
	private final String streamId;

	@JsonProperty("values")
	private final Map<String, Object> values;

	DataPoint() {
		this(null, null, null);
	}

	public DataPoint(String dataSetKey, String streamId, Map<String, Object> values) {
		this.dataSetKey = dataSetKey;
		this.streamId = streamId;
		this.values = values;
	}

	public String getDataSetKey() {
		return dataSetKey;
	}

	public String getStreamId() {
		return streamId;
	}

	public Map<String, Object> getValues() {
		return values;
	}

}