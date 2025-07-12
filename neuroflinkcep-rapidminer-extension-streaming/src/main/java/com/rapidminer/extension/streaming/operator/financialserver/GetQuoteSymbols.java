/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator.financialserver;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;


/**
 * Class for operator that will invoke API level command to get list of Quotes from the Spring Financial server for all
 * supported stock exchanges. This and all sub-operators of SocketBaseOperator class will use the SpringAPIWrapper to to
 * asynchronously send command and process response (when received). Quotes are also referred as Symbols, which are
 * represented in the form: "Exchange|SymbolCode|Name|Contract specification". The separator is ASCII 124 character
 *
 * @author Edwin Yaqub
 * @since 0.1.0
 */
public class GetQuoteSymbols extends SocketBaseOperator {

	private final OutputPort exampleSetOutput = getOutputPorts().createPort("example set symbols");
	public static final String PARAMETER_STREAM_WINDOW_TIME_MILLISEC = "stream_time_(ms)";
	private static final Logger LOGGER = LogService.getRoot();

	/**
	 * @param description
	 */
	public GetQuoteSymbols(OperatorDescription description) {
		super(description);
	}

	@Override
	public void doWork() throws OperatorException {
		super.initialize();
		// Get operator parameters
		int timeToWaitForStreamingResponse = getParameterAsInt(PARAMETER_STREAM_WINDOW_TIME_MILLISEC);
		LOGGER.log(Level.INFO,
			"*** Got Time Parameter for windowing on streaming data: " + timeToWaitForStreamingResponse);
		// Result to be a populated in SymbolsList
		ArrayList<String> symbolsList = new ArrayList<>();
		// Follow 3-Step transaction: Login, Command, Logout
		int resultOfLoginCommand = getWrapper().loginAction(getUsername(), getPassword());
		LOGGER.log(Level.INFO,
			"*** Login Response Code: " + resultOfLoginCommand + " Response: "
				+ getWrapper().getLoginCommandResponse());
		// Step 1: Logged In
		if (getWrapper().isLoggedIn()) {
			// Step 2: Call operation
			getWrapper().getSymbolsAction();
			try {
				// Sleep current thread for n seconds to receive symbols list
				Thread.sleep(timeToWaitForStreamingResponse);
				symbolsList = getWrapper().getSymbolsList();
				LOGGER.log(Level.INFO, "*** QuoteList Response size: " + symbolsList.size());
				// Step 3: Logout (will stop the thread)
				getWrapper().logoutAction();
				if (getWrapper().getConnection().isThreadRunning()) {
					LOGGER.log(Level.WARNING, "Sleeping phase 2");
					// Sleep current thread for n seconds to logout
					Thread.sleep(timeToWaitForStreamingResponse);
				}
			} catch (InterruptedException e) {
				LOGGER.log(Level.WARNING, "Sleeping problems: " + e.getMessage(), e);
			}
		}

		// Create ExampleSet from Symbols list and deliver.
		ExampleSet quotesExampleSet = null;
		if (symbolsList != null && symbolsList.size() > 0) {
			List<Attribute> listOfAtts = new LinkedList<>();
			Attribute newAttribute = AttributeFactory.createAttribute(SocketBaseOperator.SYMBOLS, Ontology.NOMINAL);
			listOfAtts.add(newAttribute);
			ExampleSetBuilder exampleSetBuilder = ExampleSets.from(listOfAtts);
			for (String symbol : symbolsList) {
				double[] values = new double[listOfAtts.size()];// 1 attribute
				String asciiReplaced = symbol.replaceAll("[^\\p{ASCII}]", "|");
				values[0] = newAttribute.getMapping().mapString(asciiReplaced);
				exampleSetBuilder.addDataRow(new DoubleArrayDataRow(values));
			}
			quotesExampleSet = exampleSetBuilder.build();
		}
		// reset list in wrapper
		getWrapper().clearSymbolsList();
		exampleSetOutput.deliver(quotesExampleSet);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		/*
		// To use in getQuote and getDepth operators.
		ParameterType type = new ParameterTypeSuggestion(PARAMETER_SYMBOLS,
				I18N.getGUIMessage("operator.parameter.streaming_fetch_symbols.description"),
				new SymbolsSuggestionProvider());
		type.setOptional(false);
		types.add(type);
		 */
		ParameterTypeInt timeParameter = new ParameterTypeInt(PARAMETER_STREAM_WINDOW_TIME_MILLISEC,
			I18N.getGUIMessage("operator.parameter.streaming.fetch_symbols.description"),
			Integer.MIN_VALUE, Integer.MAX_VALUE, 5000);
		types.add(timeParameter);
		return types;
	}

}