/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.agnostic_workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * @author Fabian Temme
 * @since 0.5.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AWPlacementOperator {

	private String name = null;
	@JsonProperty("old_path")
	private String oldPath = null;
	@JsonProperty("new_path")
	private String newPath = null;

	public AWPlacementOperator() {
	}

	public AWPlacementOperator(AWPlacementOperator aWPlacementOperator) {
		this.name = aWPlacementOperator.getName();
		this.oldPath = aWPlacementOperator.getOldPath();
		this.newPath = aWPlacementOperator.getNewPath();
	}

	/**
	 * Returns the name of the operator placed.
	 *
	 * @return name of the operator placed
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the operator to be placed. Returns itself, so that set methods can be chained.
	 *
	 * @param name
	 * 		new name of the operator to be placed
	 * @return this {@link AWPlacementOperator}
	 */
	public AWPlacementOperator setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Returns the old path of the checkpoint stored.
	 *
	 * @return old path of the checkpoint stored
	 */
	public String getOldPath() {
		return oldPath;
	}

	/**
	 * Sets the old path of the checkpoint of the operator to be placed. Returns itself, so that set methods can be
	 * chained.
	 *
	 * @param oldPath
	 * 		new old path of the checkpoint of the operator to be placed
	 * @return this {@link AWPlacementOperator}
	 */
	public AWPlacementOperator setOldPath(String oldPath) {
		this.oldPath = oldPath;
		return this;
	}

	/**
	 * Returns the new path of the checkpoint to be stored.
	 *
	 * @return new path of the checkpoint to be stored
	 */
	public String getNewPath() {
		return newPath;
	}

	/**
	 * Sets the new path of the checkpoint of the operator to be placed. Returns itself, so that set methods can be
	 * chained.
	 *
	 * @param newPath
	 * 		new newPath of the checkpoint of the operator to be placed
	 * @return this {@link AWPlacementOperator}
	 */
	public AWPlacementOperator setNewPath(String newPath) {
		this.newPath = newPath;
		return this;
	}
}
