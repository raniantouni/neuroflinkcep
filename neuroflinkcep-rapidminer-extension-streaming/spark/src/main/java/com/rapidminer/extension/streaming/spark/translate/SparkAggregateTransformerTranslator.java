/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.spark.translate;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.functions;
import org.json.JSONObject;

import com.rapidminer.extension.streaming.utility.graph.transform.AggregateTransformer;

import scala.Tuple2;


/**
 * Spark specific translator for Aggregate
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class SparkAggregateTransformerTranslator {

	private final Dataset<JSONObject> stream;

	public SparkAggregateTransformerTranslator(Dataset<JSONObject> stream) {
		this.stream = stream;
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @param aggregateTransformer configurations for the transformation
	 * @return transformed stream
	 */
	public Dataset<JSONObject> translate(AggregateTransformer aggregateTransformer) {
		String key = aggregateTransformer.getKey();
		String valueKey = aggregateTransformer.getValueKey();
		String timeKey = "processing_time";
		String watermarkThreshold = "0 seconds";
		String windowThreshold = aggregateTransformer.getWindowLength() + " seconds";

		AggregateTransformer.Function aggrFunc = aggregateTransformer.getFunction();
		String resultKey = String.format("%s-%s", aggrFunc.toString(), valueKey);

		return stream
			.map(getRowMapper(key, valueKey), Encoders.tuple(Encoders.STRING(), Encoders.DOUBLE()))
			.toDF(key, valueKey)
			.withColumn(timeKey, functions.current_timestamp())
			.withWatermark(timeKey, watermarkThreshold)
			.groupBy(functions.window(functions.col(timeKey), windowThreshold), functions.col(key))
			.agg(getAggregation(aggrFunc, valueKey).as(resultKey))
			.map(getResultMapper(key, resultKey), Encoders.kryo(JSONObject.class));
	}

	/**
	 * @param func
	 * @param colName to be aggregated
	 * @return the correct aggregation column for the enum
	 */
	private Column getAggregation(AggregateTransformer.Function func, String colName) {
		switch (func) {
			case AVG:
				return functions.avg(colName);
			case MAX:
				return functions.max(colName);
			case MIN:
				return functions.min(colName);
			case SUM:
				return functions.sum(colName);
			case COUNT:
			default:
				return functions.count(colName);
		}
	}

	/**
	 * @param key
	 * @param valueKey
	 * @return the mapper that transforms an object into the 'key' and 'value'
	 */
	private MapFunction<JSONObject, Tuple2<String, Double>> getRowMapper(String key, String valueKey) {
		return (MapFunction<JSONObject, Tuple2<String, Double>>) obj ->
			new Tuple2<>(obj.getString(key), obj.getDouble(valueKey));
	}

	/**
	 * @param key
	 * @param resultKey
	 * @return the mapper that transforms the row into the result object
	 */
	private MapFunction<Row, JSONObject> getResultMapper(String key, String resultKey) {
		return (MapFunction<Row, JSONObject>) row ->
			new JSONObject()
				.put(key, row.get(row.fieldIndex(key)))
				.put(resultKey, row.get(row.fieldIndex(resultKey)));
	}

}