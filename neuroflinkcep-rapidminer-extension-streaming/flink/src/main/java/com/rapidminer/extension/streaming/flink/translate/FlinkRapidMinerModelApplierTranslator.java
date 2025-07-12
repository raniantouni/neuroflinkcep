/*
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.flink.translate;

import java.util.Map;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.json.JSONObject;

import com.rapidminer.extension.streaming.raap.RapidMinerModelApplier;
import com.rapidminer.extension.streaming.raap.data.DataType;
import com.rapidminer.extension.streaming.utility.RaaPHandler;
import com.rapidminer.extension.streaming.utility.graph.transform.RapidMinerModelApplierTransformer;


/**
 * Flink specific translator for RapidMiner Model Application
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public class FlinkRapidMinerModelApplierTranslator {

	private final DataStream<JSONObject> stream;

	/**
	 * Constructor
	 *
	 * @param stream upstream
	 */
	public FlinkRapidMinerModelApplierTranslator(DataStream<JSONObject> stream) {
		this.stream = stream;
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @param rmModelApplierTransformer
	 * @return transformed stream
	 */
	public DataStream<JSONObject> translate(RapidMinerModelApplierTransformer rmModelApplierTransformer) {
		String rmHome = rmModelApplierTransformer.getRmHome();
		String rmUserHome = rmModelApplierTransformer.getRmUserHome();
		String modelFileName = rmModelApplierTransformer.getModelFileName();
		Map<String, DataType> featureTypeMap = rmModelApplierTransformer.getFeatureToType();

		return stream.map(new RMModelDeploymentApplier(rmHome, rmUserHome, modelFileName, featureTypeMap));
	}

	/**
	 * This class initializes RapidMiner as a plugin and uses it for applying models
	 */
	private static class RMModelDeploymentApplier extends RichMapFunction<JSONObject, JSONObject> {

		private final String rmHome;

		private final String rmUserHome;

		private final String modelFileName;

		private final Map<String, DataType> featureTypeMap;

		private RapidMinerModelApplier modelApplier;

		RMModelDeploymentApplier(String rmHome,
										String rmUserHome,
										String modelFileName,
										Map<String, DataType> featureTypeMap) {
			this.rmHome = rmHome;
			this.rmUserHome = rmUserHome;
			this.modelFileName = modelFileName;
			this.featureTypeMap = featureTypeMap;
		}

		@Override
		public void open(Configuration parameters) throws Exception {
			modelApplier = RaaPHandler.initializeApplier(rmHome, rmUserHome, modelFileName);
		}

		@Override
		public JSONObject map(JSONObject value) throws Exception {
			return RaaPHandler.apply(modelApplier, featureTypeMap, value);
		}

	}

}