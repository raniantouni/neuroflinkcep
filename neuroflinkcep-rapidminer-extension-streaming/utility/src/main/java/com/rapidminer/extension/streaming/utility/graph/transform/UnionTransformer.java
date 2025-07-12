/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.graph.transform;

import java.util.List;

import com.rapidminer.extension.streaming.utility.graph.StreamConsumer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamGraphNodeVisitor;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;


/**
 * Representation of the union operation
 *
 * @author Mate Torok
 * @since 0.3.0
 */
public class UnionTransformer extends Transformer {

	private List<StreamProducer> parents;

	private StreamConsumer child;

	UnionTransformer() {
		super(-1);
	}

	UnionTransformer(Builder builder) {
		super(builder.graph.getNewId());
		this.parents = builder.parents;
	}

	public List<StreamProducer> getParents() {
		return parents;
	}

	public StreamConsumer getChild() {
		return child;
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
		private List<StreamProducer> parents;

		public Builder(StreamGraph graph) {
			this.graph = graph;
		}

		public UnionTransformer build() {
			UnionTransformer node = new UnionTransformer(this);
			if (node.parents != null) {
				node.parents.forEach(parent -> parent.registerChild(node));
			}

			return node;
		}

		public Builder withParents(List<StreamProducer> parents) {
			this.parents = parents;
			return this;
		}

	}

}