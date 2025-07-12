/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.infore;


/**
 * Maritime specific constants on RapidMiner Studio side
 *
 * @author Mate Torok
 * @since 0.6.0
 */
public final class MaritimeConstants {

	public static final String HEADER_AUTHORIZATION_KEY = "Authorization";

	public static final String HEADER_AUTHORIZATION_VALUE_PATTERN = "Bearer %s";

	public static final String API_JOB = "/api/job";

	public static final String API_LOGIN = "/api/login";

	private MaritimeConstants() {
	}

}