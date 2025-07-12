/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.graph;

/**
 * Core functionality for graph nodes
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public interface StreamGraphNode {

	/**
	 * @return unique id for graph node
	 */
	long getId();

	/**
	 * Accepts visitor
	 *
	 * @param visitor
	 */
	void accept(StreamGraphNodeVisitor visitor);

}