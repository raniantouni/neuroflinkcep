package com.rapidminer.extension.streaming.flink.translate;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.json.JSONObject;

import com.rapidminer.extension.streaming.utility.financialserver.SpringAPIWrapper;
import com.rapidminer.extension.streaming.utility.graph.source.SpringFinancialSource;


/**
 * Flink specific connector for Spring Techno's Financial Server. This source function reads bytes from the socket into
 * a buffer and convert them to strings, which are collected and returned as per user-defined time parameters in a
 * continuous stream.
 *
 * @author Edwin Yaqub
 * @since 0.1.0
 */
public class SpringFinancialSourceSocketTextStreamFunction implements SourceFunction<String> {

	private static final long serialVersionUID = 1L;

	//Default connection timeout when connecting to the server socket (infinite).
	private static final int CONNECTION_TIMEOUT_TIME = 0;
	private SpringAPIWrapper springAPIWrapper;
	private String hostname;
	private int port;
	private String username;
	private String password;
	// Optional delay to batch records of quotes or depths (expected due to 3rd party server delays)
	private int streamingWindowTimeMilliseconds;
	private int disconnectTimeMilliseconds;
	private String symbol;
	private boolean retrieveQuotes = true;
	private int resultOfLoginCommand = -1;
	//variable to control streaming functionality for Flink-based run() method in this class.
	private volatile boolean isRunning = true;

	public SpringFinancialSourceSocketTextStreamFunction(String ipAddress, String port, String username, String password) {
		this.hostname = ipAddress;
		this.port = Integer.parseInt(port);
		this.username = username;
		this.password = password;
		springAPIWrapper = new SpringAPIWrapper(this.hostname, this.port);
		if (springAPIWrapper.isLoggedIn()) {
			springAPIWrapper.logoutAction();
		}


	}

	@Override
	public void run(SourceContext<String> ctx) throws Exception {
		resultOfLoginCommand = springAPIWrapper.loginAction(this.username, this.password);
		ArrayList<String> listOfQuotes = null;
		ArrayList<String> listOfDepths = null;
		while (isRunning) {
			if (springAPIWrapper.isLoggedIn()) {
				if (retrieveQuotes) {
					//Retrieving Quotes (Level 1 data)
					try {
						springAPIWrapper.startQuoteAction(this.symbol);
						//Optionally give some time to collect next round of quotes.
						Thread.sleep(streamingWindowTimeMilliseconds);
						listOfQuotes = springAPIWrapper.getQuotesList();
						if (listOfQuotes != null && listOfQuotes.size() > 0) {
							for (String string : listOfQuotes) {
								//Skip the command that comes as first retrieval row (Form: "ORDERQUOTE, Added Eurex·846959·NoExpiry!")
								if (string != null && string.contains(SpringFinancialSource.ORDERQUOTE)) {
									continue; //Dont pass on.
								}
								if (string != null && string.length() > 0) {
									string = string.trim();
									String[] splits = string.split(",");
									JSONObject jsonObject = new JSONObject();
									if (splits.length > 3) {
										//trim away preceding index and space
										splits[0] = splits[0].substring(splits[0].indexOf(" ") + 1);
										jsonObject.put(SpringFinancialSource.KEY, SpringFinancialSource.L1);
										jsonObject.put(SpringFinancialSource.DATE_TIME, splits[0] + " " + splits[1]);
										jsonObject.put(SpringFinancialSource.STOCK_ID, symbol);
										jsonObject.put(SpringFinancialSource.PRICE, splits[2]);
										jsonObject.put(SpringFinancialSource.VOLUME, splits[3]);
										// Hand over the stream records to Flink framework
										ctx.collect(jsonObject.toString());
									}
								}
							}
						}
						// Clear quotes list in the wrapper to avoid duplication and buffer overflow.
						springAPIWrapper.clearQuotesList();
					} catch (Exception e) {}
				} else {
					//Retrieving Depths (Level 2 data)
					try {
						springAPIWrapper.startDepthAction(this.symbol);
						//Optionally give some time to collect next round of depths.
						Thread.sleep(streamingWindowTimeMilliseconds);
						listOfDepths = springAPIWrapper.getDepthsList();
						if (listOfDepths != null && listOfDepths.size() > 0) {
							for (String string : listOfDepths) {
								//Skip the command that comes as first retrieval row (Form: "ORDERQUOTE, Added Eurex·846959·NoExpiry!")
								if (string != null && string.contains(SpringFinancialSource.ORDER)) {
									continue; //Dont pass on.
								}
								if (string != null && string.length() > 0) {
									string = string.trim();
									String[] splits = string.split(",");
									JSONObject jsonObject = new JSONObject();
									if (splits.length > 4) {
										jsonObject.put(SpringFinancialSource.KEY, SpringFinancialSource.L2);
										jsonObject.put(SpringFinancialSource.DATE_TIME, splits[1] + " " + splits[2]);
										jsonObject.put(SpringFinancialSource.STOCK_ID, symbol); //symbol is in splits[3]
										//Decompose values into multiple single events with quadruple<Ask price, Ask volume, Bid price, Bid volume>

										String multiplexedEvent = splits[4];
										int position = 1;
										StringTokenizer stringTokenizer = new StringTokenizer(multiplexedEvent, ": ");
										while (stringTokenizer.hasMoreTokens()) {
											JSONObject singleEvent = jsonObject;
											singleEvent.put("Pos", position);
											position++;
											//Ask price, Ask volume, Bid price, Bid volume
											singleEvent.put(SpringFinancialSource.ASK_PRICE, stringTokenizer.nextToken());
											singleEvent.put(SpringFinancialSource.ASK_VOLUME, stringTokenizer.nextToken());
											singleEvent.put(SpringFinancialSource.BID_PRICE, stringTokenizer.nextToken());
											singleEvent.put(SpringFinancialSource.BID_VOLUME, stringTokenizer.nextToken());
											// Hand over the stream records to Flink framework
											ctx.collect(singleEvent.toString());
										}
									}
								}
							}
						}
						// Clear depths list in the wrapper to avoid duplication and buffer overflow.
						springAPIWrapper.clearDepthsList();
					} catch (Exception e) {}
				}
			}
		}
	}

	@Override
	public void cancel() {
		// Streaming application (job) has been stopped or cancelled.
		// Follow the disconnect scheme for flink and financial server
		this.isRunning = false;

		// Send the appropriate stop command to server
		if (retrieveQuotes) {
			springAPIWrapper.stopQuoteAction(this.symbol);
		} else {
			springAPIWrapper.stopDepthAction(this.symbol);
		}
		// Now logout and disconnect from server gracefully.
		springAPIWrapper.logoutAction();
		if (springAPIWrapper.getConnection().isThreadRunning()) {
			// Sleep current thread for n seconds to logout
			try {
				Thread.sleep(disconnectTimeMilliseconds);
			} catch (InterruptedException e) {}
		}

	}

	public void setStreamingWindowTimeMilliseconds(int streamingWindowTimeMilliseconds) {
		this.streamingWindowTimeMilliseconds = streamingWindowTimeMilliseconds;
	}

	public void setDisconnectTimeMilliseconds(int disconnectTimeMilliseconds) {
		this.disconnectTimeMilliseconds = disconnectTimeMilliseconds;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public void setRetrieveQuotes(boolean retrieveQuotes) {
		this.retrieveQuotes = retrieveQuotes;
	}

}