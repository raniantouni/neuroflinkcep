/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.api.infore.maritime;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * MarineTraffic (Akka) cluster JWT response for login request
 *
 * @author Mate Torok
 * @since 0.6.0
 */
public class JwtResponse implements Serializable {

	@JsonProperty("jwt")
	private final String jwt;

	JwtResponse() {
		this(null);
	}

	JwtResponse(String jwt) {
		this.jwt = jwt;
	}

	public String getJwt() {
		return jwt;
	}

}