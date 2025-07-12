/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.graph;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.Lists;


/**
 * Workflow representation (graph of nodes)
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamGraph {

	private final String name;

	private final AtomicLong idCounter = new AtomicLong(-1);

	private final List<StreamSource> sources = Lists.newArrayList();

	private final List<String> artifacts = Lists.newArrayList();

	public StreamGraph() {
		this("StreamGraph");
	}

	public StreamGraph(String name) {
		this.name = name;
	}

	/**
	 * @return new, unique ID for this workflow
	 */
	public long getNewId() {
		return idCounter.incrementAndGet();
	}

	/**
	 * Registers the source
	 *
	 * @param source
	 */
	public void registerSource(StreamSource source) {
		sources.add(source);
	}

	/**
	 * @return collection of sources
	 */
	public List<StreamSource> getSources() {
		return Collections.unmodifiableList(sources);
	}

	/**
	 * @return name of this workflow
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return modifiable list of artifacts
	 */
	public List<String> getArtifacts() {
		return artifacts;
	}

}