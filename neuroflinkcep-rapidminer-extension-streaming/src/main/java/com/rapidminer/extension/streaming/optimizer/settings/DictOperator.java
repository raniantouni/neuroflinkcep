/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.settings;

import java.util.List;
import java.util.Map;


/**
 * This is a container class holding the dictionary information of a single operator class in the
 * {@link OperatorDictionary} used by the INFORE optimizer.
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
public class DictOperator {

	private String classKey = null;

	private int[] costCoefficients = null;

	private int inputRate = 0;

//	private Map<String,DictPlatform> platforms = null;

	private List<Map<String,DictSite>> sites = null;

	/**
	 * Creates a new {@link DictOperator} instance with the fields set to default values.
	 */
	public DictOperator() {
	}

	/**
	 * Returns the class key (the key used in RapidMiner to identify the class of the operator) of
	 * the {@link DictOperator}.
	 *
	 * @return class key (the key used in RapidMiner to identify the class of the operator) of the
	 * {@link DictOperator}
	 */
	public String getClassKey() {
		return classKey;
	}

	/**
	 * Sets the class key (the key used in RapidMiner to identify the class of the operator) of the
	 * {@link DictOperator} to the provided one. Returns itself, so that set methods can be
	 * chained.
	 *
	 * @param classKey
	 * 		new class key (the key used in RapidMiner to identify the class of the operator) of the
	 *        {@link DictOperator}
	 * @return this {@link DictOperator}
	 */
	public DictOperator setClassKey(String classKey) {
		this.classKey = classKey;
		return this;
	}

	public int[] getCostCoefficients() {
		return costCoefficients;
	}

	public DictOperator setCostCoefficients(int[] costCoefficients) {
		this.costCoefficients = costCoefficients;
		return this;
	}

	public int getInputRate() {
		return inputRate;
	}

	public DictOperator setInputRate(int inputRate) {
		this.inputRate = inputRate;
		return this;
	}

//	/**
//	 * Returns the platforms for which physical implementations of the {@link DictOperator} are
//	 * available.
//	 *
//	 * @return platforms for which physical implementations of the {@link DictOperator} are
//	 * available
//	 */
//	public Map<String,DictPlatform> getPlatforms() {
//		return platforms;
//	}

//	/**
//	 * Sets the platforms for which physical implementations of the {@link DictOperator} are
//	 * available to the provided ones. Returns itself, so that set methods can be chained.
//	 *
//	 * @param platforms
//	 * 		new {@link DictPlatforms} for which physical implementations of the {@link DictOperator} are available
//	 * @return this {@link DictOperator}
//	 */
//	public DictOperator setPlatforms(Map<String,DictPlatform> platforms) {
//		this.platforms = platforms;
//		return this;
//	}

	public List<Map<String,DictSite>> getSites() {
		return sites;
	}

	public DictOperator setSites(List<Map<String,DictSite>> sites) {
		this.sites = sites;
		return this;
	}
}