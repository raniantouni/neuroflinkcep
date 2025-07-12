/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import com.rapidminer.connection.gui.ConnectionGUIRegistry;
import com.rapidminer.extension.kafka_connector.connections.KafkaConnectionHandler;
import com.rapidminer.extension.streaming.connection.gui.ConfigurableConnectionGUIProvider;

import org.apache.commons.io.FileUtils;

import com.rapidminer.connection.ConnectionHandlerRegistry;
import com.rapidminer.extension.streaming.connection.FlinkConnectionHandler;
import com.rapidminer.extension.streaming.connection.SparkConnectionHandler;
import com.rapidminer.extension.streaming.connection.financialserver.SocketConnectionHandler;
import com.rapidminer.extension.streaming.connection.infore.MaritimeConnectionHandler;
import com.rapidminer.extension.streaming.connection.optimizer.OptimizerConnectionHandler;
import com.rapidminer.extension.streaming.deploy.management.DashboardDockablePanel;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.plugin.Plugin;


/**
 * This class provides hooks for initialization and its methods are called via reflection by RapidMiner Studio. Without
 * this class and its predefined methods, an extension will not be loaded.
 *
 * @author Research Team
 * @since 0.1.0
 */
public final class PluginInitStreaming {

	private static final Logger LOGGER = LogService.getRoot();

	private static final String PLUGIN_ID = "rmx_streaming";

	private static final String FLINK_JAR_NAME = "flink-%s.jar";

	private static final String SPARK_JAR_NAME = "spark-%s.jar";

	private static final String RAAP_JAR_NAME = "rapidminer-as-plugin-%s.jar";

	private static final String RAAP_COMMON_JAR_NAME = "rapidminer-as-plugin-common-%s.jar";

	private static final String SPARK_CONFIG_NAME = "rm-spark-defaults.conf";

	private static Path WORKSPACE = null;

	private static Path SPARK_CONFIG = null;

	private static Path SPARK_JAR = null;

	private static Path FLINK_JAR = null;

	public static final String STREAMING_NAMESPACE = "streaming";

	private PluginInitStreaming() {
		// Utility class constructor
	}

	/**
	 * @return the class loader for the plugin
	 */
	public static ClassLoader getPluginLoader() {
		return PluginInitStreaming.class.getClassLoader();
	}

	/**
	 * This method will be called directly after the extension is initialized. This is the first hook during start up.
	 * No initialization of the operators or renderers has taken place when this is called.
	 */
	public static void initPlugin() {
		// Register connection handlers
		ConnectionHandlerRegistry.getInstance().registerHandler(MaritimeConnectionHandler.getINSTANCE());
		ConnectionHandlerRegistry.getInstance().registerHandler(FlinkConnectionHandler.getINSTANCE());
		ConnectionHandlerRegistry.getInstance().registerHandler(SparkConnectionHandler.getINSTANCE());
		ConnectionHandlerRegistry.getInstance().registerHandler(OptimizerConnectionHandler.getINSTANCE());
		ConnectionHandlerRegistry.getInstance().registerHandler(new SocketConnectionHandler());

		Plugin plugin = Plugin.getPluginByExtensionId(PLUGIN_ID);

		// Initialize workspace
		LOGGER.fine("Initializing workspace");
		WORKSPACE = FileSystemService.getPluginRapidMinerDir(PLUGIN_ID).toPath();

		// Extract files from extension JAR into workspace (for future use)
		LOGGER.fine("Extracting files into workspace");
		extractFiles(plugin);
	}

	/**
	 * This method is called during start up as the second hook. It is called before the gui of the mainframe is
	 * created. The Mainframe is given to adapt the gui. The operators and renderers have been registered in the
	 * meanwhile.
	 *
	 * @param mainframe the RapidMiner Studio {@link MainFrame}.
	 */
	public static void initGui(MainFrame mainframe) {
		// Maritime connection
		ConnectionGUIRegistry.INSTANCE.registerGUIProvider(
			new ConfigurableConnectionGUIProvider(),
			MaritimeConnectionHandler.getINSTANCE().getType());

		// Flink connection
		ConnectionGUIRegistry.INSTANCE.registerGUIProvider(
			new ConfigurableConnectionGUIProvider(),
			FlinkConnectionHandler.getINSTANCE().getType());

		// Spark connection
		ConnectionGUIRegistry.INSTANCE.registerGUIProvider(
			new ConfigurableConnectionGUIProvider(),
			SparkConnectionHandler.getINSTANCE().getType());

		// Kafka connection
		ConnectionGUIRegistry.INSTANCE.registerGUIProvider(
			new ConfigurableConnectionGUIProvider(),
			KafkaConnectionHandler.getINSTANCE().getType());

		// Dashboard
		mainframe.getDockingDesktop().registerDockable(new DashboardDockablePanel());
	}

	/**
	 * The last hook before the splash screen is closed. Third in the row.
	 */
	public static void initFinalChecks() {
	}

	/**
	 * Will be called as fourth method, directly before the UpdateManager is used for checking updates. Location for
	 * exchanging the UpdateManager. The name of this method unfortunately is a result of a historical typo, so it's a
	 * little bit misleading.
	 */
	public static void initPluginManager() {
	}

	/**
	 * @return path to Flink (fat-)jar
	 */
	public static Path getFlinkJar() {
		return FLINK_JAR;
	}

	/**
	 * @return path to Spark (fat-)jar
	 */
	public static Path getSparkJar() {
		return SPARK_JAR;
	}

	/**
	 * @return path to Spark configuration file
	 */
	public static Path getSparkConfig() {
		return SPARK_CONFIG;
	}

	/**
	 * @return workspace dir path for the extension
	 */
	public static Path getWorkspace() {
		return WORKSPACE;
	}

	/**
	 * Goes through a list of files and extracts them from the extension (plugin) JAR
	 *
	 * @param plugin
	 */
	private static void extractFiles(Plugin plugin) {
		String version = plugin.getVersion();

		SPARK_CONFIG = extractFile(plugin, SPARK_CONFIG_NAME, false);
		FLINK_JAR = extractFile(plugin, format(FLINK_JAR_NAME, version), false);
		SPARK_JAR = extractFile(plugin, format(SPARK_JAR_NAME, version), false);
		extractFile(plugin, format(RAAP_JAR_NAME, version), false);
		extractFile(plugin, format(RAAP_COMMON_JAR_NAME, version), false);
	}

	/**
	 * Extracts a file from the plugin JAR
	 *
	 * @param plugin        JAR archive (source)
	 * @param fileName      name of the JAR entry to be extracted
	 * @param checkIfExists flags whether extraction is always necessary
	 * @return to the extracted file
	 */
	private static Path extractFile(Plugin plugin, String fileName, boolean checkIfExists) {
		JarFile jarFile = plugin.getArchive();
		Path filePath = WORKSPACE.resolve(fileName);

		// If flag is set to replace file or file does not exist at all
		if (!checkIfExists || !Files.exists(filePath)) {
			JarEntry entry = jarFile.getJarEntry(fileName);
			if (entry == null) {
				LOGGER.severe("Entry not found: " + fileName);
			}

			try (InputStream inputStream = jarFile.getInputStream(entry)) {
				FileUtils.copyToFile(inputStream, filePath.toFile());
			} catch (IOException e) {
				LOGGER.severe(format("Entry (%s) could not be extracted: %s", fileName, e.getMessage()));
			}
		}

		return filePath.toAbsolutePath();
	}

}