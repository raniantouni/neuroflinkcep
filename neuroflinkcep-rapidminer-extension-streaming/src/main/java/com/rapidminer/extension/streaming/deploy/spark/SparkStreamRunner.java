/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.spark;

import static com.rapidminer.extension.streaming.deploy.StreamRunnerConstants.RM_CONF_CLUSTER_HOST;
import static com.rapidminer.extension.streaming.deploy.StreamRunnerConstants.RM_CONF_CLUSTER_PORT;
import static com.rapidminer.extension.streaming.deploy.spark.SparkConstants.HDFS_FOLDER_JARS;
import static com.rapidminer.extension.streaming.deploy.spark.SparkConstants.RM_CONF_HDFS_PATH;
import static com.rapidminer.extension.streaming.deploy.spark.SparkConstants.RM_CONF_HDFS_URI;
import static com.rapidminer.extension.streaming.utility.HdfsHandler.HDFS_PATH_SEPARATOR;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.joinWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.hadoop.conf.Configuration;

import com.google.common.collect.Lists;
import com.rapidminer.extension.streaming.PluginInitStreaming;
import com.rapidminer.extension.streaming.deploy.StreamRunner;
import com.rapidminer.extension.streaming.deploy.StreamRunnerException;
import com.rapidminer.extension.streaming.utility.HdfsHandler;
import com.rapidminer.extension.streaming.utility.ResourceManager;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.tools.LogService;


/**
 * Spark implementation of the interface that is capable of initiating jobs remotely on a cluster
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class SparkStreamRunner implements StreamRunner {

	private static final Logger LOGGER = LogService.getRoot();

	private final Properties config;

	private final Path jarFile;

	private final SparkRestClient restClient;

	private final HdfsHandler hdfsHandler;

	public SparkStreamRunner(Properties config) throws URISyntaxException {
		this.config = buildMergedConfig(config);
		this.jarFile = PluginInitStreaming.getSparkJar();

		String host = config.getProperty(RM_CONF_CLUSTER_HOST);
		String port = config.getProperty(RM_CONF_CLUSTER_PORT);
		this.restClient = new SparkRestClient(host, port);

		this.hdfsHandler = buildHdfsHandler();
	}

	@Override
	public String execute(StreamGraph graph) throws StreamRunnerException {
		LOGGER.fine("Starting execution procedure for: " + graph.getName());

		try {
			// Kryo serialises graph into a temporary file
			String graphPrefix = "rapidminer_rmx_streaming_spark_graph_";
			LOGGER.fine("Serializing graph: " + graph.getName());
			File graphFile = ResourceManager.serializeGraph(graphPrefix, graph).toFile();
			File sparkConfig = saveConfig(config);

			// Put serialized graph + configuration + other artifacts into the new JAR
			String jarPrefix = "rapidminer_rmx_streaming_spark_job_";
			List<File> artifacts = graph.getArtifacts().stream().map(File::new).collect(Collectors.toList());
			List<File> toEmbed = Lists.newArrayList(artifacts);
			toEmbed.add(graphFile);
			toEmbed.add(sparkConfig);

			LOGGER.fine("Embedding: " + Arrays.toString(toEmbed.stream().map(File::getAbsolutePath).toArray()));
			Path newJarPath = ResourceManager.embedFiles(jarFile.toFile(), jarPrefix, toEmbed);

			// Adjust list of artifacts in the graph (with the file-names)
			graph.getArtifacts().clear();
			graph.getArtifacts().addAll(artifacts.stream().map(File::getName).collect(Collectors.toList()));

			// Copy fat-JAR to HDFS destination
			String hdfsRootPath = config.getProperty(RM_CONF_HDFS_PATH, HDFS_PATH_SEPARATOR);
			String dstPath = joinWith(HDFS_PATH_SEPARATOR, hdfsRootPath, HDFS_FOLDER_JARS);
			String hdfsJarDst = hdfsHandler.copyLocalFile(newJarPath.toFile(), dstPath).get();

			// Submit via REST (Apache-Livy on cluster side)
			List<String> args = Lists.newArrayList(graphFile.getName(), sparkConfig.getName());
			String name = graph.getName() + "-" + RandomStringUtils.randomAlphanumeric(10);
			return restClient.submit(hdfsJarDst, name, config, args);
		} catch (IOException | ExecutionException | InterruptedException e) {
			LOGGER.severe("Unsuccessful file operation: " + e.getMessage());
			throw new StreamRunnerException("File operation could not be executed -> unsuccessful submission", e);
		}
	}

	@Override
	public void abort() throws StreamRunnerException {
		try {
			hdfsHandler.abort();
		} catch (IOException e) {
			throw new StreamRunnerException("Error occurred while aborting execution/initiation", e);
		}
	}

	/**
	 * This method merges the configuration coming from the UI (~User) and the default values stored in the resources.
	 *
	 * @return merged configuration for HDFS + Spark
	 */
	private Properties buildMergedConfig(Properties config) {
		// Load default config first
		File sparkConfig = PluginInitStreaming.getSparkConfig().toFile();
		Properties merged = new Properties();
		try {
			merged.load(new FileInputStream(sparkConfig));
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Could not load default configuration for Spark", e);
		}

		// Merge UI config into it
		config.forEach((key, value) -> merged.setProperty((String) key, (String) value));

		return merged;
	}

	/**
	 * Serializes the configuration into a temporary file (by simply storing them in a single key-value file)
	 *
	 * @return file object to this new, temporary file
	 */
	private File saveConfig(Properties config) throws IOException {
		// Save configuration into a temporary file (format: "key<TAB>value<NEWLINE>")
		File file = ResourceManager.createTempFile("rm-spark_", ".conf").toFile();
		try (FileWriter writer = new FileWriter(file)) {
			for (Map.Entry<Object, Object> entry : config.entrySet()) {
				String key = (String) entry.getKey();
				String value = (String) entry.getValue();
				writer.write(format(SparkConstants.SPARK_CONFIG_LINE_PATTERN, key, value));
			}
		}

		return file;
	}

	/**
	 * @return newly build instance, properly configured
	 * @throws URISyntaxException
	 */
	private HdfsHandler buildHdfsHandler() throws URISyntaxException {
		LOGGER.fine("Building HdfsHandler");
		Configuration hdfsConfig = new Configuration();

		// Add arbitrary HDFS properties
		this.config.entrySet()
			.forEach(entry -> hdfsConfig.set((String) entry.getKey(), (String) entry.getValue()));

		return new HdfsHandler(this.config.getProperty(RM_CONF_HDFS_URI), hdfsConfig);
	}

}