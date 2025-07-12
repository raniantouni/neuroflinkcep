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
import com.rapidminer.extension.streaming.utility.graph.transform.ParseFieldTransformer;
import com.rapidminer.extension.streaming.utility.graph.transform.Transformer;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeString;


/**
 * Parse field operator for stream graphs
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamParseField extends AbstractStreamTransformOperator {

	private static final String PARAMETER_KEY = "key";

	private static final String PARAMETER_KEYS = "keys";

	public StreamParseField(OperatorDescription description) {
		super(description);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType key = new ParameterTypeString(PARAMETER_KEY, "Key to be parsed", false);
		ParameterType keys = new ParameterTypeEnumeration(PARAMETER_KEYS, "Keys to be parsed.", key);
		types.add(keys);

		return types;
	}

	@Override
	public Transformer createStreamProducer(StreamGraph graph, StreamDataContainer inData) throws UserError {
		return new ParseFieldTransformer.Builder(graph)
			.withKeys(Sets.newHashSet(ParameterTypeEnumeration.transformString2List(getParameterAsString(PARAMETER_KEYS))))
			.withParent(inData.getLastNode())
			.build();
	}
}