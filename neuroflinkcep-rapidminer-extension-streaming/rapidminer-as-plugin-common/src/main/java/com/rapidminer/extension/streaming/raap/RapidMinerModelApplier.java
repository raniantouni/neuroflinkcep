/*
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.raap;

import java.io.Serializable;

import com.rapidminer.extension.streaming.raap.data.DataTable;


/**
 * Functionality for using a RapidMiner model.
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public interface RapidMinerModelApplier extends Serializable {

	/**
	 * This method applies the model on the input data.
	 * We use a custom domain to represent the data (kind of DTO-s in our domain) to keep
	 * client (application/job wanting to use RapidMiner as a plugin) and
	 * service (application offering RapidMiner as a service) completely separate,
	 * in every aspect (e.x.: classloader hierarchy).
	 *
	 * @param input data in "our" domain
	 * @return output of the model represented in "our" domain
	 * @throws Exception if any error occurs during applying the model
	 */
	DataTable apply(DataTable input) throws Exception;

}