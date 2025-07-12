/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.utility.graph.StreamGraph;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;
import com.rapidminer.extension.streaming.utility.graph.transform.UnionTransformer;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPortExtender;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.Precondition;
import com.rapidminer.operator.ports.metadata.SimplePrecondition;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.container.Pair;


/**
 * Union operator for stream graphs
 *
 * @author Mate Torok
 * @since 0.3.0
 */
public class StreamUnion extends AbstractStreamOperator {

	private final InputPortExtender inputs = new InputPortExtender("in stream", getInputPorts()) {

		@Override
		protected Precondition makePrecondition(InputPort port) {
			int size = inputs.getManagedPorts().size();
			return new SimplePrecondition(port, new MetaData(StreamDataContainer.class), size < 2);
		}

	};

	private final OutputPort output = getOutputPorts().createPort("output stream");

	public StreamUnion(OperatorDescription description) {
		super(description);

		inputs.ensureMinimumNumberOfPorts(2);
		inputs.start();

		getTransformer().addGenerationRule(output, StreamDataContainer.class);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		return super.getParameterTypes();
	}

	@Override
	public Pair<StreamGraph, List<StreamDataContainer>> getStreamDataInputs() throws UserError {
		List<StreamDataContainer> inputList = inputs.getData(StreamDataContainer.class, true);

		StreamGraph graph = inputList.get(0).getStreamGraph();
		return new Pair<>(graph, inputList);
	}

	@Override
	public List<StreamProducer> addToGraph(StreamGraph graph, List<StreamDataContainer> streamDataInputs) throws UserError {
		List<StreamProducer> producerList = streamDataInputs
			.stream()
			.map(StreamDataContainer::getLastNode)
			.collect(Collectors.toList());
		return Collections.singletonList(new UnionTransformer.Builder(graph)
			.withParents(producerList)
			.build());
	}

	@Override
	public void deliverStreamDataOutputs(StreamGraph graph, List<StreamProducer> streamProducers) throws UserError {
		StreamDataContainer outData = new StreamDataContainer(graph, streamProducers.get(0));
		output.deliver(outData);
	}
}