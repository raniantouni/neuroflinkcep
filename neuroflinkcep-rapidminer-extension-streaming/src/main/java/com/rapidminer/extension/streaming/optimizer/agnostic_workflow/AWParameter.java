/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.agnostic_workflow;

import com.rapidminer.parameter.ParameterType;


/**
 * This is a container class for the parameter properties of an {@link AWOperator} in an agnostic
 * workflow. It contains the information of the parameters of operators in an {@link
 * AgnosticWorkflow}. It is used to serialize the information to JSON, so that the INFORE optimizer
 * can process it.
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
public class AWParameter {
	
	private String key = "";
	
	private String value = "";
	
	private String defaultValue = "";
	
	private String range = null;
	
	private Class<? extends ParameterType> typeClass = ParameterType.class;
	
	/**
	 * Creates a new {@link AWParameter} instance with the fields set to default values.
	 */
	public AWParameter() {}

	public AWParameter(AWParameter aWParameter) {
		this.key = aWParameter.getKey();
		this.value = aWParameter.getValue();
		this.defaultValue = aWParameter.getDefaultValue();
		this.range = aWParameter.getRange();
		this.typeClass = aWParameter.getTypeClass();
	}


	/**
	 * Returns the key of the {@link AWParameter}.
	 *
	 * @return key of the {@link AWParameter}
	 */
	public String getKey() {
		return key;
	}
	
	/**
	 * Sets the key of the {@link AWParameter} to the provided one. Returns itself, so that set
	 * methods can be chained.
	 *
	 * @param key
	 * 		new key of the {@link AWParameter}
	 * @return this {@link AWParameter}
	 */
	public AWParameter setKey(String key) {
		this.key = key;
		return this;
	}
	
	/**
	 * Returns the current value of the {@link AWParameter}.
	 *
	 * @return current value of the {@link AWParameter}
	 */
	public String getValue() {
		return value;
	}
	
	/**
	 * Sets the current value of the {@link AWParameter} to the provided one. Returns itself, so
	 * that set methods can be chained.
	 *
	 * @param value
	 * 		new current value of the {@link AWParameter}
	 * @return this {@link AWParameter}
	 */
	public AWParameter setValue(String value) {
		this.value = value;
		return this;
	}
	
	/**
	 * Returns the default value of the {@link AWParameter}.
	 *
	 * @return default value of the {@link AWParameter}
	 */
	public String getDefaultValue() {
		return defaultValue;
	}
	
	/**
	 * Sets the default value of the {@link AWParameter} to the provided one. Returns itself, so
	 * that set methods can be chained.
	 *
	 * @param defaultValue
	 * 		new default value of the {@link AWParameter}
	 * @return this {@link AWParameter}
	 */
	public AWParameter setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}
	
	/**
	 * Returns the range of the {@link AWParameter}.
	 *
	 * @return range of the {@link AWParameter}
	 */
	public String getRange() {
		return range;
	}
	
	/**
	 * Sets the range of the {@link AWParameter} to the provided one. Returns itself, so that set
	 * methods can be chained.
	 *
	 * @param range
	 * 		new range of the {@link AWParameter}
	 * @return this {@link AWParameter}
	 */
	public AWParameter setRange(String range) {
		this.range = range;
		return this;
	}
	
	/**
	 * Returns the class of the {@link ParameterType} which is described by the {@link
	 * AWParameter}.
	 *
	 * @return class of the {@link ParameterType} which is described by the {@link AWParameter}
	 */
	public Class<? extends ParameterType> getTypeClass() {
		return typeClass;
	}
	
	/**
	 * Sets the class of the {@link ParameterType} which is described by the {@link AWParameter} to
	 * the provided one. Returns itself, so that set methods can be chained.
	 *
	 * @param typeClass
	 * 		new class of the {@link ParameterType} which is described by the {@link AWParameter}
	 * @return this {@link AWParameter}
	 */
	public AWParameter setTypeClass(Class<? extends ParameterType> typeClass) {
		this.typeClass = typeClass;
		return this;
	}
	
}
