/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.graph.infore.synopsis;

import java.util.Properties;

import com.rapidminer.extension.streaming.utility.api.infore.synopsis.SynopsisType;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamGraphNode;


/**
 * Base for Synopsis graph nodes
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public abstract class BaseSynopsisNode implements StreamGraphNode {

	private final long id;

	private String topic;

	private Properties config;

	private String dataSetKey;

	private SynopsisType synopsis;

	private long uId;

	private String streamIdKey;

	private long parallelism;

	private String[] parameters;

	BaseSynopsisNode(long id) {
		this.id = id;
	}

	BaseSynopsisNode(Builder<?> builder) {
		this.id = builder.graph.getNewId();
		this.topic = builder.topic;
		this.config = builder.config;
		this.dataSetKey = builder.dataSetKey;
		this.synopsis = builder.synopsis;
		this.uId = builder.uId;
		this.streamIdKey = builder.streamIdKey;
		this.parallelism = builder.parallelism;
		this.parameters = builder.parameters;
	}

	@Override
	public long getId() {
		return id;
	}

	public String getTopic() {
		return topic;
	}

	public Properties getConfig() {
		return config;
	}

	public String getDataSetKey() {
		return dataSetKey;
	}

	public SynopsisType getSynopsis() {
		return synopsis;
	}

	public long getUId() {
		return uId;
	}

	public String getStreamIdKey() {
		return streamIdKey;
	}

	public long getParallelism() {
		return parallelism;
	}

	public String[] getParameters() {
		return parameters;
	}

	/**
	 * Builder for the operation
	 */
	public static abstract class Builder<T extends Builder> {

		protected final StreamGraph graph;
		protected String topic;
		protected Properties config;
		protected String dataSetKey;
		protected SynopsisType synopsis;
		protected long uId;
		protected String streamIdKey;
		protected long parallelism;
		protected String[] parameters;

		public Builder(StreamGraph graph) {
			this.graph = graph;
		}

		public T withTopic(String topic) {
			this.topic = topic;
			return self();
		}

		public T withConfiguration(Properties config) {
			this.config = config;
			return self();
		}

		public T withDataSetKey(String dataSetKey) {
			this.dataSetKey = dataSetKey;
			return self();
		}

		public T withSynopsis(SynopsisType synopsis) {
			this.synopsis = synopsis;
			return self();
		}

		public T withUId(long uId) {
			this.uId = uId;
			return self();
		}

		public T withStreamIdKey(String streamIdKey) {
			this.streamIdKey = streamIdKey;
			return self();
		}

		public T withParallelism(long parallelism) {
			this.parallelism = parallelism;
			return self();
		}

		public T withParameters(String[] parameters) {
			this.parameters = parameters;
			return self();
		}

		protected abstract T self();

	}

}