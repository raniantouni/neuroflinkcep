/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator;

import java.util.List;

import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.transform.MapTransformer;
import com.rapidminer.extension.streaming.utility.graph.transform.Transformer;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;


/**
 * Map operator for stream graphs
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamMap extends AbstractStreamTransformOperator {

	private static final String PARAMETER_KEY = "key";

	private static final String PARAMETER_VALUE = "value";

	public StreamMap(OperatorDescription description) {
		super(description);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType key = new ParameterTypeString(
			PARAMETER_KEY,
			"Key of the value to be mapped.",
			false);
		types.add(key);

		ParameterType value = new ParameterTypeString(
			PARAMETER_VALUE,
			"New value for the keyed field.",
			false);
		types.add(value);

		return types;
	}

	@Override
	public Transformer createStreamProducer(StreamGraph graph, StreamDataContainer inData) throws UserError {
		return new MapTransformer.Builder(graph)
			.withKey(getParameterAsString(PARAMETER_KEY))
			.withNewValue(getParameterAsString(PARAMETER_VALUE))
			.withParent(inData.getLastNode())
			.build();
	}
}