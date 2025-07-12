/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.graph.transform;

import com.rapidminer.extension.streaming.utility.graph.StreamConsumer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamGraphNodeVisitor;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;


/**
 * Representation of the timestamp operation
 *
 * @author Mate Torok
 * @since 0.5.0
 */
public class TimestampTransformer extends Transformer {

	private StreamProducer parent;

	private StreamConsumer child;

	private String key;

	private String format;

	private boolean useUnixTime;

	TimestampTransformer() {
		super(-1);
	}

	TimestampTransformer(Builder builder) {
		super(builder.graph.getNewId());
		this.parent = builder.parent;
		this.key = builder.key;
		this.format = builder.format;
		this.useUnixTime = builder.useUnixTime;
	}

	public StreamProducer getParent() {
		return parent;
	}

	public StreamConsumer getChild() {
		return child;
	}

	public String getKey() {
		return key;
	}

	public String getFormat() {
		return format;
	}

	public boolean isUseUnixTime() {
		return useUnixTime;
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
		private String key;
		private String format;
		private boolean useUnixTime;
		private StreamProducer parent;

		public Builder(StreamGraph graph) {
			this.graph = graph;
		}

		public TimestampTransformer build() {
			TimestampTransformer node = new TimestampTransformer(this);
			if (node.parent != null) {
				node.parent.registerChild(node);
			}

			return node;
		}

		public Builder withKey(String key) {
			this.key = key;
			return this;
		}

		public Builder withFormat(String format) {
			this.format = format;
			return this;
		}

		public Builder withUseUnixTime(boolean useUnixTime) {
			this.useUnixTime = useUnixTime;
			return this;
		}

		public Builder withParent(StreamProducer parent) {
			this.parent = parent;
			return this;
		}

	}

}