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
 * Representation of the stringify operation for fields.
 * This allows fields in a document to be replaced by their string equivalent.
 * <br/>
 * Let's say in the following document we want to replace the "estimation" JSON nested object with its string equivalent:
 * <br/>
 * <b>from</b>:
 * <ul>
 *     <li>{..., "estimation" : "{"t": 1}", ...}</li>
 * </ul>
 * <b>to</b>:
 * <ul>
 *     <li>{..., "estimation" : "{\"t\": 1}", ...}</li>
 * </ul>
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StringifyFieldTransformer extends Transformer {

	private StreamProducer parent;

	private StreamConsumer child;

	private Set<String> keys;

	StringifyFieldTransformer() {
		super(-1);
	}

	StringifyFieldTransformer(Builder builder) {
		super(builder.graph.getNewId());
		this.parent = builder.parent;
		this.keys = builder.keys;
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
		private StreamProducer parent;

		public Builder(StreamGraph graph) {
			this.graph = graph;
		}

		public StringifyFieldTransformer build() {
			StringifyFieldTransformer node = new StringifyFieldTransformer(this);
			if (node.parent != null) {
				node.parent.registerChild(node);
			}

			return node;
		}

		public Builder withKeys(Set<String> keys) {
			this.keys = keys;
			return this;
		}

		public Builder withParent(StreamProducer parent) {
			this.parent = parent;
			return this;
		}

	}

}