/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator;

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
 * This is an abstract implementation for a {@link StreamOperator} which performs a 1 on 1 transformation of streamed
 * data.
 * <p>
 * It provides an {@link #input} and {@link #output} port and implements the {@link #getStreamDataInputs()}, {@link
 * #addToGraph(StreamGraph, List)} and {@link #deliverStreamDataOutputs(StreamGraph, List)} methods.
 * <p>
 * Subclasses need to define how the single {@link StreamProducer}, for the transformation functionality, is created by
 * implementing {@link #createStreamProducer(StreamGraph, StreamDataContainer)}.
 *
 * @author Fabian Temme
 * @since 0.6.1
 */
public abstract class AbstractStreamTransformOperator extends AbstractStreamOperator {

	protected final InputPort input = getInputPorts().createPort("input stream", StreamDataContainer.class);

	protected final OutputPort output = getOutputPorts().createPort("output stream");

	public AbstractStreamTransformOperator(OperatorDescription description) {
		super(description);

		getTransformer().addPassThroughRule(input, output);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * A single {@link StreamDataContainer} is retrieved from the {@link #input} port and the {@link StreamGraph} of
	 * this container together with the singleton list of the container is returned.
	 */
	@Override
	public Pair<StreamGraph, List<StreamDataContainer>> getStreamDataInputs() throws UserError {
		StreamDataContainer inData = input.getData(StreamDataContainer.class);
		return new Pair<>(inData.getStreamGraph(), Collections.singletonList(inData));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * A singleton list is created from the result of {@link #createStreamProducer(StreamGraph, StreamDataContainer)}.
	 * For the {@link StreamDataContainer} of the {@link #createStreamProducer(StreamGraph, StreamDataContainer)}
	 * call, the first entry of the {@code streamDataInputs} list is used.
	 */
	@Override
	public List<StreamProducer> addToGraph(StreamGraph graph, List<StreamDataContainer> streamDataInputs) throws UserError {
		return Collections.singletonList(createStreamProducer(graph, streamDataInputs.get(0)));
	}

	/**
	 * This method defines how the provided {@code graph} and {@code inData} are used to add the stream
	 * transformation functionality of this {@link AbstractStreamTransformOperator} to the {@code graph} and how the
	 * result {@link StreamProducer} is created.
	 *
	 @param graph
	  *    {@link StreamGraph} to which the stream transformation functionality of this {@link AbstractStreamTransformOperator} shall be
	  *    added.
	  * @param inData
	 *    {@link StreamDataContainer} used as input for the stream transformation functionality of this {@link
	 *    AbstractStreamTransformOperator}
	 * @return {@link StreamProducer} producing the result of the stream transformation functionality
	 */
	protected abstract StreamProducer createStreamProducer(StreamGraph graph, StreamDataContainer inData) throws UserError;

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

}
