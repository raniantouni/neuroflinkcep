/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.common.collect.ImmutableMap;
import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.transform.FilterTransformer;
import com.rapidminer.extension.streaming.utility.graph.transform.Transformer;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.LogService;


/**
 * Filter operator for stream graphs
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamFilter extends AbstractStreamTransformOperator {
	private static final Logger logger = LogService.getRoot();

	private static final String PARAMETER_KEY = "key";

	private static final String PARAMETER_OPERATOR = "operator";

	private static final String PARAMETER_VALUE = "value";

	private static final Map<String, FilterTransformer.Operator> OPERATOR_MAP = buildOperatorMap();

	public StreamFilter(OperatorDescription description) {
		super(description);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		logger.info("GetParameter is starting.");

		ParameterType key = new ParameterTypeString(
			PARAMETER_KEY,
			"Key of the value to be evaluated in the filter predicate.",
			false);
		types.add(key);

		ParameterType value = new ParameterTypeString(
			PARAMETER_VALUE,
			"Right-value (operand) of the filter predicate.",
			false);
		types.add(value);

		ParameterType function = new ParameterTypeCategory(
			PARAMETER_OPERATOR,
			"Operator to be used when evaluating the filter predicate.",
			OPERATOR_MAP.keySet().toArray(new String[]{}),
			0);
		types.add(function);

		return types;
	}

	@Override
	public Transformer createStreamProducer(StreamGraph graph, StreamDataContainer inData) throws UserError {
		return new FilterTransformer.Builder(graph)
			.withKey(getParameterAsString(PARAMETER_KEY))
			.withOperator(OPERATOR_MAP.get(getParameterAsString(PARAMETER_OPERATOR)))
			.withValue(getParameterAsString(PARAMETER_VALUE))
			.withParent(inData.getLastNode())
			.build();
	}

	/**
	 * @return newly built mapping between operator names and actual operator objects (enums)
	 */
	private static Map<String, FilterTransformer.Operator> buildOperatorMap() {
		return new ImmutableMap.Builder<String, FilterTransformer.Operator>()
			.put("Equal to", FilterTransformer.Operator.EQUAL)
			.put("Not equal to", FilterTransformer.Operator.NOT_EQUAL)
			.put("Greater than", FilterTransformer.Operator.GREATER_THAN)
			.put("Greater than or equal to", FilterTransformer.Operator.GREATER_THAN_OR_EQUAL)
			.put("Less than", FilterTransformer.Operator.LESS_THAN)
			.put("Less than or equal to", FilterTransformer.Operator.LESS_THAN_OR_EQUAL)
				.put("String starts with", FilterTransformer.Operator.STARTS_WITH)
				.build();
	}

}