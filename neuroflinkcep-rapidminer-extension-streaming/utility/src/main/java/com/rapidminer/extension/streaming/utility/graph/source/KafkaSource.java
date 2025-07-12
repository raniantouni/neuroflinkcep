/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.graph.source;

import java.util.Properties;

import com.rapidminer.extension.streaming.utility.graph.StreamConsumer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamGraphNodeVisitor;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;
import com.rapidminer.extension.streaming.utility.graph.StreamSource;


/**
 * Representation of the Kafka source operation
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class KafkaSource implements StreamSource, StreamProducer {

	private final long id;

	private StreamConsumer child;

	private String topic;

	private boolean startFromEarliest;

	private Properties config;

	KafkaSource() {
		this.id = -1;
	}

	KafkaSource(Builder builder) {
		this.id = builder.graph.getNewId();
		this.topic = builder.topic;
		this.config = builder.config;
		this.startFromEarliest = builder.startFromEarliest;
	}

	public StreamConsumer getChild() {
		return child;
	}

	public String getTopic() {
		return topic;
	}

	public Properties getConfig() {
		return config;
	}

	public boolean startFromEarliest() {
		return startFromEarliest;
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
	public void accept(StreamGraphNodeVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public void registerChild(StreamConsumer child) {
		this.child = child;
	}

	/**
	 * Builder for the operation
	 */
	public static class Builder {

		private final StreamGraph graph;
		private String topic;
		private Properties config;
		private boolean startFromEarliest = false;

		public Builder(StreamGraph graph) {
			this.graph = graph;
		}

		public KafkaSource build() {
			return new KafkaSource(this);
		}

		public Builder withTopic(String topic) {
			this.topic = topic;
			return this;
		}

		public Builder withConfiguration(Properties config) {
			this.config = config;
			return this;
		}

		public Builder withStartFromEarliest(boolean value) {
			this.startFromEarliest = value;
			return this;
		}

	}

}