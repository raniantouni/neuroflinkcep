/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.management.db;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents the endpoint for a job
 *
 * @author Mate Torok
 * @since 0.2.0
 */
public class StreamingEndpoint {

	@JsonProperty
	private String host;

	@JsonProperty
	private String port;

	public StreamingEndpoint() {
		this(null, null);
	}

	public StreamingEndpoint(String host, String port) {
		this.host = host;
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public String getPort() {
		return port;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		StreamingEndpoint that = (StreamingEndpoint) o;
		return Objects.equals(host, that.host) &&
			Objects.equals(port, that.port);
	}

	@Override
	public int hashCode() {
		return Objects.hash(host, port);
	}

}