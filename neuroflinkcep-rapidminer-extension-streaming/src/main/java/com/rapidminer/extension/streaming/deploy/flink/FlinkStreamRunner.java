/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.flink;

import static com.rapidminer.extension.streaming.deploy.StreamRunnerConstants.RM_CONF_CLUSTER_HOST;
import static com.rapidminer.extension.streaming.deploy.StreamRunnerConstants.RM_CONF_CLUSTER_PORT;
import static com.rapidminer.extension.streaming.deploy.flink.FlinkConstants.RM_CONF_FLINK_PARALLELISM;
import static java.util.Collections.singletonList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.rapidminer.extension.streaming.PluginInitStreaming;
import com.rapidminer.extension.streaming.deploy.StreamRunner;
import com.rapidminer.extension.streaming.deploy.StreamRunnerException;
import com.rapidminer.extension.streaming.utility.ResourceManager;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.tools.LogService;


/**
 * Flink implementation of the interface that is capable of initiating jobs remotely on a cluster
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class FlinkStreamRunner implements StreamRunner {

	private static final Logger LOGGER = LogService.getRoot();

	private final int parallelism;

	private final Path jarFile;

	private final FlinkRestClient client;

	public FlinkStreamRunner(Properties config) {
		jarFile = PluginInitStreaming.getFlinkJar();
		parallelism = Integer.parseInt(config.getProperty(RM_CONF_FLINK_PARALLELISM));

		String host = config.getProperty(RM_CONF_CLUSTER_HOST);
		String port = config.getProperty(RM_CONF_CLUSTER_PORT);
		client = new FlinkRestClient(host, port);
	}

	@Override
	public String execute(StreamGraph graph) throws StreamRunnerException {
		String graphName = graph.getName();
		LOGGER.fine("Starting execution on Flink: " + graphName);

		try {
			// Kryo serialize graph
			String graphPrefix = "rapidminer_rmx_streaming_flink_graph_";
			LOGGER.fine("Serializing graph: " + graph.getName());
			File graphFile = ResourceManager.serializeGraph(graphPrefix, graph).toFile();

			// Put serialized graph + other artifacts into the new JAR
			String jarPrefix = "rapidminer_rmx_streaming_flink_job_";
			List<File> artifacts = graph.getArtifacts().stream().map(File::new).collect(Collectors.toList());
			List<File> toEmbed = Lists.newArrayList(artifacts);
			toEmbed.add(graphFile);

			LOGGER.fine("Embedding: " + Arrays.toString(toEmbed.stream().map(File::getAbsolutePath).toArray()));
			Path newJarPath = ResourceManager.embedFiles(jarFile.toFile(), jarPrefix, toEmbed);

			// Adjust list of artifacts in the graph (with the file-names)
			graph.getArtifacts().clear();
			graph.getArtifacts().addAll(artifacts.stream().map(File::getName).collect(Collectors.toList()));

			// Start job
			String jobId = client.uploadAndSubmit(
				newJarPath.toString(),
				singletonList(graphFile.getName()),
				parallelism,
				FlinkConstants.FLINK_MAIN_CLASS);

			LOGGER.fine("Started Flink job: " + jobId);
			return jobId;
		} catch (IOException e) {
			LOGGER.severe("Could not initiate Flink job: " + e.getMessage());
			throw new StreamRunnerException(String.format("Could not submit job: '%s'", graphName), e);
		}
	}

	@Override
	public void abort() throws StreamRunnerException {
		client.abort();
	}

}