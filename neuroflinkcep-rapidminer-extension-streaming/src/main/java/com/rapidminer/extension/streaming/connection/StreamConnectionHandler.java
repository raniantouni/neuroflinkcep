/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.connection;

import java.util.Properties;

import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.extension.streaming.deploy.StreamRunnerType;


/**
 * Functionality for handling StreamRunner connections
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public interface StreamConnectionHandler {

	/**
	 * @return runner type for which the handler is providing a connection
	 */
	StreamRunnerType getRunnerType();

	/**
	 * @param connConfig connection object
	 * @return newly built generic property container for runners
	 */
	Properties buildClusterConfiguration(ConnectionConfiguration connConfig);

}