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
 * This operator fetches a stream of real-time depths from the Spring Financial data server. It does so for all the
 * input depth symbols. Its command specification is similar to the API commands used by the Get Quotes operator.
 * <p>
 * Note: This operator is not yet tested as the Depths feature is only available for few symbols (unknown at this
 * time).
 *
 * @author Edwin Yaqub
 * @since 0.1.0
 */
public class GetDepths extends SocketBaseOperator {

	public static final String PARAMETER_STREAM_WINDOW_TIME_MILLISEC = "stream_time_(ms)";
	public static final String PARAMETER_LOGOUT_TIME_MILLISEC = "disconnect_time_(ms)";
	private final InputPort exampleSetInputPort = getInputPorts().createPort("example set of symbols", ExampleSet.class);
	private final OutputPort collectionOutputPort = getOutputPorts().createPort("collection of real time depths");
	private ExampleSet symbolsExampleSet = null;
	private IOObjectCollection<ExampleSet> outputCollection = null;
	private static final Logger LOGGER = LogService.getRoot();

	/**
	 * @param description
	 */
	public GetDepths(OperatorDescription description) {
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
		// Result to be a populated in "depthsOfSymbols" list
		ArrayList<String> depthsOfSymbols = new ArrayList<>();

		// Follow 3-Step transaction: Login, Command (start, stop), Logout
		int resultOfLoginCommand = getWrapper().loginAction(getUsername(), getPassword());
		LOGGER.log(Level.INFO, "*** Login Response Code: " + resultOfLoginCommand + " Response: "
			+ getWrapper().getLoginCommandResponse());
		// Step 1: Logged In
		if (getWrapper().isLoggedIn()) {
			if (symbolsExampleSet != null) {
				String symbolPerExample = null;
				Attribute symbolAttribute = symbolsExampleSet.getAttributes().get(SocketBaseOperator.SYMBOLS);
				for (Example example : symbolsExampleSet) {
					if (symbolAttribute != null) {
						symbolPerExample = example.getNominalValue(symbolAttribute);
						LOGGER.log(Level.INFO, "*** Symbol attribute exists in Input Table: " + symbolPerExample);
						// Send 'OrderDepth' command (sendSymbol in DataConnector sends in expected syntax)
						getWrapper().startDepthAction(symbolPerExample);
						try {
							Thread.sleep(timeToWaitForStreamingResponse);
							depthsOfSymbols.addAll(getWrapper().getDepthsList());
							LOGGER.log(Level.INFO,
								"***  Depths for current Symbol: " + getWrapper().getDepthsList().size());
							// Clear the depths list in the wrapper to avoid duplication
							getWrapper().clearDepthsList();
							LOGGER.log(Level.INFO, "***  Total Depths Till Now: " + depthsOfSymbols.size());
							getWrapper().stopDepthAction(symbolPerExample);
						} catch (InterruptedException e) {
							LOGGER.log(Level.WARNING, "Interrupted Exception: " + e.getMessage(), e);
						}
					}
				}// loop ends
				getWrapper().logoutAction();
				if (getWrapper().getConnection().isThreadRunning()) {
					try {
						LOGGER.log(Level.WARNING, "Sleeping for (ms): " + timeToWaitForLogout);
						Thread.sleep(timeToWaitForLogout);
					} catch (InterruptedException e) {
						LOGGER.log(Level.WARNING, "Interrupted Exception: " + e.getMessage(), e);
					}
				}
			}
		}
		// Create collection to deliver
		outputCollection = getCollectionFromDepths(depthsOfSymbols);
		collectionOutputPort.deliver(outputCollection);
	}

	// Note: The syntax of Depth is different from Quotes (from GetQuotes operator), hence we need to parse Depths
	// differently.
	private IOObjectCollection<ExampleSet> getCollectionFromDepths(ArrayList<String> depthsOfSymbols) {
		IOObjectCollection<ExampleSet> collection = new IOObjectCollection<ExampleSet>();
		// Create ExampleSet from Depths list and deliver.
		ExampleSet depthsExampleSet = null;
		if (depthsOfSymbols != null && depthsOfSymbols.size() > 0) {
			List<Attribute> listOfAtts = new LinkedList<>();
			Attribute newAttribute = AttributeFactory.createAttribute(SocketBaseOperator.DEPTHS, Ontology.NOMINAL);
			listOfAtts.add(newAttribute);
			ExampleSetBuilder exampleSetBuilder = ExampleSets.from(listOfAtts);
			Annotations annotations = null;
			String prefixOfCurrentDepth = null;
			String prefixOfPreviousDepth = null;
			int indexOfSecondCommaInPrefix = 1;
			int indexOfSecondCommaInPreviousPrefix = 1;

			for (int i = 0; i < depthsOfSymbols.size(); i++) {
				String depth = depthsOfSymbols.get(i);
				indexOfSecondCommaInPrefix = depth.indexOf(",", 6);
				prefixOfCurrentDepth = depth.substring(0, indexOfSecondCommaInPrefix + 1);
				LOGGER.log(Level.INFO,
					"*** PREFIX of Current Depth = " + prefixOfCurrentDepth + " at List.index: " + i);
				if (i > 0) {
					LOGGER.log(Level.INFO, "*** i>0 ******************");
					indexOfSecondCommaInPreviousPrefix = depthsOfSymbols.get(i - 1).indexOf(",", 6);
					prefixOfPreviousDepth = depthsOfSymbols.get(i - 1).substring(0,
						indexOfSecondCommaInPreviousPrefix);
					LOGGER.log(Level.INFO, "*** PREFIX of Previous Depth = "
						+ (prefixOfPreviousDepth != null ? prefixOfPreviousDepth : "First Row"));
				}
				// Two cases: First case is for the very first row, we cannot compare with last row which will be null
				// Second case is for all rows after the first one
				if (prefixOfPreviousDepth != null && prefixOfCurrentDepth.startsWith(prefixOfPreviousDepth)
					|| prefixOfPreviousDepth == null && prefixOfCurrentDepth != null) {

					if (annotations == null) {
						annotations = new Annotations();
						annotations.setAnnotation(SocketBaseOperator.ORDERDEPTH_ANNOTATION, prefixOfCurrentDepth);
					}
					// If the prefix of this depth is same as prefix of last depth, then
					// Keep creating rows for current ExampleSet
					double[] values = new double[listOfAtts.size()];// 1 attribute
					// String asciiReplaced = quote.replaceAll("[^\\p{ASCII}]", ".");
					values[0] = newAttribute.getMapping().mapString(depth);
					exampleSetBuilder.addDataRow(new DoubleArrayDataRow(values));
				} else {
					depthsExampleSet = exampleSetBuilder.build();
					// save new annotation, reset builder
					depthsExampleSet.getAnnotations().addAll(annotations);
					collection.add(depthsExampleSet);
					// Create new ExampleSetBuilder
					exampleSetBuilder = ExampleSets.from(listOfAtts);
					annotations = new Annotations();
					annotations.setAnnotation(SocketBaseOperator.ORDERDEPTH_ANNOTATION, prefixOfCurrentDepth);
					// Dont miss the first row
					double[] values = new double[listOfAtts.size()];// 1 attribute
					// String asciiReplaced = quote.replaceAll("[^\\p{ASCII}]", ".");
					values[0] = newAttribute.getMapping().mapString(depth);
					exampleSetBuilder.addDataRow(new DoubleArrayDataRow(values));
				}
			}
			// build and add last ExampleSet
			if (annotations != null) {
				// Create ExampleSet from last data-rows in builder
				depthsExampleSet = exampleSetBuilder.build();
				depthsExampleSet.getAnnotations().addAll(annotations);
				collection.add(depthsExampleSet);
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
			I18N.getGUIMessage("operator.parameter.streaming.get_depths.description"), Integer.MIN_VALUE,
			Integer.MAX_VALUE, 10000);
		types.add(streamingTimeParameter);

		ParameterTypeInt logoutTimeParameter = new ParameterTypeInt(PARAMETER_LOGOUT_TIME_MILLISEC,
			I18N.getGUIMessage("operator.parameter.streaming.graceful_disconnect.description"), Integer.MIN_VALUE,
			Integer.MAX_VALUE, 1000);
		types.add(logoutTimeParameter);

		return types;
	}

}