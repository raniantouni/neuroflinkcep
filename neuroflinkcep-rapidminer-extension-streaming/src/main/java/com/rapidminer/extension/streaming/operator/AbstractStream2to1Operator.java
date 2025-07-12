/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.tools.container.Pair;


/**
 * This is an abstract implementation for a {@link StreamOperator} which performs a 2 on 1 transformation of streamed
 * data.
 * <p>
 * It provides two input ports ({@link #firstStream} and {@link #secondStream}) and an {@link #output} port and
 * implements the {@link #getStreamDataInputs()}, {@link #addToGraph(StreamGraph, List)} and {@link
 * #deliverStreamDataOutputs(StreamGraph, List)} methods.
 * <p>
 * Subclasses need to define how the single {@link StreamProducer}, for the transformation functionality, is created by
 * implementing {@link #createProducer(StreamGraph, StreamDataContainer, StreamDataContainer)}. Subclasses also needs to
 * define the name of the input ports ({@link #getFirstInputName()} and {@link #getSecondInputName()}).
 *
 * @author Fabian Temme
 * @since 0.6.1
 */
public abstract class AbstractStream2to1Operator extends AbstractStreamOperator {

	protected final InputPort firstStream = getInputPorts().createPort(getFirstInputName(), StreamDataContainer.class);

	protected final InputPort secondStream =
		getInputPorts().createPort(getSecondInputName(), StreamDataContainer.class);

	protected final OutputPort output = getOutputPorts().createPort("output stream");

	public AbstractStream2to1Operator(OperatorDescription description) {
		super(description);
		getTransformer().addPassThroughRule(firstStream, output);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Two {@link StreamDataContainer} are retrieved from the {@link #firstStream} and {@link #secondStream} port and
	 * the {@link StreamGraph} of the first container together with the list of both container are returned.
	 */
	@Override
	public Pair<StreamGraph, List<StreamDataContainer>> getStreamDataInputs() throws UserError {
		StreamDataContainer firstStreamData = firstStream.getData(StreamDataContainer.class);
		StreamDataContainer secondStreamData = secondStream.getData(StreamDataContainer.class);
		StreamGraph graph = firstStreamData.getStreamGraph();
		return new Pair<>(graph, Arrays.asList(firstStreamData, secondStreamData));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * A singleton list is created from the result of {@link #createProducer(StreamGraph, StreamDataContainer,
	 * StreamDataContainer)}. For the {@link StreamDataContainer} of the {@link #createProducer(StreamGraph,
	 * StreamDataContainer, StreamDataContainer)} call, the first two entries of the {@code streamDataInputs} list are
	 * used.
	 */
	@Override
	public List<StreamProducer> addToGraph(StreamGraph graph, List<StreamDataContainer> streamDataInputs) throws UserError {
		StreamDataContainer firstStreamData = streamDataInputs.get(0);
		StreamDataContainer secondStreamData = streamDataInputs.get(1);
		return Collections.singletonList(createProducer(graph, firstStreamData, secondStreamData));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * A new {@link StreamDataContainer} is created with the provided {@code graph} and the first entry of the provided
	 * {@code streamProducers} list.
	 */
	@Override
	public void deliverStreamDataOutputs(StreamGraph graph, List<StreamProducer> streamProducers) throws UserError {
		StreamDataContainer outData = new StreamDataContainer(graph, streamProducers.get(0));
		output.deliver(outData);
	}

	/**
	 * Name used for the {@link #firstStream} input port.
	 *
	 * @return Name used for the {@link #firstStream} input port
	 */
	protected abstract String getFirstInputName();

	/**
	 * Name used for the {@link #secondStream} input port.
	 *
	 * @return Name used for the {@link #secondStream} input port
	 */
	protected abstract String getSecondInputName();

	/**
	 * This method defines how the provided {@code graph} and {@code inData} are used to add the stream transformation
	 * functionality of this {@link AbstractStream2to1Operator} to the {@code graph} and how the result {@link
	 * StreamProducer} is created.
	 *
	 * @param graph
	 *    {@link StreamGraph} to which the stream transformation functionality of this
	 *    {@link AbstractStream2to1Operator}
	 * 	shall be added.
	 * @param firstStreamData
	 *    {@link StreamDataContainer} used as the first input for the stream transformation functionality of this
	 *    {@link
	 *    AbstractStream2to1Operator}
	 * @param secondStreamData
	 *    {@link StreamDataContainer} used as the second input for the stream transformation functionality of this
	 *    {@link
	 *    AbstractStream2to1Operator}
	 * @return {@link StreamProducer} producing the result of the stream transformation functionality
	 */
	protected abstract StreamProducer createProducer(StreamGraph graph, StreamDataContainer firstStreamData,
													 StreamDataContainer secondStreamData) throws UserError;
}
