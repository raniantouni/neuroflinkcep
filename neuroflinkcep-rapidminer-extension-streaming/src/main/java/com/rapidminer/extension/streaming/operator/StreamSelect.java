/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator;

import java.util.List;

import com.google.common.collect.Sets;
import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.transform.SelectTransformer;
import com.rapidminer.extension.streaming.utility.graph.transform.Transformer;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeString;


/**
 * Select operator for stream graphs
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamSelect extends AbstractStreamTransformOperator {

	private static final String PARAMETER_KEY = "key";

	private static final String PARAMETER_KEYS = "keys";

	private static final String PARAMETER_FLATTEN = "with_flatten";

	public StreamSelect(OperatorDescription description) {
		super(description);

		getTransformer().addPassThroughRule(input, output);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType key = new ParameterTypeString(PARAMETER_KEY, "Key to be selected", false);
		ParameterType keys = new ParameterTypeEnumeration(PARAMETER_KEYS, "Keys to be selected.", key);
		types.add(keys);

		ParameterType flatten =
			new ParameterTypeBoolean(PARAMETER_FLATTEN, "If true, structure will be flattened", false);
		types.add(flatten);

		return types;
	}

	@Override
	public Transformer createStreamProducer(StreamGraph graph, StreamDataContainer inData) throws UserError {
		return new SelectTransformer.Builder(graph)
			.withKeys(Sets.newHashSet(ParameterTypeEnumeration.transformString2List(getParameterAsString(PARAMETER_KEYS))))
			.withFlatten(getParameterAsBoolean(PARAMETER_FLATTEN))
			.withParent(inData.getLastNode())
			.build();
	}
}