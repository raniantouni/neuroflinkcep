/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.spark.translate;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.spark.sql.Dataset;
import org.json.JSONObject;


/**
 * Spark specific translator for Duplicate
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class SparkDuplicateStreamTransformerTranslator {

	private final Dataset<JSONObject> stream;

	public SparkDuplicateStreamTransformerTranslator(Dataset<JSONObject> stream) {
		this.stream = stream;
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @return duplicated stream(s)
	 */
	public Pair<Dataset<JSONObject>, Dataset<JSONObject>> translate() {
		return ImmutablePair.of(stream, stream);
	}

}