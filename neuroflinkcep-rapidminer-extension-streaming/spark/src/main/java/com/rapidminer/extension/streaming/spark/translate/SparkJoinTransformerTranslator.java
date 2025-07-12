/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.spark.translate;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.functions;
import org.json.JSONObject;

import com.rapidminer.extension.streaming.utility.graph.transform.JoinTransformer;

import scala.Tuple2;
import scala.Tuple3;


/**
 * Spark specific translator for Join
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class SparkJoinTransformerTranslator {

	private static final String LEFT = "left";

	private static final String LEFT_OBJ = "left_obj";

	private static final String LEFT_TIME_KEY = "l_processing_time";

	private static final String LEFT_WINDOW = "l_window";

	private static final String RIGHT = "right";

	private static final String RIGHT_OBJ = "right_obj";

	private static final String RIGHT_TIME_KEY = "r_processing_time";

	private static final String RIGHT_WINDOW = "r_window";

	private static final String NEW_KEY = "key";

	private static final String OBJECT = "obj";

	private static final String WATERMARK = "0 seconds";

	private final Dataset<JSONObject> leftStream;

	private final Dataset<JSONObject> rightStream;

	public SparkJoinTransformerTranslator(Dataset<JSONObject> leftStream, Dataset<JSONObject> rightStream) {
		this.leftStream = leftStream;
		this.rightStream = rightStream;
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @param joinTransformer configurations for the transformation
	 * @return transformed stream
	 */
	public Dataset<JSONObject> translate(JoinTransformer joinTransformer) {
		String leftKey = joinTransformer.getLeftKey();
		String rightKey = joinTransformer.getRightKey();

		String newLeftKey = "left_" + leftKey;
		String newRightKey = "right_" + rightKey;

		String windowThreshold = joinTransformer.getWindowLength() + " seconds";

		Dataset<Row> leftRow = leftStream
			.map(getRowMapper(leftKey), Encoders.tuple(Encoders.STRING(), Encoders.kryo(JSONObject.class)))
			.toDF(newLeftKey, OBJECT)
			.withColumn(LEFT_TIME_KEY, functions.current_timestamp())
			.withColumn(LEFT_WINDOW, functions.window(functions.col(LEFT_TIME_KEY), windowThreshold))
			.withWatermark(LEFT_TIME_KEY, WATERMARK);

		Dataset<Row> rightRow = rightStream
			.map(getRowMapper(rightKey), Encoders.tuple(Encoders.STRING(), Encoders.kryo(JSONObject.class)))
			.toDF(newRightKey, OBJECT)
			.withColumn(RIGHT_TIME_KEY, functions.current_timestamp())
			.withColumn(RIGHT_WINDOW, functions.window(functions.col(RIGHT_TIME_KEY), windowThreshold))
			.withWatermark(RIGHT_TIME_KEY, WATERMARK);

		return leftRow
			.joinWith(rightRow, functions.expr(String.format("%s == %s AND %s == %s", LEFT_WINDOW, RIGHT_WINDOW, newLeftKey, newRightKey)))
			.toDF(LEFT, RIGHT)
			.withColumn(NEW_KEY, functions.expr(String.format("%s.%s", LEFT, newLeftKey)))
			.withColumn(LEFT_OBJ, functions.expr(String.format("%s.%s", LEFT, OBJECT)))
			.withColumn(RIGHT_OBJ, functions.expr(String.format("%s.%s", RIGHT, OBJECT)))
			.select(functions.col(NEW_KEY), functions.col(LEFT_OBJ), functions.col(RIGHT_OBJ))
			.as(Encoders.tuple(Encoders.STRING(), Encoders.kryo(JSONObject.class), Encoders.kryo(JSONObject.class)))
			.map(getResultMapper(), Encoders.kryo(JSONObject.class));
	}

	/**
	 * @param key
	 * @return the mapper that transforms an object into the 'key' and 'obj'
	 */
	private MapFunction<JSONObject, Tuple2<String, JSONObject>> getRowMapper(String key) {
		return (MapFunction<JSONObject, Tuple2<String, JSONObject>>) obj ->
			new Tuple2<>(obj.getString(key), obj);
	}

	/**
	 * @return the mapper that transforms the row into the result object
	 */
	private MapFunction<Tuple3<String, JSONObject, JSONObject>, JSONObject> getResultMapper() {
		return (MapFunction<Tuple3<String, JSONObject, JSONObject>, JSONObject>) tuple ->
			new JSONObject()
				.put(NEW_KEY, tuple._1())
				.put(LEFT, tuple._2().toMap())
				.put(RIGHT, tuple._3().toMap());
	}

}