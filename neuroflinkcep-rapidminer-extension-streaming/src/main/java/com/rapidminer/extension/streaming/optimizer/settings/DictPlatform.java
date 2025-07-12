/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.settings;

import java.util.Map;


/**
 * This is a container class for the physical implementation of a {@link DictOperator} on a specific
 * streaming platform.
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
public class DictPlatform {
	
	private String operatorName = null;
	
	private int staticCost = 0;
	
	private Map<String,Integer> migrationCosts = null;
	
	/**
	 * Creates a new {@link DictPlatform} instance with the fields set to default values.
	 */
	public DictPlatform() {
	}
	
	/**
	 * Returns the name of the physical implementation of the operator on the corresponding
	 * platform.
	 *
	 * @return name of the physical implementation of the operator on the corresponding platform
	 */
	public String getOperatorName() {
		return operatorName;
	}
	
	/**
	 * Sets name of the physical implementation of the operator on the corresponding platform to the
	 * provided one. Returns itself, so that set methods can be chained.
	 *
	 * @param operatorName
	 * 		new name of the physical implementation of the operator on the corresponding platform
	 * @return this {@link DictPlatform}
	 */
	public DictPlatform setOperatorName(String operatorName) {
		this.operatorName = operatorName;
		return this;
	}
	
	public int getStaticCost() {
		return staticCost;
	}
	
	public DictPlatform setStaticCost(int staticCost) {
		this.staticCost = staticCost;
		return this;
	}
	
	public Map<String,Integer> getMigrationCosts() {
		return migrationCosts;
	}
	
	public DictPlatform setMigrationCosts(Map<String,Integer> migrationCosts) {
		this.migrationCosts = migrationCosts;
		return this;
	}
}
