/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.graph.transform;

import java.util.Set;

import com.rapidminer.extension.streaming.utility.graph.StreamConsumer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamGraphNodeVisitor;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;


/**
 * Representation of the select operation
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class SelectTransformer extends Transformer {

	private StreamProducer parent;

	private StreamConsumer child;

	private Set<String> keys;

	private boolean flatten;

	SelectTransformer() {
		super(-1);
	}

	SelectTransformer(Builder builder) {
		super(builder.graph.getNewId());
		this.parent = builder.parent;
		this.keys = builder.keys;
		this.flatten = builder.flatten;
	}

	public StreamProducer getParent() {
		return parent;
	}

	public StreamConsumer getChild() {
		return child;
	}

	public Set<String> getKeys() {
		return keys;
	}

	public boolean flatten() {
		return flatten;
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
		private Set<String> keys;
		private boolean flatten = false;
		private StreamProducer parent;

		public Builder(StreamGraph graph) {
			this.graph = graph;
		}

		public SelectTransformer build() {
			SelectTransformer node = new SelectTransformer(this);
			if (node.parent != null) {
				node.parent.registerChild(node);
			}

			return node;
		}

		public Builder withKeys(Set<String> keys) {
			this.keys = keys;
			return this;
		}

		public Builder withFlatten(boolean flatten) {
			this.flatten = flatten;
			return this;
		}

		public Builder withParent(StreamProducer parent) {
			this.parent = parent;
			return this;
		}

	}

}