/*
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.spark.translate;

import java.util.Map;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.json.JSONObject;

import com.rapidminer.extension.streaming.raap.RapidMinerModelApplier;
import com.rapidminer.extension.streaming.raap.data.DataType;
import com.rapidminer.extension.streaming.utility.RaaPHandler;
import com.rapidminer.extension.streaming.utility.graph.transform.RapidMinerModelApplierTransformer;


/**
 * Spark specific translator for RapidMiner Model Application
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public class SparkRapidMinerModelApplierTranslator {

	private final Dataset<JSONObject> stream;

	/**
	 * Constructor
	 *
	 * @param stream upstream
	 */
	public SparkRapidMinerModelApplierTranslator(Dataset<JSONObject> stream) {
		this.stream = stream;
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @param rmModelApplierTransformer
	 * @return transformed stream
	 */
	public Dataset<JSONObject> translate(RapidMinerModelApplierTransformer rmModelApplierTransformer) {
		String rmHome = rmModelApplierTransformer.getRmHome();
		String rmUserHome = rmModelApplierTransformer.getRmUserHome();
		String modelFileName = rmModelApplierTransformer.getModelFileName();
		Map<String, DataType> featureTypeMap = rmModelApplierTransformer.getFeatureToType();

		return stream.map(
			new RMModelDeploymentApplier(rmHome, rmUserHome, modelFileName, featureTypeMap),
			Encoders.bean(JSONObject.class));
	}

	/**
	 * This class initializes RapidMiner as a plugin and uses it for applying models
	 */
	public static class RMModelDeploymentApplier implements MapFunction<JSONObject, JSONObject> {

		private final String rmHome;

		private final String rmUserHome;

		private final String modelFileName;

		private final Map<String, DataType> featureTypeMap;

		private transient RapidMinerModelApplier modelApplier;

		public RMModelDeploymentApplier(String rmHome,
										String rmUserHome,
										String modelFileName,
										Map<String, DataType> featureTypeMap) {
			this.rmHome = rmHome;
			this.rmUserHome = rmUserHome;
			this.modelFileName = modelFileName;
			this.featureTypeMap = featureTypeMap;
		}

		@Override
		public JSONObject call(JSONObject value) throws Exception {
			ensureScorer();
			return RaaPHandler.apply(modelApplier, featureTypeMap, value);
		}

		private void ensureScorer() throws Exception {
			if (modelApplier == null) {
				modelApplier = RaaPHandler.initializeApplier(rmHome, rmUserHome, modelFileName);
			}
		}

	}

}