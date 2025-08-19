/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.agnostic_workflow;

import java.util.ArrayList;
import java.util.List;


/**
 * This is a container class for the schema properties of a data set at an {@link AWPort} of an
 * {@link AWOperator} in an agnostic workflow. It contains the information of the size of the data
 * set, and a list of {@link AWAttribute}s. It is used to serialize the information to JSON, so that
 * the INFORE optimizer can process it.
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
public class AWSchema {
	
	private Boolean fromMetaData = null;
	
	private Integer size = null;
	
	private List<AWAttribute> attributes = new ArrayList<>();
	
	/**
	 * Creates a new {@link AWSchema} instance with the fields set to default values.
	 */
	public AWSchema() {}

	public AWSchema(AWSchema aWSchema) {
		this.fromMetaData = aWSchema.getFromMetaData();
		this.size = aWSchema.getSize();
		this.attributes = aWSchema.getAttributes();
	}

	/**
	 * Returns the size of the data set.
	 *
	 * @return size of the data set
	 */
	public Integer getSize() {
		return size;
	}
	
	/**
	 * Sets the size of the data set to the provided one. Returns itself, so that set methods can be
	 * chained.
	 *
	 * @param size
	 * 		new size of the data set
	 * @return this {@link AWSchema}
	 */
	public AWSchema setSize(Integer size) {
		this.size = size;
		return this;
	}
	
	/**
	 * Returns the list of {@link AWAttribute}s of the {@link AWSchema}.
	 *
	 * @return list of {@link AWAttribute}s of the {@link AWSchema}
	 */
	public List<AWAttribute> getAttributes() {
		return attributes;
	}
	
	/**
	 * Sets the list of {@link AWAttribute}s of the {@link AWSchema} to the provided one. Returns
	 * itself, so that set methods can be chained.
	 *
	 * @param attributes
	 * 		new  list of {@link AWAttribute}s of the {@link AWSchema}
	 * @return this {@link AWSchema}
	 */
	public AWSchema setAttributes(List<AWAttribute> attributes) {
		this.attributes = attributes;
		return this;
	}
	
	/**
	 * Returns {@code true} if the information were received from meta data or {@code false} when
	 * they were received from real data.
	 *
	 * @return {@code true} if the information were received from meta data or {@code false} when
	 * they were received from real data
	 */
	public Boolean getFromMetaData() {
		return fromMetaData;
	}
	
	/**
	 * Sets if the information of this {@link AWSchema} were received from meta data or real data.
	 * Returns itself, so that set methods can be chained.
	 *
	 * @param fromMetaData
	 *        {@code true} if the information were received from meta data or {@code false} when they
	 * 		were received from real data
	 * @return this {@link AWSchema}
	 */
	public AWSchema setFromMetaData(Boolean fromMetaData) {
		this.fromMetaData = fromMetaData;
		return this;
	}
	
}
