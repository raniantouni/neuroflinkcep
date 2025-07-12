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
import com.rapidminer.extension.streaming.utility.graph.transform.DuplicateStreamTransformer;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.tools.container.Pair;


/**
 * Duplicate operator for stream graphs
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamDuplicate extends AbstractStreamOperator {

	private final InputPort input = getInputPorts().createPort("input stream", StreamDataContainer.class);

	private final OutputPort output1 = getOutputPorts().createPort("output stream 1");

	private final OutputPort output2 = getOutputPorts().createPort("output stream 2");

	public StreamDuplicate(OperatorDescription description) {
		super(description);

		getTransformer().addPassThroughRule(input, output1);
		getTransformer().addPassThroughRule(input, output2);
	}

	@Override
	public Pair<StreamGraph, List<StreamDataContainer>> getStreamDataInputs() throws UserError {
		StreamDataContainer inData = input.getData(StreamDataContainer.class);
		return new Pair<>(inData.getStreamGraph(), Collections.singletonList(inData));
	}

	@Override
	public List<StreamProducer> addToGraph(StreamGraph graph, List<StreamDataContainer> streamDataInputs) throws UserError {
		StreamDataContainer inData = streamDataInputs.get(0);
		StreamProducer duplicate = new DuplicateStreamTransformer.Builder(graph)
			.withParent(inData.getLastNode())
			.build();
		return Arrays.asList(duplicate, duplicate);
	}

	@Override
	public void deliverStreamDataOutputs(StreamGraph graph, List<StreamProducer> streamProducers) throws UserError {
		StreamDataContainer outData1 = new StreamDataContainer(graph, streamProducers.get(0));
		StreamDataContainer outData2 = new StreamDataContainer(graph, streamProducers.get(1));
		output1.deliver(outData1);
		output2.deliver(outData2);
	}
}