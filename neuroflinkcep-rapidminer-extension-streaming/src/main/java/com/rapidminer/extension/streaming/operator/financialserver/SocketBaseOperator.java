/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator.financialserver;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.connection.util.ConnectionInformationSelector;
import com.rapidminer.connection.util.ConnectionSelectionProvider;
import com.rapidminer.extension.streaming.connection.financialserver.SocketConnectionHandler;
import com.rapidminer.extension.streaming.connection.financialserver.SpringAPIWrapper;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.tools.LogService;


/**
 * Base for sockets
 *
 * @author Edwin Yaqub
 * @since 0.1.0
 */
public class SocketBaseOperator extends Operator implements ConnectionSelectionProvider {

	public static final String SYMBOLS = "symbols";
	public static final String QUOTES = "quotes";
	public static final String DEPTHS = "depths";
	public static final String ORDERQUOTE_KEYWORD = "ORDERQUOTE";
	public static final String ORDERQUOTE_ANNOTATION = "Quote Order";
	// DataConnector.resolveResponse has DEPTH (logically would be ORDERDEPTH)
	public static final String DEPTH_KEYWORD = "DEPTH";
	public static final String ORDERDEPTH_ANNOTATION = "Depth Order";
	public static final int DEFAULT_POLLING_TIME_MILLISECONDS = 1000;

	protected InputPort conInput = getInputPorts().createPort("con", ConnectionInformationContainerIOObject.class);
	protected ConnectionInformationContainerIOObject connectionInformation = null;
	protected ConnectionInformationSelector selector;
	private String serverIP = null;
	private String serverPort = null;
	private String username = null;
	private String password = null;
	// consider to make it singleton
	private SpringAPIWrapper wrapper = null;
	private static final Logger LOGGER = LogService.getRoot();

	public SocketBaseOperator(OperatorDescription description) {
		super(description);
	}

	protected void initialize() {
		try {
			connectionInformation = conInput.getData(ConnectionInformationContainerIOObject.class);
		} catch (UserError e) {
			LOGGER.log(Level.WARNING, "Socket initialization error", e);
		}
		if (connectionInformation != null) {
			// Get properties from connection object
			this.serverIP = connectionInformation.getConnectionInformation().getConfiguration()
				.getParameter(SocketConnectionHandler.GROUP_KEY + "." + SocketConnectionHandler.SERVER_IP)
				.getValue();
			this.serverPort = connectionInformation.getConnectionInformation().getConfiguration()
				.getParameter(SocketConnectionHandler.GROUP_KEY + "." + SocketConnectionHandler.SERVER_PORT)
				.getValue();
			this.username = connectionInformation.getConnectionInformation().getConfiguration()
				.getParameter(SocketConnectionHandler.GROUP_KEY + "." + SocketConnectionHandler.USERNAME)
				.getValue();
			this.password = connectionInformation.getConnectionInformation().getConfiguration()
				.getParameter(SocketConnectionHandler.GROUP_KEY + "." + SocketConnectionHandler.PASSWORD)
				.getValue();

			// Initialize API Wrapper
			this.wrapper = new SpringAPIWrapper(this.serverIP,
				Integer.parseInt(this.serverPort != null ? this.serverPort
					: wrapper.getConnection().getDefaultServerPort() + ""));
		}

	}

	/**
	 * @return
	 */
	public String getServerIP() {
		return serverIP;
	}

	/**
	 * @param serverIP
	 */
	public void setServerIP(String serverIP) {
		this.serverIP = serverIP;
	}

	/**
	 * @return
	 */
	public String getServerPort() {
		return serverPort;
	}

	/**
	 * @param serverPort
	 */
	public void setServerPort(String serverPort) {
		this.serverPort = serverPort;
	}

	/**
	 * @return
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public ConnectionInformationSelector getConnectionSelector() {
		return this.selector;
	}

	@Override
	public void setConnectionSelector(ConnectionInformationSelector selector) {
		this.selector = selector;
	}

	/**
	 * @return
	 */
	public SpringAPIWrapper getWrapper() {
		return wrapper;
	}

	/**
	 * @param wrapper
	 */
	public void setWrapper(SpringAPIWrapper wrapper) {
		this.wrapper = wrapper;
	}

}