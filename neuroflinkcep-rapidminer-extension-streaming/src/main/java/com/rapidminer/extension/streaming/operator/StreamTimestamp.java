/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator;

import java.util.List;

import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.transform.TimestampTransformer;
import com.rapidminer.extension.streaming.utility.graph.transform.Transformer;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDateFormat;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;


/**
 * Operator for stream graphs to put a timestamp into an event
 *
 * @author Mate Torok
 * @since 0.5.0
 */
public class StreamTimestamp extends AbstractStreamTransformOperator {

	private static final String PARAMETER_KEY = "key";

	private static final String PARAMETER_FORMAT_AS_NUMBER = "format_as_number";

	public StreamTimestamp(OperatorDescription description) {
		super(description);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType key = new ParameterTypeString(
			PARAMETER_KEY,
			"Key to use for the timestamp.",
			false);
		types.add(key);

		ParameterType unixTime = new ParameterTypeBoolean(
			PARAMETER_FORMAT_AS_NUMBER,
			"Use number representation (unix time seconds) ?",
			true);
		types.add(unixTime);

		ParameterType format = new ParameterTypeDateFormat(
			ParameterTypeDateFormat.PARAMETER_DATE_FORMAT,
			"Format for the timestamp.",
			false);
		format.setOptional(true);
		format.setDefaultValue(ParameterTypeDateFormat.DATE_TIME_FORMAT_ISO8601_UTC_MS);
		format.registerDependencyCondition(
			new BooleanParameterCondition(this, PARAMETER_FORMAT_AS_NUMBER, true, false));
		types.add(format);

		return types;
	}

	@Override
	public Transformer createStreamProducer(StreamGraph graph, StreamDataContainer inData) throws UserError {
		return new TimestampTransformer.Builder(graph)
			.withKey(getParameterAsString(PARAMETER_KEY))
			.withUseUnixTime(getParameterAsBoolean(PARAMETER_FORMAT_AS_NUMBER))
			.withFormat(getParameterAsString(ParameterTypeDateFormat.PARAMETER_DATE_FORMAT))
			.withParent(inData.getLastNode())
			.build();
	}
}