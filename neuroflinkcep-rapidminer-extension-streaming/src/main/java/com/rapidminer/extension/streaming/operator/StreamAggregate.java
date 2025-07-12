/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.transform.AggregateTransformer;
import com.rapidminer.extension.streaming.utility.graph.transform.Transformer;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeLong;
import com.rapidminer.parameter.ParameterTypeString;


/**
 * Aggregate operator for stream graphs
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamAggregate extends AbstractStreamTransformOperator {

	private static final String PARAMETER_KEY = "key";

	private static final String PARAMETER_VALUE_KEY = "value_key";

	private static final String PARAMETER_WINDOW_LENGTH = "window_length";

	private static final String PARAMETER_FUNCTION = "function";

	private static final Map<String, AggregateTransformer.Function> FUNCTION_MAP = buildFunctionMap();

	public StreamAggregate(OperatorDescription description) {
		super(description);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType key = new ParameterTypeString(
			PARAMETER_KEY,
			"Key to be used for partitioning the data (determines the granularity of the aggregation).",
			false);
		types.add(key);

		ParameterType valueKey = new ParameterTypeString(
			PARAMETER_VALUE_KEY,
			"Key of the value to be used in the aggregation function (e.g. amount that will be summed).",
			false);
		types.add(valueKey);

		ParameterType windowLength = new ParameterTypeLong(
			PARAMETER_WINDOW_LENGTH,
			"Length of time-window over which aggregation occurs.",
			0,
			Long.MAX_VALUE,
			false);
		types.add(windowLength);

		ParameterType function = new ParameterTypeCategory(
			PARAMETER_FUNCTION,
			"Aggregation function to be used.",
			FUNCTION_MAP.keySet().toArray(new String[]{}),
			0);
		types.add(function);

		return types;
	}

	@Override
	public Transformer createStreamProducer(StreamGraph graph, StreamDataContainer inData) throws UserError {
		return new AggregateTransformer.Builder(graph)
			.withKey(getParameterAsString(PARAMETER_KEY))
			.withValueKey(getParameterAsString(PARAMETER_VALUE_KEY))
			.withWindowLength(getParameterAsLong(PARAMETER_WINDOW_LENGTH))
			.withFunction(FUNCTION_MAP.get(getParameterAsString(PARAMETER_FUNCTION)))
			.withParent(inData.getLastNode())
			.build();
	}

	/**
	 * @return newly built mapping between function names and actual function objects (enums)
	 */
	private static Map<String, AggregateTransformer.Function> buildFunctionMap() {
		return new ImmutableMap.Builder<String, AggregateTransformer.Function>()
			.put("Sum", AggregateTransformer.Function.SUM)
			.put("Average", AggregateTransformer.Function.AVG)
			.put("Count", AggregateTransformer.Function.COUNT)
			.put("Minimum", AggregateTransformer.Function.MIN)
			.put("Maximum", AggregateTransformer.Function.MAX)
			.build();
	}

}