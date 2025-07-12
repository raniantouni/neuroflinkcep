/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.connection;

import static com.rapidminer.extension.streaming.deploy.StreamRunnerConstants.*;
import static com.rapidminer.extension.streaming.deploy.spark.SparkConstants.RM_CONF_HDFS_PATH;
import static com.rapidminer.extension.streaming.deploy.spark.SparkConstants.RM_CONF_HDFS_URI;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.rapidminer.extension.streaming.deploy.management.JobSummary;
import com.rapidminer.extension.streaming.deploy.spark.SparkRestClient;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.rapidminer.connection.ConnectionHandler;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationBuilder;
import com.rapidminer.connection.configuration.ConfigurationParameter;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.configuration.ConnectionConfigurationBuilder;
import com.rapidminer.connection.util.ParameterUtility;
import com.rapidminer.connection.util.TestExecutionContext;
import com.rapidminer.connection.util.TestResult;
import com.rapidminer.connection.util.ValidationResult;
import com.rapidminer.extension.streaming.PluginInitStreaming;
import com.rapidminer.extension.streaming.deploy.StreamRunnerType;


/**
 * Spark connection handler
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public final class SparkConnectionHandler implements ConnectionHandler, StreamConnectionHandler {

	public static final String TYPE = PluginInitStreaming.STREAMING_NAMESPACE + ":" + "spark";

	private static final SparkConnectionHandler INSTANCE = new SparkConnectionHandler();

	private static final String GROUP_SPARK_CONNECTION_PROPS = "spark_connection_properties";

	private static final String GROUP_SPARK_CLUSTER_CONF = "spark_cluster_config";

	private static final String PARAMETER_SPARK_HOST = "spark_host";

	private static final String PARAMETER_SPARK_PORT = "spark_port";

	private static final String PARAMETER_REMOTE_DASHBOARD = "remote_dashboard";

	private static final String GROUP_HDFS_CONNECTION_PROPS = "hdfs_connection_properties";

	private static final String GROUP_HDFS_CLUSTER_CONF = "hdfs_cluster_config";

	private static final String PARAMETER_HDFS_URI = "hdfs_uri";

	private static final String PARAMETER_HDFS_PATH = "hdfs_path";

	private static final String PARAMETER_STATE_STORAGE_LOCATION = "state_storage_location";

	private SparkConnectionHandler() {
	}

	public static SparkConnectionHandler getINSTANCE() {
		return INSTANCE;
	}

	@Override
	public ConnectionInformation createNewConnectionInformation(String name) {
		List<ConfigurationParameter> sparkClusterConfig = Lists.newArrayList(
			ParameterUtility.getCPBuilder(PARAMETER_SPARK_HOST).build(),
			ParameterUtility.getCPBuilder(PARAMETER_SPARK_PORT).build(),
			ParameterUtility.getCPBuilder(PARAMETER_REMOTE_DASHBOARD).build(),
			ParameterUtility.getCPBuilder(PARAMETER_STATE_STORAGE_LOCATION).build()
		);
		List<ConfigurationParameter> hdfsClusterConfig = Lists.newArrayList(
			ParameterUtility.getCPBuilder(PARAMETER_HDFS_URI).build(),
			ParameterUtility.getCPBuilder(PARAMETER_HDFS_PATH).build()
		);

		ConnectionConfiguration config = new ConnectionConfigurationBuilder(name, getType())
			.withDescription("This is an Apache Spark connection")
			.withKeys(GROUP_SPARK_CLUSTER_CONF, sparkClusterConfig)
			.withKeys(GROUP_HDFS_CLUSTER_CONF, hdfsClusterConfig)
			.build();

		return new ConnectionInformationBuilder(config).build();
	}

	@Override
	public void initialize() {
	}

	@Override
	public boolean isInitialized() {
		return true;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public StreamRunnerType getRunnerType() {
		return StreamRunnerType.SPARK;
	}

	@Override
	public Properties buildClusterConfiguration(ConnectionConfiguration config) {
		Properties props = new Properties();
		props.setProperty(RM_CONF_CLUSTER_HOST, config.getValue(GROUP_SPARK_CLUSTER_CONF + "." + PARAMETER_SPARK_HOST));
		props.setProperty(RM_CONF_CLUSTER_PORT, config.getValue(GROUP_SPARK_CLUSTER_CONF + "." + PARAMETER_SPARK_PORT));
		props.setProperty(RM_CONF_CLUSTER_REMOTE_DASHBOARD, config.getValue(GROUP_SPARK_CLUSTER_CONF + "." + PARAMETER_REMOTE_DASHBOARD));
		props.setProperty(RM_CONF_CLUSTER_STATE_STORAGE_LOCATION, config.getValue(GROUP_SPARK_CLUSTER_CONF + "." + PARAMETER_STATE_STORAGE_LOCATION));
		props.setProperty(RM_CONF_HDFS_URI, config.getValue(GROUP_HDFS_CLUSTER_CONF + "." + PARAMETER_HDFS_URI));
		props.setProperty(RM_CONF_HDFS_PATH, config.getValue(GROUP_HDFS_CLUSTER_CONF + "." + PARAMETER_HDFS_PATH));

		// Add arbitrary properties for both Spark and HDFS to the result (with a special prefix)
		String[] propPrefixes = new String[]{GROUP_SPARK_CONNECTION_PROPS + ".", GROUP_HDFS_CONNECTION_PROPS + "."};
		config.getKeyMap().entrySet()
			.stream()
			.filter(entry -> StringUtils.startsWithAny(entry.getKey(), propPrefixes))
			.map(Map.Entry::getValue)
			.forEach(configParam -> props.put(configParam.getName(), configParam.getValue()));

		return props;
	}

	@Override
	public ValidationResult validate(ConnectionInformation object) {

		// Spark Cluster Group
		String clusterGroup = SparkConnectionHandler.GROUP_SPARK_CLUSTER_CONF;
		String hostAddress = clusterGroup + "." + SparkConnectionHandler.PARAMETER_SPARK_HOST;
		String port = clusterGroup + "." + SparkConnectionHandler.PARAMETER_SPARK_PORT;
		String remoteDashboard = clusterGroup + "." + SparkConnectionHandler.PARAMETER_REMOTE_DASHBOARD;
		String stateStorage = clusterGroup + "." + SparkConnectionHandler.PARAMETER_STATE_STORAGE_LOCATION;

		//HDFS Group
		String hdfsGroup = SparkConnectionHandler.GROUP_HDFS_CLUSTER_CONF;
		String hdfsPath= hdfsGroup + "." + SparkConnectionHandler.PARAMETER_HDFS_PATH;
		String hdfsPort = hdfsGroup + "." + SparkConnectionHandler.PARAMETER_HDFS_URI;



		ConnectionConfiguration configuration = object.getConfiguration();

		// Check Host
		ConfigurationParameter hostKey = configuration.getParameter(hostAddress);
		boolean isHostSet = ParameterUtility.isValueSet(hostKey);
		if(!isHostSet){
			return ValidationResult.failure(
					ValidationResult.I18N_KEY_FAILED,
					Collections.singletonMap(hostAddress, "Host address cannot be empty"),
					"Host address cannot be empty");
		}

		// Check Host
		ConfigurationParameter portKey = configuration.getParameter(port);
		boolean isPortSet = ParameterUtility.isValueSet(portKey);
		if(!isPortSet){
			return ValidationResult.failure(
					ValidationResult.I18N_KEY_FAILED,
					Collections.singletonMap(hostAddress, "Port number cannot be empty"),
					"Port number cannot be empty");
		}

		// Check Dashboard
		ConfigurationParameter dashboardKey = configuration.getParameter(remoteDashboard);
		boolean isDashboardSet = ParameterUtility.isValueSet(dashboardKey);
		if(!isDashboardSet){
			return ValidationResult.failure(
					ValidationResult.I18N_KEY_FAILED,
					Collections.singletonMap(hostAddress, "Dashboard URL cannot be empty"),
					"Dashboard URL  cannot be empty");
		}

		// Check State Storage

		ConfigurationParameter stateStorageKey = configuration.getParameter(stateStorage);
		boolean isStateStorageSet = ParameterUtility.isValueSet(stateStorageKey);
		if(!isStateStorageSet){
			return ValidationResult.failure(
					ValidationResult.I18N_KEY_FAILED,
					Collections.singletonMap(hostAddress, "Dashboard URL cannot be empty"),
					"Dashboard URL  cannot be empty");
		}

		// Check HDFS Host
		ConfigurationParameter hdfsHostKey = configuration.getParameter(hdfsPath);
		boolean isHDFSPathSet = ParameterUtility.isValueSet(hdfsHostKey);
		if(!isHDFSPathSet){
			return ValidationResult.failure(
					ValidationResult.I18N_KEY_FAILED,
					Collections.singletonMap(hostAddress, "HDFS path cannot be empty"),
					"HDFS path  cannot be empty");
		}

		// Check HDFS port
		ConfigurationParameter hdfsPortKey = configuration.getParameter(hdfsPort);
		boolean isHDFSPort = ParameterUtility.isValueSet(hdfsPortKey);
		if(!isHDFSPort){
			return ValidationResult.failure(
					ValidationResult.I18N_KEY_FAILED,
					Collections.singletonMap(hostAddress, "HDFS Port number cannot be empty"),
					"HDFS Port number cannot be empty");
		}

		return ValidationResult.success(ValidationResult.I18N_KEY_SUCCESS);

	}

	@Override
	public TestResult test(TestExecutionContext<ConnectionInformation> testContext) {

		ConnectionConfiguration testConfig = testContext.getSubject().getConfiguration();

		String host = testConfig.getValue(GROUP_SPARK_CLUSTER_CONF +"." + PARAMETER_SPARK_HOST);
		String port = testConfig.getValue(GROUP_SPARK_CLUSTER_CONF +"." + PARAMETER_SPARK_PORT);

		SparkRestClient client = new SparkRestClient(host,port);

		boolean isReachable = client.isReachable();

		if(isReachable){
			return TestResult.success(TestResult.I18N_KEY_SUCCESS);
		} else {
			return TestResult.failure(TestResult.I18N_KEY_FAILED, "Could not connect to Server");
		}

	}

	/**
	 * This method returns a LinkedHashMap representing the menu-groups for the UI ("ordered" map!).
	 * The key is the i18n key for the menu group name and the value marks if this group will be a container
	 * for an arbitrary number of properties.
	 *
	 * @return see above
	 */
	public LinkedHashMap<String, Boolean> menuGroups() {
		LinkedHashMap<String, Boolean> orderedMenu = Maps.newLinkedHashMap();
		orderedMenu.put(GROUP_SPARK_CLUSTER_CONF, false);
		orderedMenu.put(GROUP_SPARK_CONNECTION_PROPS, true);
		orderedMenu.put(GROUP_HDFS_CLUSTER_CONF, false);
		orderedMenu.put(GROUP_HDFS_CONNECTION_PROPS, true);
		return orderedMenu;
	}

}