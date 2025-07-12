/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy;

/**
 * Streaming extension specific exception
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamRunnerException extends Exception {

	private static final long serialVersionUID = 1L;

	public StreamRunnerException(String message) {
		super(message);
	}

	public StreamRunnerException(String message, Throwable cause) {
		super(message, cause);
	}

}