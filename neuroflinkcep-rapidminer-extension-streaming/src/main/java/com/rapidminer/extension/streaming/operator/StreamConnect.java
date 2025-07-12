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
import com.rapidminer.extension.streaming.utility.graph.transform.ConnectTransformer;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;


/**
 * Connect operator for stream graphs
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamConnect extends AbstractStream2to1Operator {

	private static final String PARAMETER_CONTROL_KEY = "control_key";

	private static final String PARAMETER_DATA_KEY = "data_key";

	public StreamConnect(OperatorDescription description) {
		super(description);
	}

	@Override
	protected String getFirstInputName() {
		return "control stream";
	}

	@Override
	protected String getSecondInputName() {
		return "data stream";
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType leftKey = new ParameterTypeString(
			PARAMETER_CONTROL_KEY,
			"Key of the value to be used on the control-stream.",
			false);
		types.add(leftKey);

		ParameterType rightKey = new ParameterTypeString(
			PARAMETER_DATA_KEY,
			"Key of the value to be used on the data-stream.",
			false);
		types.add(rightKey);

		return types;
	}

	@Override
	protected StreamProducer createProducer(StreamGraph graph, StreamDataContainer firstStreamData,
											StreamDataContainer secondStreamData) throws UserError {
		return new ConnectTransformer.Builder(graph)
			.withControlKey(getParameterAsString(PARAMETER_CONTROL_KEY))
			.withDataKey(getParameterAsString(PARAMETER_DATA_KEY))
			.withControlParent(firstStreamData.getLastNode())
			.withDataParent(secondStreamData.getLastNode())
			.build();
	}

}