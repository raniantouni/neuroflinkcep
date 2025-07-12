/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.settings;

import java.util.List;
import java.util.Map;

/**
 * @author Fabian Temme
 * @since 0.4.0
 */
public class DictSite {

	private int staticCost = 0;

	private Map<String,Integer> migrationCosts = null;

	private List<Map<String, DictPlatform>> platforms = null;

	public List<Map<String, DictPlatform>> getPlatforms() {
		return platforms;
	}

	public void setPlatforms(List<Map<String, DictPlatform>> platforms) {
		this.platforms = platforms;
	}

	/**
	 * Creates a new {@link DictSite} instance with the fields set to default values.
	 */
	public DictSite() {
	}

	public int getStaticCost() {
		return staticCost;
	}

	public DictSite setStaticCost(int staticCost) {
		this.staticCost = staticCost;
		return this;
	}

	public Map<String,Integer> getMigrationCosts() {
		return migrationCosts;
	}

	public DictSite setMigrationCosts(Map<String,Integer> migrationCosts) {
		this.migrationCosts = migrationCosts;
		return this;
	}
}