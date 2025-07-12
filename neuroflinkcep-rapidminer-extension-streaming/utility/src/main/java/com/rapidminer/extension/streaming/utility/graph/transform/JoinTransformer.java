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
 * Representation of the join operation
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class JoinTransformer extends Transformer {

	private StreamProducer leftParent;

	private StreamProducer rightParent;

	private StreamConsumer child;

	private String leftKey;

	private String rightKey;

	private long windowLength = 1;

	JoinTransformer() {
		super(-1);
	}

	JoinTransformer(Builder builder) {
		super(builder.graph.getNewId());
		this.leftParent = builder.leftParent;
		this.rightParent = builder.rightParent;
		this.leftKey = builder.leftKey;
		this.rightKey = builder.rightKey;
	}

	public StreamProducer getLeftParent() {
		return leftParent;
	}

	public StreamProducer getRightParent() {
		return rightParent;
	}

	public StreamConsumer getChild() {
		return child;
	}

	public String getLeftKey() {
		return leftKey;
	}

	public String getRightKey() {
		return rightKey;
	}

	public long getWindowLength() {
		return windowLength;
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
		private String leftKey;
		private String rightKey;
		private StreamProducer leftParent;
		private StreamProducer rightParent;
		private long windowLength;

		public Builder(StreamGraph graph) {
			this.graph = graph;
		}

		public JoinTransformer build() {
			JoinTransformer node = new JoinTransformer(this);
			if (node.leftParent != null) {
				node.leftParent.registerChild(node);
			}

			if (node.rightParent != null) {
				node.rightParent.registerChild(node);
			}

			return node;
		}

		public Builder withLeftKey(String key) {
			this.leftKey = key;
			return this;
		}

		public Builder withRightKey(String value) {
			this.rightKey = value;
			return this;
		}

		public Builder withLeftParent(StreamProducer parent) {
			this.leftParent = parent;
			return this;
		}

		public Builder withRightParent(StreamProducer parent) {
			this.rightParent = parent;
			return this;
		}

		public Builder withWindowLength(long windowLength) {
			this.windowLength = windowLength;
			return this;
		}

	}

}