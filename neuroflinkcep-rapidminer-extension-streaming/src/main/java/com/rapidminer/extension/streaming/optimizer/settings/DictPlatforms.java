/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.settings;

/**
 * This is a container class for the platforms for which physical implementations of a {@link
 * DictOperator} are available. Currently only {@link #spark} and {@link #flink}  can be specified.
 * If they are not specified (by calling the corresponding {@link #setSpark(DictPlatform)}, {@link
 * #setFlink(DictPlatform)} method), there is no physical implementation of the {@link DictOperator}
 * on this platform.
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
public class DictPlatforms {
	
	private DictPlatform spark = null;
	
	private DictPlatform flink = null;
	
	/**
	 * Creates a new {@link DictPlatforms} instance with the fields set to default values.
	 */
	public DictPlatforms() {
	}
	
	/**
	 * Returns the {@link DictPlatform} describing the physical implementation of the {@link
	 * DictOperator} in spark.
	 *
	 * @return {@link DictPlatform} describing the physical implementation of the {@link
	 * DictOperator} in spark
	 */
	public DictPlatform getSpark() {
		return spark;
	}
	
	/**
	 * Sets the {@link DictPlatform} describing the physical implementation of the {@link
	 * DictOperator} in spark to the provided one. Returns itself, so that set methods can be
	 * chained.
	 *
	 * @param spark
	 *        {@link DictPlatform} describing the physical implementation of the {@link DictOperator} in
	 * 		spark
	 * @return this {@link DictPlatform}
	 */
	public DictPlatforms setSpark(DictPlatform spark) {
		this.spark = spark;
		return this;
	}
	
	/**
	 * Returns the {@link DictPlatform} describing the physical implementation of the {@link
	 * DictOperator} in flink.
	 *
	 * @return @link DictPlatform} describing the physical implementation of the {@link
	 * DictOperator} in flink
	 */
	public DictPlatform getFlink() {
		return flink;
	}
	
	/**
	 * Sets {@link DictPlatform} describing the physical implementation of the {@link DictOperator}
	 * in flink to the provided one. Returns itself, so that set methods can be chained.
	 *
	 * @param flink
	 *        {@link DictPlatform} describing the physical implementation of the {@link DictOperator} in
	 * 		flink
	 * @return this {@link DictPlatform}
	 */
	public DictPlatforms setFlink(DictPlatform flink) {
		this.flink = flink;
		return this;
	}
	
}
