/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.connection.financialserver;

import static com.rapidminer.connection.util.ParameterUtility.getCPBuilder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.rapidminer.connection.ConnectionHandler;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.ConnectionInformationBuilder;
import com.rapidminer.connection.configuration.ConnectionConfigurationBuilder;
import com.rapidminer.connection.util.TestExecutionContext;
import com.rapidminer.connection.util.TestResult;
import com.rapidminer.connection.util.ValidationResult;
import com.rapidminer.tools.LogService;


/**
 * Socket handler
 *
 * @author Edwin Yaqub
 * @since 0.1.0
 */
public class SocketConnectionHandler implements ConnectionHandler {

	// This is name of connection shown in Studio
	// The syntax is matched with the properperty in GUIStreaming file to pick up label and icon
	public static final String CONNECTION_TYPE = "streaming:spring";
	// This is the Tab in connection dialogue, under which parameters can be grouped
	public static final String GROUP_KEY = "Spring Financial Server";
	public static final String SERVER_IP = "IP Address:";
	public static final String SERVER_PORT = "Port Number:";
	public static final String USERNAME = "Username";
	public static final String PASSWORD = "Password";
	private static final Logger LOGGER = LogService.getRoot();

	@Override
	public void initialize() {
	}

	// @SuppressWarnings({ "resource", "unused" })
	@Override
	public boolean isInitialized() {
		// Just check that server is reachable, no authentication
		return true;
	}

	@Override
	public String getType() {
		return CONNECTION_TYPE;
	}

	@Override
	public ValidationResult validate(ConnectionInformation object) {
		return ValidationResult.success("No validation performed");
	}

	@Override
	public TestResult test(TestExecutionContext<ConnectionInformation> testContext) {
		try {
			String ipAddress = testContext.getSubject().getConfiguration()
				.getParameter(SocketConnectionHandler.GROUP_KEY + "." + SocketConnectionHandler.SERVER_IP)
				.getValue();
			String portNumber = testContext.getSubject().getConfiguration()
				.getParameter(SocketConnectionHandler.GROUP_KEY + "." + SocketConnectionHandler.SERVER_PORT)
				.getValue();
			String username = testContext.getSubject().getConfiguration()
				.getParameter(SocketConnectionHandler.GROUP_KEY + "." + SocketConnectionHandler.USERNAME)
				.getValue();
			String password = testContext.getSubject().getConfiguration()
				.getParameter(SocketConnectionHandler.GROUP_KEY + "." + SocketConnectionHandler.PASSWORD)
				.getValue();

			Socket tcpSocket = new Socket(ipAddress, Integer.parseInt(portNumber));
			BufferedOutputStream outputStream = new BufferedOutputStream(tcpSocket.getOutputStream());
			BufferedInputStream inputStream = new BufferedInputStream(tcpSocket.getInputStream());
			return TestResult.success("Connection Successful !");
		} catch (NoRouteToHostException e) {
			return TestResult.failure("test.connection_failed",
				"Error connecting with Server: +" + e.getLocalizedMessage());
		} catch (IOException e) {
			return TestResult.failure("test.connection_failed",
				"Error connecting with Server: " + e.getLocalizedMessage());
		}
	}

	@Override
	public ConnectionInformation createNewConnectionInformation(String name) {
		ConnectionConfigurationBuilder configurationBuilder = new ConnectionConfigurationBuilder(name, getType());

		configurationBuilder.withKeys(GROUP_KEY, Collections.singletonList(getCPBuilder(SERVER_PORT + "").build()));
		configurationBuilder.withKeys(GROUP_KEY, Collections.singletonList(getCPBuilder(SERVER_IP).build()));
		configurationBuilder.withKeys(GROUP_KEY, Collections.singletonList(getCPBuilder(PASSWORD, true).build()));
		configurationBuilder.withKeys(GROUP_KEY, Collections.singletonList(getCPBuilder(USERNAME).build()));

		configurationBuilder.withDescription("Connect with Financial Server of Spring Technologies");
		ConnectionInformationBuilder informationBuilder = new ConnectionInformationBuilder(
			configurationBuilder.build());
		return informationBuilder.build();
	}

}