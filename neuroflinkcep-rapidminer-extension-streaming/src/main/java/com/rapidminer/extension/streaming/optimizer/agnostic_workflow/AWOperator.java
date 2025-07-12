/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.agnostic_workflow;

import java.util.ArrayList;
import java.util.List;

import com.rapidminer.operator.Operator;


/**
 * This is a container class for the operator properties in an agnostic workflow. It contains the
 * information of a single operator in an {@link AgnosticWorkflow}. It is used to serialize the
 * information to JSON, so that the INFORE optimizer can process it.
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
public class AWOperator {
	
	private String name = "";
	
	private String classKey = "";
	
	private Class<? extends Operator> operatorClass = Operator.class;
	
	private boolean isEnabled = true;
	
	private List<AWPort> inputPortsAndSchemas = new ArrayList<>();
	
	private List<AWPort> outputPortsAndSchemas = new ArrayList<>();
	
	private List<AWParameter> parameters = new ArrayList<>();
	
	private boolean hasSubprocesses = false;
	
	private Integer numberOfSubprocesses = null;
	
	private List<AgnosticWorkflow> innerWorkflows = null;
	
	private String platformName = null;
	
	/**
	 * Creates a new {@link AWOperator} instance with the string fields set to default values.
	 */
	public AWOperator() {
	}
	
	/**
	 * Returns the name of the operator.
	 *
	 * @return name of the operator
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the name of the {@link AWOperator} to the provided one. Returns itself, so that set
	 * methods can be chained.
	 *
	 * @param name
	 * 		new name of the {@link AWOperator}
	 * @return this {@link AWOperator}
	 */
	public AWOperator setName(String name) {
		this.name = name;
		return this;
	}
	
	/**
	 * Returns the class key (the key used in RapidMiner to identify the class of the operator) of
	 * the {@link AWOperator}.
	 *
	 * @return class key (the key used in RapidMiner to identify the class of the operator) of the
	 * {@link AWOperator}
	 */
	public String getClassKey() {
		return classKey;
	}
	
	/**
	 * Sets the class key (the key used in RapidMiner to identify the class of the operator) of the
	 * {@link AWOperator} to the provided one. Returns itself, so that set methods can be chained.
	 *
	 * @param classKey
	 * 		new class key (the key used in RapidMiner to identify the class of the operator) of the
	 *        {@link AWOperator}
	 * @return this {@link AWOperator}
	 */
	public AWOperator setClassKey(String classKey) {
		this.classKey = classKey;
		return this;
	}
	
	/**
	 * Returns the {@link Class} of the operator described by this {@link AWOperator}.
	 *
	 * @return the {@link Class} of the operator described by this {@link AWOperator}
	 */
	public Class<? extends Operator> getOperatorClass() {
		return operatorClass;
	}
	
	/**
	 * Sets the {@link Class} of the operator described by this {@link AWOperator} to the provided
	 * one. Returns itself, so that set methods can be chained.
	 *
	 * @param operatorClass
	 * 		new {@link Class} of the operator described by this {@link AWOperator}
	 * @return this {@link AWOperator}
	 */
	public AWOperator setOperatorClass(Class<? extends Operator> operatorClass) {
		this.operatorClass = operatorClass;
		return this;
	}
	
	/**
	 * Returns if the described operator is enabled.
	 *
	 * @return {@code true} if the described operator is enabled, {@code false} otherwise
	 */
	public boolean getIsEnabled() {
		return isEnabled;
	}
	
	/**
	 * Sets if the operator described by this {@link AWOperator} is enabled. Returns itself, so that
	 * set methods can be chained.
	 *
	 * @param isEnabled
	 *        {@code true} if the operator described by this {@link AWOperator} is enabled
	 * @return this {@link AWOperator}
	 */
	public AWOperator setIsEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
		return this;
	}
	
	/**
	 * Returns if the operator described by this {@link AWOperator} has subprocesses.
	 *
	 * @return {@code true} if the operator described by this {@link AWOperator} has subprocesses,
	 * {@code false} otherwise
	 */
	public boolean getHasSubprocesses() {
		return hasSubprocesses;
	}
	
	/**
	 * Sets if the operator described by this {@link AWOperator} has subprocesses. If so, the number
	 * of subprocesses ({@link #setNumberOfSubprocesses(Integer)}) and the list of inner {@link
	 * AgnosticWorkflow}s ({@link #setInnerWorkflows(List)} have to be set. Returns itself, so that
	 * set methods can be chained.
	 *
	 * @param hasSubprocesses
	 *        {@code true} if the operator described by this {@link AWOperator} has subprocess
	 * @return this {@link AWOperator}
	 */
	public AWOperator setHasSubprocesses(boolean hasSubprocesses) {
		this.hasSubprocesses = hasSubprocesses;
		return this;
	}
	
	/**
	 * Returns the number of subprocess of this {@link AWOperator}. Can be {@code null} if the
	 * operator does not have any subprocesses ({@link #getHasSubprocesses()} returns {@code
	 * false}).
	 *
	 * @return number of subprocess of this {@link AWOperator}. Can be {@code null} if the operator
	 * does not have any subprocesses
	 */
	public Integer getNumberOfSubprocesses() {
		return numberOfSubprocesses;
	}
	
	/**
	 * Sets the number of subprocesses of the {@link AWOperator} to the provided one. If the number
	 * of subprocesses to something different than {@code null}, {@link
	 * #setHasSubprocesses(boolean)} has to be called and the list of inner {@link
	 * AgnosticWorkflow}s ({@link #setInnerWorkflows(List)} have to be set. Returns itself, so that
	 * set methods can be chained.
	 *
	 * @param numberOfSubprocesses
	 * 		new name of the {@link AWOperator}
	 * @return this {@link AWOperator}
	 */
	public AWOperator setNumberOfSubprocesses(Integer numberOfSubprocesses) {
		this.numberOfSubprocesses = numberOfSubprocesses;
		return this;
	}
	
	/**
	 * Returns the list of inner workflows of this {@link AWOperator}. Can be {@code null} if the
	 * operator does not have any subprocesses ({@link #getHasSubprocesses()} returns {@code
	 * false}).
	 *
	 * @return list of inner workflows of this {@link AWOperator}. Can be {@code null} if the
	 * operator does not have any subprocesses
	 */
	public List<AgnosticWorkflow> getInnerWorkflows() {
		return innerWorkflows;
	}
	
	/**
	 * Sets the list of inner workflows ({@link AgnosticWorkflow}s) of the {@link AWOperator} to the provided one. If the list
	 * of inner workflows is set to something different than {@code null}, {@link
	 * #setHasSubprocesses(boolean)} has to be called and the number of subprocesses ({@link
	 * #setNumberOfSubprocesses(Integer)} has to be set. Returns itself, so that set methods can be
	 * chained.
	 *
	 * @param innerWorkflows
	 * 		new list of inner workflows of the {@link AWOperator}
	 * @return this {@link AWOperator}
	 */
	public AWOperator setInnerWorkflows(List<AgnosticWorkflow> innerWorkflows) {
		this.innerWorkflows = innerWorkflows;
		return this;
	}
	
	/**
	 * Returns the list of {@link AWParameter}s of this {@link AWOperator}.
	 *
	 * @return list of {@link AWParameter}s of this {@link AWOperator}
	 */
	public List<AWParameter> getParameters() {
		return parameters;
	}
	
	/**
	 * Sets the list of {@link AWParameter}s of the {@link AWOperator} to the provided one. Returns itself, so that
	 * set methods can be chained.
	 *
	 * @param parameters
	 * 		new list of {@link AWParameter}s of the {@link AWOperator}
	 * @return this {@link AWOperator}
	 */
	public AWOperator setParameters(List<AWParameter> parameters) {
		this.parameters = parameters;
		return this;
	}
	
	/**
	 * Returns the list of {@link AWPort}s of the input ports of this {@link AWOperator}.
	 *
	 * @return list of {@link AWPort}s of the input ports of this {@link AWOperator}
	 */
	public List<AWPort> getInputPortsAndSchemas() {
		return inputPortsAndSchemas;
	}
	
	/**
	 * Sets the list of {@link AWPort}s of the input ports of the {@link AWOperator} to the provided one. Returns itself, so that
	 * set methods can be chained.
	 *
	 * @param inputPortsAndSchemas
	 * 		new list of {@link AWPort}s of the input ports of the {@link AWOperator}
	 * @return this {@link AWOperator}
	 */
	public AWOperator setInputPortsAndSchemas(List<AWPort> inputPortsAndSchemas) {
		this.inputPortsAndSchemas = inputPortsAndSchemas;
		return this;
	}
	
	/**
	 * Returns the list of {@link AWPort}s of the output ports of this {@link AWOperator}.
	 *
	 * @return list of {@link AWPort}s of the output ports of this {@link AWOperator}
	 */
	public List<AWPort> getOutputPortsAndSchemas() {
		return outputPortsAndSchemas;
	}
	
	/**
	 * Sets the list of {@link AWPort}s of the output ports of the {@link AWOperator} to the provided one. Returns itself, so that
	 * set methods can be chained.
	 *
	 * @param outputPortsAndSchemas
	 * 		new list of {@link AWPort}s of the output ports of the {@link AWOperator}
	 * @return this {@link AWOperator}
	 */
	public AWOperator setOutputPortsAndSchemas(List<AWPort> outputPortsAndSchemas) {
		this.outputPortsAndSchemas = outputPortsAndSchemas;
		return this;
	}
	
	/**
	 * Returns the name of the platform this {@link AWOperator} is placed in.
	 *
	 * @return name of the platform this {@link AWOperator} is placed in
	 */
	public String getPlatformName() {
		return platformName;
	}
	
	/**
	 * Sets the name of the platform this {@link AWOperator} is placed in to the provided one. Returns itself, so that set
	 * methods can be chained.
	 *
	 * @param platformName
	 * 		new name of the platform this {@link AWOperator} is placed in
	 * @return this {@link AWOperator}
	 */
	public AWOperator setPlatformName(String platformName) {
		this.platformName = platformName;
		return this;
	}
	
}
