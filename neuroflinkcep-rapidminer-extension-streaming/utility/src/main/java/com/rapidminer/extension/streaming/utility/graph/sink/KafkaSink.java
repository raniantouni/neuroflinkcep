/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.graph.sink;

import java.util.Properties;

import com.rapidminer.extension.streaming.utility.graph.StreamConsumer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamGraphNodeVisitor;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;
import com.rapidminer.extension.streaming.utility.graph.StreamSink;


/**
 * Representation of the Kafka sink operation
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class KafkaSink implements StreamSink, StreamConsumer {

	private final long id;

	private StreamProducer parent;

	private String topic;

	private String key;

	private Properties config;

	KafkaSink() {
		this.id = -1;
	}

	KafkaSink(Builder builder) {
		this.id = builder.graph.getNewId();
		this.parent = builder.parent;
		this.topic = builder.topic;
		this.key = builder.key;
		this.config = builder.config;
	}

	public String getTopic() {
		return topic;
	}

	public String getKey() {
		return key;
	}

	public Properties getConfig() {
		return config;
	}

	public StreamProducer getParent() {
		return parent;
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
	public void accept(StreamGraphNodeVisitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Builder for the operation
	 */
	public static class Builder {

		private final StreamGraph graph;
		private String topic;
		private String key;
		private Properties config;
		private StreamProducer parent;

		public Builder(StreamGraph graph) {
			this.graph = graph;
		}

		public KafkaSink build() {
			KafkaSink node = new KafkaSink(this);
			if (node.parent != null) {
				node.parent.registerChild(node);
			}

			return node;
		}

		public Builder withTopic(String topic) {
			this.topic = topic;
			return this;
		}

		public Builder withKey(String key) {
			this.key = key;
			return this;
		}

		public Builder withConfiguration(Properties config) {
			this.config = config;
			return this;
		}

		public Builder withParent(StreamProducer parent) {
			this.parent = parent;
			return this;
		}

	}

}