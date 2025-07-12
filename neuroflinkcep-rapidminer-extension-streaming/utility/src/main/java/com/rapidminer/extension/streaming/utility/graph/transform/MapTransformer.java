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
 * Representation of the map operation
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class MapTransformer extends Transformer {

	private StreamProducer parent;

	private StreamConsumer child;

	private String key;

	private String newValue;

	MapTransformer() {
		super(-1);
	}

	MapTransformer(Builder builder) {
		super(builder.graph.getNewId());
		this.parent = builder.parent;
		this.key = builder.key;
		this.newValue = builder.newValue;
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

	public String getNewValue() {
		return newValue;
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
		private String newValue;
		private StreamProducer parent;

		public Builder(StreamGraph graph) {
			this.graph = graph;
		}

		public MapTransformer build() {
			MapTransformer node = new MapTransformer(this);
			if (node.parent != null) {
				node.parent.registerChild(node);
			}

			return node;
		}

		public Builder withKey(String key) {
			this.key = key;
			return this;
		}

		public Builder withNewValue(String value) {
			this.newValue = value;
			return this;
		}

		public Builder withParent(StreamProducer parent) {
			this.parent = parent;
			return this;
		}

	}

}