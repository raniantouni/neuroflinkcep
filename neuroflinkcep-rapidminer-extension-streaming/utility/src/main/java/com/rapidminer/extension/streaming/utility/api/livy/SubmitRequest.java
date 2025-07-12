/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.api.livy;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Apache-Livy SUBMIT Batch Request
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class SubmitRequest {

	@JsonProperty("file")
	private final String file;

	@JsonProperty("className")
	private final String className;

	@JsonProperty("name")
	private final String name;

	@JsonProperty("args")
	private final List<String> args;

	@JsonProperty("conf")
	private final Map<String,String> conf;

	SubmitRequest() {
		this(null, null, null, null, null);
	}

	public SubmitRequest(String file,
						 String className,
						 String name,
						 List<String> args,
						 Map<String, String> conf) {
		this.file = file;
		this.className = className;
		this.name = name;
		this.args = args;
		this.conf = conf;
	}

}