/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.management.db.infore;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rapidminer.extension.streaming.deploy.management.db.StreamingEndpoint;


/**
 * Represents the endpoint for a Maritime job
 *
 * @author Mate Torok
 * @since 0.6.0
 */
public class MaritimeStreamingEndpoint extends StreamingEndpoint {

	@JsonProperty
	private String connectionLocation;

	public MaritimeStreamingEndpoint() {
		this(null);
	}

	public MaritimeStreamingEndpoint(String connectionLocation) {
		super(null, null);
		this.connectionLocation = connectionLocation;
	}

	public String getConnectionLocation() {
		return connectionLocation;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MaritimeStreamingEndpoint that = (MaritimeStreamingEndpoint) o;
		return Objects.equals(this.connectionLocation, that.connectionLocation);
	}

	@Override
	public int hashCode() {
		return Objects.hash(connectionLocation);
	}

}