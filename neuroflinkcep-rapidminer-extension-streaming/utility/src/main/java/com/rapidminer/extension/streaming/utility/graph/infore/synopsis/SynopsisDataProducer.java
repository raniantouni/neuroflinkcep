/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.graph.infore.synopsis;

import com.rapidminer.extension.streaming.utility.graph.StreamConsumer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamGraphNodeVisitor;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;
import com.rapidminer.extension.streaming.utility.graph.StreamSink;


/**
 * Representation of the SDE data producer. This node is responsible for providing upstream data to the SDE.
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class SynopsisDataProducer extends BaseSynopsisNode implements StreamSink, StreamConsumer {

	private StreamProducer parent;

	SynopsisDataProducer() {
		super(-1);
	}

	SynopsisDataProducer(Builder builder) {
		super(builder);
		this.parent = builder.parent;
	}

	@Override
	public void accept(StreamGraphNodeVisitor visitor) {
		visitor.visit(this);
	}

	public StreamProducer getParent() {
		return parent;
	}

	/**
	 * Builder for the operation
	 */
	public static class Builder extends BaseSynopsisNode.Builder<Builder> {

		private StreamProducer parent;

		public Builder(StreamGraph graph) {
			super(graph);
		}

		public Builder withParent(StreamProducer parent) {
			this.parent = parent;
			return this;
		}

		public SynopsisDataProducer build() {
			SynopsisDataProducer node = new SynopsisDataProducer(this);
			if (node.parent != null) {
				node.parent.registerChild(node);
			}
			return node;
		}

		@Override
		protected Builder self() {
			return this;
		}

	}

}