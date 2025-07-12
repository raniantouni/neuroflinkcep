/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.agnostic_workflow;

import com.rapidminer.extension.streaming.operator.StreamingNest;


/**
 * This is a container class holding the information of a connection between operators, which is
 * splitted after the optimization of the INFORE optimizer (e.g. the optimizer places the operators
 * in different {@link StreamingNest} operators).
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
public class SplittedConnection {
	
	private AWOperatorConnection originalConnection;
	
	private String fromStreamingNestName;
	
	private String toStreamingNestName;
	
	private boolean isStreamingConnection;
	
	/**
	 * Creates a new {@link SplittedConnection} instance with the fields set to default values.
	 */
	public SplittedConnection() {
	}
	
	/**
	 * Returns {@link AWOperatorConnection} which describes the original (non-splitted) connection.
	 *
	 * @return {@link AWOperatorConnection} which describes the original (non-splitted) connection
	 */
	public AWOperatorConnection getOriginalConnection() {
		return originalConnection;
	}
	
	/**
	 * Sets the {@link AWOperatorConnection} which describes the original (non-splitted) connection
	 * to the provided one. Returns itself, so that set methods can be chained.
	 *
	 * @param originalConnection
	 * 		new {@link AWOperatorConnection} which describes the original (non-splitted) connection of
	 * 		the data set
	 * @return this {@link SplittedConnection}
	 */
	public SplittedConnection setOriginalConnection(AWOperatorConnection originalConnection) {
		this.originalConnection = originalConnection;
		return this;
	}
	
	/**
	 * Returns the name of the {@link StreamingNest} operator in which the source operator of the
	 * connection is placed in.
	 *
	 * @return the name of the {@link StreamingNest} operator in which the source operator of the
	 * connection is placed in
	 */
	public String getFromStreamingNestName() {
		return fromStreamingNestName;
	}
	
	/**
	 * Sets the name of the {@link StreamingNest} operator in which the source operator of the
	 * connection is placed in to the provided one. Returns itself, so that set methods can be
	 * chained.
	 *
	 * @param fromStreamingNestName
	 * 		new name of the {@link StreamingNest} operator in which the source operator of the
	 * 		connection is placed in
	 * @return this {@link SplittedConnection}
	 */
	public SplittedConnection setFromStreamingNestName(String fromStreamingNestName) {
		this.fromStreamingNestName = fromStreamingNestName;
		return this;
	}
	
	/**
	 * Returns the name of the {@link StreamingNest} operator in which the sink operator of the
	 * connection is placed in.
	 *
	 * @return the name of the {@link StreamingNest} operator in which the sink operator of the
	 * connection is placed in
	 */
	public String getToStreamingNestName() {
		return toStreamingNestName;
	}
	
	/**
	 * Sets the name of the {@link StreamingNest} operator in which the sink operator of the
	 * connection is placed in to the provided one. Returns itself, so that set methods can be
	 * chained.
	 *
	 * @param toStreamingNestName
	 * 		new name of the {@link StreamingNest} operator in which the sink operator of the connection
	 * 		is placed in
	 * @return this {@link SplittedConnection}
	 */
	public SplittedConnection setToStreamingNestName(String toStreamingNestName) {
		this.toStreamingNestName = toStreamingNestName;
		return this;
	}
	
	/**
	 * Returns {@code true} if the original connection is between two streaming operators, {@code
	 * false} if it is a process control connection.
	 *
	 * @return {@code true} if the original connection is between two streaming operators, {@code *
	 * false} if it is a process control connection
	 */
	public boolean isStreamingConnection() {
		return isStreamingConnection;
	}
	
	/**
	 * Sets if the original connection is between two streaming operators, or if it is a process
	 * control connection. Returns itself, so that set methods can be chained.
	 *
	 * @param isStreamingConnection
	 *        {@code true} if the original connection is between two streaming operators, {@code false}
	 * 		if it is a process control connection
	 * @return this {@link SplittedConnection}
	 */
	public SplittedConnection setIsStreamingConnection(boolean isStreamingConnection) {
		this.isStreamingConnection = isStreamingConnection;
		return this;
	}
	
	@Override
	public String toString() {
		
		StringBuilder builder = new StringBuilder("Splitted");
		if (isStreamingConnection) {
			builder.append(" (Streaming connection)");
		}
		builder.append(" connections: from ")
			   .append(originalConnection.getFromOperator())
			   .append(".")
			   .append(originalConnection.getFromPort())
			   .append(" (placed in ")
			   .append(fromStreamingNestName)
			   .append(") to ")
			   .append(originalConnection.getToOperator())
			   .append(".")
			   .append(originalConnection.getToPort())
			   .append(" (placed in ")
			   .append(toStreamingNestName)
			   .append(")");
		return builder.toString();
	}
	
}
