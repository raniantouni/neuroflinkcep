/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator;

import java.util.List;

import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;
import com.rapidminer.extension.streaming.utility.graph.transform.JoinTransformer;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeLong;
import com.rapidminer.parameter.ParameterTypeString;


/**
 * Join operator for stream graphs
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamJoin extends AbstractStream2to1Operator {

	private static final String PARAMETER_LEFT_KEY = "left_key";

	private static final String PARAMETER_RIGHT_KEY = "right_key";

	private static final String PARAMETER_WINDOW_LENGTH = "window_length";

	public StreamJoin(OperatorDescription description) {
		super(description);
	}

	@Override
	protected String getFirstInputName() {
		return "input stream 1";
	}

	@Override
	protected String getSecondInputName() {
		return "input stream 2";
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType leftKey = new ParameterTypeString(
			PARAMETER_LEFT_KEY,
			"Key of the value to be used on the left-stream.",
			false);
		types.add(leftKey);

		ParameterType rightKey = new ParameterTypeString(
			PARAMETER_RIGHT_KEY,
			"Key of the value to be used on the right-stream.",
			false);
		types.add(rightKey);

		ParameterType windowLength = new ParameterTypeLong(
			PARAMETER_WINDOW_LENGTH,
			"Length of the window to be used for joining.",
			0,
			Long.MAX_VALUE,
			false);
		types.add(windowLength);

		return types;
	}

	@Override
	protected StreamProducer createProducer(StreamGraph graph, StreamDataContainer firstStreamData,
											StreamDataContainer secondStreamData) throws UserError {
		return new JoinTransformer.Builder(graph)
			.withLeftKey(getParameterAsString(PARAMETER_LEFT_KEY))
			.withRightKey(getParameterAsString(PARAMETER_RIGHT_KEY))
			.withWindowLength(getParameterAsLong(PARAMETER_WINDOW_LENGTH))
			.withLeftParent(firstStreamData.getLastNode())
			.withRightParent(secondStreamData.getLastNode())
			.build();
	}
}