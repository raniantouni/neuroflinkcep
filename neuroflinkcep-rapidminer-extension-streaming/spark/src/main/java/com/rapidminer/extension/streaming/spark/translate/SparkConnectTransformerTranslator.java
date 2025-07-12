/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.spark.translate;

import java.util.List;
import java.util.Map;

import org.apache.spark.api.java.function.FlatMapGroupsWithStateFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.streaming.GroupStateTimeout;
import org.apache.spark.sql.streaming.OutputMode;
import org.json.JSONObject;

import com.google.common.collect.Lists;
import com.rapidminer.extension.streaming.utility.graph.transform.ConnectTransformer;

import scala.Tuple3;


/**
 * Spark specific translator for Connect
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class SparkConnectTransformerTranslator {

	private static final String CONTROL_TYPE = "control";

	private static final String DATA_TYPE = "data";

	private final Dataset<JSONObject> controlStream;

	private final Dataset<JSONObject> dataStream;

	public SparkConnectTransformerTranslator(Dataset<JSONObject> controlStream, Dataset<JSONObject> dataStream) {
		this.controlStream = controlStream;
		this.dataStream = dataStream;
	}

	/**
	 * @return lambda for retrieving the key from the enrichment tuple
	 */
	private static MapFunction<Tuple3<String, String, JSONObject>, String> enrichmentKey() {
		return (MapFunction<Tuple3<String, String, JSONObject>, String>)
			tuple -> tuple._1();
	}

	/**
	 * @param key
	 * @param type
	 * @return lambda for enriching a object (with key and type)
	 */
	private static MapFunction<JSONObject, Tuple3<String, String, JSONObject>> enrichData(String key, String type) {
		return (MapFunction<JSONObject, Tuple3<String, String, JSONObject>>)
			data -> Tuple3.apply(data.getString(key), type, data);
	}

	/**
	 * @return lambda that executes stateful processing on the unified data(-batches)
	 */
	private static FlatMapGroupsWithStateFunction<String, Tuple3<String, String, JSONObject>, JSONObject, JSONObject> connectorFunction() {
		return (FlatMapGroupsWithStateFunction<String, Tuple3<String, String, JSONObject>, JSONObject, JSONObject>)
			(key, values, state) -> {
				JSONObject controlObj = state.exists() ? state.get() : null;
				List<JSONObject> dataObjects = Lists.newArrayListWithCapacity(16);

				// Collect all objects + update control object to the latest
				while (values.hasNext()) {
					Tuple3<String, String, JSONObject> enrichedObject = values.next();
					JSONObject object = enrichedObject._3();
					// Branch based on event type (control or data)
					if (CONTROL_TYPE.equals(enrichedObject._2())) {
						controlObj = object;
						state.update(object);
					} else {
						dataObjects.add(object);
					}
				}

				// Connect control object to data objects
				if (controlObj != null) {
					final Map<String, Object> controlMap = controlObj.toMap();
					dataObjects
						.stream()
						.forEach(dataObj -> joinObjects(dataObj, controlMap));
				}

				return dataObjects.iterator();
			};
	}

	/**
	 * Puts every key-value pair from 'src' into the 'target' whose key is not already present there
	 *
	 * @param target
	 * @param src
	 */
	private static void joinObjects(JSONObject target, Map<String, Object> src) {
		for (Map.Entry<String, Object> entry : src.entrySet()) {
			String key = entry.getKey();
			if (!target.has(key)) {
				target.put(key, entry.getValue());
			}
		}
	}

	/**
	 * Executes transformations on the stream
	 *
	 * @param connectTransformer configurations for the transformation
	 * @return transformed stream
	 */
	public Dataset<JSONObject> translate(ConnectTransformer connectTransformer) {
		// Enrich control stream
		Dataset<Tuple3<String, String, JSONObject>> enrichedControlStream = controlStream
			.map(
				enrichData(connectTransformer.getControlKey(), CONTROL_TYPE),
				Encoders.tuple(Encoders.STRING(), Encoders.STRING(), Encoders.kryo(JSONObject.class)));

		// Enrich data stream
		Dataset<Tuple3<String, String, JSONObject>> enrichedDataStream = dataStream
			.map(
				enrichData(connectTransformer.getDataKey(), DATA_TYPE),
				Encoders.tuple(Encoders.STRING(), Encoders.STRING(), Encoders.kryo(JSONObject.class)));

		// Take the union of the 2 streams and process the result in a stateful manner with flatMap...
		return enrichedControlStream
			.union(enrichedDataStream)
			.groupByKey(enrichmentKey(), Encoders.STRING())
			.flatMapGroupsWithState(
				connectorFunction(),
				OutputMode.Append(),
				Encoders.kryo(JSONObject.class),
				Encoders.kryo(JSONObject.class),
				GroupStateTimeout.NoTimeout());
	}

}