/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.connection.financialserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.rapidminer.Process;
import com.rapidminer.extension.streaming.operator.financialserver.GetQuoteSymbols;
import com.rapidminer.extension.streaming.operator.financialserver.SocketBaseOperator;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.LogService;


/**
 * This class is not used and will be removed in future version of streaming extension.
 *
 * A Thread with two main functionalities:
 * <p>
 * 1) Provide a bridge between RapidMiner framework's process execution and an external source that is emitting Events,
 * based on which the reactive operators are to be executed. In this case, the Socket Nest spins up this Thread
 * centrally and upon receiving of event (asynchronously) by this Thread, the Event (string) is passed to all nested
 * operators in the Subprocess. In this manner, the Socket Nest and Connection Handler together extend the state machine
 * that controls the execution life cycle of RapidMiner process. This means that this Thread class is hooked into the
 * Nest Socket operator for the only purpose to pass the received socket data to Subprocess operators. The actual
 * execution of Subprocess takes place in the Nest Socket operator. The back and forth asynchronous bridging is achieved
 * by manually manipulating breakpoints in a ProcessStateListener, which is added to the Socket Nest operator.
 * <p>
 * 2) This Thread class also handles the connection and all API-level calls with the Socket Data Server (in this case,
 * the financial streams data server from Spring)
 *
 * @author Edwin Yaqub
 * @since 0.1.0
 */
public class ConnectionManager1 extends Thread {

	// Socket related properties
	private Socket tcpSocket = null;
	// Spring's Quote Server IP: 5.175.24.176
	public static final String SERVER_IP = "localhost";
	public static final int SERVER_Port = 9003; // Quote Server Port
	private DataOutputStream out = null;
	private DataInputStream in = null;
	public static final int OK = 0;
	public static final int NO_INTERNET = 1;
	public static final int CONNECTION_ERROR = 2;
	private volatile boolean running = false;
	public boolean sending = false;

	private int pollingTime = SocketBaseOperator.DEFAULT_POLLING_TIME_MILLISECONDS;
	private static final Logger LOGGER = LogService.getRoot();
	private com.rapidminer.Process parent = null;

	public ConnectionManager1() {
	}

	public ConnectionManager1(Process parentProcess) {
		parent = parentProcess;
	}

	// Server communication methods come below

	/**
	 * connect to server using socket connection
	 *
	 * @return Returns connection status, one of DataConnector.OK, DataConnector.NO_INTERNET,
	 * DataConnector.CONNECTION_ERROR
	 */
	public int connect() {
		try {
			tcpSocket = new Socket(SERVER_IP, SERVER_Port);
			out = new DataOutputStream(new BufferedOutputStream(tcpSocket.getOutputStream()));
			in = new DataInputStream(new BufferedInputStream(tcpSocket.getInputStream()));
			LOGGER.log(Level.INFO, "++ Connected Successfully!l");
			running = true;
			start();
		} catch (NoRouteToHostException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			return NO_INTERNET;
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			return CONNECTION_ERROR;
		}
		return OK;
	}

	/**
	 * Run method of the extended Thread class, to read server response stream
	 */
	@Override
	public void run() {
		while (running) {
			try {
				LOGGER.log(Level.INFO, "++ Inside Listener Thread run() - polling");
				sleep(pollingTime);
			} catch (InterruptedException e1) {
				LOGGER.log(Level.INFO, "++ Error trying to sleep.", e1);
			}
			if (!sending && tcpSocket != null && in != null) {
				try {
					StringBuilder response = new StringBuilder();
					int c;
					while (in != null && (c = in.read()) != -1) {
						response.append((char) c);
						// All messages are separated by // '!'
						if ((char) c == '!') {
							break;
						}
					}
					resolveResponse(response.toString());
				} catch (Exception e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
		LOGGER.log(Level.INFO, "++ Stopped Listening Thread");
	}

	private void resolveResponse(String response) {
		LOGGER.log(Level.INFO, "++ Resolving this Response: " + response);
		if (parent != null) {
			ExecutionUnit executionUnit = parent.getRootOperator().getSubprocesses().get(0);
			for (Operator operator : executionUnit.getAllInnerOperators()) {
				if (operator instanceof GetQuoteSymbols) {
					LOGGER.log(Level.INFO, "++ Got a Socket Related Nested Operator! Passing it the server response");
					// ((GetQuotes) operator).setEventString(response);
				}
			}
			// parent.pause();
			parent.resume();
		} else {
			LOGGER.log(Level.INFO, "++ Sub Process Execution Unit is null - Cannot Execute");
		}
	}

	protected void setRunning(boolean flag) {
		this.running = flag;
		if (flag) {
			LOGGER.log(Level.INFO, "+ Instructing Thread to Start");
		} else {
			LOGGER.log(Level.INFO, "+ Instructing Thread to Stop");
		}
	}

	/**
	 * Stop the server TCP IP connection
	 */
	public void closeCommunication() {
		setRunning(false);
		try {
			// Thread.currentThread().sleep(100);
			// stop();
			in.close();
			out.close();
			tcpSocket.shutdownInput();
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.log(Level.SEVERE, "+ Error closing I/O Streams or Socket Shutdown");
			if (in != null) {
				in = null;
			}
			if (out != null) {
				out = null;
			}
			if (tcpSocket != null) {
				if (!tcpSocket.isClosed()) {
					try {
						tcpSocket.close();
					} catch (IOException e1) {
						e1.printStackTrace();
						LOGGER.log(Level.SEVERE, "+ Error closing Socket");
						tcpSocket = null;
					}
				}
			}
		}
		LOGGER.log(Level.INFO, "+ Closed ALL connection objects");
	}

	/**
	 * Stop the server TCP IP connection
	 */
	public void stopRunning() {
		running = false;
		try {
			stop();
		} catch (Exception e) {
		}
		try {
			in.close();
			in = null;
		} catch (Exception e) {
		}
		try {
			out.close();
			out = null;
		} catch (Exception e) {
		}
		try {
			tcpSocket.shutdownInput();
		} catch (Exception e) {
		}
		try {
			tcpSocket.shutdownOutput();
		} catch (Exception e) {
		}
		try {
			tcpSocket.close();
		} catch (Exception e) {
		}
	}

	// Getters and Setters

	/**
	 * @return
	 */
	public int getPollingTime() {
		return pollingTime;
	}

	/**
	 * @param pollingTime
	 */
	public void setPollingTime(int pollingTime) {
		this.pollingTime = pollingTime;
	}

	// Server API related methods come below:

	/*
	 * Login to server with username and password
	 */
	public void login(String username, String pw) throws IOException {
		sendData(username + ",login," + pw + "!");
	}

	public void sendData(String command) throws IOException {
		if (out != null) {
			out.writeBytes(command);
			out.flush();
			LOGGER.log(Level.INFO, "Sent Command : " + command);
		}
	}

	public void mockLogin(String username, String pw) throws IOException {
		String loginCommand = username + ",login," + pw + "!" + "\n";
		if (out != null) {
			out.writeBytes(loginCommand);
			out.flush();
			LOGGER.log(Level.INFO, "Send Command : " + loginCommand);
		}
	}

}