/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.settings;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rapidminer.extension.streaming.optimizer.agnostic_workflow.AgnosticWorkflow;


/**
 * This is a container class for the response of the Optimizer Service
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OptimizerResponse {
	
	private String id = null;
	private String optimizationRequestId = null;
	@JsonFormat(pattern = "MMM dd, yyyy, hh:mm:ss aa", locale = "en-US")
	private Date modifiedAt = null;
	private String networkName = null;
	private String dictionaryName = null;
	private String algorithmUsed = null;
	private AgnosticWorkflow workflow = null;
	private String operatorsPretty = null;
	private String performance = null;
	
	/**
	 * Creates a new {@link AgnosticWorkflow} instance with the fields set to {@code null} or new
	 * {@link ArrayList}s.
	 */
	public OptimizerResponse() {}
	
	/**
	 * Returns the id of the optimizer response
	 *
	 * @return id of the optimizer response
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Sets the id of this optimizer response. Returns itself, so that set methods can be chained.
	 *
	 * @param id
	 * 		new id of this optimizer response
	 * @return this {@link OptimizerResponse}
	 */
	public OptimizerResponse setId(String id) {
		this.id = id;
		return this;
	}

	/**
	 * Returns the id of the request for optimization
	 *
	 * @return id of the request for optimization
	 */
	public String getOptimizationRequestId() {
		return optimizationRequestId;
	}

	/**
	 * Sets the id of the request for optimization. Returns itself, so that set methods can be
	 * chained.
	 *
	 * @param optimizationRequestId
	 * 		new id of the request for optimization
	 * @return this {@link OptimizerResponse}
	 */
	public OptimizerResponse setOptimizationRequestId(String optimizationRequestId) {
		this.optimizationRequestId = optimizationRequestId;
		return this;
	}

	public Date getModifiedAt() {
		return modifiedAt;
	}

	public OptimizerResponse setModifiedAt(Date modifiedAt) {
		this.modifiedAt = modifiedAt;
		return this;
	}

	/**
	 * Returns the name of the network used in this optimization
	 *
	 * @return name of the network used in this optimization
	 */
	public String getNetworkName() {
		return networkName;
	}
	
	/**
	 * Sets the name of the network used in this optimization. Returns itself, so that set methods
	 * can be chained.
	 *
	 * @param networkName
	 * 		new name of the network used in this optimization
	 * @return this {@link OptimizerResponse}
	 */
	public OptimizerResponse setNetworkName(String networkName) {
		this.networkName = networkName;
		return this;
	}
	
	/**
	 * Returns the name of the dictionary used in this optimization
	 *
	 * @return name of the dictionary used in this optimization
	 */
	public String getDictionaryName() {
		return dictionaryName;
	}
	
	/**
	 * Sets the name of the dictionary used in this optimization. Returns itself, so that set
	 * methods can be chained.
	 *
	 * @param dictionaryName
	 * 		new name of the dictionary used in this optimization
	 * @return this {@link OptimizerResponse}
	 */
	public OptimizerResponse setDictionaryName(String dictionaryName) {
		this.dictionaryName = dictionaryName;
		return this;
	}
	
	/**
	 * Returns the name of the algorithm used in this optimization
	 *
	 * @return name of the algorithm used in this optimization
	 */
	public String getAlgorithmUsed() {
		return algorithmUsed;
	}
	
	/**
	 * Sets the name of the algorithm used in this optimization. Returns itself, so that set methods
	 * can be chained.
	 *
	 * @param algorithmUsed
	 * 		new name of the algorithm used in this optimization
	 * @return this {@link OptimizerResponse}
	 */
	public OptimizerResponse setAlgorithmUsed(String algorithmUsed) {
		this.algorithmUsed = algorithmUsed;
		return this;
	}
	
	/**
	 * Returns the optimized workflow.
	 *
	 * @return optimized workflow
	 */
	public AgnosticWorkflow getWorkflow() {
		return workflow;
	}
	
	/**
	 * Sets the optimized workflow. Returns itself, so that set methods can be chained.
	 *
	 * @param workflow
	 * 		new optimized workflow
	 * @return this {@link OptimizerResponse}
	 */
	public OptimizerResponse setWorkflow(AgnosticWorkflow workflow) {
		this.workflow = workflow;
		return this;
	}
	
	/**
	 * Returns a description of the operator cost and placement evaluation
	 *
	 * @return description of the operator cost and placement evaluation
	 */
	public String getOperatorsPretty() {
		return operatorsPretty;
	}
	
	/**
	 * Sets the description of the operator cost and placement evaluation. Returns itself, so that
	 * set methods can be chained.
	 *
	 * @param operatorsPretty
	 * 		new description of the operator cost and placement evaluation
	 * @return this {@link OptimizerResponse}
	 */
	public OptimizerResponse setOperatorsPretty(String operatorsPretty) {
		this.operatorsPretty = operatorsPretty;
		return this;
	}
	
	/**
	 * Returns a the performance of this optimization
	 *
	 * @return performance of this optimization
	 */
	public String getPerformance() {
		return performance;
	}
	
	/**
	 * Sets the performance of this optimization. Returns itself, so that set methods can be
	 * chained.
	 *
	 * @param performance
	 * 		new performance of this optimization
	 * @return this {@link OptimizerResponse}
	 */
	public OptimizerResponse setPerformance(String performance) {
		this.performance = performance;
		return this;
	}
}
