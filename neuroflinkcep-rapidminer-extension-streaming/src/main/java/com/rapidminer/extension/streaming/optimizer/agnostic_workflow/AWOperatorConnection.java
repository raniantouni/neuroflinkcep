/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.agnostic_workflow;

import com.rapidminer.extension.streaming.optimizer.agnostic_workflow.AWPort.PortType;


/**
 * This is a container class for the operator connection properties in an agnostic workflow. It
 * contains the information of connections between output ports and input ports of different
 * operators in an {@link AgnosticWorkflow}. It is used to serialize the information to JSON, so
 * that the INFORE optimizer can process it.
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
public class AWOperatorConnection {
	
	private String fromOperator = "";
	private String fromPort = "";
	private PortType fromPortType = null;
	private String toOperator = "";
	private String toPort = "";
	private PortType toPortType = null;
	
	/**
	 * Creates a new {@link AWOperatorConnection} instance with the fields set to default values.
	 */
	public AWOperatorConnection() {}
	
	/**
	 * Returns the name of the source operator of the {@link AWOperatorConnection}.
	 *
	 * @return name of the source operator of the {@link AWOperatorConnection}
	 */
	public String getFromOperator() {
		return fromOperator;
	}
	
	/**
	 * Sets the name of the source operator of the {@link AWOperatorConnection} to the provided one.
	 * Returns itself, so that set methods can be chained.
	 *
	 * @param fromOperator
	 * 		new name of the source operator of the {@link AWOperatorConnection}
	 * @return this {@link AWOperatorConnection}
	 */
	public AWOperatorConnection setFromOperator(String fromOperator) {
		this.fromOperator = fromOperator;
		return this;
	}
	
	/**
	 * Returns the name of the source port of the {@link AWOperatorConnection}.
	 *
	 * @return name of the source port of the {@link AWOperatorConnection}
	 */
	public String getFromPort() {
		return fromPort;
	}
	
	/**
	 * Sets the name of the source port of the {@link AWOperatorConnection} to the provided one.
	 * Returns itself, so that set methods can be chained.
	 *
	 * @param fromPort
	 * 		new name of the source port of the {@link AWOperatorConnection}
	 * @return this {@link AWOperatorConnection}
	 */
	public AWOperatorConnection setFromPort(String fromPort) {
		this.fromPort = fromPort;
		return this;
	}
	
	/**
	 * Returns the name of the sink operator of the {@link AWOperatorConnection}.
	 *
	 * @return name of the sink operator of the {@link AWOperatorConnection}
	 */
	public String getToOperator() {
		return toOperator;
	}
	
	/**
	 * Sets the name of the sink operator of the {@link AWOperatorConnection} to the provided one.
	 * Returns itself, so that set methods can be chained.
	 *
	 * @param toOperator
	 * 		new name of the sink operator of the {@link AWOperatorConnection}
	 * @return this {@link AWOperatorConnection}
	 */
	public AWOperatorConnection setToOperator(String toOperator) {
		this.toOperator = toOperator;
		return this;
	}
	
	/**
	 * Returns the name of the sink port of the {@link AWOperatorConnection}.
	 *
	 * @return name of the sink port of the {@link AWOperatorConnection}
	 */
	public String getToPort() {
		return toPort;
	}
	
	/**
	 * Sets the name of the sink port of the {@link AWOperatorConnection} to the provided one.
	 * Returns itself, so that set methods can be chained.
	 *
	 * @param toPort
	 * 		new name of the sink port of the {@link AWOperatorConnection}
	 * @return this {@link AWOperatorConnection}
	 */
	public AWOperatorConnection setToPort(String toPort) {
		this.toPort = toPort;
		return this;
	}
	
	/**
	 * Returns the {@link PortType} of the source port of the {@link AWOperatorConnection}.
	 *
	 * @return {@link PortType} of the source port of the {@link AWOperatorConnection}
	 */
	public PortType getFromPortType() {
		return fromPortType;
	}
	
	/**
	 * Sets the {@link PortType} of the source port of the {@link AWOperatorConnection} to the
	 * provided one. Returns itself, so that set methods can be chained.
	 *
	 * @param fromPortType
	 * 		new {@link PortType} of the source port of the {@link AWOperatorConnection}
	 * @return this {@link AWOperatorConnection}
	 */
	public AWOperatorConnection setFromPortType(PortType fromPortType) {
		this.fromPortType = fromPortType;
		return this;
	}
	
	/**
	 * Returns the {@link PortType} of the sink port of the {@link AWOperatorConnection}.
	 *
	 * @return {@link PortType} of the sink port of the {@link AWOperatorConnection}
	 */
	public PortType getToPortType() {
		return toPortType;
	}
	
	/**
	 * Sets the {@link PortType} of the sink port of the {@link AWOperatorConnection} to the
	 * provided one. Returns itself, so that set methods can be chained.
	 *
	 * @param toPortType
	 * 		new {@link PortType} of the sink port of the {@link AWOperatorConnection}
	 * @return this {@link AWOperatorConnection}
	 */
	public AWOperatorConnection setToPortType(PortType toPortType) {
		this.toPortType = toPortType;
		return this;
	}
	
}
