/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator;

import java.util.List;

import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.tools.container.Pair;


/**
 * This interface defines a stream processing operator which can be placed inside a {@link StreamingNest} operator and
 * deployed to a streaming platform.
 *
 * @author Fabian Temme
 * @since 0.6.1
 */
public interface StreamOperator {

	/**
	 * This method defines how the {@link StreamGraph} and the {@link StreamDataContainer} are retrieved, which are
	 * used
	 * as input of the stream processing functionality of this {@link StreamOperator}. The order of the {@link
	 * StreamDataContainer} in the returned values should be in the order of the ports of the operator. The returned
	 * values will be used in {@link #addToGraph(StreamGraph, List)} to update the graph with the stream processing
	 * functionality.
	 *
	 * @return Pair with the {@link StreamGraph} and a list of {@link StreamDataContainer} which are used as input of
	 * the stream processing functionality of this {@link StreamOperator}
	 */
	Pair<StreamGraph, List<StreamDataContainer>> getStreamDataInputs() throws UserError;

	/**
	 * This method adds the stream processing functionality of the {@link StreamOperator} to the provided {@code
	 * graph}.
	 * The provided {@code streamDataInputs} can be used to configure the functionality.
	 * <p>
	 * The resulting list of {@link StreamProducer}s is returned. The return value can be an empty list if no {@link
	 * StreamProducer} are created for this {@link StreamOperator}.
	 *
	 * @param graph
	 *    {@link StreamGraph} to which the stream processing functionality of this {@link StreamOperator} shall be
	 *    added.
	 * @param streamDataInputs
	 *    {@link StreamDataContainer} used as input for the stream processing functionality of this {@link
	 *    StreamOperator}
	 * @return resulting list of {@link StreamProducer}s, can be an empty list if no {@link StreamProducer} is created
	 * for this {@link StreamOperator}
	 */
	List<StreamProducer> addToGraph(StreamGraph graph, List<StreamDataContainer> streamDataInputs) throws UserError;

	/**
	 * This method defines how the {@code graph} and {@code streamProducers} returned by
	 * {@link #addToGraph(StreamGraph,
	 * List)} are delivered to output ports of the {@link StreamProducer}.
	 *
	 * @param graph
	 *    {@link StreamGraph} to which the stream processing functionality of this {@link StreamOperator} is added.
	 * @param streamProducers
	 * 	List of {@link StreamProducer}s which contain the output of the stream processing functionality of this {@link
	 *    StreamOperator}.
	 */
	void deliverStreamDataOutputs(StreamGraph graph, List<StreamProducer> streamProducers) throws UserError;

	/**
	 * Default implementation which can be used for the {@link Operator#doWork()} method of a {@link StreamOperator}.
	 * {@link #getStreamDataInputs()}, {@link #logProcessing(String)}, {@link #addToGraph(StreamGraph, List)} and
	 * {@link
	 * #deliverStreamDataOutputs(StreamGraph, List)} are executed after each other.
	 */
	default void doWorkDefault() throws OperatorException {
		Pair<StreamGraph, List<StreamDataContainer>> inputs = getStreamDataInputs();
		StreamGraph graph = inputs.getFirst();
		logProcessing(graph.getName());

		List<StreamProducer> streamProducers = addToGraph(graph, inputs.getSecond());
		deliverStreamDataOutputs(graph, streamProducers);
	}

	void logProcessing(String graphName);

}
