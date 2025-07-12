/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.agnostic_workflow;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * This is a container class for the placement of operators on a streaming platform. It contains the
 * information of the name of the platform and the names of the operators placed there. The INFORE
 * optimizer fills this by in the optimization process.
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AWPlacementPlatform {
	
	private String platformName = null;
	private List<AWPlacementOperator> operators = null;
	
	/**
	 * Creates a new {@link AWPlacementPlatform} instance with the fields set to default values.
	 */
	public AWPlacementPlatform() {
	}
	
	/**
	 * Returns the name of the {@link AWPlacementPlatform}.
	 *
	 * @return name of the {@link AWPlacementPlatform}
	 */
	public String getPlatformName() {
		return platformName;
	}
	
	/**
	 * Sets the name of the {@link AWPlacementPlatform} to the provided one. Returns itself, so that
	 * set methods can be chained.
	 *
	 * @param platformName
	 * 		new name of the {@link AWPlacementPlatform}
	 * @return this {@link AWPlacementPlatform}
	 */
	public AWPlacementPlatform setPlatformName(String platformName) {
		this.platformName = platformName;
		return this;
	}
	
	/**
	 * Returns the list of names of the operator placed in the {@link AWPlacementPlatform}.
	 *
	 * @return list of names of the operator placed in the {@link AWPlacementPlatform}
	 */
	public List<AWPlacementOperator> getOperators() {
		return operators;
	}
	
	/**
	 * Sets the list of names of the operator placed in the {@link AWPlacementPlatform} to the
	 * provided one. Returns itself, so that set methods can be chained.
	 *
	 * @param operators
	 * 		new list of names of the operator placed in the {@link AWPlacementPlatform}
	 * @return this {@link AWPlacementPlatform}
	 */
	public AWPlacementPlatform setOperators(List<AWPlacementOperator> operators) {
		this.operators = operators;
		return this;
	}
	
}
