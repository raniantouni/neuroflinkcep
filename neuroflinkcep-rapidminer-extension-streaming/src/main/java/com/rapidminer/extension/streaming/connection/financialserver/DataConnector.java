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
import java.util.Vector;


/**
 * Class template to handle TCP/IP data connection
 * <p>
 * Note: need to adjust access modifiers.
 *
 * @author Stefan Burkard, Holger Arndt, Edwin Yaqub (made some modifications)
 * @since 0.1.0
 */
public class DataConnector extends Thread {

	private String serverIP = null;
	private int serverPort = 9003;// hard coded for now to this default

	private volatile boolean running = false;
	private Socket tcpSocket = null;
	private DataOutputStream out = null;
	private DataInputStream in = null;
	public static final int OK = 0;
	public static final int NO_INTERNET = 1;
	public static final int CONNECTION_ERROR = 2;
	private String userName = "";
	private final QuoteInterface quote;
	public boolean sending = false;

	public DataConnector(QuoteInterface quote, String serverIP, int serverPort) {
		this.quote = quote;
		this.serverIP = serverIP;
		this.serverPort = serverPort;
	}

	/**
	 * connect to server using socket connection
	 *
	 * @return Returns connection status, one of DataConnector.OK, DataConnector.NO_INTERNET,
	 * DataConnector.CONNECTION_ERROR
	 */
	public int connect() {
		try {
			tcpSocket = new Socket(serverIP, serverPort);
			out = new DataOutputStream(new BufferedOutputStream(
				tcpSocket.getOutputStream()));
			in = new DataInputStream(new BufferedInputStream(
				tcpSocket.getInputStream()));

			running = true;
			start();
			return OK;
		} catch (NoRouteToHostException e) {
			return NO_INTERNET;
		} catch (IOException e) {
			return CONNECTION_ERROR;
		}
	}

	/**
	 * Run method of the extended Thread class, to read server response stream
	 */
	@Override
	public void run() {
		while (running) {
			if (!sending && tcpSocket != null && in != null) {
				try {
					StringBuilder response = new StringBuilder();
					int c;
					while (in != null && (c = in.read()) != -1) {
						response.append((char) c);
						if ((char) c == '!') {// All messages are separated by
							// '!'
							break;
						}
					}
					resolveResponse(response.toString());
				} catch (Exception ex) {}
			}
		}
	}

	/*
	 * Login to server with username and password
	 */
	public void login(String username, String pw) throws IOException {
		sendData(username + ",login," + pw + "!");
		this.userName = username;
		// DataOutputController.init();
	}

	/**
	 * Request server to send symbols list
	 *
	 * @throws IOException
	 */
	public void getSymbols() throws IOException {
		sendData(userName + ",quotelist!");
	}

	/**
	 * Request server to start quotes for symbol
	 *
	 * @param symbol The Object symbol to send to server
	 * @throws IOException
	 */
	public void startQuote(Object symbol) throws IOException {
		String request = userName + ",orderquote,";
		sendData(request, false);
		sendSymbol(symbol + "");
	}

	/**
	 * Request server to to stop quotes for symbol
	 *
	 * @param symbol The Object symbol to send to server
	 * @throws IOException
	 */
	public void stopQuote(Object symbol) throws IOException {
		String request = userName + ",cancelquote,";
		sendData(request, false);
		sendSymbol(symbol + "");
	}

	/**
	 * Request server to to start depth for symbol
	 *
	 * @param symbol The Object symbol to send to server
	 * @throws IOException
	 */
	public void startDepth(Object symbol) throws IOException {
		String request = userName + ",orderdepth,";
		sendData(request, false);
		sendSymbol(symbol + "");
	}

	/**
	 * Request server to to stop depth for symbol
	 *
	 * @param symbol The Object symbol to send to server
	 * @throws IOException
	 */
	public void stopDepth(Object symbol) throws IOException {
		String request = userName + ",canceldepth,";
		sendData(request, false);
		sendSymbol(symbol + "");
	}

	/**
	 * private method to only send symbol data to server (this method invoke from an other method inside this class)
	 *
	 * @param symbol The String symbol to send to server
	 * @throws IOException
	 */
	private void sendSymbol(String symbol) throws IOException {
		if (out != null && symbol != null && !symbol.trim().isEmpty()) {
			symbol = symbol.trim();
			String[] ar = symbol.split("\\|");
			out.writeBytes(ar[0]);
			out.write(183);
			out.writeBytes(ar[1]);
			out.write(183);
			out.writeBytes(ar[2]);
			out.writeBytes("!");
			out.flush();
		}
	}

	/**
	 * Send data to server
	 *
	 * @param line The String line to send to server
	 * @throws IOException
	 */
	public void sendData(String line) throws IOException {
		sendData(line, true);
	}

	/**
	 * Send data to server and prints sent data in the standard output (command line)
	 *
	 * @param line  The String line to send to server
	 * @param print Whether it should print in the command line, or not
	 * @throws IOException
	 */
	public void sendData(String line, boolean print) throws IOException {
		// All messages are separated by '!'
		if (out != null) {
			out.writeBytes(line);
			out.flush();
		}
	}

	/**
	 * Decide the actions for the received response from the server
	 *
	 * @param response- The response line received from server
	 */
	private void resolveResponse(String response) {
		if (response == null) {
			return;
		}

		if (response.replace("!", "").trim().isEmpty()) {
			return;
		}
		response = response.trim();
		if (response.toUpperCase().startsWith("LOGIN,")) {
			// Pass on the response to APIWrapper
			String responseSoFar = this.quote.getLoginCommandResponse();
			this.quote.setLoginCommandResponse(responseSoFar + "\n" + response);
		} else if (response.toUpperCase().startsWith("ORDERQUOTE,")) {
			this.quote.addQuote(response);
			// counter++;
		} else if (response.toUpperCase().startsWith("QUOTELIST,")) {
			response = response.substring(10);
			String[] lines = response.split("\n");
			Vector list = new Vector();
			for (int i = 0; i < lines.length; i++) {
				if (lines[i] != null) {
					String line = lines[i];
					String[] data = line.split("\\|");
					if (data.length > 3) {
						//list.add(data[0] + "[^\\u{183}" + data[1] + "[^\\u{183}" + data[3]);
						list.add(data[0] + "|" + data[1] + "|" + data[3]);
					}
				}
			}
			// implemented this in APIWrapper:
			this.quote.setQuotesList(list, false);

			// Set response in Operator
			// String responseSoFar = ((InvokeCommand) this.operator).getSymbolsListCommandResponse();
			// ((InvokeCommand) this.operator).setSymbolsListCommandResponse(responseSoFar + "\n" + list.toString());

		} else if (response.toUpperCase().startsWith("HEARTBEAT,")) {
			try {
				sendData(userName + ",heartbeatanswer,!");
			} catch (IOException ex) {}
		} else if (response.toUpperCase().startsWith("QUOTE,")) {
			String[] ar = response.split(",");
			if (ar.length > 7) {
				String line = ar[6] + "," + ar[7] + "," + ar[4] + "," + ar[5];
				quote.addQuote(ar[1] + " " + line);
				/*
				if (quote.shouldSaveQuote()) {// Save quote data to file
					String fileName = ar[1];
					try {
						DataOutputController.saveQuoteData(fileName, line);
					} catch (Exception ex) {
						quote.log("Error Writing to File, " + fileName + " : "
								+ ex.getMessage());
					}
				}
				 */
			}
		} else if (response.toUpperCase().startsWith("DEPTH,")) {
			String line = response.endsWith("!") ? response.substring(0,
				response.length() - 1) : response;
			quote.addDepth(line);
		} else if (response.toUpperCase().startsWith("ERROR,")) {
			quote.log(response);
		} else {
			quote.log("SERVER:" + response);
		}
	}

	/**
	 * Stop the server TCP IP connection
	 */
	public void stopRunning() {
		running = false;
		try {
			stop();
		} catch (Exception e) {}
		try {
			in.close();
			in = null;
		} catch (Exception e) {}
		try {
			out.close();
			out = null;
		} catch (Exception e) {}
		try {
			tcpSocket.shutdownInput();
		} catch (Exception e) {}
		try {
			tcpSocket.shutdownOutput();
		} catch (Exception e) {}
		try {
			tcpSocket.close();
		} catch (Exception e) {}
	}

	public boolean isThreadRunning() {
		return running;
	}

	/**
	 * @return
	 */
	public int getDefaultServerPort() {
		return serverPort;
	}

}