/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.settings;

import java.util.Arrays;


/**
 * @author Fabian Temme
 * @since 0.4.0
 */
public class OptimizationParameters {
	
	
	
	/**
	 * This enum provides the currently supported optimization algorithms of the INFORE optimizer.
	 *
	 * @author Fabian Temme
	 * @since 0.1.0
	 */
	public enum OptimizerAlgorithm {
		/**
		 * fast but with large memory footprint
		 */
		A_STAR("a*", "op-A*"),
		/**
		 * slow but optimal
		 */
		EXHAUSTIVE("DAG*4CER", "op-ES"),
		/**
		 * Fast but approximate solutions
		 */
		GREEDY("greedy", "op-GS"),
		/**
		 * Automatic (Recommended)
		 */
		AUTO("automatic", "auto");
		
		private final String description;
		private final String key;
		
		OptimizerAlgorithm(String description, String key) {
			this.description = description;
			this.key = key;
		}
		
		/**
		 * Returns a descriptive name of the algorithm, which can be used as the value in the
		 * parameter category.
		 *
		 * @return descriptive name of the algorithm, which can be used as the value in the
		 * parameter category
		 */
		public String getDescription() {
			return description;
		}
		
		/**
		 * Returns the key used by the INFORE optimizer for this algorithm.
		 *
		 * @return key used by the INFORE optimizer for this algorithm
		 */
		public String getKey() {
			return key;
		}
		
		/**
		 * Returns the {@link OptimizerAlgorithm} for which {@link OptimizerAlgorithm#toString()} is
		 * equal to the provided String.
		 *
		 * @param text
		 * 		String which is checked against {@link OptimizerAlgorithm#toString()}.
		 * @return {@link OptimizerAlgorithm} for which {@link OptimizerAlgorithm#toString()} is
		 * equal to the provided String
		 * @throws IllegalArgumentException
		 * 		if provided String is not equal to any {@link OptimizerAlgorithm#toString()} method
		 * 		calls
		 */
		public static OptimizerAlgorithm getMethod(String text) {
			for (OptimizerAlgorithm method : OptimizerAlgorithm.values()) {
				if (method.description.equals(text)) {
					return method;
				}
			}
			throw new IllegalArgumentException(
					"Provided text (" + text + ") does not match with an OptimizerAlgorithm. " + "Allowed texts are: " + Arrays
							.toString(OptimizerAlgorithm.values()));
		}
	}
	
	
	private boolean continuous = true;
	
	private String networkName = null;
	
	private String dictionaryName = null;
	
	private String algorithm = null;
	
	private Long parallelism = 0L;
	
	private String description = null;
	
	private long timeout_ms = 0;
	
	private Long numOfPlans = 0L;
	
	public OptimizationParameters() {
	}
	
	/**
	 * Returns if the optimization should run continuously.
	 *
	 * @return if the optimization should run continuously
	 */
	public boolean isContinuous() {
		return continuous;
	}
	
	/**
	 * Sets if the optimization should run continuously. Returns itself, so that set
	 * methods can be chained.
	 *
	 * @param continuous
	 * 		{@code true} if the optimization should run continuously, {@code false} otherwise
	 * @return this {@link OptimizationParameters}
	 */
	public OptimizationParameters setContinuous(boolean continuous) {
		this.continuous = continuous;
		return this;
	}
	
	/**
	 * Returns the name of the network to be used.
	 *
	 * @return name of the network to be used
	 */
	public String getNetworkName() {
		return networkName;
	}
	
	/**
	 * Sets the name of the network to be used to the provided one. Returns itself, so that set
	 * methods can be chained.
	 *
	 * @param networkName
	 * 		name of the network to be used
	 * @return this {@link OptimizationParameters}
	 */
	public OptimizationParameters setNetworkName(String networkName) {
		this.networkName = networkName;
		return this;
	}
	
	/**
	 * Returns the name of the dictionary to be used.
	 *
	 * @return name of the dictionary to be used
	 */
	public String getDictionaryName() {
		return dictionaryName;
	}
	
	/**
	 * Sets the name of the dictionary to be used to the provided one. Returns itself, so that set
	 * methods can be chained.
	 *
	 * @param dictionaryName
	 * 		name of the dictionary to be used
	 * @return this {@link OptimizationParameters}
	 */
	public OptimizationParameters setDictionaryName(String dictionaryName) {
		this.dictionaryName = dictionaryName;
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
	 * @return this {@link OptimizationParameters}
	 */
	public OptimizationParameters setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
		return this;
	}
	
	/**
	 * Returns the name of the cost estimation algorithm of the optimizer to be used.
	 *
	 * @return name of the cost estimation algorithm of the optimizer to be used
	 */
	public String getDescription() {
		return description;
	}
	
	/**
	 * Sets the name
	 *
	 * @param description
	 * 		name
	 * @return this {@link OptimizationParameters}
	 */
	public OptimizationParameters setDescription(String description) {
		this.description = description;
		return this;
	}
	
	/**
	 * Returns the parallelism level of the optimizer to be used.
	 *
	 * @return parallelism level of the optimizer to be used
	 */
	public Long getParallelism() {
		return parallelism;
	}
	
	/**
	 * Sets the parallelism level of the optimizer to be used to the provided one. Returns itself,
	 * so that set methods can be chained.
	 *
	 * @param parallelism
	 * 		parallelism level of the optimizer to be used
	 * @return this {@link OptimizationParameters}
	 */
	public OptimizationParameters setParallelism(Long parallelism) {
		this.parallelism = parallelism;
		return this;
	}
	
	/**
	 * Returns the timeout of the optimizer.
	 *
	 * @return timeout of the optimizer
	 */
	public long getTimeout_ms() {
		return timeout_ms;
	}
	
	/**
	 * Sets the timeout of the optimizer to the provided one. Returns itself, so that set methods
	 * can be chained.
	 *
	 * @param timeout_ms
	 * 		timeout of the optimizer
	 * @return this {@link OptimizationParameters}
	 */
	public OptimizationParameters setTimeout_ms(long timeout_ms) {
		this.timeout_ms = timeout_ms;
		return this;
	}
	
	/**
	 * Returns the number of plans to be optimized by the optimizer.
	 *
	 * @return number of plans to be optimized by the optimizer
	 */
	public Long getNumOfPlans() {
		return numOfPlans;
	}
	
	/**
	 * Sets the number of plans to be optimized by the optimizer to the provided one. Returns itself, so that set methods
	 * can be chained.
	 *
	 * @param numOfPlans
	 * 		number of plans to be optimized by the optimizer
	 * @return this {@link OptimizationParameters}
	 */
	public OptimizationParameters setNumOfPlans(Long numOfPlans) {
		this.numOfPlans = numOfPlans;
		return this;
	}
}
