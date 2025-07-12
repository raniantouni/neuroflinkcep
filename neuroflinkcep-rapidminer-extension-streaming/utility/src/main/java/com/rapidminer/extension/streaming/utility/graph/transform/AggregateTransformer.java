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
 * Representation of the aggregation operation
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class AggregateTransformer extends Transformer {

	public enum Function {MIN, SUM, COUNT, MAX, AVG}

	private StreamProducer parent;

	private StreamConsumer child;

	private String key;

	private String valueKey;

	private long windowLength = 1;

	private Function function = Function.SUM;

	AggregateTransformer() {
		super(-1);
	}

	AggregateTransformer(Builder builder) {
		super(builder.graph.getNewId());
		this.parent = builder.parent;
		this.key = builder.key;
		this.valueKey = builder.valueKey;
		this.windowLength = builder.windowLength;
		this.function = builder.function;
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

	public String getValueKey() {
		return valueKey;
	}

	public long getWindowLength() {
		return windowLength;
	}

	public Function getFunction() {
		return function;
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
		private long windowLength;
		private StreamProducer parent;
		private Function function;
		private String valueKey;

		public Builder(StreamGraph graph) {
			this.graph = graph;
		}

		public AggregateTransformer build() {
			AggregateTransformer node = new AggregateTransformer(this);
			if (node.parent != null) {
				node.parent.registerChild(node);
			}

			return node;
		}

		public Builder withKey(String key) {
			this.key = key;
			return this;
		}

		public Builder withWindowLength(long windowLength) {
			this.windowLength = windowLength;
			return this;
		}

		public Builder withParent(StreamProducer parent) {
			this.parent = parent;
			return this;
		}

		public Builder withFunction(Function function) {
			this.function = function;
			return this;
		}

		public Builder withValueKey(String valueKey) {
			this.valueKey = valueKey;
			return this;
		}
	}

}