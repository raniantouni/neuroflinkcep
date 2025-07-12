/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.api.infore.synopsis;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents a request to the SDE
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class Request implements Serializable {

	@JsonProperty("dataSetkey")
	private final String dataSetKey;

	@JsonProperty("requestID")
	private final RequestType requestType;

	@JsonProperty("synopsisID")
	private final SynopsisType synopsisType;

	@JsonProperty("uid")
	private final long uId;

	@JsonProperty("streamID")
	private final String streamId;

	@JsonProperty("param")
	private final String[] parameters;

	@JsonProperty("noOfP")
	private final long parallelism;

	Request() {
		this(null, RequestType.REMOVE, SynopsisType.AMS, -1, null, null, -1);
	}

	public Request(
		String dataSetKey,
		RequestType requestType,
		SynopsisType synopsisType,
		long uId,
		String streamId,
		String[] parameters,
		long parallelism) {
		this.dataSetKey = dataSetKey;
		this.requestType = requestType;
		this.synopsisType = synopsisType;
		this.uId = uId;
		this.streamId = streamId;
		this.parameters = parameters;
		this.parallelism = parallelism;
	}

	public String getDataSetKey() {
		return dataSetKey;
	}

	public RequestType getRequestType() {
		return requestType;
	}

	public SynopsisType getSynopsisType() {
		return synopsisType;
	}

	public long getuId() {
		return uId;
	}

	public String getStreamId() {
		return streamId;
	}

	public String[] getParameters() {
		return parameters;
	}

	public long getParallelism() {
		return parallelism;
	}

}