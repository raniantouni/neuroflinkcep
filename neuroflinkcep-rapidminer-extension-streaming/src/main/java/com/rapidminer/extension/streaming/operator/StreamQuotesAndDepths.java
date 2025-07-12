/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.operator.financialserver.SocketBaseOperator;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;
import com.rapidminer.extension.streaming.utility.graph.source.SpringFinancialSource;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.container.Pair;


/**
 * Operator for retrieving quotes (L1) and depths (L2) streams as part of stream graph
 *
 * @author Edwin Yaqub
 * @since 0.1.0
 */
public class StreamQuotesAndDepths extends SocketBaseOperator implements StreamOperator {

	private static final Logger LOGGER = LogService.getRoot();

	private static final String PARAMETER_STREAM_WINDOW_TIME_MILLISEC = SpringFinancialSource.STREAM_TIME_MS;
	private static final String PARAMETER_DISCONNECT_TIME_MILLISEC = SpringFinancialSource.DISCONNECT_TIME_MS;

	private static final String PARAMETER_SYMBOL = "stock_symbol";
	private static final String PARAMETER_SOURCE_TYPE = "select_operation";
	private static final String[] SOURCE_TYPES = new String[]{"quotes", "depths"};
	private static final int SOURCE_TYPE_QUOTES = 0;
	private static final int SOURCE_TYPE_DEPTHS = 1;

	private final OutputPort output = getOutputPorts().createPort("output stream");

	public StreamQuotesAndDepths(OperatorDescription description) {
		super(description);
		getTransformer().addGenerationRule(output, StreamDataContainer.class);
	}

	@Override
	public Pair<StreamGraph, List<StreamDataContainer>> getStreamDataInputs() throws UserError {
		StreamGraph graph = ((StreamingNest) getExecutionUnit().getEnclosingOperator()).getGraph();
		return new Pair<>(graph, new ArrayList<>());
	}

	@Override
	public List<StreamProducer> addToGraph(StreamGraph graph, List<StreamDataContainer> streamDataInputs) throws UserError {
		Properties properties = new Properties();
		properties.setProperty(SpringFinancialSource.HOST, getServerIP());
		properties.setProperty(SpringFinancialSource.PORT, getServerPort());
		properties.setProperty(SpringFinancialSource.USERNAME, getUsername());
		properties.setProperty(SpringFinancialSource.PASSWORD, getPassword());

		// Get Parameters
		properties.setProperty(SpringFinancialSource.SYMBOL, getParameterAsString(PARAMETER_SYMBOL));
		properties.setProperty(PARAMETER_STREAM_WINDOW_TIME_MILLISEC,
			getParameterAsInt(PARAMETER_STREAM_WINDOW_TIME_MILLISEC) + "");
		properties.setProperty(PARAMETER_DISCONNECT_TIME_MILLISEC,
			getParameterAsInt(PARAMETER_DISCONNECT_TIME_MILLISEC) + "");

		switch (getParameterAsInt(PARAMETER_SOURCE_TYPE)) {
			case SOURCE_TYPE_QUOTES:
				properties.setProperty(SpringFinancialSource.RETRIEVE_QUOTES,
					SpringFinancialSource.RETRIEVE_QUOTES_YES);
				break;
			case SOURCE_TYPE_DEPTHS:
				properties.setProperty(SpringFinancialSource.RETRIEVE_QUOTES,
					SpringFinancialSource.RETRIEVE_QUOTES_NO);
				break;
		}

		SpringFinancialSource source = new SpringFinancialSource.Builder(graph).withConfiguration(properties).build();
		graph.registerSource(source);
		return Collections.singletonList(source);
	}

	@Override
	public void logProcessing(String graphName) {
		LOGGER.fine("Processing " + getName() + " for: " + graphName);
	}

	@Override
	public void deliverStreamDataOutputs(StreamGraph graph, List<StreamProducer> streamProducers) throws UserError {
		StreamDataContainer outData = new StreamDataContainer(graph, streamProducers.get(0));
		output.deliver(outData);
	}

	@Override
	public void doWork() throws OperatorException {
		doWorkDefault();
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterTypeString symbolParameter =
			new ParameterTypeString(PARAMETER_SYMBOL, "symbol for stock", false, false);
		types.add(symbolParameter);

		ParameterTypeInt streamingTimeParameter = new ParameterTypeInt(PARAMETER_STREAM_WINDOW_TIME_MILLISEC,
			I18N.getGUIMessage("operator.parameter.streaming.order_quotes.description"), Integer.MIN_VALUE,
			Integer.MAX_VALUE, 5000);
		types.add(streamingTimeParameter);

		ParameterTypeCategory resourceTypeParameter = new ParameterTypeCategory(PARAMETER_SOURCE_TYPE,
			"Select quotes or depths as retrieval operation",
			SOURCE_TYPES, SOURCE_TYPE_QUOTES, false);
		types.add(resourceTypeParameter);

		ParameterTypeInt logoutTimeParameter = new ParameterTypeInt(PARAMETER_DISCONNECT_TIME_MILLISEC,
			I18N.getGUIMessage("operator.parameter.streaming.graceful_disconnect.description"), Integer.MIN_VALUE,
			Integer.MAX_VALUE, 1000);
		types.add(logoutTimeParameter);

		return types;
	}
}
