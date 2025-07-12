/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.spark;

import java.io.IOException;
import java.util.Properties;

import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rapidminer.extension.streaming.utility.ResourceManager;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;


/**
 * Main entry point for the Spark job
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamGraphProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(StreamGraphProcessor.class);

	/**
	 * Driver logic:
	 * <ol>
	 *     <li>Fetches serialized graph from HDFS</li>
	 *     <li>Translates graph into Spark specific DSL and starts its execution</li>
	 *     <li>Waits for completion</li>
	 * </ol>
	 *
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		int argc = args.length;
		if (argc < 2) {
			throw new IllegalArgumentException("Job cannot be started without StreamGraph and configuration");
		}

		// Fetch arguments
		String graphFileName = args[0];
		String configFileName = args[1];

		// Fetch resources from fat-JAR
		Properties configuration = getConfiguration(configFileName);
		StreamGraph graph = getGraph(graphFileName);
		String graphName = graph.getName();

		// Build Spark session and traverse graph (translation, sinks start the streams)
		SparkSession sparkSession = buildSession(configuration, graphName);
		SparkTranslationManager translationManager = new SparkTranslationManager(sparkSession);

		LOGGER.info("Initiating graph translation");
		graph.getSources().forEach(source -> source.accept(translationManager));

		// Await stream termination(s)
		translationManager
			.getStreams()
			.forEach(streamingQuery -> {
				try {
					streamingQuery.awaitTermination();
				} catch (StreamingQueryException e) {
					LOGGER.error("Error while awaiting stream termination", e);
				}
			});
	}

	/**
	 * Builds the session for job (this is the driver program in Spark's terms)
	 *
	 * @param configuration
	 * @param graphName
	 * @return newly built Spark session object
	 */
	private static SparkSession buildSession(Properties configuration, String graphName) {
		LOGGER.info("Building SparkSession for: {} with {}", graphName, configuration);
		SparkSession.Builder builder = SparkSession.builder();

		// Setting every property in the configuration for the session
		configuration.entrySet()
			.stream()
			.forEach(entry -> builder.config((String) entry.getKey(), (String) entry.getValue()));

		return builder
			.appName(graphName)
			.getOrCreate();
	}

	/**
	 * Retrieves the configuration from this JAR
	 *
	 * @param fileName
	 * @return configuration
	 * @throws IOException
	 */
	private static Properties getConfiguration(String fileName) throws IOException {
		LOGGER.info("Fetching configuration: {}", fileName);
		Properties configuration = new Properties();
		configuration.load(ResourceManager.retrieve(fileName));
		return configuration;
	}

	/**
	 * Retrieves the serialized workflow from this JAR and de-serializes it
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