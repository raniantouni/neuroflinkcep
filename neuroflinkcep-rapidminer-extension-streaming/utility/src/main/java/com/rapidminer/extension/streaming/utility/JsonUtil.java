/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Utility class responsible for JSON related actions
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public final class JsonUtil {
	
	private static final ObjectMapper JSON_MAPPER = createMapper();

	private JsonUtil() {
		// Utility
	}

	/**
	 * Using an internal ObjectMapper instance this method takes care of JSON serialization of any
	 * object.
	 *
	 * @param obj
	 * 		to be transformed into JSON
	 * @return JSON string equivalent of the input parameter OR null if an error occurs
	 */
	public static String toJson(Object obj) {
		try {
			return JSON_MAPPER.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Could not convert to JSON: " + obj, e);
		}
	}

	/**
	 * Using an internal {@link ObjectMapper} instance this method takes care of the JSON
	 * deserialization of an provided object. The {@code valueTye} of the class in which the {@code
	 * jsonString} shall be converted has to be provided as well
	 *
	 * @param jsonString
	 * 		JSON string to be deserialized
	 * @param valueTye
	 * 		Class type in which the JSON string shall be converted
	 * @param <T>
	 * 		Object class in which the JSON string is converted
	 * @return deserialized instance of type &lt;T&gt; of the provided {@code jsonString}
	 * @throws IOException
	 * 		if the deserialization went wrong
	 */
	public static <T> T fromString(String jsonString, Class<T> valueTye) throws IOException {
		return JSON_MAPPER.readValue(jsonString, valueTye);
	}

	/**
	 * Using an internal {@link ObjectMapper} instance this method takes care of the JSON
	 * deserialization of an provided object. The {@code valueTye} of the class in which the {@code
	 * jsonString} shall be converted has to be provided as well
	 *
	 * @param jsonStream
	 * 		JSON string to be deserialized (from the stream)
	 * @param valueTye
	 * 		Class type in which the JSON string shall be converted
	 * @param <T>
	 * 		Object class in which the JSON string is converted
	 * @return deserialized instance of type &lt;T&gt; of the provided {@code jsonString}
	 * @throws IOException
	 * 		if the deserialization went wrong
	 */
	public static <T> T fromString(InputStream jsonStream, Class<T> valueTye) throws IOException {
		return JSON_MAPPER.readValue(jsonStream, valueTye);
	}

	/**
	 * @return builds the mapper responsible for serialization/deserialization
	 */
	private static ObjectMapper createMapper() {
		return new ObjectMapper()
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	}

}