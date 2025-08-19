/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.agnostic_workflow;

import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.ResultObjectAdapter;


/**
 * This is a container class for the port properties of an {@link AWOperator} in an agnostic
 * workflow. It contains the information of a single port of an {@link AWOperator} in an {@link
 * AgnosticWorkflow}. It is used to serialize the information to JSON, so that the INFORE optimizer
 * can process it.
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
public class AWPort {



	public enum PortType {
		INPUT_PORT, OUTPUT_PORT, INNER_OUTPUT_PORT, INNER_INPUT_PORT;
		
		@Override
		public String toString() {
			return super.toString().toLowerCase();
		}
		
	}
	
	private String name = "";
	
	private Class<? extends IOObject> objectClass = ResultObjectAdapter.class;
	
	private PortType portType = null;
	
	private boolean isConnected = false;
	
	private AWSchema schema = new AWSchema();
	
	/**
	 * Creates a new {@link AWPort} instance with the fields set to default values.
	 */
	public AWPort() {}

	public AWPort(AWPort aWPort) {
		this.name = aWPort.getName();
		this.objectClass = aWPort.getObjectClass();
		this.portType = aWPort.getPortType();
		this.isConnected = aWPort.getIsConnected();
		this.schema = aWPort.getSchema();
	}
	/**
	 * Returns the name of the {@link AWPort}.
	 *
	 * @return name of the {@link AWPort}
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the name of the {@link AWPort} to the provided one. Returns itself, so that set methods
	 * can be chained.
	 *
	 * @param name
	 * 		new name of the {@link AWPort}
	 * @return this {@link AWPort}
	 */
	public AWPort setName(String name) {
		this.name = name;
		return this;
	}
	
	/**
	 * Returns the {@link IOObject} class of the object of the {@link AWPort}.
	 *
	 * @return the {@link IOObject} class of the object of the {@link AWPort}
	 */
	public Class<? extends IOObject> getObjectClass() {
		return objectClass;
	}
	
	/**
	 * Sets the {@link IOObject} class of the object of the {@link AWPort} to the provided one.
	 * Returns itself, so that set methods can be chained.
	 *
	 * @param objectClass
	 * 		new {@link IOObject} class of the object of the {@link AWPort}
	 * @return this {@link AWPort}
	 */
	public AWPort setObjectClass(Class<? extends IOObject> objectClass) {
		this.objectClass = objectClass;
		return this;
	}
	
	/**
	 * Returns the {@link PortType} of the {@link AWPort}.
	 *
	 * @return {@link PortType} of the {@link AWPort}
	 */
	public PortType getPortType() {
		return portType;
	}
	
	/**
	 * Sets the {@link PortType} of the {@link AWPort} to the provided one. Returns itself, so that
	 * set methods can be chained.
	 *
	 * @param portType
	 * 		new {@link PortType} of the {@link AWPort}
	 * @return this {@link AWPort}
	 */
	public AWPort setPortType(PortType portType) {
		this.portType = portType;
		return this;
	}
	
	/**
	 * Returns {@code true} if there is a connection to another port.
	 *
	 * @return {@code true} if there is a connection to another port, {@code false} otherwise.
	 */
	public boolean getIsConnected() {
		return isConnected;
	}
	
	/**
	 * Sets if the {@link AWPort} is connected to another port. Returns itself, so that set methods
	 * can be chained.
	 *
	 * @param isConnected
	 *        {@code true} if the {@link AWPort} is connected to another port, {@code false} otherwise
	 * @return this {@link AWPort}
	 */
	public AWPort setIsConnected(boolean isConnected) {
		this.isConnected = isConnected;
		return this;
	}
	
	/**
	 * Returns the {@link AWSchema} of the object of the {@link AWPort}.
	 *
	 * @return {@link AWSchema} of the object of the {@link AWPort}
	 */
	public AWSchema getSchema() {
		return schema;
	}
	
	/**
	 * Sets the {@link AWSchema} of the object of the {@link AWPort} to the provided one. Returns
	 * itself, so that set methods can be chained.
	 *
	 * @param schema
	 * 		new {@link AWSchema} of the object of the {@link AWPort}
	 * @return this {@link AWPort}
	 */
	public AWPort setSchema(AWSchema schema) {
		this.schema = schema;
		return this;
	}
	
}
