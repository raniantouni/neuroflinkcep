/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.settings;

/**
 * This is a container class holding the configuration for the INFORE optimizer. The method {@link
 * #create()} provides the functionality to create a dummy {@link OptimizerConfig}.
 *
 * @author Fabian Temme
 * @since 0.1.0
 * @deprecated since 0.1.0 (INF-61), cause there is no configuration JSON for the INFORE optimizer
 * anymore. The version and algorithm are provided in the {@link OptimizerRequest}
 */
@Deprecated
public class OptimizerConfig {
	
	private String version = null;
	
	private String algorithm = null;
	
	private String costEstimator = null;
	
	private int parallelism = 0;
	
	/**
	 * Creates a new {@link OptimizerConfig} instance with the fields set to default values.
	 */
	public OptimizerConfig() {
	}
	
	/**
	 * Returns the version of the optimizer to be used.
	 *
	 * @return version of the optimizer to be used
	 */
	public String getVersion() {
		return version;
	}
	
	/**
	 * Sets the version of the optimizer to be used to the provided one. Returns itself, so that set
	 * methods can be chained.
	 *
	 * @param version
	 * 		version of the optimizer to be used
	 * @return this {@link OptimizerConfig}
	 */
	public OptimizerConfig setVersion(String version) {
		this.version = version;
		return this;
	}
	
	/**
	 * Returns the name of the optimization algorithm of the optimizer to be used.
	 *
	 * @return name of the optimization algorithm of the optimizer to be used
	 */
	public String getAlgorithm() {
		return algorithm;
	}
	
	/**
	 * Sets the name of the optimization algorithm of the optimizer to be used to the provided one.
	 * Returns itself, so that set methods can be chained.
	 *
	 * @param algorithm
	 * 		name of the optimization algorithm of the optimizer to be used
	 * @return this {@link OptimizerConfig}
	 */
	public OptimizerConfig setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
		return this;
	}
	
	/**
	 * Returns the name of the cost estimation algorithm of the optimizer to be used.
	 *
	 * @return name of the cost estimation algorithm of the optimizer to be used
	 */
	public String getCostEstimator() {
		return costEstimator;
	}
	
	/**
	 * Sets the name of the cost estimation algorithm of the optimizer to be used to the provided
	 * one. Returns itself, so that set methods can be chained.
	 *
	 * @param costEstimator
	 * 		name of the cost estimation algorithm of the optimizer to be used
	 * @return this {@link OptimizerConfig}
	 */
	public OptimizerConfig setCostEstimator(String costEstimator) {
		this.costEstimator = costEstimator;
		return this;
	}
	
	/**
	 * Returns the parallelism level of the optimizer to be used.
	 *
	 * @return parallelism level of the optimizer to be used
	 */
	public int getParallelism() {
		return parallelism;
	}
	
	/**
	 * Sets the parallelism level of the optimizer to be used to the provided one. Returns itself,
	 * so that set methods can be chained.
	 *
	 * @param parallelism
	 * 		parallelism level of the optimizer to be used
	 * @return this {@link OptimizerConfig}
	 */
	public OptimizerConfig setParallelism(int parallelism) {
		this.parallelism = parallelism;
		return this;
	}
	
	/**
	 * Creates a dummy {@link OptimizerConfig} configuration with hardcoded values:
	 *
	 * <ul>
	 *     <li>version: 1.0</li>
	 *     <li>optimization algorithm: exhaustive</li>
	 *     <li>cost estimation algorithm: dummy</li>
	 *     <li>parallelism level: 1.0</li>
	 * </ul>
	 *
	 * @return dummy {@link OptimizerConfig} configuration with hardcoded values
	 */
	public static OptimizerConfig create() {
		return new OptimizerConfig().setVersion("1.0")
									.setAlgorithm("exhaustive")
									.setCostEstimator("dummy")
									.setParallelism(1);
	}
	
}
