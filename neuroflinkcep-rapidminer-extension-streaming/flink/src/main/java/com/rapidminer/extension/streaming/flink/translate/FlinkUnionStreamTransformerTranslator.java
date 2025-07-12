/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.flink.translate;

import java.util.List;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.json.JSONObject;

import com.google.common.collect.Lists;


/**
 * Flink specific translator for Union
 *
 * @author Mate Torok
 * @since 0.3.0
 */
public class FlinkUnionStreamTransformerTranslator {

	private final DataStream<JSONObject> head;

	private final List<DataStream<JSONObject>> tailList;

	public FlinkUnionStreamTransformerTranslator(List<DataStream<JSONObject>> streams) {
		List<DataStream<JSONObject>> copy = Lists.newArrayList(streams);
		this.head = copy.remove(0);
		this.tailList = copy;
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @return union stream
	 */
	@SuppressWarnings("unchecked")
	public DataStream<JSONObject> translate() {
		DataStream<JSONObject> resultSet = head;

		for(DataStream<JSONObject> current : tailList) {
			resultSet = resultSet.union(current);
		}

		return resultSet;
	}

}