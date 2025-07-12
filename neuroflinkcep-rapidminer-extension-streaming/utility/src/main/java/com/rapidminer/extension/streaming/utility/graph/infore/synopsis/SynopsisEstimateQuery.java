/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.graph.infore.synopsis;

import com.rapidminer.extension.streaming.utility.api.infore.synopsis.RequestType;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamGraphNodeVisitor;
import com.rapidminer.extension.streaming.utility.graph.StreamSource;


/**
 * Representation of the SDE query estimation emitter. This is a self-contained, single unit doing periodic
 * estimate-request emission.
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class SynopsisEstimateQuery extends BaseSynopsisNode implements StreamSource {

	private long frequency;

	private RequestType requestType;

	SynopsisEstimateQuery() {
		super(-1);
	}

	SynopsisEstimateQuery(Builder builder) {
		super(builder);
		this.frequency = builder.frequency;
		this.requestType = builder.requestType;
	}

	public long getFrequency() {
		return frequency;
	}

	public RequestType getRequestType() {
		return requestType;
	}

	@Override
	public void accept(StreamGraphNodeVisitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Builder for the operation
	 */
	public static class Builder extends BaseSynopsisNode.Builder<Builder> {

		private long frequency;

		public RequestType requestType = RequestType.ADD;

		public Builder(StreamGraph graph) {
			super(graph);
		}

		public Builder withFrequency(long frequency) {
			this.frequency = frequency;
			return this;
		}

		public Builder withRequestType(RequestType requestType) {
			this.requestType = requestType;
			return this;
		}

		public SynopsisEstimateQuery build() {
			return new SynopsisEstimateQuery(this);
		}

		@Override
		protected Builder self() {
			return this;
		}

	}

}