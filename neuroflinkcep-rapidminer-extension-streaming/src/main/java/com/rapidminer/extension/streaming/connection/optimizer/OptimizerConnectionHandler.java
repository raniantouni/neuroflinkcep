/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.connection.optimizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.rapidminer.connection.ConnectionHandler;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationBuilder;
import com.rapidminer.connection.configuration.ConfigurationParameter;
import com.rapidminer.connection.configuration.ConnectionConfigurationBuilder;
import com.rapidminer.connection.util.ParameterUtility;
import com.rapidminer.connection.util.TestExecutionContext;
import com.rapidminer.connection.util.TestResult;
import com.rapidminer.connection.util.ValidationResult;
import com.rapidminer.connection.valueprovider.handler.ValueProviderHandlerRegistry;
import com.rapidminer.extension.streaming.PluginInitStreaming;
import com.rapidminer.extension.streaming.optimizer.connection.OptimizerConnection;
import com.rapidminer.operator.Operator;


/**
 * This {@link ConnectionHandler} provides the possibility to create connection objects able to
 * connect to the INFORE Optimizer Service.
 * <p>
 * Currently the necessary parameters are {@value #PARAMETER_HOST}, {@value #PARAMETER_PORT},
 * {@value #PARAMETER_USER}, {@value #PARAMETER_PASSWORD}. An instance of the {@link
 * OptimizerConnectionHandler} can be retrieved by {@link #getINSTANCE()}.
 * <p>
 * The method {@link #createConnection(ConnectionInformation, Operator)} can be used to create a
 * {@link OptimizerConnection} (which actually handles the communication with the INFORE Optimizer
 * Service) from a {@link ConnectionInformation} created by this {@link
 * OptimizerConnectionHandler}.
 *
 * @author Fabian Temme
 * @since 0.2.0
 */
public class OptimizerConnectionHandler implements ConnectionHandler {

	private static final OptimizerConnectionHandler INSTANCE = new OptimizerConnectionHandler();

	public static final String TYPE = PluginInitStreaming.STREAMING_NAMESPACE + ":" + "infore_optimizer";

	public static final String GROUP_CONNECTION_PROPS = "connection_properties";

	public static final String PARAMETER_HOST = "host";
	public static final String PARAMETER_PORT = "port";
	public static final String PARAMETER_FILE_SERVER_HOST = "file_server_host";
	public static final String PARAMETER_FILE_SERVER_PORT = "file_server_port";
	public static final String PARAMETER_USER = "user";
	public static final String PARAMETER_PASSWORD = "password";

	public static OptimizerConnectionHandler getINSTANCE() {
		return INSTANCE;
	}

	public static OptimizerConnection createConnection(ConnectionInformation connectionInformation,
			Operator operator) {
		Map<String,String> values = ValueProviderHandlerRegistry.getInstance()
																.injectValues(connectionInformation,
																			  operator, false);
		return new OptimizerConnection(values.get(fullKey(PARAMETER_HOST)),
									   values.get(fullKey(PARAMETER_PORT)),
                                       values.get(fullKey(PARAMETER_FILE_SERVER_HOST)),
				                       values.get(fullKey(PARAMETER_FILE_SERVER_PORT)),
				                       values.get(fullKey(PARAMETER_USER)),
									   values.get(fullKey(PARAMETER_PASSWORD)),
									   operator.getLogger());
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public void initialize() {
		// noop
	}

	@Override
	public boolean isInitialized() {
		return true;
	}

	@Override
	public ConnectionInformation createNewConnectionInformation(String name) {
		ConnectionConfigurationBuilder builder = new ConnectionConfigurationBuilder(name,
																					getType());
		List<ConfigurationParameter> connectionPropsParams = new ArrayList<>();
		connectionPropsParams.add(ParameterUtility.getCPBuilder(PARAMETER_HOST).build());
		connectionPropsParams.add(ParameterUtility.getCPBuilder(PARAMETER_PORT).build());
		connectionPropsParams.add(ParameterUtility.getCPBuilder(PARAMETER_FILE_SERVER_HOST).build());
		connectionPropsParams.add(ParameterUtility.getCPBuilder(PARAMETER_FILE_SERVER_PORT).build());
		connectionPropsParams.add(ParameterUtility.getCPBuilder(PARAMETER_USER).build());
		connectionPropsParams.add(ParameterUtility.getCPBuilder(PARAMETER_PASSWORD, true).build());

		builder.withDescription(getDescription())
			   .withKeys(GROUP_CONNECTION_PROPS, connectionPropsParams);
		return new ConnectionInformationBuilder(builder.build()).build();
	}

	@Override
	public ValidationResult validate(ConnectionInformation object) {
		Map<String,String> errorMap = new LinkedHashMap<>();
		ParameterUtility.validateParameterValue(GROUP_CONNECTION_PROPS, PARAMETER_HOST,
												object.getConfiguration(), errorMap :: put);
		ParameterUtility.validateParameterValue(GROUP_CONNECTION_PROPS, PARAMETER_PORT,
												object.getConfiguration(), errorMap :: put);
		ParameterUtility.validateParameterValue(GROUP_CONNECTION_PROPS, PARAMETER_FILE_SERVER_HOST,
				object.getConfiguration(), errorMap :: put);
		ParameterUtility.validateParameterValue(GROUP_CONNECTION_PROPS, PARAMETER_FILE_SERVER_PORT,
				object.getConfiguration(), errorMap :: put);
		ParameterUtility.validateParameterValue(GROUP_CONNECTION_PROPS, PARAMETER_USER,
												object.getConfiguration(), errorMap :: put);
		ParameterUtility.validateParameterValue(GROUP_CONNECTION_PROPS, PARAMETER_PASSWORD,
												object.getConfiguration(), errorMap :: put);
		if (errorMap.isEmpty()) {
			return ValidationResult.success(ValidationResult.I18N_KEY_SUCCESS);
		}
		return ValidationResult.failure(ValidationResult.I18N_KEY_FAILURE, errorMap);
	}

	@Override
	public TestResult test(TestExecutionContext<ConnectionInformation> testContext) {
		if (testContext == null || testContext.getSubject() == null) {
			return TestResult.nullable();
		}
		// TODO: Actually test the connection to the INFORE Optimizer Service
		return TestResult.success(TestResult.I18N_KEY_NOT_IMPLEMENTED);
	}

	private String getDescription() {
		return "This is a connection to an INFORE Optimizer Service.";
	}

	private static String fullKey(String parameter) {
		return GROUP_CONNECTION_PROPS + "." + parameter;
	}
}
