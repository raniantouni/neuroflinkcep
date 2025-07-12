/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.settings;

import java.util.List;


/**
 * This is a container class to hold the information of a single computing site (physical computing
 * cluster with one or more streaming platforms available). The {@link OptimizerSite} is used by the
 * INFORE optimizer to perform the optimization and decide on the placement of the operators on the
 * different platforms and sites.
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
public class OptimizerSite {
	
	private String siteName = null;
	
	private List<OptimizerPlatform> availablePlatforms = null;
	
	/**
	 * Creates a new {@link OptimizerSite} instance with the fields set to default values.
	 */
	public OptimizerSite() {
	}
	
	/**
	 * Returns the name of the {@link OptimizerSite}.
	 *
	 * @return name of the {@link OptimizerSite}
	 */
	public String getSiteName() {
		return siteName;
	}
	
	/**
	 * Sets the name of the {@link OptimizerSite} to the provided one. Returns itself, so that set
	 * methods can be chained.
	 *
	 * @param siteName
	 * 		new name of the {@link OptimizerSite}
	 * @return this {@link OptimizerSite}
	 */
	public OptimizerSite setSiteName(String siteName) {
		this.siteName = siteName;
		return this;
	}
	
	/**
	 * Returns the list of available {@link OptimizerPlatform}s at the {@link OptimizerSite}.
	 *
	 * @return list of available {@link OptimizerPlatform}s at the {@link OptimizerSite}
	 */
	public List<OptimizerPlatform> getAvailablePlatforms() {
		return availablePlatforms;
	}
	
	/**
	 * Sets the list of available {@link OptimizerPlatform}s at the {@link OptimizerSite} to the
	 * provided one. Returns itself, so that set methods can be chained.
	 *
	 * @param availablePlatforms
	 * 		new list of available {@link OptimizerPlatform}s at the {@link OptimizerSite}
	 * @return this {@link OptimizerSite}
	 */
	public OptimizerSite setAvailablePlatforms(List<OptimizerPlatform> availablePlatforms) {
		this.availablePlatforms = availablePlatforms;
		return this;
	}
	
}
