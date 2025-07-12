/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.settings;

import java.util.ArrayList;

import com.rapidminer.extension.streaming.optimizer.agnostic_workflow.AgnosticWorkflow;
import com.rapidminer.extension.streaming.optimizer.settings.OptimizationParameters.OptimizerAlgorithm;


/**
 * This is a container class for the request send to the INFORE Optimizer Service
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
public class OptimizerRequest extends AgnosticWorkflow {
	
	private OptimizationParameters optimizationParameters = null;
	
	/**
	 * Creates a new {@link OptimizerRequest} instance with the fields set to default values.
	 */
	public OptimizerRequest() {
	}
	
	/**
	 * Returns the optimization parameter to be used
	 *
	 * @return optimization parameter to be used
	 */
	public OptimizationParameters getOptimizationParameters() {
		return optimizationParameters;
	}
	
	/**
	 * Sets the optimization parameters to be used to the provided one. Returns itself, so that set
	 * methods can be chained.
	 *
	 * @param optimizationParameters
	 * 		optimization parameters to be used
	 * @return this {@link OptimizationParameters}
	 */
	public OptimizerRequest setOptimizationParameters(
			OptimizationParameters optimizationParameters) {
		this.optimizationParameters = optimizationParameters;
		return this;
	}
	
	/**
	 * Creates an {@link OptimizerRequest} configuration with the provided values (and some
	 * hardcoded values, which could be changed after by using the corresponding set methods). The
	 * hardcoded values are
	 *
	 * <ul>
	 *     <li>parallelism level: 1.0</li>
	 *     <li>timeout: 10000 ms</li>
	 *     <li>description: "" (empty string)</li>
	 * </ul>
	 *
	 * @return dummy {@link OptimizerRequest} configuration with hardcoded values
	 */
	public static OptimizerRequest create(String networkName, String dictionaryName,
										  OptimizerAlgorithm algorithm, AgnosticWorkflow workflow, boolean continuous,
										  Long numberOfPlans) {
		OptimizerRequest result = new OptimizerRequest();
		result.setEnclosingOperatorName(workflow.getEnclosingOperatorName());
		result.setWorkflowName(workflow.getWorkflowName());
		result.setOperators(new ArrayList<>(workflow.getOperators()));
		result.setOperatorConnections(new ArrayList<>(workflow.getOperatorConnections()));
		result.setInnerSinksPortsAndSchemas(
				new ArrayList<>(workflow.getInnerSinksPortsAndSchemas()));
		result.setInnerSourcesPortsAndSchemas(
				new ArrayList<>(workflow.getInnerSourcesPortsAndSchemas()));
		result.setPlacementSites(new ArrayList<>(workflow.getPlacementSites()));
		OptimizationParameters parameters = new OptimizationParameters().setNetworkName(networkName)
																		.setContinuous(continuous)
																		.setDictionaryName(
																				dictionaryName)
																		.setAlgorithm(
																				algorithm.getKey())
																		.setParallelism(1L)
																		.setTimeout_ms(50000)
																		.setDescription("Test")
																		.setNumOfPlans(numberOfPlans);
		result.setOptimizationParameters(parameters);
		return result;
	}
}
