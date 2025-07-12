/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.api.infore.synopsis;

import com.fasterxml.jackson.annotation.JsonValue;


/**
 * Available data synopsis types
 *
 * @author David Arnu
 * @since 0.1.0
 */
public enum SynopsisType {

	COUNT_MIN(1, "count min"),
	BLOOM_FILTER(2, "bloom filter"),
	AMS(3, "ams"),
	DFT(4, "dft"),
	LSH(5, "lsh"),
	CORE_SETS(6, "core sets"),
	HYPER_LOG_LOG(7, "hyper log log"),
	STICKY_SAMPLING(8, "sticky sampling"),
	LOSSY_COUNTING(9, "lossy counting"),
	CHAIN_SAMPLER(10, "chain sampler"),
	GK_QUANTILES(11, "gk quantiles"),
	MT(12, "maritime"),
	TOP_K(13, "top k"),
	OPT_DIST_WIN_SAMPLING(14, "optimal distributed window sampling"),
	OPT_DIST_SAMPLING(15, "optimal distributed sampling"),
	WIN_QUANTILES(16, "window quantiles");

	private final int id;

	private final String text;

	SynopsisType(int id, String text) {
		this.id = id;
		this.text = text;
	}

	@Override
	public String toString() {
		return text;
	}

	@JsonValue
	public int getId() {
		return id;
	}

	/**
	 * Returns the {@link SynopsisType} for which {@link SynopsisType#toString()} is equal to the provided String.
	 *
	 * @param text String which is checked against {@link SynopsisType#toString()}.
	 * @return {@link SynopsisType} for which {@link SynopsisType#toString()} is equal to the provided String
	 * @throws IllegalArgumentException if provided String is not equal to any {@link SynopsisType#toString()}
	 */
	public static SynopsisType fromString(String text) {
		for (SynopsisType synopsisType : SynopsisType.values()) {
			if (text.equals(synopsisType.text)) {
				return synopsisType;
			}
		}
		throw new IllegalArgumentException(
			String.format(
				"Provided string (%s) does not match with any known Synopsis. Allowed types are: %s",
				text,
				SynopsisType.values()));
	}

}