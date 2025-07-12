/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator.athena;

import static com.rapidminer.extension.streaming.connection.StreamingConnectionHelper.createKafkaSelector;
import static com.rapidminer.extension.streaming.operator.athena.SDEConstants.EstimationType;
import static com.rapidminer.extension.streaming.operator.athena.SDEConstants.ITEM_LIST_SEPARATOR;
import static com.rapidminer.extension.streaming.operator.athena.SDEConstants.PARAMETER_DATA_TOPIC;
import static com.rapidminer.extension.streaming.operator.athena.SDEConstants.PARAMETER_ESTIMATE_FREQUENCY;
import static com.rapidminer.extension.streaming.operator.athena.SDEConstants.PARAMETER_ESTIMATE_PARAMS;
import static com.rapidminer.extension.streaming.operator.athena.SDEConstants.PARAMETER_ESTIMATE_TYPE;
import static com.rapidminer.extension.streaming.operator.athena.SDEConstants.PARAMETER_OUTPUT_TOPIC;
import static com.rapidminer.extension.streaming.operator.athena.SDEConstants.PARAMETER_REQUEST_TOPIC;
import static com.rapidminer.extension.streaming.operator.athena.SDEConstants.PARAMETER_SYNOPSIS_DATA_SET_KEY;
import static com.rapidminer.extension.streaming.operator.athena.SDEConstants.PARAMETER_SYNOPSIS_PARALLELISM;
import static com.rapidminer.extension.streaming.operator.athena.SDEConstants.PARAMETER_SYNOPSIS_PARAMS;
import static com.rapidminer.extension.streaming.operator.athena.SDEConstants.PARAMETER_SYNOPSIS_STREAM_ID_KEY;
import static com.rapidminer.extension.streaming.operator.athena.SDEConstants.PARAMETER_SYNOPSIS_TYPE;
import static com.rapidminer.extension.streaming.operator.athena.SDEConstants.PARAMETER_SYNOPSIS_U_ID;
import static com.rapidminer.extension.streaming.utility.JsonUtil.toJson;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.connection.util.ConnectionInformationSelector;
import com.rapidminer.extension.kafka_connector.connections.KafkaConnectionHandler;
import com.rapidminer.extension.streaming.deploy.KafkaClient;
import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.operator.AbstractStreamTransformOperator;
import com.rapidminer.extension.streaming.utility.api.infore.synopsis.Request;
import com.rapidminer.extension.streaming.utility.api.infore.synopsis.RequestType;
import com.rapidminer.extension.streaming.utility.api.infore.synopsis.SynopsisType;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;
import com.rapidminer.extension.streaming.utility.graph.infore.synopsis.BaseSynopsisNode;
import com.rapidminer.extension.streaming.utility.graph.infore.synopsis.SynopsisDataProducer;
import com.rapidminer.extension.streaming.utility.graph.infore.synopsis.SynopsisEstimateQuery;
import com.rapidminer.extension.streaming.utility.graph.source.KafkaSource;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.NonEqualTypeCondition;
import com.rapidminer.tools.container.Pair;


/**
 * Operator for interacting with the Synopsis Data Engine. 3 graph nodes will be created:
 * <ul>
 *     <li>{@link SynopsisEstimateQuery}: periodically or continuously queries SDE</li>
 *     <li>{@link SynopsisDataProducer}: producing data for SDE</li>
 * </ul>
 *
 * @author David Arnu, Mate Torok
 * @since 0.1.0
 */
public class StreamSDEOperator extends AbstractStreamTransformOperator {

	private final ConnectionInformationSelector connectionSDE = createKafkaSelector(this, "connection");

	private Properties clusterConfig;

	public StreamSDEOperator(OperatorDescription description) {
		super(description);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Overwriting the {@link #doWork()} method from {@link AbstractStreamTransformOperator} to insert {@link
	 * #configureSynopsisEngine()}
	 */
	@Override
	public void doWork() throws OperatorException {
		Pair<StreamGraph, List<StreamDataContainer>> inputs = getStreamDataInputs();
		StreamGraph graph = inputs.getFirst();
		logProcessing(graph.getName());

		List<StreamProducer> streamProducers = addToGraph(graph, inputs.getSecond());

		// Send "Add" and potentially continuous "Estimate" request to SDE
		configureSynopsisEngine();

		deliverStreamDataOutputs(graph, streamProducers);
	}

	@Override
	protected StreamProducer createStreamProducer(StreamGraph graph, StreamDataContainer inData) throws UserError {
		// Setup cluster connection config
		ConnectionConfiguration connConfig = connectionSDE.getConnection().getConfiguration();
		clusterConfig = KafkaConnectionHandler.getINSTANCE().buildClusterConfiguration(connConfig);
		return extendGraph(inData);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType synopsis = new ParameterTypeCategory(
			PARAMETER_SYNOPSIS_TYPE,
			"The synopsis applied on the stream.",
			Arrays.stream(SynopsisType.values()).map(Enum::toString).toArray(String[]::new),
			0,
			false);
		types.add(synopsis);

		ParameterType synDataSetKey = new ParameterTypeString(
			PARAMETER_SYNOPSIS_DATA_SET_KEY,
			"Data set key.",
			false);
		types.add(synDataSetKey);

		ParameterType snyParams = new ParameterTypeString(
			PARAMETER_SYNOPSIS_PARAMS,
			"Synopsis parameters (comma separated).",
			false);
		types.add(snyParams);

		ParameterType synParalellism = new ParameterTypeString(
			PARAMETER_SYNOPSIS_PARALLELISM,
			"Synopsis parallelism.",
			false);
		types.add(synParalellism);

		ParameterType synUId = new ParameterTypeInt(
			PARAMETER_SYNOPSIS_U_ID,
			"Synopsis UId.",
			0,
			Integer.MAX_VALUE,
			false);
		types.add(synUId);

		ParameterType synStreamId = new ParameterTypeString(
			PARAMETER_SYNOPSIS_STREAM_ID_KEY,
			"Key in the data that should be used as StreamId.",
			false);
		types.add(synStreamId);

		ParameterType estimateCont = new ParameterTypeCategory(
			PARAMETER_ESTIMATE_TYPE,
			"Type of synopsis estimation.",
			EstimationType.valueStrings(),
			0,
			false);
		types.add(estimateCont);

		ParameterType estimateFreq = new ParameterTypeInt(
			PARAMETER_ESTIMATE_FREQUENCY,
			"Estimate frequency.",
			1,
			Integer.MAX_VALUE,
			true);
		estimateFreq.setDefaultValue(1);
		estimateFreq.registerDependencyCondition(
			new NonEqualTypeCondition(this, PARAMETER_ESTIMATE_TYPE, EstimationType.valueStrings(), false, 2));
		types.add(estimateFreq);

		ParameterType estimateParams = new ParameterTypeString(
			PARAMETER_ESTIMATE_PARAMS,
			"Estimate request params (comma separated).",
			true);
		types.add(estimateParams);

		ParameterType synReqTopic = new ParameterTypeString(PARAMETER_REQUEST_TOPIC, "Request topic.", false);
		types.add(synReqTopic);

		ParameterType synDataTopic = new ParameterTypeString(PARAMETER_DATA_TOPIC, "Data topic.", false);
		types.add(synDataTopic);

		ParameterType synOutputTopic = new ParameterTypeString(PARAMETER_OUTPUT_TOPIC, "Output topic.", false);
		types.add(synOutputTopic);

		return types;
	}

	/**
	 * Extends the graph with synopsis logic
	 *
	 * @param inData
	 * @return node representing the incoming synopsis data for the downstream
	 */
	private StreamProducer extendGraph(StreamDataContainer inData) throws UndefinedParameterError {
		StreamGraph graph = inData.getStreamGraph();
		StreamProducer lastNode = inData.getLastNode();

		// Data producer for SDE (sink, will be connected to the graph via its parent)
		SynopsisDataProducer dataProducer = enrichBuilder(new SynopsisDataProducer.Builder(graph))
			.withTopic(getParameterAsString(PARAMETER_DATA_TOPIC))
			.withParent(lastNode)
			.build();

		// Synopsis consumer from SDE (source)
		KafkaSource synopsisConsumer = new KafkaSource.Builder(graph)
			.withConfiguration(clusterConfig)
			.withTopic(getParameterAsString(PARAMETER_OUTPUT_TOPIC))
			.build();
		graph.registerSource(synopsisConsumer);

		// Estimate request emitter (source) if not continuous
		EstimationType estimationType = EstimationType.fromName(getParameterAsString(PARAMETER_ESTIMATE_TYPE));
		if (estimationType != EstimationType.CONTINUOUS) {
			SynopsisEstimateQuery estimateQuery = enrichBuilder(new SynopsisEstimateQuery.Builder(graph))
				.withTopic(getParameterAsString(PARAMETER_REQUEST_TOPIC))
				.withFrequency(getParameterAsInt(PARAMETER_ESTIMATE_FREQUENCY))
				.withParameters(getParams(PARAMETER_ESTIMATE_PARAMS))
				.build();
			graph.registerSource(estimateQuery);
		}

		return synopsisConsumer;
	}

	/**
	 * For the sake of avoiding redundant code
	 *
	 * @param builder
	 * @throws UndefinedParameterError
	 */
	private <T extends BaseSynopsisNode.Builder<T>> T enrichBuilder(T builder) throws UndefinedParameterError {
		return builder
			.withConfiguration(clusterConfig)
			.withDataSetKey(getParameterAsString(PARAMETER_SYNOPSIS_DATA_SET_KEY))
			.withUId(getParameterAsInt(PARAMETER_SYNOPSIS_U_ID))
			.withParallelism(getParameterAsInt(PARAMETER_SYNOPSIS_PARALLELISM))
			.withStreamIdKey(getParameterAsString(PARAMETER_SYNOPSIS_STREAM_ID_KEY))
			.withSynopsis(SynopsisType.fromString(getParameterAsString(PARAMETER_SYNOPSIS_TYPE)));
	}

	/**
	 * Sends an "Add" request with the synopsis configuration to the appropriate Kafka topic thus activating the
	 * synopsis collection if not continuous. Otherwise a continuous estimation request will be dispatched.
	 */
	private void configureSynopsisEngine() throws OperatorException {
		KafkaClient kafkaClient = new KafkaClient(clusterConfig);
		String streamIdKey = getParameterAsString(PARAMETER_SYNOPSIS_STREAM_ID_KEY);
		String reqTopic = getParameterAsString(PARAMETER_REQUEST_TOPIC);
		String dataSetKey = getParameterAsString(PARAMETER_SYNOPSIS_DATA_SET_KEY);
		SynopsisType synopsis = SynopsisType.fromString(getParameterAsString(PARAMETER_SYNOPSIS_TYPE));
		int uid = getParameterAsInt(PARAMETER_SYNOPSIS_U_ID);
		String[] synParams = getParams(PARAMETER_SYNOPSIS_PARAMS);
		int parallelism = getParameterAsInt(PARAMETER_SYNOPSIS_PARALLELISM);

		// Request to send to SDE
		LOGGER.fine("Configuring SDE estimation");
		Request request = null;

		switch (EstimationType.fromName(getParameterAsString(PARAMETER_ESTIMATE_TYPE))) {
			case NORMAL:
				request = new Request(dataSetKey, RequestType.ADD, synopsis, uid, streamIdKey, synParams, parallelism);
				break;
			case WITH_RANDOM_PARTITIONING:
				request = new Request(dataSetKey, RequestType.ADD_WITH_RANDOM_PARTITIONING, synopsis, uid, streamIdKey, synParams, parallelism);
				break;
			case CONTINUOUS:
				request = new Request(dataSetKey, RequestType.CONTINUOUS, synopsis, uid, streamIdKey, synParams, parallelism);
				break;
			case ADVANCED:
				request = new Request(dataSetKey, RequestType.ADVANCED_ESTIMATE, synopsis, uid, streamIdKey, synParams, parallelism);
				break;
			default:
				throw new OperatorException("Invalid synopsis estimation type");
		}

		kafkaClient.send(reqTopic, request.getDataSetKey(), toJson(request));
	}

	/**
	 * Takes the parameter as a string, splits it by ",", trims the elements and returns the resulting String array
	 *
	 * @param key
	 * @return see above
	 * @throws UndefinedParameterError
	 */
	private String[] getParams(String key) throws UndefinedParameterError {
		String param = getParameterAsString(key);
		if (param == null) {
			return null;
		} else {
			return Arrays.stream(StringUtils.split(param, ITEM_LIST_SEPARATOR))
			.peek(StringUtils::trim)
			.toArray(String[]::new);
		}
	}

}