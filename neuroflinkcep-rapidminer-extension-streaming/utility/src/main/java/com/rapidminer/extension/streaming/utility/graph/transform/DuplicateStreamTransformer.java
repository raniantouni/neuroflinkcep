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
 * Representation of the duplicate operation
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class DuplicateStreamTransformer extends Transformer {

	private StreamProducer parent;

	private StreamConsumer leftChild;

	private StreamConsumer rightChild;

	DuplicateStreamTransformer() {
		super(-1);
	}

	DuplicateStreamTransformer(Builder builder) {
		super(builder.graph.getNewId());
		this.parent = builder.parent;
	}

	public StreamProducer getParent() {
		return parent;
	}

	public StreamConsumer getLeftChild() {
		return leftChild;
	}

	public StreamConsumer getRightChild() {
		return rightChild;
	}

	@Override
	public void accept(StreamGraphNodeVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public void registerChild(StreamConsumer child) {
		if (this.leftChild == null) {
			this.leftChild = child;
		} else {
			this.rightChild = child;
		}
	}

	/**
	 * Builder for the operation
	 */
	public static class Builder {

		private final StreamGraph graph;
		private StreamProducer parent;

		public Builder(StreamGraph graph) {
			this.graph = graph;
		}

		public DuplicateStreamTransformer build() {
			DuplicateStreamTransformer node = new DuplicateStreamTransformer(this);
			if (node.parent != null) {
				node.parent.registerChild(node);
			}

			return node;
		}

		public Builder withParent(StreamProducer parent) {
			this.parent = parent;
			return this;
		}

	}

}