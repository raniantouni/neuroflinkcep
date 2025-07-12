/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.management;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;


/**
 * Utility for timestamp/date-time handling
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public final class DateTimeUtil {

	private static String UTC = "UTC";

	private DateTimeUtil() {}

	/**
	 * @return UTC epoch seconds
	 */
	public static long getTimestamp() {
		return ZonedDateTime.now(ZoneId.of(UTC)).toEpochSecond();
	}

	/**
	 * @param epochs seconds
	 * @return UTC zone date-time
	 */
	public static ZonedDateTime getUTCDateTime(long epochs) {
		return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochs), ZoneId.of(UTC));
	}

}