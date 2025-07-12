/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator;

import java.util.logging.Logger;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.LogService;


/**
 * This is an abstract implementation of the {@link StreamOperator} interface. It uses {@link #doWorkDefault()} for the
 * {@link #doWork()} and the {@link #LOGGER} variable to log the processing in {@link #logProcessing(String)}.
 *
 * @author Fabian Temme
 * @since 0.6.1
 */
public abstract class AbstractStreamOperator extends Operator implements StreamOperator {

	protected static final Logger LOGGER = LogService.getRoot();

	public AbstractStreamOperator(OperatorDescription description) {
		super(description);
	}

	@Override
	public void doWork() throws OperatorException {
		doWorkDefault();
	}

	@Override
	public void logProcessing(String graphName) {
		LOGGER.fine("Processing " + getName() + " for: " + graphName);
	}
}
