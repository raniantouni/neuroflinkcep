/**
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.raap;

import java.io.InputStream;

import org.pf4j.ExtensionPoint;


/**
 * Functionality for creating a RapidMiner model applier.
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public interface RapidMinerModelApplierFactory extends ExtensionPoint {

	/**
	 * Creates the model applier instance
	 *
	 * @param modelStream to initialize the applier with (the actual RapidMiner model)
	 * @return newly built instance
	 * @throws Exception if anything goes wrong during construction
	 */
	RapidMinerModelApplier create(InputStream modelStream) throws Exception;

}