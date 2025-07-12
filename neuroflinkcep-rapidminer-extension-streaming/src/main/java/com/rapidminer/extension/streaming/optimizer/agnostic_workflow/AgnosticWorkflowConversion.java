/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.agnostic_workflow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.util.Pair;

import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.extension.streaming.ioobject.StreamDataContainer;
import com.rapidminer.extension.streaming.operator.StreamingNest;
import com.rapidminer.extension.streaming.optimizer.agnostic_workflow.AWPort.PortType;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.OutputPorts;
import com.rapidminer.operator.ports.Port;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.MDInteger;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.OperatorService;


/**
 * This class is used to convert a RapidMiner process (an {@link ExecutionUnit} into its {@link AgnosticWorkflow}
 * representation and wise versa.
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
public class AgnosticWorkflowConversion {

	/**
	 * Class key for the {@link StreamingNest} operator in {@link ExecutionUnit}s.
	 */
	public static final String NEST_CLASS_KEY = "streaming:streaming_nest";
	/**
	 * Name of the {@link InputPort} for the connection of the the {@link StreamingNest} operator
	 */
	public static final String NEST_INPUT_PORT_NAME = "connection";
	/**
	 * Key of the job name parameter of the {@link StreamingNest} operator
	 */
	public static final String NEST_JOB_NAME_PARAM_KEY = "job_name";
	/**
	 * Range of the job name parameter of the {@link StreamingNest} operator
	 */
	public static final String NEST_JOB_NAME_PARAM_RANGE = "string";
	/**
	 * Name of the subprocess of the {@link StreamingNest} operator.
	 */
	public static final String NEST_WORKFLOW_NAME = "Streaming";
	/**
	 * Appendix for the Operators in the optimized subprocess.
	 */
	public static final String OPERATOR_NAME_APPENDIX = " (optimized)";

	private AgnosticWorkflowConversion() {
	}

	/**
	 * Converts the provided {@link ExecutionUnit} into its {@link AgnosticWorkflow} representation.
	 * <p>
	 * The inner sources and sink information are retrieved by calling {@link #getPortsAndSchemas(List, PortType)} for
	 * the corresponding ports. The operator connections and the operators are retrieved by calling {@link
	 * #fillAWOperatorAndConnections(ExecutionUnit)} (which iteratively calls processToAgnosticWorkflow(ExecutionUnit)
	 * in case operators have subprocesses).
	 *
	 * @param process
	 *        {@link ExecutionUnit} which is converted into an {@link AgnosticWorkflow}
	 * @return the {@link AgnosticWorkflow} representation of the provided {@code process}
	 */
	public static AgnosticWorkflow processToAgnosticWorkflow(ExecutionUnit process) throws
			UndefinedParameterError {
		String workflowName = process.getName();
		String enclosingOperatorName = process.getEnclosingOperator().getName();

		List<AWPort> innerSourcesPortsAndSchemas = new ArrayList<>(
				getPortsAndSchemas(process.getInnerSources().getAllPorts(),
						PortType.INNER_OUTPUT_PORT));
		List<AWPort> innerSinksPortsAndSchemas = new ArrayList<>(
				getPortsAndSchemas(process.getInnerSinks().getAllPorts(),
						PortType.INNER_INPUT_PORT));

		Pair<List<AWOperatorConnection>, List<AWOperator>> pair = fillAWOperatorAndConnections(
				process);

		List<AWOperatorConnection> operatorConnections = pair.getFirst();
		List<AWOperator> operators = pair.getSecond();

		return new AgnosticWorkflow().setOperatorConnections(operatorConnections)
				.setOperators(operators)
				.setWorkflowName(workflowName)
				.setEnclosingOperatorName(enclosingOperatorName)
				.setInnerSinksPortsAndSchemas(innerSinksPortsAndSchemas)
				.setInnerSourcesPortsAndSchemas(innerSourcesPortsAndSchemas);
	}

	/**
	 * Creates an {@link ExecutionUnit} from the provided information. The {@code enclosingOperator} needs to be
	 * provided, as well as the {@code subprocessIndex} which describes in which subprocess of the {@code
	 * enclosingOperator} the created {@link ExecutionUnit} shall be added. Also the {@link AgnosticWorkflow}
	 * representation of the to be created {@link ExecutionUnit} needs to be provided.
	 * <p>
	 * The subprocess at position {@code subprocessIndex} is retrieved from the {@code enclosingOperator}. First the
	 * {@link Operator}s represented by the {@link AWOperator} in the {@code workflow} are created and added to the
	 * subprocess. If the {@link AWOperator} has subprocesses, they are created and added as well by calling
	 * agnosticWorkflowToProcess(OperatorChain, AgnosticWorkflow, int).
	 * <p>
	 * Then all {@link AWOperatorConnection} are retrieved and the corresponding ports are connected.
	 * <p>
	 * The updated subprocess is returned.
	 *
	 * @param enclosingOperator
	 * 		The enclosing {@link OperatorChain} in which the subprocess is created
	 * @param workflow
	 * 		The {@link AgnosticWorkflow} representation of the to be created subprocess
	 * @param subprocessIndex
	 * 		The index of the to be created subprocess in the {@code enclosingOperator}.
	 * @return the updated subprocess of the {@code enclosingOperator}
	 */
	public static ExecutionUnit agnosticWorkflowToProcess(OperatorChain enclosingOperator,
														  AgnosticWorkflow workflow, int subprocessIndex) throws OperatorCreationException {
		String enclosingOperatorName = workflow.getEnclosingOperatorName();

		if (!enclosingOperator.getName().equals(enclosingOperatorName)) {
			throw new IllegalArgumentException(
					"Enclosing operator name from the agnostic workflow is not equal to the name of the provided " +
							"enclosing operator");
		}

		if (subprocessIndex >= enclosingOperator.getNumberOfSubprocesses()) {
			throw new IllegalArgumentException(
					"Provided subprocess index is larger or equal to the number of subprocesses of the provided " +
							"enclosing operator");
		}

		List<AWOperatorConnection> connections = workflow.getOperatorConnections();
		List<AWOperator> operators = workflow.getOperators();

		ExecutionUnit process = enclosingOperator.getSubprocess(subprocessIndex);
		process.setName(workflow.getWorkflowName());

		for (AWOperator awOperator : operators) {
			Operator operator = OperatorService.createOperator(awOperator.getOperatorClass());
			operator.rename(awOperator.getName());
			operator.setEnabled(awOperator.getIsEnabled());

			List<AWParameter> parameters = awOperator.getParameters();
			for (AWParameter awParameter : parameters) {
				operator.setParameter(awParameter.getKey(), awParameter.getValue());
			}

			if (awOperator.getHasSubprocesses()) {
				OperatorChain chain = (OperatorChain) operator;
				for (int index = 0; index < chain.getNumberOfSubprocesses(); index++) {
					agnosticWorkflowToProcess(chain, awOperator.getInnerWorkflows().get(index),
							index);
				}
			}
			process.addOperator(operator);
		}

		for (AWOperatorConnection awConnection : connections) {
			String fromOperatorName = awConnection.getFromOperator();
			String fromPortName = awConnection.getFromPort();
			PortType fromPortType = awConnection.getFromPortType();
			String toOperatorName = awConnection.getToOperator();
			String toPortName = awConnection.getToPort();
			PortType toPortType = awConnection.getToPortType();

			OutputPort outputPort = null;
			if (fromPortType == null || fromPortType == PortType.OUTPUT_PORT) {
				outputPort = process.getOperatorByName(fromOperatorName)
						.getOutputPorts()
						.getPortByName(fromPortName);
			} else if (fromPortType == PortType.INNER_OUTPUT_PORT) {
				outputPort = process.getInnerSources().getPortByName(fromPortName);
			}
			InputPort inputPort = null;
			if (toPortType == null || toPortType == PortType.INPUT_PORT) {
				inputPort = process.getOperatorByName(toOperatorName)
						.getInputPorts()
						.getPortByName(toPortName);
			} else if (toPortType == PortType.INNER_INPUT_PORT) {
				inputPort = process.getInnerSinks().getPortByName(toPortName);
			}

			if (inputPort == null) {
				throw new IllegalArgumentException(
						"Could not retrieve input port for connection: " + fromOperatorName + "." + fromPortName +
								" -> " + toOperatorName + "." + toPortName);
			}
			if (outputPort == null) {
				throw new IllegalArgumentException(
						"Could not retrieve outputport port for connection: " + fromOperatorName + "." + fromPortName +
								" -> " + toOperatorName + "." + toPortName);
			}
			outputPort.connectTo(inputPort);
		}

		return process;
	}

	private static Pair<List<AWOperatorConnection>, List<AWOperator>> fillAWOperatorAndConnections(
			ExecutionUnit process) throws UndefinedParameterError {
		// Initialize variables
		List<AWOperator> operators = new ArrayList<>();
		String enclosingOperatorName = process.getEnclosingOperator().getName();
		// Add connections from the inner sources of this Subprocess
		List<AWOperatorConnection> operatorConnections = new ArrayList<>(
				getAWOperatorConnectionsFromPorts(process.getInnerSources(), enclosingOperatorName,
						enclosingOperatorName));

		// Loop over the operators in the process
		for (Operator operator : process.getOperators()) {
			// get the variables for all operators (independent if their are OperatorChains or only
			// Operators
			boolean isEnabled = operator.isEnabled();
			String operatorName = operator.getName();
			String classKey = operator.getOperatorDescription().getKey();
			Class<? extends Operator> operatorClass = operator.getClass();

			List<AWPort> inputPortsAndSchemas = getPortsAndSchemas(
					operator.getInputPorts().getAllPorts(), PortType.INPUT_PORT);
			List<AWPort> outputPortsAndSchemas = getPortsAndSchemas(
					operator.getOutputPorts().getAllPorts(), PortType.OUTPUT_PORT);
			List<AWParameter> parameters = getAWParameters(operator);

			// Initialize variables for OperatorChains
			boolean isChain = false;
			Integer numberSubprocesses = null;
			List<AgnosticWorkflow> innerWorkflows = null;
			// Fill OperatorChain variables in case we have an OperatorChain
			if (operator instanceof OperatorChain) {
				OperatorChain chain = (OperatorChain) operator;
				isChain = true;
				numberSubprocesses = chain.getNumberOfSubprocesses();
				innerWorkflows = new ArrayList<>();
				// fill recursive the workflow information for the inner workflows
				for (ExecutionUnit subprocess : chain.getSubprocesses()) {
					innerWorkflows.add(processToAgnosticWorkflow(subprocess));
				}
			}

			// add the information for this operator to the operators list
			operators.add(new AWOperator().setName(operatorName)
					.setClassKey(classKey)
					.setOperatorClass(operatorClass)
					.setHasSubprocesses(isChain)
					.setIsEnabled(isEnabled)
					.setNumberOfSubprocesses(numberSubprocesses)
					.setInputPortsAndSchemas(inputPortsAndSchemas)
					.setOutputPortsAndSchemas(outputPortsAndSchemas)
					.setParameters(parameters)
					.setInnerWorkflows(innerWorkflows));

			// Add the connections from the output ports from this operator to the
			// operatorConnections list
			operatorConnections.addAll(
					getAWOperatorConnectionsFromPorts(operator.getOutputPorts(), operator.getName(),
							enclosingOperatorName));
		}

		// return the pair of Lists, containing the operatorConnections and the operators (which
		// have recursively also the workflows of subprocesses, in case of OperatorChains.
		return new Pair<>(operatorConnections, operators);
	}

	private static List<AWOperatorConnection> getAWOperatorConnectionsFromPorts(
			OutputPorts outputPorts, String fromOperatorName, String enclosingOperatorName) {
		List<AWOperatorConnection> operatorConnections = new ArrayList<>();

		PortType fromType = PortType.OUTPUT_PORT;
		if (fromOperatorName.equals(enclosingOperatorName)) {
			fromType = PortType.INNER_OUTPUT_PORT;
		}

		for (OutputPort outputPort : outputPorts.getAllPorts()) {
			if (outputPort.isConnected()) {
				InputPort dest = outputPort.getDestination();
				String toOperatorName = dest.getPorts().getOwner().getOperator().getName();
				PortType toType = PortType.INPUT_PORT;
				if (toOperatorName.equals(enclosingOperatorName)) {
					toType = PortType.INNER_INPUT_PORT;
				}
				operatorConnections.add(new AWOperatorConnection().setFromOperator(fromOperatorName)
						.setFromPort(outputPort.getName())
						.setToOperator(toOperatorName)
						.setToPort(dest.getName())
						.setFromPortType(fromType)
						.setToPortType(toType));
			}
		}
		return operatorConnections;
	}

	@SuppressWarnings({"deprecation", "rawtypes"})
	private static List<AWPort> getPortsAndSchemas(List<? extends Port> ports, PortType portType) {
		List<AWPort> inputPortsAndSchemas = new ArrayList<>();

		for (Port port : ports) {
			IOObject data = port.getRawData();
			Class<? extends IOObject> objectClass = data != null ? data.getClass() : null;
			AWSchema schema = null;
			if (data == null) {
				// Maybe we can use MetaData
				MetaData metaData = port.getMetaData();
				objectClass = metaData != null ? metaData.getObjectClass() : null;
				if (metaData instanceof ExampleSetMetaData) {
					schema = new AWSchema().setFromMetaData(true);
					ExampleSetMetaData emd = (ExampleSetMetaData) metaData;
					MDInteger numberOfExamples = emd.getNumberOfExamples();
					if (numberOfExamples.isKnown()) {
						schema.setSize(numberOfExamples.getValue());
					}
					schema.setAttributes(getAttributeList(emd));
				}
			} else if (data instanceof ExampleSet) {
				schema = new AWSchema().setFromMetaData(false);
				ExampleSet exampleSet = (ExampleSet) data;
				schema.setSize(exampleSet.size());
				schema.setAttributes(getAttributeList(exampleSet));
			}
			inputPortsAndSchemas.add(new AWPort().setIsConnected(port.isConnected())
					.setPortType(portType)
					.setName(port.getName())
					.setObjectClass(objectClass)
					.setSchema(schema));
		}

		return inputPortsAndSchemas;
	}

	private static List<AWAttribute> getAttributeList(ExampleSetMetaData emd) {
		List<AWAttribute> attributes = new ArrayList<>();

		for (AttributeMetaData amd : emd.getAllAttributes()) {
			attributes.add(new AWAttribute().setName(amd.getName())
					.setSpecialRole(amd.getRole())
					.setType(Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(
							amd.getValueType())));
		}

		return attributes;
	}

	private static List<AWAttribute> getAttributeList(ExampleSet exampleSet) {
		List<AWAttribute> attributes = new ArrayList<>();
		Iterator<AttributeRole> iterator = exampleSet.getAttributes().allAttributeRoles();

		while (iterator.hasNext()) {
			AttributeRole role = iterator.next();
			attributes.add(new AWAttribute().setName(role.getAttribute().getName())
					.setSpecialRole(role.getSpecialName())
					.setType(Ontology.ATTRIBUTE_VALUE_TYPE.mapIndex(
							role.getAttribute().getValueType())));
		}

		return attributes;
	}

	private static List<AWParameter> getAWParameters(Operator operator) throws
			UndefinedParameterError {
		List<AWParameter> parameters = new ArrayList<>();

		for (ParameterType parameter : operator.getParameters().getParameterTypes()) {
			String key = parameter.getKey();
			String value = operator.getParameter(key);
			String defaultValue = parameter.getDefaultValueAsString();
			Class<? extends ParameterType> typeClass = parameter.getClass();
			String range = parameter.getRange();
			parameters.add(new AWParameter().setKey(key)
					.setValue(value)
					.setDefaultValue(defaultValue)
					.setTypeClass(typeClass)
					.setRange(range));
		}

		return parameters;
	}

	/**
	 * This process updates an {@link AgnosticWorkflow} for which the optimizer decided on the placement of the
	 * operators (performed the optimization).
	 * <p>
	 * First all {@link AWOperator} in the provided {@link AgnosticWorkflow} are renamed by appending {@value
	 * #OPERATOR_NAME_APPENDIX}. Also the {@link AWOperatorConnection}s and the names of the operators in the {@link
	 * AWPlacementSite}s are adapted. This enables to place the optimized operators in the same Process as the logical
	 * operators.
	 * <p>
	 * The method than loops through the different combinations of {@link AWPlacementSite} and {@link
	 * AWPlacementPlatform}s and creates the corresponding {@link StreamingNest} operators. All {@link AWOperator}s
	 * which are placed in the corresponding site-platform are added to the subprocess of the new streaming nest
	 * operator. {@link AWOperatorConnection}s which connect operators inside the site-platform are added as well,
	 * while
	 * for {@link AWOperatorConnection} between operators of different site-platforms, a List of {@link
	 * SplittedConnection}s is created.
	 * <p>
	 * The updated {@link AgnosticWorkflow} and the {@link List} of {@link SplittedConnection} is returned.
	 *
	 * @param workflow
	 *        {@link AgnosticWorkflow} which contains the information about the placement of the operators
	 * @return updated {@link AgnosticWorkflow} and the {@link List} of {@link SplittedConnection}
	 */
	public static Pair<AgnosticWorkflow,List<SplittedConnection>> updateOptimizedWorkflow(
			AgnosticWorkflow workflow, String identifier, String workflowId) {
		// append OPERATOR_NAME_APPENDIX to the operator names (and the corresponding entries in
		// the connections)
		updateOperatorNames(workflow, workflow.getEnclosingOperatorName());
		// Append OPERATOR_NAME_APPENDIX to the names in the placementSites.
		for (AWPlacementSite site : workflow.getPlacementSites()) {
			for (AWPlacementPlatform platform : site.getAvailablePlatforms()) {
				if (!platform.getOperators().isEmpty()) {
					platform.getOperators().replaceAll(operator -> operator.setName(
							operator.getName() + OPERATOR_NAME_APPENDIX));
				}
			}
		}

		List<SplittedConnection> splittedConnection = new ArrayList<>();
		List<AWOperator> operators = new ArrayList<>();

		for (AWPlacementSite site : workflow.getPlacementSites()) {
			for (AWPlacementPlatform platform : site.getAvailablePlatforms()) {
				if (!platform.getOperators().isEmpty()) {
					String streamingNestName = constructStreamingNestName(site, platform, identifier);
					// Retrieve the innerOperators and inner operator connections for this Streaming
					// Nest operator
					List<AWOperator> innerOperators = new ArrayList<>();
					List<AWOperatorConnection> innerConnections = new ArrayList<>();
					List<String> placedOperators = new ArrayList<>();
					platform.getOperators().forEach((op -> placedOperators.add(op.getName())));
					for (AWPlacementOperator placementOperator : platform.getOperators()) {
						Pair<AWOperator, Pair<List<AWOperatorConnection>, List<SplittedConnection>>>
								operatorAndConnections = findOperatorAndConnections(
								placementOperator.getName(), workflow, placedOperators, streamingNestName, identifier);
						AWOperator operator = operatorAndConnections.getFirst();
						if (operator == null) {
							throw new IllegalArgumentException(
									"Could not found placed operator (name: " + placementOperator.getName() + ") in the " +
											"workflow.");
						}
						innerOperators.add(operator);
						innerConnections.addAll(operatorAndConnections.getSecond().getFirst());
						splittedConnection.addAll(operatorAndConnections.getSecond().getSecond());
					}
					// Put together the innerWorkflow and create the list to add to the AW nest
					// operator.
					List<AgnosticWorkflow> innerWorkflows = new ArrayList<>();
					AgnosticWorkflow innerWorkflow = new AgnosticWorkflow().setEnclosingOperatorName(
									streamingNestName)
							.setInnerSinksPortsAndSchemas(
									new ArrayList<>())
							.setInnerSourcesPortsAndSchemas(
									new ArrayList<>())
							.setWorkflowName(
									NEST_WORKFLOW_NAME)
							.setOperatorConnections(
									innerConnections)
							.setOperators(
									innerOperators);
					innerWorkflows.add(innerWorkflow);
					// create the input and output ports for the AW nest operator
					List<AWPort> inputPortsAndSchemas = new ArrayList<>();
					inputPortsAndSchemas.add(new AWPort().setName(NEST_INPUT_PORT_NAME)
							.setIsConnected(false)
							.setPortType(PortType.INPUT_PORT));
					inputPortsAndSchemas.add(new AWPort().setName("in")
							.setIsConnected(false)
							.setPortType(PortType.INPUT_PORT));
					List<AWPort> outputPortsAndSchemas = new ArrayList<>();
					outputPortsAndSchemas.add(new AWPort().setName("out")
							.setIsConnected(false)
							.setPortType(PortType.INPUT_PORT));
					// create the one parameter for the AW nest operator
					List<AWParameter> parameters = new ArrayList<>();
					parameters.add(new AWParameter().setKey(NEST_JOB_NAME_PARAM_KEY)
													.setValue(streamingNestName)
													.setDefaultValue("")
													.setRange(NEST_JOB_NAME_PARAM_RANGE)
													.setTypeClass(ParameterTypeString.class));
					parameters.add(new AWParameter().setKey(StreamingNest.PARAMETER_OPTIMIZATION_WORKFLOW_ID)
													.setValue(workflowId)
													.setDefaultValue("")
													.setRange(NEST_JOB_NAME_PARAM_RANGE)
													.setTypeClass(ParameterTypeString.class));
					// create the nest operator and add it to the operators list.
					AWOperator nest = new AWOperator().setName(streamingNestName)
							.setClassKey(NEST_CLASS_KEY)
							.setHasSubprocesses(true)
							.setIsEnabled(true)
							.setInnerWorkflows(innerWorkflows)
							.setInputPortsAndSchemas(inputPortsAndSchemas)
							.setNumberOfSubprocesses(1)
							.setOperatorClass(StreamingNest.class)
							.setOutputPortsAndSchemas(
									outputPortsAndSchemas)
							.setParameters(parameters)
							.setPlatformName(streamingNestName);
					operators.add(nest);
				}
			}
		}

		AgnosticWorkflow updatedWorkflow = new AgnosticWorkflow().setEnclosingOperatorName(
						workflow.getEnclosingOperatorName())
				.setInnerSinksPortsAndSchemas(
						workflow.getInnerSinksPortsAndSchemas())
				.setInnerSourcesPortsAndSchemas(
						workflow.getInnerSourcesPortsAndSchemas())
				.setWorkflowName(
						workflow.getWorkflowName())
				.setOperators(operators)
				.setOperatorConnections(
						new ArrayList<>());
		return new Pair<>(updatedWorkflow, splittedConnection);
	}

	private static void updateOperatorNames(AgnosticWorkflow workflow,
											String newEnclosingOperatorName) {
		workflow.setEnclosingOperatorName(newEnclosingOperatorName);
		List<AWOperatorConnection> connections = workflow.getOperatorConnections();
		for (AWOperator awOperator : workflow.getOperators()) {
			String oldName = awOperator.getName();
			String newName = oldName + OPERATOR_NAME_APPENDIX;
			awOperator.setName(newName);
			updateConnectionsWithNewName(oldName, newName, connections);
			if (awOperator.getHasSubprocesses()) {
				for (AgnosticWorkflow innerWorkflow : awOperator.getInnerWorkflows()) {
					updateOperatorNames(innerWorkflow, newName);
				}
			}
		}
	}

	private static void updateConnectionsWithNewName(String oldName, String newName,
													 List<AWOperatorConnection> connections) {
		for (AWOperatorConnection connection : connections) {
			if (connection.getFromOperator().equals(oldName)) {
				connection.setFromOperator(newName);
			}
			if (connection.getToOperator().equals(oldName)) {
				connection.setToOperator(newName);
			}
		}
	}

	public static String constructStreamingNestName(AWPlacementSite site,
													AWPlacementPlatform platform, String identifier) {
		return constructStreamingNestName(site.getSiteName(), platform.getPlatformName(), identifier);
	}

	public static String constructStreamingNestName(String siteName, String platformName, String identifier) {
		return "Streaming (" + siteName + "_" + platformName + ")_" + identifier;
	}

	private static Pair<AWOperator, Pair<List<AWOperatorConnection>, List<SplittedConnection>>> findOperatorAndConnections(
			String toBePlacedOp, AgnosticWorkflow workflow, List<String> placedOperators,
			String streamingNestName, String identifier) {
		// Loop over the operator in the current AgnosticWorkflow. Check if the toBePlacedOp is in
		// this workflow. Also collect the list of OperatorChains (operators with subprocesses) in
		// this workflow which can be searched for the toBePlacedOp.
		AWOperator toBePlacedAWOp = null;
		List<AWOperator> operatorChains = new ArrayList<>();
		for (AWOperator op : workflow.getOperators()) {
			if (op.getName().equals(toBePlacedOp)) {
				toBePlacedAWOp = op;
				break;
			}
			if (op.getHasSubprocesses()) {
				operatorChains.add(op);
			}
		}
		// If the toBePlacedOp is not in the current workflow, loop over the OperatorChains and
		// search for it in their inner workflows.
		if (toBePlacedAWOp == null) {
			for (AWOperator operatorChain : operatorChains) {
				for (AgnosticWorkflow innerWorkflow : operatorChain.getInnerWorkflows()) {
					Pair<AWOperator, Pair<List<AWOperatorConnection>, List<SplittedConnection>>> result =
							findOperatorAndConnections(
									toBePlacedOp, innerWorkflow, placedOperators, streamingNestName, identifier);
					// Return if the toBePlacedOp is found.
					if (result.getFirst() != null) {
						return result;
					}
				}
			}
		}
		// If the toBePlacedOp is not found, return a null/empty result:
		if (toBePlacedAWOp == null) {
			return new Pair<>(null, new Pair<>(new ArrayList<>(), new ArrayList<>()));
		}

		// toBePlacedOp is found, now check for the OperatorConnections in the current workflow
		List<AWOperatorConnection> innerConnections = new ArrayList<>();
		List<SplittedConnection> splittedConnection = new ArrayList<>();
		for (AWOperatorConnection conn : workflow.getOperatorConnections()) {
			if (conn.getFromOperator().equals(toBePlacedOp)) {
				if (placedOperators.contains(conn.getToOperator())) {
					// if the toOperator is also in this Streaming Nest operator (in the
					// placedOperator list) it is a inner Connection in this Streaming Nest
					// operator.
					innerConnections.add(conn);
				} else {
					// if the toOperator is not in the placedOperator list, we have to check where
					// the toOperator is placed and if the connection is a streaming connection
					// (between two streaming operators) or not.
					// Find the name of the Streaming Nest in which the toOperator is placed
					String toStreamingNestName = findPlacement(conn.getToOperator(), workflow, identifier);
					if (toStreamingNestName == null) {
						throw new IllegalArgumentException(
								"Could not found the placement of the operator: " + conn.getToOperator());
					}
					// Check if the connection is a streaming connection by checking the object
					// class of the corresponding AWPort.
					boolean streamingConnection = false;
					List<AWPort> ports;
					if (conn.getFromPortType() == null || conn.getFromPortType() == PortType.OUTPUT_PORT) {
						ports = toBePlacedAWOp.getOutputPortsAndSchemas();
					} else {
						ports = workflow.getInnerSourcesPortsAndSchemas();
					}
					for (AWPort port : ports) {
						if (port.getName().equals(conn.getFromPort())) {
							if (port.getObjectClass().equals(StreamDataContainer.class)) {
								streamingConnection = true;
							}
							break;
						}
					}
					// Add the information about the splitted connection to the SplittedConnection
					// list.
					splittedConnection.add(new SplittedConnection().setOriginalConnection(conn)
							.setFromStreamingNestName(
									streamingNestName)
							.setToStreamingNestName(
									toStreamingNestName)
							.setIsStreamingConnection(
									streamingConnection));
				}
			}
		}
		return new Pair<>(toBePlacedAWOp, new Pair<>(innerConnections, splittedConnection));
	}

	private static String findPlacement(String toOperatorName, AgnosticWorkflow workflow, String identifier) {
		for (AWPlacementSite site : workflow.getPlacementSites()) {
			for (AWPlacementPlatform platform : site.getAvailablePlatforms()) {
				for (AWPlacementOperator operator : platform.getOperators()) {
					if (toOperatorName.equals(operator.getName())) {
						return constructStreamingNestName(site, platform,identifier);
					}
				}
			}
		}
		return null;
	}

}
