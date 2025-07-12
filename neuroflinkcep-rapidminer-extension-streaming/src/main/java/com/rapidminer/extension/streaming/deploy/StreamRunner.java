/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy;

import com.rapidminer.extension.streaming.utility.graph.StreamGraph;


/**
 * Functionality for executing StreamGraph-s (platform specific implementations differ greatly)
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public interface StreamRunner {

	/**
	 * Executes/initiates graph execution for the workflow defined in the StreamGraph
	 *
	 * @param graph job
	 * @return job-ID for the newly created/dispatched job
	 * @throws StreamRunnerException in case of any issues
	 */
	String execute(StreamGraph graph) throws StreamRunnerException;

	/**
	 * Abort the execution/initiation (as much as possible)
	 * @throws StreamRunnerException in case of any issues
	 */
	void abort() throws StreamRunnerException;

}