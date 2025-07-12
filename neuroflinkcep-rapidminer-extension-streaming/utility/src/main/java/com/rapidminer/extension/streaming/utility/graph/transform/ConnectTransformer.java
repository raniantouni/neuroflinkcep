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
 * Representation of the connect operation
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class ConnectTransformer extends Transformer {

	private StreamProducer controlParent;

	private StreamProducer dataParent;

	private StreamConsumer child;

	private String controlKey;

	private String dataKey;

	ConnectTransformer() {
		super(-1);
	}

	ConnectTransformer(Builder builder) {
		super(builder.graph.getNewId());
		this.controlParent = builder.controlParent;
		this.dataParent = builder.dataParent;
		this.controlKey = builder.controlKey;
		this.dataKey = builder.dataKey;
	}

	public StreamProducer getControlParent() {
		return controlParent;
	}

	public StreamProducer getDataParent() {
		return dataParent;
	}

	public StreamConsumer getChild() {
		return child;
	}

	public String getControlKey() {
		return controlKey;
	}

	public String getDataKey() {
		return dataKey;
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
		private StreamProducer controlParent;
		private StreamProducer dataParent;
		private String controlKey;
		private String dataKey;

		public Builder(StreamGraph graph) {
			this.graph = graph;
		}

		public ConnectTransformer build() {
			ConnectTransformer node = new ConnectTransformer(this);
			if (node.controlParent != null) {
				node.controlParent.registerChild(node);
			}

			if (node.dataParent != null) {
				node.dataParent.registerChild(node);
			}

			return node;
		}

		public Builder withControlKey(String key) {
			this.controlKey = key;
			return this;
		}

		public Builder withDataKey(String value) {
			this.dataKey = value;
			return this;
		}

		public Builder withControlParent(StreamProducer parent) {
			this.controlParent = parent;
			return this;
		}

		public Builder withDataParent(StreamProducer parent) {
			this.dataParent = parent;
			return this;
		}

	}

}