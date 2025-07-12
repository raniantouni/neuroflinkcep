package com.rapidminer.extension.streaming.flink.translate;

import java.util.Properties;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rapidminer.extension.streaming.utility.graph.source.SpringFinancialSource;


/**
 * Flink specific translator for SpringFinancialSource
 *
 * @author Edwin Yaqub
 * @since 0.1.0
 */
public class FlinkSpringFinancialSourceTranslator {

	private final StreamExecutionEnvironment executionEnv;

	public FlinkSpringFinancialSourceTranslator(StreamExecutionEnvironment executionEnv) {
		this.executionEnv = executionEnv;
	}

	/**
	 * Sources SpringFinancialSource for a data stream
	 *
	 * @param source configurations for the source
	 * @return new stream
	 */
	public DataStream<JSONObject> translate(SpringFinancialSource source) {
		Properties config = source.getConfig();
		String ipAddress = config.getProperty(SpringFinancialSource.HOST);
		String port = config.getProperty(SpringFinancialSource.PORT);
		String username = config.getProperty(SpringFinancialSource.USERNAME);
		String password = config.getProperty(SpringFinancialSource.PASSWORD);

		int streamingWindowTimeMilliseconds = Integer.parseInt(config.getProperty(SpringFinancialSource.STREAM_TIME_MS));
		int disconnectTimeMilliseconds = Integer.parseInt(config.getProperty(SpringFinancialSource.DISCONNECT_TIME_MS));
		String symbol = config.getProperty(SpringFinancialSource.SYMBOL);
		boolean retrieveQuotes = config.getProperty(SpringFinancialSource.RETRIEVE_QUOTES).equals(SpringFinancialSource.RETRIEVE_QUOTES_YES);

		// Call Flink connector class (wraps SpringTechno financial stream socket server)
		try {
			SpringFinancialSourceSocketTextStreamFunction consumer = new SpringFinancialSourceSocketTextStreamFunction(ipAddress, port, username, password);
			consumer.setStreamingWindowTimeMilliseconds(streamingWindowTimeMilliseconds);
			consumer.setDisconnectTimeMilliseconds(disconnectTimeMilliseconds);
			consumer.setSymbol(symbol);
			consumer.setRetrieveQuotes(retrieveQuotes);
			if (consumer != null) {
				return executionEnv.addSource(consumer).map(JSONObject::new);
			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
	}
}
