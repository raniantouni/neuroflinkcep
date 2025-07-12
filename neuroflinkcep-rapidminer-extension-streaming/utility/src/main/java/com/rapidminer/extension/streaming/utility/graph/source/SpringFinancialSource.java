package com.rapidminer.extension.streaming.utility.graph.source;

import java.util.Properties;

import com.rapidminer.extension.streaming.utility.graph.StreamConsumer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamGraphNodeVisitor;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;
import com.rapidminer.extension.streaming.utility.graph.StreamSource;


/**
 * Representation of the Spring Financial Stream source operation
 *
 * @author Edwin Yaqub
 * @since 0.1.0
 */
public class SpringFinancialSource implements StreamSource, StreamProducer {

	public static final String HOST = "HOST";
	public static final String PORT = "PORT";
	public static final String USERNAME = "USERNAME";
	public static final String PASSWORD = "PASSWORD";
	public static final String SYMBOL = "SYMBOL";
	public static final String RETRIEVE_QUOTES = "RETRIEVE_QUOTES";
	public static final String RETRIEVE_QUOTES_YES = "YES";
	public static final String RETRIEVE_QUOTES_NO = "NO";
	public static final String STREAM_TIME_MS = "stream_time_(ms)";
	public static final String DISCONNECT_TIME_MS = "disconnect_time_(ms)";
	public static final String ORDERQUOTE = "ORDERQUOTE";
	public static final String ORDER = "ORDER";
	public static final String KEY = "key";
	public static final String L1 = "L1";
	public static final String L2 = "L2";
	public static final String DATE_TIME = "DateTime";
	public static final String STOCK_ID = "StockID";
	public static final String PRICE = "price";
	public static final String VOLUME = "Volume";
	public static final String POSITION = "Pos";
	public static final String ASK_PRICE = "Ask price";
	public static final String ASK_VOLUME = "Ask volume";
	public static final String BID_PRICE = "Bid price";
	public static final String BID_VOLUME = "Bid volume";

	private final long id;
	private StreamConsumer child;
	private Properties config;

	SpringFinancialSource() {
		this.id = -1;
	}

	SpringFinancialSource(SpringFinancialSource.Builder builder) {
		this.id = builder.graph.getNewId();
		this.config = builder.config;
	}

	@Override
	public void registerChild(StreamConsumer child) {
		this.child = child;
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
	public void accept(StreamGraphNodeVisitor visitor) {
		visitor.visit(this);
	}

	public Properties getConfig() {
		return config;
	}

	public StreamConsumer getChild() {
		return child;
	}

	/**
	 * Builder for the operation
	 */
	public static class Builder {

		private final StreamGraph graph;
		private Properties config;

		public Builder(StreamGraph graph) {
			this.graph = graph;
		}

		public SpringFinancialSource build() {
			return new SpringFinancialSource(this);
		}

		public SpringFinancialSource.Builder withConfiguration(Properties config) {
			this.config = config;
			return this;
		}

	}
}
