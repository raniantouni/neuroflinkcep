/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.agnostic_workflow;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * This is a container class for different computing sites, where streaming operators can be
 * pplaced. It contains the information of the name of the site and list of {@link
 * AWPlacementPlatform}s available there. The INFORE optimizer fills this by in the optimization
 * process.
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AWPlacementSite {
	
	private String siteName = null;
	private List<AWPlacementPlatform> availablePlatforms = null;
	
	/**
	 * Creates a new {@link AWPlacementSite} instance with the fields set to default values.
	 */
	public AWPlacementSite() {
	}
	public AWPlacementSite(AWPlacementSite other) {
		this.siteName = other.siteName;
		if (other.availablePlatforms != null) {
			this.availablePlatforms = new ArrayList<>();
			for (AWPlacementPlatform platform : other.availablePlatforms) {
				this.availablePlatforms.add(new AWPlacementPlatform(platform));
			}
		}
	}
	/**
	 * Returns the name of the {@link AWPlacementSite}.
	 *
	 * @return name of the {@link AWPlacementSite}
	 */
	public String getSiteName() {
		return siteName;
	}
	
	/**
	 * Sets the name of the {@link AWPlacementSite} to the provided one. Returns itself, so that set
	 * methods can be chained.
	 *
	 * @param siteName
	 * 		new name of the {@link AWPlacementSite}
	 * @return this {@link AWPlacementSite}
	 */
	public AWPlacementSite setSiteName(String siteName) {
		this.siteName = siteName;
		return this;
	}
	
	/**
	 * Returns the list of available {@link AWPlacementPlatform}s of the {@link AWPlacementSite}.
	 *
	 * @return list of available {@link AWPlacementPlatform}s of the {@link AWPlacementSite}
	 */
	public List<AWPlacementPlatform> getAvailablePlatforms() {
		return availablePlatforms;
	}
	
	/**
	 * Sets the list of available {@link AWPlacementPlatform}s of the {@link AWPlacementSite} to the
	 * provided one. Returns itself, so that set methods can be chained.
	 *
	 * @param availablePlatforms
	 * 		new list of available {@link AWPlacementPlatform}s of the {@link AWPlacementSite}
	 * @return this {@link AWPlacementSite}
	 */
	public AWPlacementSite setAvailablePlatforms(List<AWPlacementPlatform> availablePlatforms) {
		this.availablePlatforms = availablePlatforms;
		return this;
	}
	public AWPlacementPlatform getPlatformByName(String name) {
		if (availablePlatforms == null) {
			return null;
		}
		return availablePlatforms.stream()
				.filter(p -> p.getPlatformName() != null && p.getPlatformName().equals(name))
				.findFirst()
				.orElse(null);
	}
	
}
