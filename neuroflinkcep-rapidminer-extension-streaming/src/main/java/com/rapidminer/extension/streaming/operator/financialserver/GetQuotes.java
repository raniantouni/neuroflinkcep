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
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.operator.Annotations;
import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.Ontology;


/**
 * This operator fetches a stream of real-time quotes from the Spring Financial data server. It does so for all the
 * input quote symbols.
 *
 * @author Edwin Yaqub
 * @since 0.1.0
 */
public class GetQuotes extends SocketBaseOperator {

	public static final String PARAMETER_STREAM_WINDOW_TIME_MILLISEC = "stream_time_(ms)";
	public static final String PARAMETER_LOGOUT_TIME_MILLISEC = "disconnect_time_(ms)";
	private final InputPort exampleSetInputPort = getInputPorts().createPort("example set of symbols", ExampleSet.class);
	private final OutputPort collectionOutputPort = getOutputPorts().createPort("collection of real time quotes");
	private ExampleSet symbolsExampleSet = null;
	private IOObjectCollection<ExampleSet> outputCollection = null;
	private static final Logger LOGGER = LogService.getRoot();

	public GetQuotes(OperatorDescription description) {
		super(description);
		exampleSetInputPort.addPrecondition(new ExampleSetPrecondition(exampleSetInputPort,
			new String[]{SocketBaseOperator.SYMBOLS}, Ontology.NOMINAL));
		getTransformer().addGenerationRule(collectionOutputPort, IOObjectCollection.class);
	}

	@Override
	public void doWork() throws OperatorException {
		super.initialize();
		try {
			symbolsExampleSet = exampleSetInputPort.getDataOrNull(ExampleSet.class);
		} catch (UserError e) {
			LOGGER.log(Level.WARNING, "Invalid or empty ExampleSet at input port.", e);
			throw e;
		}
		// Get operator parameters
		int timeToWaitForStreamingResponse = getParameterAsInt(PARAMETER_STREAM_WINDOW_TIME_MILLISEC);
		int timeToWaitForLogout = getParameterAsInt(PARAMETER_LOGOUT_TIME_MILLISEC);
		LOGGER.log(Level.INFO,
			"*** Got Time Parameter for windowing on streaming data: " + timeToWaitForStreamingResponse);
		// Result to be a populated in "quotesOfSymbols" list
		ArrayList<String> quotesOfSymbols = new ArrayList<>();
		// Follow 3-Step transaction: Login, Command (start, stop), Logout
		int resultOfLoginCommand = getWrapper().loginAction(getUsername(), getPassword());
		LOGGER.log(Level.INFO, "*** Login Response Code: " + resultOfLoginCommand + " Response: "
			+ getWrapper().getLoginCommandResponse());
		// Step 1: Logged In
		if (getWrapper().isLoggedIn()) {
			// Step 2: Call operation (Order a real-time quote of a symbol)
			// Loop over all symbols in passed ExampleSet of Symbols. This list should be short, else it may take a long
			// time to get quotes as this is a real-time operation.
			if (symbolsExampleSet != null) {
				String symbolPerExample = null;
				Attribute symbolAttribute = symbolsExampleSet.getAttributes().get(SocketBaseOperator.SYMBOLS);
				for (Example example : symbolsExampleSet) {
					if (symbolAttribute != null) {
						symbolPerExample = example.getNominalValue(symbolAttribute);
						LOGGER.log(Level.INFO, "*** Symbol attribute exists in Input Table: " + symbolPerExample);
						// Send 'OrderQuote' command (sendSymbol in DataConnector sends in expected syntax)
						// Step 3a: Start streaming (Request Quotes for Symbol)
						LOGGER.log(Level.INFO, "*** Adjusted Symbol for placing Order as: " + symbolPerExample);
						getWrapper().startQuoteAction(symbolPerExample);

						try {
							// Sleep current thread for n seconds to receive symbols list
							Thread.sleep(timeToWaitForStreamingResponse);
							// Store all quotes received till timeout
							quotesOfSymbols.addAll(getWrapper().getQuotesList());
							LOGGER.log(Level.INFO,
								"***  Quotes for current Symbol: " + getWrapper().getQuotesList().size());
							// Clear the quotes list in the wrapper to avoid duplication
							getWrapper().clearQuotesList();
							LOGGER.log(Level.INFO, "***  Total Quotes Till Now: " + quotesOfSymbols.size());
							// Step 3b: Stop streaming (Stop Quote before Logout)
							getWrapper().stopQuoteAction(symbolPerExample);

						} catch (InterruptedException e) {
							LOGGER.log(Level.WARNING, "Interrupted Exception: " + e.getMessage(), e);
						}
					}
				}// loop ends
				// Step 4: Final step is Logout
				getWrapper().logoutAction();
				// TODO: wrap this method in wrapper
				if (getWrapper().getConnection().isThreadRunning()) {
					// Sleep current thread for n seconds to logout
					try {
						LOGGER.log(Level.WARNING, "Sleeping for (ms): " + timeToWaitForLogout);
						Thread.sleep(timeToWaitForLogout);
					} catch (InterruptedException e) {
						LOGGER.log(Level.WARNING, "Interrupted Exception: " + e.getMessage(), e);
					}
				}
			}
		}
		//Create collection to deliver
		outputCollection = getCollectionFromQuotes(quotesOfSymbols);
		collectionOutputPort.deliver(outputCollection);
	}

	private IOObjectCollection<ExampleSet> getCollectionFromQuotes(ArrayList<String> quotesOfSymbols) {
		IOObjectCollection<ExampleSet> collection = new IOObjectCollection<ExampleSet>();
		// Create ExampleSet from Quotes list and deliver.
		ExampleSet quotesExampleSet = null;
		if (quotesOfSymbols != null && quotesOfSymbols.size() > 0) {
			List<Attribute> listOfAtts = new LinkedList<>();
			Attribute newAttribute = AttributeFactory.createAttribute(SocketBaseOperator.QUOTES, Ontology.NOMINAL);
			listOfAtts.add(newAttribute);
			ExampleSetBuilder exampleSetBuilder = null;// = ExampleSets.from(listOfAtts);
			Annotations annotations = null;
			for (String quote : quotesOfSymbols) {
				if (quote.startsWith(SocketBaseOperator.ORDERQUOTE_KEYWORD)) {
					if (annotations != null) {
						// Create ExampleSet from last data-rows in builder
						quotesExampleSet = exampleSetBuilder.build();
						quotesExampleSet.getAnnotations().addAll(annotations);
						collection.add(quotesExampleSet);
					}
					// save new annotation, reset builder
					annotations = new Annotations();
					annotations.setAnnotation(SocketBaseOperator.ORDERQUOTE_ANNOTATION, quote);
					exampleSetBuilder = ExampleSets.from(listOfAtts);
				} else {
					// Keep creating rows for current ExampleSet
					double[] values = new double[listOfAtts.size()];// 1 attribute
					// String asciiReplaced = quote.replaceAll("[^\\p{ASCII}]", ".");
					values[0] = newAttribute.getMapping().mapString(quote);
					exampleSetBuilder.addDataRow(new DoubleArrayDataRow(values));
				}
			}
			// build and add last ExampleSet
			if (annotations != null) {
				// Create ExampleSet from last data-rows in builder
				quotesExampleSet = exampleSetBuilder.build();
				quotesExampleSet.getAnnotations().addAll(annotations);
				collection.add(quotesExampleSet);
			}
		} else {
			LOGGER.log(Level.WARNING, "*** Nothing to output.");
		}
		return collection;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		ParameterTypeInt streamingTimeParameter = new ParameterTypeInt(PARAMETER_STREAM_WINDOW_TIME_MILLISEC,
			I18N.getGUIMessage("operator.parameter.streaming.order_quotes.description"), Integer.MIN_VALUE,
			Integer.MAX_VALUE, 10000);
		types.add(streamingTimeParameter);

		ParameterTypeInt logoutTimeParameter = new ParameterTypeInt(PARAMETER_LOGOUT_TIME_MILLISEC,
			I18N.getGUIMessage("operator.parameter.streaming.graceful_disconnect.description"), Integer.MIN_VALUE,
			Integer.MAX_VALUE, 1000);
		types.add(logoutTimeParameter);

		return types;
	}

}