/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer;/*
 * Copyright (C) 2016-2022 RapidMiner GmbH
 */

import static com.rapidminer.extension.streaming.PluginInitStreaming.getPluginLoader;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.avro.data.Json;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.math3.util.Pair;

import com.google.common.collect.Maps;
import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.connection.configuration.ConnectionConfiguration;
import com.rapidminer.extension.kafka_connector.operator.AbstractKafkaOperator;
import com.rapidminer.extension.streaming.connection.optimizer.OptimizerConnectionHandler;
import com.rapidminer.extension.streaming.deploy.management.DateTimeUtil;
import com.rapidminer.extension.streaming.deploy.management.api.Status;
import com.rapidminer.extension.streaming.deploy.management.api.Type;
import com.rapidminer.extension.streaming.deploy.management.db.Job;
import com.rapidminer.extension.streaming.deploy.management.db.ManagementDAO;
import com.rapidminer.extension.streaming.deploy.management.db.StreamingEndpoint;
import com.rapidminer.extension.streaming.deploy.management.db.Workflow;
import com.rapidminer.extension.streaming.deploy.management.db.infore.InforeOptimizerStreamingEndpoint;
import com.rapidminer.extension.streaming.operator.StreamKafkaSink;
import com.rapidminer.extension.streaming.operator.StreamKafkaSource;
import com.rapidminer.extension.streaming.operator.StreamingNest;
import com.rapidminer.extension.streaming.operator.StreamingOptimizationOperator;
import com.rapidminer.extension.streaming.optimizer.agnostic_workflow.AWOperator;
import com.rapidminer.extension.streaming.optimizer.agnostic_workflow.AWOperatorConnection;
import com.rapidminer.extension.streaming.optimizer.agnostic_workflow.AgnosticWorkflow;
import com.rapidminer.extension.streaming.optimizer.agnostic_workflow.AgnosticWorkflowConversion;
import com.rapidminer.extension.streaming.optimizer.agnostic_workflow.SplittedConnection;
import com.rapidminer.extension.streaming.optimizer.connection.OptimizerConnection;
import com.rapidminer.extension.streaming.optimizer.settings.Network;
import com.rapidminer.extension.streaming.optimizer.settings.OperatorDictionary;
import com.rapidminer.extension.streaming.optimizer.settings.OptimizationParameters;
import com.rapidminer.extension.streaming.optimizer.settings.OptimizerRequest;
import com.rapidminer.extension.streaming.optimizer.settings.OptimizerResponse;
import com.rapidminer.extension.streaming.utility.JsonUtil;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererController;
import com.rapidminer.gui.flow.processrendering.view.ProcessRendererView;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.IOMultiplier;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.ClassLoaderSwapper;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.OperatorService;
import com.rapidminer.tools.container.Triple;


/**
 * @author Fabian Temme
 * @since 0.6.1
 */
public class OptimizationHelper {

	/**
	 * work in progress, will be maybe deleted later
	 */
	public static final String PARAMETER_WRITE_AND_READ_FROM_JSON = "write_and_read_from_json";
	/**
	 * work in progress, will be maybe deleted later
	 */
	public static final String PARAMETER_NETWORK_JSON = "network_(write)";
	/**
	 * work in progress, will be maybe deleted later
	 */
	public static final String PARAMETER_DICTIONARY_JSON = "dictionary_(write)";
	/**
	 * work in progress, will be maybe deleted later
	 */
	public static final String PARAMETER_REQUEST_JSON = "request_(write)";
	/**
	 * work in progress, will be maybe deleted later
	 */
	public static final String PARAMETER_OUTPUT_WORKFLOW_JSON = "workflow_(write)";
	/**
	 * work in progress, will be maybe deleted later
	 */
	public static final String PARAMETER_OPTIMIZER_RESPONSE = "optimizer_response_(write)";
	/**
	 * work in progress, will be maybe deleted later
	 */
	public static final String PARAMETER_OUTPUT_OPTIMIZED_WORKFLOW_JSON = "optimized_workflow_(write)";
	/**
	 * work in progress, will be maybe deleted later
	 */
	public static final String PARAMETER_INPUT_OPTIMIZER_RESPONSE_JSON = "optimizer_response_(read)";

	/**
	 * Internal field to directly access the Logger.
	 */
	private static final Logger LOGGER = LogService.getRoot();

	/**
	 * This method retrieves the response of the INFORE Optimizer by utilizing the provided {@link
	 * StreamingOptimizationOperator} and {@code availableSites}.
	 * <p>
	 * The necessary configuration JSON files (see {@link Network}, {@link OperatorDictionary}, {@link
	 * AgnosticWorkflow}, {@link OptimizerRequest}) are created.
	 * <p>
	 * An {@link OptimizerConnection} is established and the configuration files pushed to it.
	 * <p>
	 * In addition a corresponding entry in the streaming management DB is created (with the optimizerConnection). The
	 * ID of this entry is returned as the second value of the return {@link Pair}.
	 * <p>
	 * The response JSON of the INFORE optimizer is retrieved and returned.
	 * <p>
	 * In case the parameter {@value #PARAMETER_WRITE_AND_READ_FROM_JSON} of the {@code operator} is {@code true}, the
	 * configuration files and the response of the Optimizer are also written to disc. In addition, the returned
	 * response JSON will be read from a different file, so that it can be adapted.
	 *
	 * @param operator
	 *    {@link StreamingOptimizationOperator} which is used to get the response of the INFORE optimizer
	 * @param availableSites
	 * 	Map between the name of a computing site and the available platforms ({@link
	 *    ConnectionInformationContainerIOObject}s at this site
	 * @param continuous
	 * @param numberOfPlans
	 * @return Triple with the response JSON of the optimizer, the id of the created workflow in the management DB
	 * and the id of the created job in the management DB.
	 */
	public static Triple<OptimizerResponse, String, String> getOptimizerResponse(StreamingOptimizationOperator operator,
																	  Map<String,
		List<ConnectionInformationContainerIOObject>> availableSites, boolean continuous, Long numberOfPlans) throws OperatorException,
		OperatorCreationException {
		// Create the necessary objects for the optimizer
		Network network =
			Network.create(operator.getParameterAsString(StreamingOptimizationOperator.PARAMETER_NETWORK_NAME),
				availableSites);
		OperatorDictionary dictionary = OperatorDictionary.createFromNetwork(
			operator.getParameterAsString(StreamingOptimizationOperator.PARAMETER_DICTIONARY_NAME), network, operator.getSubprocess(0).getEnabledOperators());
		AgnosticWorkflow workflow = AgnosticWorkflowConversion.processToAgnosticWorkflow(
			operator.getSubprocess(0)).setWorkflowName(operator.getParameterAsString(StreamingOptimizationOperator.PARAMETER_WORKFLOW_NAME));
		OptimizationParameters.OptimizerAlgorithm algorithm = OptimizationParameters.OptimizerAlgorithm.getMethod(
			operator.getParameterAsString(StreamingOptimizationOperator.PARAMETER_ALGORITHM));
		OptimizerRequest request = OptimizerRequest.create(network.getNetwork(),
			dictionary.getDictionaryName(),
			algorithm, workflow, continuous, numberOfPlans);

		long timeOutSessionConnect =
			operator.getParameterAsLong(StreamingOptimizationOperator.PARAMETER_CONNECT_TIME_OUT);
		// convert to milliseconds
		int pollingTimeOut =
			operator.getParameterAsInt(StreamingOptimizationOperator.PARAMETER_POLLING_TIME_OUT) * 1000;

		// Convert the objects for the optimizer to JSON strings
		String networkJSON = JsonUtil.toJson(network);
		String dictionaryJSON = JsonUtil.toJson(dictionary);
		String workflowJSON = JsonUtil.toJson(workflow);
		String requestJSON = JsonUtil.toJson(request);

		if (operator.getParameterAsBoolean(PARAMETER_WRITE_AND_READ_FROM_JSON)) {
			OptimizationHelper.writeConfigurationFiles(operator, networkJSON, requestJSON, dictionaryJSON,
				workflowJSON);
		}

		// Create the OptimizerConnection
		OptimizerConnection optimizerConnection = OptimizerConnectionHandler.createConnection(
			operator.getOptimizerSelector().getConnection(), operator);
		LOGGER.info("Optimizer connection created" + optimizerConnection.toString());

		// Create the entry in the streaming management db
		String workflowName =
			operator.getProcess().getProcessLocation().getShortName() + "-" + RandomStringUtils.randomAlphanumeric(5);

		Workflow managementWorkflow = new Workflow(
			UUID.randomUUID().toString(),
			workflowName,
			operator.getProcess().getProcessLocation().toString(),
			DateTimeUtil.getTimestamp(),
			Maps.newHashMap());

		ManagementDAO.addOrUpdate(managementWorkflow);
		// Start execution asynchronously and periodically check for stop
		CompletableFuture<String> future = supplyAsync(() -> optimizerConnection.sentOptimizationRequest(
			networkJSON,
			dictionaryJSON,
			requestJSON,
			timeOutSessionConnect),
			newSingleThreadExecutor());
		String optimizationJobId;
		InforeOptimizerStreamingEndpoint optimizerEndpoint;
		try {

			while (!future.isDone()) {
				operator.checkForStop();
				Thread.sleep(1000);
			}

			String requestId = future.get();

			optimizerEndpoint = createOptimizerEndpoint(requestId,operator.getName(),operator.optimizerSelector.getConnectionLocation().getAbsoluteLocation(),
				optimizerConnection,timeOutSessionConnect,pollingTimeOut,network);
			// Save job into "DB"
			Job optimizationJob = createOptimizationJob(
				managementWorkflow,
				requestId,
				optimizerEndpoint);
			ManagementDAO.addOrUpdate(managementWorkflow.getId(), optimizationJob);
			optimizationJobId = optimizationJob.getUniqueId();
		} catch (ProcessStoppedException pse) {
			LOGGER.warning("Process stopped for optimizer request execution: '" + workflowName + "'");
			future.cancel(true);
			throw pse;
		} catch (InterruptedException | ExecutionException ee) {
			LOGGER.warning("Error while executing optimizer request '" + workflowName + "': " + ee.getMessage());
			throw new UserError(operator, ee, "stream_connection.unsuccessful");
		}

		// Let the OptimizerConnection perform the actual optimization. It will send the json
		// strings to the optimizer service, receive back the response, correctly
		// format it and return it.
		OptimizerResponse optimizerResponse;
		try {
			optimizerResponse = optimizerEndpoint.waitForNewPlanForDeployment(pollingTimeOut);
		} catch (InterruptedException e) {
			throw new OperatorException(
				"Exception during optimizer communication: " + e.getLocalizedMessage());
		}
		if (optimizerResponse == null) {
			throw new OperatorException(
				"The optimizer did not returned an optimized workflow in time. Please increase the polling time out");
		}

		// If writeAndReadFromJSON is true, write the response of the optimizer to disk. And read in
		// another json file which is used as the optimizer response. If you want to manually
		// test changed responses of the optimizer, you can copy the written file, change
		// entries to test the placement of the operators and store it as the used input optimizer
		// response.
		if (operator.getParameterAsBoolean(PARAMETER_WRITE_AND_READ_FROM_JSON)) {
			optimizerResponse = OptimizationHelper.writeResponseToFileAndUpdateWithResponseFromFile(operator,
				optimizerResponse);
		}
		return new Triple<>(optimizerResponse, managementWorkflow.getId(), optimizationJobId);
	}

	private static void writeConfigurationFiles(Operator operator, String networkJSON, String requestJSON,
												String dictionaryJSON, String workflowJSON) throws UserError {
		try (FileWriter networkWriter = new FileWriter(
			operator.getParameterAsFile(PARAMETER_NETWORK_JSON));
			 FileWriter requestWriter = new FileWriter(
				 operator.getParameterAsFile(PARAMETER_REQUEST_JSON));
			 FileWriter dictionaryWriter = new FileWriter(
				 operator.getParameterAsFile(PARAMETER_DICTIONARY_JSON));
			 FileWriter workflowWriter = new FileWriter(
				 operator.getParameterAsFile(PARAMETER_OUTPUT_WORKFLOW_JSON))) {
			networkWriter.write(networkJSON);
			requestWriter.write(requestJSON);
			dictionaryWriter.write(dictionaryJSON);
			workflowWriter.write(workflowJSON);
		} catch (IOException e) {
			LOGGER.warning("Exception!: " + e.getMessage());
		}
	}

	private static OptimizerResponse writeResponseToFileAndUpdateWithResponseFromFile(Operator operator,
																		   OptimizerResponse optimizerResponseJSON) throws UserError {
		try (FileWriter optimizerResponseWriter = new FileWriter(
			operator.getParameterAsFile(PARAMETER_OPTIMIZER_RESPONSE))) {
			optimizerResponseWriter.write(JsonUtil.toJson(optimizerResponseJSON));
		} catch (IOException e) {
			LOGGER.warning("Exception!: " + e.getMessage());
		}
		try (BufferedReader inputOptimizerResponseReader = new BufferedReader(
			new FileReader(operator.getParameterAsFile(PARAMETER_INPUT_OPTIMIZER_RESPONSE_JSON)))) {
			optimizerResponseJSON = JsonUtil.fromString(inputOptimizerResponseReader.lines()
				.collect(Collectors.joining( " ")),OptimizerResponse.class);
		} catch (IOException e) {
			LOGGER.warning("Exception!: " + e.getMessage());
		}
		return optimizerResponseJSON;
	}

	/**
	 * Updates the inner subprocess of the {@link StreamingOptimizationOperator} with the information in the provided
	 * {@code optimizerResponse}.
	 * <p>
	 * Then the inner subprocess is updated by calling
	 * {@link AgnosticWorkflowConversion#agnosticWorkflowToProcess(OperatorChain,
	 * AgnosticWorkflow, int)}.
	 *
	 * @param optimizerResponse
	 *    {@link AgnosticWorkflow} used to update the inner subprocess
	 * @param availableSites
	 * @return inner subprocess of the {@link StreamingOptimizationOperator} updated with the information in the
	 * provided {@link AgnosticWorkflow}.
	 */
	public static void updateSubprocess(StreamingOptimizationOperator optimizationOperator,
										OptimizerResponse optimizerResponse,
										String managementWorkflowID,
										Map<String, Map<String, OutputPort>> availableSites) throws
		OperatorCreationException, IOException {
		try (ClassLoaderSwapper cls = ClassLoaderSwapper.withContextClassLoader(
			getPluginLoader())) {
			// Convert the optimizer response back to an AgnosticWorkflow object.
			AgnosticWorkflow optimizerResponseAW = optimizerResponse.getWorkflow();

			// We use the timestamp of now() as the identifier in the Streaming Nest names and job
			String identifier = ZonedDateTime.now(ZoneId.of("UTC")).toString();
			// Perform an update of the AgnosticWorkflow, which actually creates the Streaming Nest
			// operators and place all operators in the corresponding nests. Also the operator
			// connections which are inside one nest are placed in the corresponding nest. All other
			// connections (which are splitted between two nests) are returned in the second value of
			// the updateAWPair.
			Pair<AgnosticWorkflow, List<SplittedConnection>> updateAWPair =
				AgnosticWorkflowConversion.updateOptimizedWorkflow(
					optimizerResponseAW, identifier, managementWorkflowID);
			AgnosticWorkflow optimizedWorkflow = updateAWPair.getFirst();

			ExecutionUnit subprocess = optimizationOperator.getSubprocess(1);
			for (OutputPort outputPort: subprocess.getInnerSources().getAllPorts()){
				if (outputPort.isConnected()){
					outputPort.disconnect();
				}
			}
			for (Operator operator : subprocess.getAllInnerOperators()) {
				operator.remove();
			}

			subprocess =
				AgnosticWorkflowConversion.agnosticWorkflowToProcess(optimizationOperator, optimizedWorkflow, 1);
			// Create the inner streaming backend connection ports and connect them to the corresponding
			// Streaming Nests.
			connectStreamingBackends(optimizedWorkflow, subprocess, availableSites, identifier);

			// Update the splitted connections, by adding Kafka Sink and Kafka Source operators (using
			// the KafkaConnection from the kafkaOutputPort) for splitted streaming connections, and
			// wire other connections through the throughput ports of the Streaming Nest operators.
			updateSplittedConnections(optimizationOperator, subprocess, updateAWPair.getSecond());
		}

		optimizationOperator.fireUpdatePublic();
	}

	/**
	 * Updates the provided {@code subprocess} with the provided list of {@link SplittedConnection}.
	 * <p>
	 * The methods loops through the {@link SplittedConnection}s. If the current connection is a streaming
	 * connections a
	 * pair of {@link StreamKafkaSink} and {@link StreamKafkaSource} operators is created, which recreates the splitted
	 * connections for the workflow. The kafka connection provided at the {@code kafkaInputPort} (and through
	 * putted to
	 * the {@code kafkaOutputPort}) is used for the Kafka Sink and Source operators. A corresponding {@link
	 * IOMultiplier} operator is created as well to provide the kafka connection to all new sink and source operators.
	 * <p>
	 * If the connection is not a streaming connection, it is restored by wiring the connection through the throughput
	 * ports of the corresponding {@link StreamingNest} operators.
	 *
	 * @param subprocess
	 *    {@link ExecutionUnit} for which the splitted connections shall be updated
	 * @param connections
	 *    {@link SplittedConnection}s which shall be restored
	 * @return subprocess with restored splitted connections
	 */
	private static ExecutionUnit updateSplittedConnections(StreamingOptimizationOperator optimizationOperator,
														   ExecutionUnit subprocess,
														   List<SplittedConnection> connections) throws OperatorCreationException {
		// check if we need to create a Multiply operator, cause we have splitted streaming
		// connections.
		OutputPort kafkaOutputPort = optimizationOperator.getKafkaOutputPort();
		int multiplyPortIndex = 0;
		IOMultiplier multiply = null;
		for (SplittedConnection connection : connections) {
			if (connection.isStreamingConnection()) {
				// if this multiply is still null, create it
				if (multiply == null) {
					multiply = OperatorService.createOperator(IOMultiplier.class);
					subprocess.addOperator(multiply, 0);
					// if the kafkaOutputPort was connected, connect the first outputPort of
					// multiply to the original target of the kafkaOutputPort
					if (kafkaOutputPort.isConnected()) {
						InputPort target = kafkaOutputPort.getDestination();
						multiply.getOutputPorts()
							.getPortByIndex(multiplyPortIndex)
							.connectTo(target);
						multiplyPortIndex++;
					}
					// Connect the kafkaOutputPort to the multiply input port
					kafkaOutputPort.connectTo(multiply.getInputPort());
					// fireUpdate to ensure that the port extender of the multiply creates a new
					// outputport
					optimizationOperator.fireUpdatePublic(multiply);
				}
				multiplyPortIndex = addKafkaOperator(optimizationOperator, subprocess, multiply, true, connection,
					multiplyPortIndex);
				optimizationOperator.fireUpdatePublic(multiply);
				multiplyPortIndex = addKafkaOperator(optimizationOperator, subprocess, multiply, false, connection,
					multiplyPortIndex);
				optimizationOperator.fireUpdatePublic(multiply);
			} else {
				StreamingNest fromNest = (StreamingNest) subprocess.getOperatorByName(
					connection.getFromStreamingNestName());
				StreamingNest toNest = (StreamingNest) subprocess.getOperatorByName(
					connection.getToStreamingNestName());
				OutputPort fromPort = fromNest.getSubprocess(0)
					.getOperatorByName(connection.getOriginalConnection()
						.getFromOperator())
					.getOutputPorts()
					.getPortByName(connection.getOriginalConnection()
						.getFromPort());
				InputPort toPort = toNest.getSubprocess(0)
					.getOperatorByName(
						connection.getOriginalConnection().getToOperator())
					.getInputPorts()
					.getPortByName(
						connection.getOriginalConnection().getToPort());
				int numberOfPortPairsFrom = fromNest.getOutputPortPairExtender()
					.getManagedPairs()
					.size();
				int numberOfPortPairsTo = toNest.getInputPortPairExtender()
					.getManagedPairs()
					.size();
				//
				fromPort.connectTo(fromNest.getSubprocess(0)
					.getInnerSinks()
					.getPortByIndex(numberOfPortPairsFrom - 1));
				fromNest.getOutputPorts()
					.getPortByIndex(numberOfPortPairsFrom - 1)
					.connectTo(toNest.getInputPorts().getPortByIndex(numberOfPortPairsTo));
				toNest.getSubprocess(0)
					.getInnerSources()
					.getPortByIndex(numberOfPortPairsTo - 1)
					.connectTo(toPort);
				optimizationOperator.fireUpdatePublic(toNest);
				optimizationOperator.fireUpdatePublic(fromNest);
			}
		}
		return subprocess;
	}

	private static void connectStreamingBackends(AgnosticWorkflow agnosticWorkflow,
												 ExecutionUnit subprocess,
												 Map<String, Map<String, OutputPort>> availableSites,
												 String identifier) {
		Map<String, InputPort> map = new LinkedHashMap<>();
		for (AWOperator operator : agnosticWorkflow.getOperators()) {
			if (operator.getOperatorClass().equals(StreamingNest.class)) {
				String platformName = operator.getPlatformName();
				Operator op = subprocess.getOperatorByName(operator.getName());
				map.put(platformName, op.getInputPorts().getPortByName("connection"));
			}
		}
		for (Map.Entry<String, Map<String, OutputPort>> entry : availableSites.entrySet()) {
			String siteName = entry.getKey();
			for (Map.Entry<String, OutputPort> portEntry : entry.getValue().entrySet()) {
				String platformName = portEntry.getKey();
				OutputPort port = portEntry.getValue();
				port.connectTo(
					map.get(AgnosticWorkflowConversion.constructStreamingNestName(siteName,
						platformName, identifier)));
			}
		}
	}

	private static int addKafkaOperator(StreamingOptimizationOperator optimizationOperator, ExecutionUnit subprocess,
										IOMultiplier multiply,
										boolean kafkaSink,
										SplittedConnection connection, int multiplyPortIndex) throws OperatorCreationException {
		String nestName;
		Operator kafkaOperator;
		String targetOperatorName;
		String targetPortName;
		AWOperatorConnection originalConnection = connection.getOriginalConnection();
		String topicName =
			originalConnection.getFromOperator() + "_" + originalConnection.getFromPort() + "_to_" + originalConnection
				.getToOperator() + "_" + originalConnection.getToPort();
		topicName = AbstractKafkaOperator.makeTopicNameValid(topicName);
		topicName = AbstractKafkaOperator.makeTopicNameNonColliding(topicName);
		if (kafkaSink) {
			// A StreamKafkaSink operator has to be placed in the fromStreamingNest. The
			// targetOperator (the fromOperator) has to be connected to the kafkaSink.
			nestName = connection.getFromStreamingNestName();
			kafkaOperator = OperatorService.createOperator(StreamKafkaSink.class);
			kafkaOperator.setParameter(StreamKafkaSink.PARAMETER_TOPIC, topicName);
			targetOperatorName = originalConnection.getFromOperator();
			targetPortName = originalConnection.getFromPort();
		} else {
			// A StreamKafkaSource operator has to be placed in the toStreaming. The kafkaSource has
			// to be connected to the targetOperator (the toOperator).
			nestName = connection.getToStreamingNestName();
			kafkaOperator = OperatorService.createOperator(StreamKafkaSource.class);
			kafkaOperator.setParameter(StreamKafkaSource.PARAMETER_TOPIC, topicName);
			targetOperatorName = originalConnection.getToOperator();
			targetPortName = originalConnection.getToPort();
		}
		// get the streaming nest operator from the subprocess
		StreamingNest nest = (StreamingNest) subprocess.getOperatorByName(nestName);
		// get the number of port pairs of the streaming nest
		int numberOfPortPairs = nest.getInputPortPairExtender().getManagedPairs().size();
		// Connect the next outputPort of the multiply to the last input port of the port
		// extender (numberOfPortPairs + 1 (for the 'connection' port of the nest) - 1
		// (cause we need an index))
		multiply.getOutputPorts()
			.getPortByIndex(multiplyPortIndex)
			.connectTo(nest.getInputPorts().getPortByIndex(numberOfPortPairs));
		multiplyPortIndex++;
		// Get the nest subprocess
		ExecutionUnit nestSubprocess = nest.getSubprocess(0);
		// Add the kafkaOperator to the nest subprocess
		nestSubprocess.addOperator(kafkaOperator);
		// connect the inner throughput port to the kafkaOperator connection port
		nestSubprocess.getInnerSources()
			.getPortByIndex(numberOfPortPairs - 1)
			.connectTo(kafkaOperator.getInputPorts().getPortByIndex(0));
		// Get the target operator
		Operator targetOperator = nestSubprocess.getOperatorByName(targetOperatorName);
		// Connect targetOperator and kafkaOperator
		if (kafkaSink) {
			targetOperator.getOutputPorts()
				.getPortByName(targetPortName)
				.connectTo(kafkaOperator.getInputPorts().getPortByIndex(1));
		} else {
			kafkaOperator.getOutputPorts()
				.getPortByIndex(1)
				.connectTo(targetOperator.getInputPorts().getPortByName(targetPortName));
		}
		optimizationOperator.fireUpdatePublic(nest);
		return multiplyPortIndex;
	}

	public static void updateProcessPanel(Operator operator) {
		if (RapidMinerGUI.getMainFrame() != null) {
			// arrange all operators
			ProcessPanel processPanel = RapidMinerGUI.getMainFrame().getProcessPanel();
			ProcessRendererView view = processPanel.getProcessRenderer();
			ProcessRendererController controller = new ProcessRendererController(view, view.getModel());
			// without the next line, the operators won't have positions yet and arrange & annotations
			// will fail...
			controller.autoFit();

			// we collect all subprocesses here - it turns out that we can actually arrange operators in
			// subprocesses, even if they are currently not visible...
			List<ExecutionUnit> allSubprocesses = new ArrayList<>();
			addSubprocesses(allSubprocesses, operator);
			controller.autoArrange(allSubprocesses);
		}
	}

	/**
	 * Adds all execution units of the current operator (if it is a chain) to the list. Invokes the method recursively
	 * for all children.
	 *
	 * @param allSubprocesses
	 * 	the list to add the execution units to
	 * @param operator
	 * 	the operator
	 */
	private static void addSubprocesses(List<ExecutionUnit> allSubprocesses, Operator operator) {
		if (operator instanceof OperatorChain) {
			OperatorChain operatorChain = (OperatorChain) operator;
			for (int i = 0; i < operatorChain.getNumberOfSubprocesses(); i++) {
				allSubprocesses.add(operatorChain.getSubprocess(i));
			}
			for (Operator innerOperator : operatorChain.getAllInnerOperators()) {
				addSubprocesses(allSubprocesses, innerOperator);
			}
		}
	}

	public static String getPlatformNameFromConnection(ConnectionInformationContainerIOObject connection){
		ConnectionConfiguration connConfig = connection.getConnectionInformation()
			.getConfiguration();
		return connConfig.getType().replace("streaming:", "");
	}

	private static InforeOptimizerStreamingEndpoint createOptimizerEndpoint(String requestId, String operatorName,
																			String connectionLocation,
																			OptimizerConnection optimizerConnection,
																			long timeOutSessionConnect,
																			int pollingTimeOut, Network network) {
		return new InforeOptimizerStreamingEndpoint(connectionLocation, optimizerConnection,
			requestId, timeOutSessionConnect, pollingTimeOut, operatorName, network);
	}

	private static Job createOptimizationJob(Workflow workflow, String requestId, StreamingEndpoint endpoint) {
		// Get job-ID and save job to "DB"
		String workflowId = workflow.getId();
		String uniqueId = UUID.randomUUID().toString();
		String name = "Optimization Request (id:'" + requestId + "')";

		return new Job(workflowId, uniqueId, requestId, endpoint, name, Type.InforeOptimizer, true,
			Status.DeployingNewPlan, null);
	}

	public static List<ParameterType> addWriteAndReadParams(Operator operator) {
		List<ParameterType> params = new ArrayList<>();

		params.add(new ParameterTypeBoolean(PARAMETER_WRITE_AND_READ_FROM_JSON, "", false, true));

		BooleanParameterCondition writeReadJsonCondition = new BooleanParameterCondition(operator,
			PARAMETER_WRITE_AND_READ_FROM_JSON,
			true,
			true);

		ParameterType type = new ParameterTypeFile(PARAMETER_NETWORK_JSON, "", "json", false);
		type.registerDependencyCondition(writeReadJsonCondition);
		params.add(type);

		type = new ParameterTypeFile(PARAMETER_REQUEST_JSON, "", "json", false);
		type.registerDependencyCondition(writeReadJsonCondition);
		params.add(type);

		type = new ParameterTypeFile(PARAMETER_DICTIONARY_JSON, "", "json", false);
		type.registerDependencyCondition(writeReadJsonCondition);
		params.add(type);

		type = new ParameterTypeFile(PARAMETER_OUTPUT_WORKFLOW_JSON, "", "json", false);
		type.registerDependencyCondition(writeReadJsonCondition);
		params.add(type);

		type = new ParameterTypeFile(PARAMETER_OPTIMIZER_RESPONSE, "", "json", false);
		type.registerDependencyCondition(writeReadJsonCondition);
		params.add(type);

		type = new ParameterTypeFile(PARAMETER_INPUT_OPTIMIZER_RESPONSE_JSON, "", "json", false);
		type.registerDependencyCondition(writeReadJsonCondition);
		params.add(type);

		type = new ParameterTypeFile(PARAMETER_OUTPUT_OPTIMIZED_WORKFLOW_JSON, "", "json", false);
		type.registerDependencyCondition(writeReadJsonCondition);
		params.add(type);

		return params;
	}
}
