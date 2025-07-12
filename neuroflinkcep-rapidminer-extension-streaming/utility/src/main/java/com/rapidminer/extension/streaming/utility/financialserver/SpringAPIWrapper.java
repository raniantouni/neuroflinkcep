/*
 * Copyright (C) 2001-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.financialserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;


/**
 * Simple wrapper on Spring Financial data server API.
 *
 * @author Edwin Yaqub (reusing parts of Spring code)
 * @since 0.1.0
 */
public class SpringAPIWrapper implements QuoteInterface, java.io.Serializable {

	//private static final Logger LOGGER = LogService.getRoot();
	public static volatile boolean IS_LOGGED_IN = false;
	private final int serverPort;
	public String loginCommandResponse = "";
	public String symbolsListCommandResponse = "";
	public String orderQuoteCommandResponse = "";
	public String cancelQuoteCommandResponse = "";
	public String orderDepthCommandResponse = "";
	public String cancelDepthCommandResponse = "";
	public ArrayList<String> symbolsList = new ArrayList<>();
	public ArrayList<String> quotesList = new ArrayList<>();
	public ArrayList<String> depthsList = new ArrayList<>();
	private DataConnector connection;
	// private Operator operator = null;
	private String serverIP = null;

	/*
	 * public SpringAPIWrapper(Operator operator) { this.operator = operator; }
	 */
	public SpringAPIWrapper(String serverIP, int serverPort) {
		this.serverIP = serverIP;
		this.serverPort = serverPort;
	}

	/**
	 * Action to login user
	 */
	public int loginAction(String username, String password) {
		// Treating -1 as code for already logged in.
		int result = -1;
		if (!isLoggedIn()) {
			// Before login, initialize connection with server and start connector thread
			connection = new DataConnector(this, serverIP, serverPort);
			result = connection.connect();
			if (result == DataConnector.OK) {
				// set true only here.
				IS_LOGGED_IN = true;
				//LOGGER.log(Level.INFO, "SERVER: Connection success");
				try {
					//LOGGER.log(Level.INFO, "SERVER: Invoking Login command");
					connection.login(username, password);
					//LOGGER.log(Level.INFO, "SERVER: Login Invoked");
				} catch (IOException ex) {
					//LOGGER.log(Level.INFO, "SERVER: Login Error : " + ex.getMessage());
					//ex.printStackTrace();
				}
			} else {
				// handle user errors later
				switch (result) {
					case DataConnector.CONNECTION_ERROR:
						//LOGGER.log(Level.INFO, "SERVER: Connection Error");
						break;
					case DataConnector.NO_INTERNET:
						//LOGGER.log(Level.INFO, "SERVER: Connection Error, Check your internet connection");
						break;
				}
			}
		}
		return result;
	}

	public void logoutAction() {
		if (connection != null) {
			connection.stopRunning();
			IS_LOGGED_IN = false;
		}
	}

	/**
	 * Action to get Symbols
	 */
	public void getSymbolsAction() {
		if (isLoggedIn()) {
			try {
				connection.getSymbols();
			} catch (IOException ex) {}
		}
	}

	/**
	 * Action to start quotes when the button pressed. This is the main wrapper method to Order a real time quote of a
	 * symbol.
	 */
	public void startQuoteAction(String symbolString) {
		if (isLoggedIn() && symbolString != null) {
			try {
				connection.sending = true;
				connection.startQuote(symbolString);
			} catch (IOException ex) {}
			connection.sending = false;
		}
	}

	/**
	 * Action to stop quote when the button pressed
	 */
	public void stopQuoteAction(String symbolString) {
		if (isLoggedIn() && symbolString != null) {
			try {
				connection.sending = true;
				connection.stopQuote(symbolString);
			} catch (IOException ex) {}
			connection.sending = false;
		}
	}

	/**
	 * Action to start depth when the button pressed
	 */
	public void startDepthAction(String symbolString) {
		if (isLoggedIn() && symbolString != null) {
			connection.sending = true;
			try {
				connection.startDepth(symbolString);
			} catch (IOException ex) {}
			connection.sending = false;
		}
	}

	/**
	 * Action to stop depth when the button pressed
	 */
	public void stopDepthAction(String symbolString) {
		if (isLoggedIn() && symbolString != null) {
			try {
				connection.sending = true;
				connection.stopDepth(symbolString);
			} catch (IOException ex) {}
			connection.sending = false;
		}
	}

	public DataConnector getConnection() {
		return connection;
	}

	public boolean isLoggedIn() {
		return IS_LOGGED_IN;
	}

	public boolean isConnectorThreadRunning() {
		return connection.isThreadRunning();
	}

	@Override
	public void enableLoginComp(boolean enabled) {
	}

	@Override
	public void log(String message) {
	}

	@Override
	public void setQuotesList(Vector<String> list, boolean clear) {
		if (list != null) {
			Iterator<String> iterator = list.iterator();
			while (iterator.hasNext()) {
				String lineValue = iterator.next();
				this.symbolsList.add(lineValue);
			}
		}
	}

	@Override
	public void addQuote(String quote) {
		if (quote != null && quote.length() > 0) {
			this.quotesList.add(quote);
		}
	}

	@Override
	public void addDepth(String depth) {
		if (depth != null && depth.length() > 0) {
			this.depthsList.add(depth);
		}
	}

	@Override
	public boolean shouldSaveQuote() {
		return false;
	}

	@Override
	public boolean shouldSaveDepth() {
		return false;
	}

	// Non-Quote interface methods

	/*
	@Override
	public String getSymbolsListCommandResponse() {
		return symbolsListCommandResponse;
	}

	@Override
	public void setSymbolsListCommandResponse(String symbolsListCommandResponse) {
		this.symbolsListCommandResponse = symbolsListCommandResponse;
	}
	 */

	/**
	 * @return
	 */
	@Override
	public String getOrderQuoteCommandResponse() {
		return orderQuoteCommandResponse;
	}

	/**
	 * @param orderQuoteCommandResponse
	 */
	@Override
	public void setOrderQuoteCommandResponse(String orderQuoteCommandResponse) {
		this.orderQuoteCommandResponse = orderQuoteCommandResponse;
	}

	/**
	 * @return
	 */
	@Override
	public String getCancelQuoteCommandResponse() {
		return cancelQuoteCommandResponse;
	}

	/**
	 * @param cancelQuoteCommandResponse
	 */
	@Override
	public void setCancelQuoteCommandResponse(String cancelQuoteCommandResponse) {
		this.cancelQuoteCommandResponse = cancelQuoteCommandResponse;
	}

	/**
	 * @return
	 */
	@Override
	public String getOrderDepthCommandResponse() {
		return orderDepthCommandResponse;
	}

	/**
	 * @param orderDepthCommandResponse
	 */
	@Override
	public void setOrderDepthCommandResponse(String orderDepthCommandResponse) {
		this.orderDepthCommandResponse = orderDepthCommandResponse;
	}

	/**
	 * @return
	 */
	@Override
	public String getCancelDepthCommandResponse() {
		return cancelDepthCommandResponse;
	}

	/**
	 * @param cancelDepthCommandResponse
	 */
	@Override
	public void setCancelDepthCommandResponse(String cancelDepthCommandResponse) {
		this.cancelDepthCommandResponse = cancelDepthCommandResponse;
	}

	/**
	 * @return
	 */
	@Override
	public String getLoginCommandResponse() {
		return loginCommandResponse;
	}

	/**
	 * @param loginCommandResponse
	 */
	@Override
	public void setLoginCommandResponse(String loginCommandResponse) {
		this.loginCommandResponse = loginCommandResponse;
	}

	/**
	 * @return
	 */
	public ArrayList<String> getSymbolsList() {
		return symbolsList;
	}

	/**
	 * @param symbolsList
	 */
	public void setSymbolsList(ArrayList<String> symbolsList) {
		this.symbolsList = symbolsList;
	}

	public void clearSymbolsList() {
		this.symbolsList.clear();
	}

	/**
	 * @return
	 */
	public ArrayList<String> getQuotesList() {
		return quotesList;
	}

	/**
	 * @param quotesList
	 */
	public void setQuotesList(ArrayList<String> quotesList) {
		this.quotesList = quotesList;
	}

	public void clearQuotesList() {
		this.quotesList.clear();
	}

	/**
	 * @return
	 */
	public ArrayList<String> getDepthsList() {
		return depthsList;
	}

	/**
	 * @param depthsList
	 */
	public void setDepthsList(ArrayList<String> depthsList) {
		this.depthsList = depthsList;
	}

	public void clearDepthsList() {
		this.depthsList.clear();
	}

}