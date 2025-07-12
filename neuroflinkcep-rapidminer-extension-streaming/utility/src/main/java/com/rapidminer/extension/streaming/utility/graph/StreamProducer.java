/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.graph;

/**
 * Functionality defined for graph nodes that produce a stream (and marker)
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public interface StreamProducer extends StreamGraphNode {

	/**
	 * Registers child node for this node
	 *
	 * @param child
	 */
	void registerChild(StreamConsumer child);

}