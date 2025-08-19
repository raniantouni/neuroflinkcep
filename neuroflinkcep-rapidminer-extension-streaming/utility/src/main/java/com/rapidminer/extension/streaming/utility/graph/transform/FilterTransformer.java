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

import java.util.Map;


/**
 * Representation of the filter operation
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class FilterTransformer extends Transformer {

	public enum Operator {EQUAL, NOT_EQUAL, GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, STARTS_WITH}

	private StreamProducer parent;

	private StreamConsumer child;

	private String key;

	private Operator operator;

	private String value;

	private Boolean neuroFlinkMode;

	private Map<String, Map<String, String>> parsedPredicates;

	FilterTransformer() {
		super(-1);
	}

	FilterTransformer(Builder builder) {
		super(builder.graph.getNewId());
		this.parent = builder.parent;
		this.key = builder.key;
		this.operator = builder.operator;
		this.value = builder.value;
		this.neuroFlinkMode = builder.neuroFlinkMode;
		this.parsedPredicates = builder.parsedPredicates;

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

	public Operator getOperator() {
		return operator;
	}

	public Boolean getNeuroFlinkMode() {
		return neuroFlinkMode;
	}

	public Map<String, Map<String, String>> getParsedPredicates() {
		return parsedPredicates;
	}

	public String getValue() {
		return value;
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
		private String value;
		private StreamProducer parent;
		private Operator operator = Operator.EQUAL;
		private Boolean neuroFlinkMode;
		private Map<String, Map<String, String>> parsedPredicates;

		public Builder(StreamGraph graph) {
			this.graph = graph;
		}

		public FilterTransformer build() {
			FilterTransformer node = new FilterTransformer(this);
			if (node.parent != null) {
				node.parent.registerChild(node);
			}

			return node;
		}

		public Builder withKey(String key) {
			this.key = key;
			return this;
		}
		public Builder withPredicates(Map<String, Map<String, String>> parsedPredicates) {
			this.parsedPredicates = parsedPredicates; return this;
		}
		public Builder withMode(Boolean neuroFlinkMode) {
			this.neuroFlinkMode = neuroFlinkMode;
			return this;
		}
		public Builder withValue(String value) {
			this.value = value;
			return this;
		}

		public Builder withParent(StreamProducer parent) {
			this.parent = parent;
			return this;
		}

		public Builder withOperator(Operator operator) {
			this.operator = operator;
			return this;
		}

	}

}