/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.ioobject;

import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;
import com.rapidminer.operator.ResultObjectAdapter;


/**
 * IO object/container for data that needs to traverse through the operator graph when building RapidMiner processes
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamDataContainer extends ResultObjectAdapter {

	private static final long serialVersionUID = 1L;

	private final StreamGraph streamGraph;

	private final StreamProducer lastNode;

	public StreamDataContainer(StreamGraph streamGraph, StreamProducer lastNode) {
		this.streamGraph = streamGraph;
		this.lastNode = lastNode;
	}

	/**
	 * @return platform independent stream graph
	 */
	public StreamGraph getStreamGraph() {
		return streamGraph;
	}

	/**
	 * @return last node of the stream graph
	 */
	public StreamProducer getLastNode() {
		return lastNode;
	}

	@Override
	public String toString() {
		return "StreamDataContainer{" +
			"streamGraph=" + streamGraph +
			", lastNode=" + lastNode +
			'}';
	}

}