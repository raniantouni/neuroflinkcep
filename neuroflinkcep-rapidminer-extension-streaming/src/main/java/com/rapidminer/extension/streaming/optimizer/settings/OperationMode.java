/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.settings;/*
 * Copyright (C) 2016-2022 RapidMiner GmbH
 */

import java.util.Arrays;


/**
 * @author Fabian Temme
 * @since 0.6.1
 */
public enum OperationMode {

	/**
	 * Performs an optimization and directly deploys the optimized workflow
	 */
	OptimizeAndDeploy("optimize and deploy"),

	/**
	 * Performs only an optimization and updates the subprocess, but not deploy the optimized workflow
	 */
	OnlyOptimize("only optimize"),

	/**
	 * Only deploys the (already updated) optimized workflow.
	 */
	OnlyDeploy("only deploy");

	private final String description;

	OperationMode(String description) {
		this.description = description;
	}

	/**
	 * Returns a descriptive name of the mode, which can be used as the value in the parameter category.
	 *
	 * @return descriptive name of the mode, which can be used as the value in the parameter category
	 */
	public String getDescription() {
		return description;
	}


	/**
	 * Returns the {@link OperationMode} for which {@link OperationMode#toString()} is equal to the provided
	 * String.
	 *
	 * @param text
	 * 	String which is checked against {@link OperationMode#toString()}.
	 * @return {@link OperationMode} for which {@link OperationMode#toString()} is equal to the provided String
	 * @throws IllegalArgumentException
	 * 	if provided String is not equal to any {@link OperationMode#toString()} method calls
	 */
	public static OperationMode getMode(String text) {
		for (OperationMode mode : OperationMode.values()) {
			if (mode.description.equals(text)) {
				return mode;
			}
		}
		throw new IllegalArgumentException(
			"Provided text (" + text + ") does not match with an OperationMode. " + "Allowed texts are: " + Arrays
				.toString(OperationMode.values()));
	}
}
