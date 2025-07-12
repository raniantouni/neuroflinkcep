/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.spark.translate;

import java.util.List;

import org.apache.spark.sql.Dataset;
import org.json.JSONObject;

import com.google.common.collect.Lists;


/**
 * Spark specific translator for Union
 *
 * @author Mate Torok
 * @since 0.3.0
 */
public class SparkUnionStreamTransformerTranslator {

	private final Dataset<JSONObject> head;

	private final List<Dataset<JSONObject>> tailList;

	public SparkUnionStreamTransformerTranslator(List<Dataset<JSONObject>> streams) {
		List<Dataset<JSONObject>> copy = Lists.newArrayList(streams);
		this.head = copy.remove(0);
		this.tailList = copy;
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @return union stream
	 */
	public Dataset<JSONObject> translate() {
		Dataset<JSONObject> resultSet = head;

		for(Dataset<JSONObject> current : tailList) {
			resultSet = resultSet.union(current);
		}

		return resultSet;
	}

}