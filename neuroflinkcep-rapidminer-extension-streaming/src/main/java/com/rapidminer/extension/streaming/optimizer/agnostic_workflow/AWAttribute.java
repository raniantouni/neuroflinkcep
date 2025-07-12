/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.agnostic_workflow;

/**
 * This is a container class for the attribute properties in an agnostic workflow. It contains the
 * information of a single attribute in an {@link AWSchema}. It is used to serialize the information
 * to JSON, so that the INFORE optimizer can process it.
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
public class AWAttribute {
	
	private String name = "";
	
	private String type = "";
	
	private String specialRole = "";
	
	/**
	 * Creates a new {@link AWAttribute} instance with all fields set to empty strings.
	 */
	public AWAttribute() {
	}
	
	/**
	 * Returns the name of the attribute.
	 *
	 * @return name of the attribute
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the name of the {@link AWAttribute} to the provided one. Returns itself, so that set
	 * methods can be chained.
	 *
	 * @param name
	 * 		new name of the {@link AWAttribute}
	 * @return this {@link AWAttribute}
	 */
	public AWAttribute setName(String name) {
		this.name = name;
		return this;
	}
	
	/**
	 * Returns the type of the attribute.
	 *
	 * @return type of the attribute
	 */
	public String getType() {
		return type;
	}
	
	/**
	 * Sets the type of the {@link AWAttribute} to the provided one. Returns itself, so that set
	 * methods can be chained.
	 *
	 * @param type
	 * 		new type of the {@link AWAttribute}
	 * @return this {@link AWAttribute}
	 */
	public AWAttribute setType(String type) {
		this.type = type;
		return this;
	}
	
	/**
	 * Returns the special role of the attribute.
	 *
	 * @return special role of the attribute
	 */
	public String getSpecialRole() {
		return specialRole;
	}
	
	/**
	 * Sets the special role of the {@link AWAttribute} to the provided one. Returns itself, so that
	 * set methods can be chained.
	 *
	 * @param specialRole
	 * 		new special role of the {@link AWAttribute}
	 * @return this {@link AWAttribute}
	 */
	public AWAttribute setSpecialRole(String specialRole) {
		this.specialRole = specialRole;
		return this;
	}
	
}
