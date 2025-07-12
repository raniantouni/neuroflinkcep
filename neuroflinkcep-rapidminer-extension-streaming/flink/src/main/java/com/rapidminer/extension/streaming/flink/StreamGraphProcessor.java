/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.flink;

import java.io.IOException;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rapidminer.extension.streaming.utility.ResourceManager;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;


/**
 * Main entry point for Flink jobs
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamGraphProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(StreamGraphProcessor.class);

	/**
	 * Driver logic:
	 * <ol>
	 *     <li>Fetches serialized graph from JAR</li>
	 *     <li>Translates graph into Flink specific DSL and starts its execution</li>
	 *     <li>Start execution</li>
	 * </ol>
	 *
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		int argc = args.length;
		if (argc < 1) {
			throw new IllegalArgumentException("Job cannot be started without StreamGraph URI");
		}
		String graphFileName = args[0];

		// Read graph from JAR
		StreamGraph graph = getGraph(graphFileName);
		String graphName = graph.getName();

		// Create environment + translation manager
		StreamExecutionEnvironment executionEnvironment = StreamExecutionEnvironment.getExecutionEnvironment();
		FlinkTranslationManager translationManager = new FlinkTranslationManager(executionEnvironment);

		// Visit sources
		LOGGER.info("Starting translation");
		graph.getSources().forEach(source -> source.accept(translationManager));

		// Initiate execution
		LOGGER.info("Starting workflow execution: {}", graphName);
		executionEnvironment.execute(graphName);
	}

	/**
	 * Retrieves the serialized workflow from this JAR
	 *
	 * @param fileName
	 * @return de-serialized workflow (StreamGraph)
	 * @throws IOException
	 */
	private static StreamGraph getGraph(String fileName) throws IOException {
		LOGGER.info("Fetching graph: {}", fileName);
		return ResourceManager.retrieveGraph(fileName);
	}

}