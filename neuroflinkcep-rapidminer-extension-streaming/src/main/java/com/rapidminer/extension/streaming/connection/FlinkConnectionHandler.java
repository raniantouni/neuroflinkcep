/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.connection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

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
import com.rapidminer.extension.kafka_connector.connections.KafkaConnectionHandler;
import com.rapidminer.extension.streaming.PluginInitStreaming;
import com.rapidminer.extension.streaming.deploy.StreamRunnerType;
import com.rapidminer.extension.streaming.deploy.flink.FlinkRestClient;
import com.rapidminer.extension.streaming.deploy.management.JobSummary;

import static com.rapidminer.extension.streaming.deploy.StreamRunnerConstants.RM_CONF_CLUSTER_HOST;
import static com.rapidminer.extension.streaming.deploy.StreamRunnerConstants.RM_CONF_CLUSTER_PORT;
import static com.rapidminer.extension.streaming.deploy.StreamRunnerConstants.RM_CONF_CLUSTER_REMOTE_DASHBOARD;
import static com.rapidminer.extension.streaming.deploy.StreamRunnerConstants.RM_CONF_CLUSTER_STATE_STORAGE_LOCATION;
import static com.rapidminer.extension.streaming.deploy.flink.FlinkConstants.*;


/**
 * Flink connection handler
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public final class FlinkConnectionHandler implements ConnectionHandler, StreamConnectionHandler {

	public static final String TYPE = PluginInitStreaming.STREAMING_NAMESPACE + ":" + "flink";

	private static final FlinkConnectionHandler INSTANCE = new FlinkConnectionHandler();

	private static final String GROUP_CONNECTION_PROPS = "connection_properties";

	private static final String GROUP_CLUSTER_CONF = "cluster_config";

	private static final String PARAMETER_HOST = "host";

	private static final String PARAMETER_PORT = "port";

	private static final String PARAMETER_PARALLELISM = "parallelism";

	private static final String PARAMETER_REMOTE_DASHBOARD = "remote_dashboard";

	private static final String PARAMETER_STATE_STORAGE_LOCATION = "state_storage_location";

	private FlinkConnectionHandler() {
	}

	public static FlinkConnectionHandler getINSTANCE() {
		return INSTANCE;
	}

	@Override
	public ConnectionInformation createNewConnectionInformation(String name) {
		List<ConfigurationParameter> clusterConfig = Lists.newArrayList(
			ParameterUtility.getCPBuilder(PARAMETER_HOST).build(),
			ParameterUtility.getCPBuilder(PARAMETER_PORT).build(),
			ParameterUtility.getCPBuilder(PARAMETER_PARALLELISM).build(),
			ParameterUtility.getCPBuilder(PARAMETER_REMOTE_DASHBOARD).build(),
			ParameterUtility.getCPBuilder(PARAMETER_STATE_STORAGE_LOCATION).build()
		);

		ConnectionConfiguration config = new ConnectionConfigurationBuilder(name, getType())
			.withDescription("This is an Apache Flink connection")
			.withKeys(GROUP_CLUSTER_CONF, clusterConfig)
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
		return StreamRunnerType.FLINK;
	}

	@Override
	public Properties buildClusterConfiguration(ConnectionConfiguration config) {
		Properties props = new Properties();
		props.setProperty(RM_CONF_CLUSTER_REMOTE_DASHBOARD, config.getValue(GROUP_CLUSTER_CONF + "." + PARAMETER_REMOTE_DASHBOARD));
		props.setProperty(RM_CONF_CLUSTER_HOST, config.getValue(GROUP_CLUSTER_CONF + "." + PARAMETER_HOST));
		props.setProperty(RM_CONF_CLUSTER_PORT, config.getValue(GROUP_CLUSTER_CONF + "." + PARAMETER_PORT));
		props.setProperty(RM_CONF_FLINK_PARALLELISM, config.getValue(GROUP_CLUSTER_CONF + "." + PARAMETER_PARALLELISM));
		props.setProperty(RM_CONF_CLUSTER_STATE_STORAGE_LOCATION, config.getValue(GROUP_CLUSTER_CONF + "." + PARAMETER_STATE_STORAGE_LOCATION));

		// TODO: we may need properties later, if not, just remove this section at some point (check menuGroups() as well)
		// Add arbitrary properties to the result
		/*config.getKeyMap().entrySet()
			.stream()
			.filter(entry -> entry.getKey().startsWith(GROUP_CONNECTION_PROPS + "."))
			.map(Map.Entry::getValue)
			.forEach(entry -> props.put(entry.getName(), entry.getValue()));*/

		return props;
	}

	@Override
	public ValidationResult validate(ConnectionInformation object) {


		String clusterGroup = FlinkConnectionHandler.GROUP_CLUSTER_CONF;
		String hostAddress = clusterGroup + "." + FlinkConnectionHandler.PARAMETER_HOST;
		String port = clusterGroup + "." + FlinkConnectionHandler.PARAMETER_PORT;
		String parallelism = clusterGroup + "." + FlinkConnectionHandler.PARAMETER_PARALLELISM;
		String remoteDashboard = clusterGroup + "." + FlinkConnectionHandler.PARAMETER_REMOTE_DASHBOARD;
		String stateStorage = clusterGroup + "." + FlinkConnectionHandler.PARAMETER_STATE_STORAGE_LOCATION;


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

		// Check Port
		ConfigurationParameter portKey = configuration.getParameter(port);
		boolean isPortSet = ParameterUtility.isValueSet(portKey);
		if(!isPortSet){
			return ValidationResult.failure(
					ValidationResult.I18N_KEY_FAILED,
					Collections.singletonMap(hostAddress, "Port number cannot be empty"),
					"Port number cannot be empty");
		}

		// Check Parallelism
		ConfigurationParameter parallelismKey = configuration.getParameter(parallelism);
		boolean isParallelismSet = ParameterUtility.isValueSet(parallelismKey);
		if(!isParallelismSet){
			return ValidationResult.failure(
					ValidationResult.I18N_KEY_FAILED,
					Collections.singletonMap(hostAddress, "Parallelism parameter cannot be empty"),
					"Parallelism parameter cannot be empty");
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


		return ValidationResult.success(ValidationResult.I18N_KEY_SUCCESS);
	}

	@Override
	public TestResult test(TestExecutionContext<ConnectionInformation> testContext) {

		ConnectionConfiguration testConfig = testContext.getSubject().getConfiguration();

		String host = testConfig.getValue(GROUP_CLUSTER_CONF +"." + PARAMETER_HOST);
		String port = testConfig.getValue(GROUP_CLUSTER_CONF +"." + PARAMETER_PORT);



		FlinkRestClient client = new FlinkRestClient(host, port);

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
		orderedMenu.put(GROUP_CLUSTER_CONF, false);
		// TODO: we may need properties later, if not, just remove this section
		//orderedMenu.put(GROUP_CONNECTION_PROPS, true);
		return orderedMenu;
	}

}