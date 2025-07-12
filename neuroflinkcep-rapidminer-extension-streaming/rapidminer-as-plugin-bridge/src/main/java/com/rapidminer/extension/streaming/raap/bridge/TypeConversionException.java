/**
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.raap.bridge;

/**
 * Specific exception for type conversion errors
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public class TypeConversionException extends Exception {

	/**
	 * Constructor
	 *
	 * @param message error message
	 */
	public TypeConversionException(String message) {
		super(message);
	}

}