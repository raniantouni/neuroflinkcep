/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.graph.transform;

import java.util.Map;

import com.rapidminer.extension.streaming.raap.data.DataType;
import com.rapidminer.extension.streaming.utility.graph.StreamConsumer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamGraphNodeVisitor;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;


/**
 * Representation of the RapidMiner Model Applier operation
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public class RapidMinerModelApplierTransformer extends Transformer {

	private StreamProducer parent;

	private StreamConsumer child;

	private Map<String, DataType> featureToType;

	private String modelFileName;

	private String rmHome;

	private String rmUserHome;

	RapidMinerModelApplierTransformer() {
		super(-1);
	}

	RapidMinerModelApplierTransformer(Builder builder) {
		super(builder.graph.getNewId());
		this.parent = builder.parent;
		this.featureToType = builder.featureToType;
		this.modelFileName = builder.modelFileName;
		this.rmHome = builder.rmHome;
		this.rmUserHome = builder.rmUserHome;
	}

	public StreamProducer getParent() {
		return parent;
	}

	public StreamConsumer getChild() {
		return child;
	}

	public Map<String, DataType> getFeatureToType() {
		return featureToType;
	}

	public String getModelFileName() {
		return modelFileName;
	}

	public String getRmHome() {
		return rmHome;
	}

	public String getRmUserHome() {
		return rmUserHome;
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
		private Map<String, DataType> featureToType;
		private String modelFileName;
		private String rmHome;
		private String rmUserHome;
		private StreamProducer parent;

		public Builder(StreamGraph graph) {
			this.graph = graph;
		}

		public RapidMinerModelApplierTransformer build() {
			RapidMinerModelApplierTransformer node = new RapidMinerModelApplierTransformer(this);
			if (node.parent != null) {
				node.parent.registerChild(node);
			}

			return node;
		}

		public Builder withModelFileName(String modelPath) {
			this.modelFileName = modelPath;
			return this;
		}

		public Builder withFeatures(Map<String, DataType> featureToType) {
			this.featureToType = featureToType;
			return this;
		}

		public Builder withRMHome(String rmHome) {
			this.rmHome = rmHome;
			return this;
		}

		public Builder withRMUserHome(String rmUserHome) {
			this.rmUserHome = rmUserHome;
			return this;
		}

		public Builder withParent(StreamProducer parent) {
			this.parent = parent;
			return this;
		}

	}

}