/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.api.infore.maritime;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * MarineTraffic (Akka) cluster JWT request for login
 *
 * @author Mate Torok
 * @since 0.6.0
 */
public class JwtRequest implements Serializable {

	@JsonProperty("username")
	private final String username;

	@JsonProperty("password")
	private final String password;

	JwtRequest() {
		this(null, null);
	}

	public JwtRequest(String username, String password) {
		this.username = username;
		this.password = password;
	}

}