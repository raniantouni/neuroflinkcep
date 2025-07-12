/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.connection.infore;

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
import com.rapidminer.extension.streaming.PluginInitStreaming;


/**
 * Maritime connection handler
 *
 * @author Mate Torok
 * @since 0.6.0
 */
public final class MaritimeConnectionHandler implements ConnectionHandler {

	public static final String TYPE = PluginInitStreaming.STREAMING_NAMESPACE + ":" + "maritime";

	public static final String PARAMETER_HOST = "host";

	public static final String PARAMETER_PORT = "port";

	public static final String PARAMETER_USERNAME = "username";

	public static final String PARAMETER_PASSWORD = "password";

	public static final String PARAMETER_ALLOW_SELF_SIGNED_CERT = "allow_self-signed_certificate";

	private static final String GROUP_CLUSTER_CONF = "cluster_config";

	private static final MaritimeConnectionHandler INSTANCE = new MaritimeConnectionHandler();

	private MaritimeConnectionHandler() {
	}

	public static MaritimeConnectionHandler getINSTANCE() {
		return INSTANCE;
	}

	@Override
	public ConnectionInformation createNewConnectionInformation(String name) {
		List<ConfigurationParameter> clusterConfig = Lists.newArrayList(
			ParameterUtility.getCPBuilder(PARAMETER_HOST).build(),
			ParameterUtility.getCPBuilder(PARAMETER_PORT).build(),
			ParameterUtility.getCPBuilder(PARAMETER_USERNAME).build(),
			ParameterUtility.getCPBuilder(PARAMETER_PASSWORD, true).build(),
			ParameterUtility.getCPBuilder(PARAMETER_ALLOW_SELF_SIGNED_CERT).withValue(String.valueOf(false)).build()
		);

		ConnectionConfiguration config = new ConnectionConfigurationBuilder(name, getType())
			.withDescription("This is an INFORE-Maritime connection")
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

	public Properties buildClusterConfiguration(ConnectionConfiguration config) {
		Properties props = new Properties();
		props.setProperty(PARAMETER_HOST, config.getValue(GROUP_CLUSTER_CONF + "." + PARAMETER_HOST));
		props.setProperty(PARAMETER_PORT, config.getValue(GROUP_CLUSTER_CONF + "." + PARAMETER_PORT));
		props.setProperty(PARAMETER_USERNAME, config.getValue(GROUP_CLUSTER_CONF + "." + PARAMETER_USERNAME));
		props.setProperty(PARAMETER_PASSWORD, config.getValue(GROUP_CLUSTER_CONF + "." + PARAMETER_PASSWORD));
		props.setProperty(PARAMETER_ALLOW_SELF_SIGNED_CERT, config.getValue(GROUP_CLUSTER_CONF+ "." + PARAMETER_ALLOW_SELF_SIGNED_CERT));

		return props;
	}

	@Override
	public ValidationResult validate(ConnectionInformation object) {
		return ValidationResult.success(ValidationResult.I18N_KEY_SUCCESS);
	}

	@Override
	public TestResult test(TestExecutionContext<ConnectionInformation> testContext) {
		return TestResult.success(TestResult.I18N_KEY_SUCCESS);
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
		return orderedMenu;
	}

}