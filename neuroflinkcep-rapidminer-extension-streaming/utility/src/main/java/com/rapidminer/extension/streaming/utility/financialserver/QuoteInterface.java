/*
 * Copyright (C) 2001-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.financialserver;

import java.util.Vector;


/**
 * This interface used to handle data between GUI main window and TCP/IP DataController class.
 *
 * @author Spring Technologies, and EdwinYaqub (RapidMiner)
 * @since 0.1.0
 */
public interface QuoteInterface {

	void enableLoginComp(boolean enabled);

	void log(String message);

	void setQuotesList(Vector<String> list, boolean clear);

	void addQuote(String quote);

	void addDepth(String depth);

	boolean shouldSaveQuote();

	boolean shouldSaveDepth();

	/* Methods added by RapidMiner for loose-coupled integration */
	//public String getSymbolsListCommandResponse();

	// public void setSymbolsListCommandResponse(String symbolsListCommandResponse);

	String getOrderQuoteCommandResponse();

	void setOrderQuoteCommandResponse(String orderQuoteCommandResponse);

	String getCancelQuoteCommandResponse();

	void setCancelQuoteCommandResponse(String cancelQuoteCommandResponse);

	String getOrderDepthCommandResponse();

	void setOrderDepthCommandResponse(String orderDepthCommandResponse);

	String getCancelDepthCommandResponse();

	void setCancelDepthCommandResponse(String cancelDepthCommandResponse);

	String getLoginCommandResponse();

	void setLoginCommandResponse(String loginCommandResponse);

}