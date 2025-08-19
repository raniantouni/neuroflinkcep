/**
 * RapidMiner Streaming Extension
 * <p>
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator;

import static com.rapidminer.connection.util.ConnectionI18N.getTypeName;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.rapidminer.extension.streaming.optimizer.agnostic_workflow.*;
import com.rapidminer.tools.LogService;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;

import com.rapidminer.connection.ConnectionInformationContainerIOObject;
import com.rapidminer.connection.util.ConnectionInformationSelector;
import com.rapidminer.extension.kafka_connector.connections.KafkaConnectionHandler;
import com.rapidminer.extension.streaming.connection.StreamConnectionHandler;
import com.rapidminer.extension.streaming.connection.StreamingConnectionHelper;
import com.rapidminer.extension.streaming.connection.optimizer.OptimizerConnectionHandler;
import com.rapidminer.extension.streaming.deploy.management.api.Status;
import com.rapidminer.extension.streaming.deploy.management.db.ManagementDAO;
import com.rapidminer.extension.streaming.optimizer.OptimizationHelper;
import com.rapidminer.extension.streaming.optimizer.settings.Network;
import com.rapidminer.extension.streaming.optimizer.settings.OperationMode;
import com.rapidminer.extension.streaming.optimizer.settings.OperatorDictionary;
import com.rapidminer.extension.streaming.optimizer.settings.OptimizationParameters.OptimizerAlgorithm;
import com.rapidminer.extension.streaming.optimizer.settings.OptimizerPlatform;
import com.rapidminer.extension.streaming.optimizer.settings.OptimizerRequest;
import com.rapidminer.extension.streaming.optimizer.settings.OptimizerResponse;
import com.rapidminer.extension.streaming.optimizer.settings.OptimizerSite;
import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.PortUserError;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.InputPortExtender;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.SubprocessTransformRule;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeEnumeration;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeLong;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.container.Triple;
import org.apache.flink.api.java.tuple.Tuple2;


/**
 * This {@link OperatorChain} provides the possibility to design a logical streaming workflow in it's subprocess and
 * only provide a collection of connections to streaming platforms on which the workflow can be deployed. The INFORE
 * optimizer is then used to decide on an optimized placement of the operators in the workflow at the different
 * platforms.
 * <p>
 * Upon execution ({@link #doWork()} of the operator, the designed workflow (in the subprocess of the operator) is
 * converted into its {@link AgnosticWorkflow} representation. In addition all configurations for the INFORE optimizer
 * are created as well ({@link Network}, {@link OperatorDictionary}, {@link OptimizerRequest}). This is provided to the
 * INFORE optimizer, which performs an optimization of the workflow based on the provided information.
 * <p>
 * The received optimized workflow is then used to update the inner subprocess (create the corresponding Streaming Nest
 * operators, place the operators inside the Nests according to the optimization, restore splitted connections).
 * <p>
 * When the inner subprocess is updated it is executed, which causes the deployment of the streaming workflows on the
 * different platforms. The optimized workflow can also be inspected in the RapidMiner GUI.
 * <p>
 * The connection information to the Optimizer Service have to be provided. The operator also provides the option to
 * write the different configurations ({@link AgnosticWorkflow}, {@link Network}, {@link OperatorDictionary}, {@link
 * OptimizerRequest}) and the response of the optimizer to disk. If this option is selected, the input for the update of
 * the inner subprocess (which is normally the response of the INFORE optimizer) is also read from disk and the updated
 * {@link AgnosticWorkflow} is written to disk as well. All file locations can be controlled by corresponding
 * parameters. This allows for easy manipulation of the interaction with the optimizer, to test things.
 * <p>
 * The operator also provides the option to perform a dummy optimization without an actual execution of the optimized
 * workflow.
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
public class StreamingOptimizationOperator extends OperatorChain {


    /**
     * This input port is used to provide the connection to the Kafka cluster which is used to communicate between the
     * different computing sites and platforms.
     */
    private final InputPort kafkaInputPort = getInputPorts().createPort("kafka connection");

    private static final Logger LOGGER = LogService.getRoot();

    public final ConnectionInformationSelector optimizerSelector = StreamingConnectionHelper.createSelector(
            this, "optimizer connection", OptimizerConnectionHandler.getINSTANCE().getType());

    /**
     * This input port extender is used to provide the collection of available streaming backends on which the workflow
     * can be deployed.
     */
    private final InputPortExtender streamingBackendsInputPort = new InputPortExtender(
            "streaming site", getInputPorts());

    /**
     * This output port is used to provide the kafka connection received at the {@link #kafkaInputPort} to the inner
     * subprocess.
     */
    private final OutputPort kafkaOutputPort = getSubprocess(1).getInnerSources()
            .createPort("kafka connection");

    public static final String PARAMETER_ALGORITHM = "optimizer_algorithm";
    public static final String PARAMETER_CONTINUOUS_OPTIMIZATION = "continuous_optimization";
    public static final String PARAMETER_NUMBER_OF_PLANS = "number_of_plans";
    public static final String PARAMETER_NETWORK_NAME = "network_name";
    public static final String PARAMETER_DICTIONARY_NAME = "dictionary_name";
    public static final String PARAMETER_WORKFLOW_NAME = "workflow_name";
    public static final String PARAMETER_STREAMING_SITES_NAMES = "streaming_sites_names";
    public static final String PARAMETER_CONNECT_TIME_OUT = "connect_time_out";
    public static final String PARAMETER_POLLING_TIME_OUT = "polling_time_out";

    public static final String PARAMETER_OPERATION_MODE = "operation_mode";
    public static final String PARAMETER_CONNECTED_PLATFORMS = "connected_platforms";

    private Map<String, Map<String, OutputPort>> availableSites = new LinkedHashMap<>();

    private boolean innerSinkConnectionInitialized = false;

    private static Map<String, StreamConnectionHandler> supportedConnections =
            StreamingConnectionHelper.createConnectionHandlerMap();

    public StreamingOptimizationOperator(OperatorDescription description) {
        super(description, "Logical Workflow", "Optimized Workflow");
        streamingBackendsInputPort.start();

        getTransformer().addRule(() -> {
            // if the kafkaOutputPort was created, we can through put the meta data of the #kafkaInputPort
            if (kafkaOutputPort != null) {
                MetaData md = kafkaInputPort.getMetaData();
                if (md != null) {
                    md = md.clone();
                    md.addToHistory(kafkaOutputPort);
                    kafkaOutputPort.deliverMD(md);
                }
            }
        });
        getTransformer().addRule(() -> {
            try {
                initializeStreamingBackendPorts();
            } catch (OperatorException e) {
                e.printStackTrace();
            }
        });
        getTransformer().addRule(new SubprocessTransformRule(getSubprocess(0)));
        getTransformer().addRule(new SubprocessTransformRule(getSubprocess(1)));
    }

    private void initializeStreamingBackendPorts() throws OperatorException {
        List<String> siteParameterValues = ParameterTypeEnumeration.transformString2List(
                getParameterAsString(PARAMETER_STREAMING_SITES_NAMES));
        Map<String, String> connectedPlatforms = new LinkedHashMap<>();
        ParameterTypeList.transformString2List(getParameterAsString(PARAMETER_CONNECTED_PLATFORMS))
                .forEach(keyValue -> connectedPlatforms.put(keyValue[0], keyValue[1]));
        if (siteParameterValues.isEmpty()) {
            // Need to convert it into an empty arraylist, cause ParameterTypeEnumeration.transformString2List()
            // returns an instance of EmptyList to which no entries can be added.
            siteParameterValues = new ArrayList<>();
        }
        for (int i = 0; i < streamingBackendsInputPort.getManagedPorts().size() - 1; i++) {
            Pair<String, List<String>> computingSiteNames = getComputingSiteNames(i, siteParameterValues, null);
            String siteName = computingSiteNames.getFirst();
            List<String> platformNames = computingSiteNames.getSecond();
            updateInnerPorts(siteName, platformNames, connectedPlatforms);
        }
    }

    private void updateInnerPorts(String siteName, List<String> platformNames, Map<String, String> connectedPlatforms) throws UndefinedParameterError {
        Map<String, OutputPort> currentPlatforms = availableSites.containsKey(siteName) ?
                availableSites.get(siteName) : new LinkedHashMap<>();
        for (String platformName : platformNames) {
            if (!currentPlatforms.containsKey(platformName)) {
                String portName = siteName + "_" + platformName;
                // Create inner connection port
                OutputPort outputPort = getSubprocess(1).getInnerSources().getPortByName(portName);
                if (outputPort == null) {
                    outputPort = getSubprocess(1).getInnerSources()
                            .createPort(portName);
                }
                currentPlatforms.put(platformName, outputPort);
                if (!outputPort.isConnected() && connectedPlatforms.containsKey(portName)) {
                    String targetOperatorName = connectedPlatforms.get(portName);
                    if (targetOperatorName != null) {
                        Operator targetOperator = getSubprocess(1).getOperatorByName(targetOperatorName);
                        if (targetOperator instanceof StreamingNest) {
                            outputPort.connectTo(targetOperator.getInputPorts().getPortByName("connection"));
                        }
                    }
                }
            }
        }
        availableSites.put(siteName, currentPlatforms);
    }

    public void initializedInnerPortsFromNetwork(Network network) {
        for (OptimizerSite site : network.getSites()) {
            String siteName = site.getSiteName();
            Map<String, OutputPort> currentPlatforms = availableSites.containsKey(siteName) ?
                    availableSites.get(siteName) : new LinkedHashMap<>();
            for (OptimizerPlatform platform : site.getAvailablePlatforms()) {
                String platformName = platform.getPlatformName();
                String portName = siteName + "_" + platformName;
                // Create inner connection port
                OutputPort outputPort = getSubprocess(1).getInnerSources().getPortByName(portName);
                if (outputPort == null) {
                    outputPort = getSubprocess(1).getInnerSources()
                            .createPort(portName);
                }
                currentPlatforms.put(platformName, outputPort);
            }
            availableSites.put(siteName, currentPlatforms);
        }
    }

    private Pair<String, List<String>> getComputingSiteNames(int i, List<String> siteParameterValues, List<ConnectionInformationContainerIOObject> connections) throws OperatorException {
        String siteName;
        List<String> platformNames = new ArrayList<>();
        // Use the connections list to add platformNames
        if (connections != null) {
            for (ConnectionInformationContainerIOObject connection : connections) {
                platformNames.add(OptimizationHelper.getPlatformNameFromConnection(connection));
            }
        }
        if (i < siteParameterValues.size()) {
            // use site and platform names from the parameters, if there are entries
            String[] paramValue = ParameterTypeTupel.transformString2Tupel(siteParameterValues.remove(i));
            siteName = paramValue[0];
            List<String> additionalPlatformNames = ParameterTypeEnumeration.transformString2List(paramValue[1]);
            if (additionalPlatformNames.size() > platformNames.size()) {
                for (int j = platformNames.size(); j < additionalPlatformNames.size(); j++) {
                    platformNames.add(additionalPlatformNames.get(j));
                }
            }
        } else {
            // otherwise create a default site name
            siteName = "site" + i;
        }
        // Create the tupel string array for the parameter
        String[] paramValue = new String[]{siteName,
                ParameterTypeEnumeration.transformEnumeration2String(platformNames)};
        // Add the updated entries back to the siteParameterValues list
        if (i < siteParameterValues.size()) {
            siteParameterValues.add(i, ParameterTypeTupel.transformTupel2String(paramValue));
        } else {
            siteParameterValues.add(ParameterTypeTupel.transformTupel2String(paramValue));
        }
        return new Pair<>(siteName, platformNames);
    }

    @Override
    public void doWork() throws OperatorException {
        // Get the operation mode and the kafka connection object
        OperationMode operationMode = OperationMode.getMode(getParameterAsString(PARAMETER_OPERATION_MODE));
        ConnectionInformationContainerIOObject kafkaConnection = loadKafkaConnection();
        Map<String, List<ConnectionInformationContainerIOObject>> availableConnections = loadAvailableConnections();
        // perform the optimization if we not only want to deploy the optimized workflow
        Pair<String, String> optimizerDBIds = null;
        if (!operationMode.equals(OperationMode.OnlyDeploy)) {
            cleanOptimizedSubprocess();
            try {
                optimizerDBIds = performOptimization(availableConnections);
            } catch (IOException | OperatorCreationException e) {
                throw new OperatorException(e.getLocalizedMessage());
            }
        }
        updateConnectedPlatformParam();
        // Deploy the optimized subprocess if we not only want to perform the optimization
        if (!operationMode.equals(OperationMode.OnlyOptimize)) {
            deployOptimizedSubprocess(kafkaConnection, availableConnections, optimizerDBIds);
        }
    }

    private void updateConnectedPlatformParam() throws UndefinedParameterError {
        Map<String, String> currentParamValue = new LinkedHashMap<>();
//		ParameterTypeList.transformString2List(getParameterAsString(PARAMETER_CONNECTED_PLATFORMS))
//			.forEach(keyValue -> currentParamValue.put(keyValue[0], keyValue[1]));

        for (Entry<String, Map<String, OutputPort>> entry : availableSites.entrySet()) {
            for (Entry<String, OutputPort> portEntry : entry.getValue().entrySet()) {
                OutputPort port = portEntry.getValue();
                String streamingNestName = null;
                if (port.isConnected()) {
                    streamingNestName = port.getOpposite().getPorts().getOwner().getName();
                }
                currentParamValue.put(port.getName(), streamingNestName);
            }
        }

        List<String[]> newParamValue = new ArrayList<>();
        currentParamValue.forEach((key, value) -> newParamValue.add(new String[]{key, value}));
        setParameter(PARAMETER_CONNECTED_PLATFORMS, ParameterTypeList.transformList2String(newParamValue));
    }

    private ConnectionInformationContainerIOObject loadKafkaConnection() throws UserError {
        ConnectionInformationContainerIOObject kafkaConnection = kafkaInputPort.getData(
                ConnectionInformationContainerIOObject.class);
        // throw a UserError, if this is not a kafka connection
        if (!kafkaConnection.getConnectionInformation()
                .getConfiguration()
                .getType()
                .equals(KafkaConnectionHandler.getINSTANCE().getType())) {
            throw new PortUserError(kafkaInputPort, "connection.mismatched_type",
                    getTypeName(KafkaConnectionHandler.getINSTANCE().getType()),
                    getTypeName(kafkaConnection.getConnectionInformation()
                            .getConfiguration()
                            .getType()));
        }
        return kafkaConnection;
    }

    private void cleanOptimizedSubprocess() {
//		// Clearing innerConnectionPorts if they already exists.
//		for (OutputPort outputPort : innerConnectionPorts.keySet()) {
//			getSubprocess(1).getInnerSources().removePort(outputPort);
//		}
//		innerConnectionPorts.clear();
        for (Operator operator : getSubprocess(1).getAllInnerOperators()) {
            operator.remove();
        }
    }

    private Map<String, List<ConnectionInformationContainerIOObject>> loadAvailableConnections() throws OperatorException {
        Map<String, List<ConnectionInformationContainerIOObject>> availableConnections = new LinkedHashMap<>();
        // Retrieve the collection of streaming backends for the optimization
        List<IOObject> streamingSites = streamingBackendsInputPort.getData(IOObject.class,
                false);
        Map<String, String> connectedPlatforms = new LinkedHashMap<>();
        ParameterTypeList.transformString2List(getParameterAsString(PARAMETER_CONNECTED_PLATFORMS))
                .forEach(keyValue -> connectedPlatforms.put(keyValue[0], keyValue[1]));
        List<String> siteParameterValues = ParameterTypeEnumeration.transformString2List(
                getParameterAsString(PARAMETER_STREAMING_SITES_NAMES));
        if (siteParameterValues.isEmpty()) {
            // Need to convert it into an empty arraylist, cause ParameterTypeEnumeration.transformString2List()
            // returns an instance of EmptyList to which no entries can be added.
            siteParameterValues = new ArrayList<>();
        }
        int i = 0;
        for (IOObject site : streamingSites) {
            List<ConnectionInformationContainerIOObject> currentPlatforms = getAndValidateCurrentPlatforms(i, site);
            // This updates the inner ports with the correct naming. It also updates the siteParameterValues
            Pair<String, List<String>> computingSiteNames = getComputingSiteNames(i, siteParameterValues, currentPlatforms);
            String siteName = computingSiteNames.getFirst();
            List<String> platformNames = computingSiteNames.getSecond();
            updateInnerPorts(siteName, platformNames, connectedPlatforms);
            availableConnections.put(siteName, currentPlatforms);
            i++;
        }
        // Update the mapping parameter
        setParameter(PARAMETER_STREAMING_SITES_NAMES, ParameterTypeEnumeration.transformEnumeration2String(siteParameterValues));
        return availableConnections;
    }

    private List<ConnectionInformationContainerIOObject> getAndValidateCurrentPlatforms(int i, IOObject site) throws OperatorException {
        List<ConnectionInformationContainerIOObject> currentPlatforms = new ArrayList<>();
        if (site instanceof ConnectionInformationContainerIOObject) {
            currentPlatforms.add((ConnectionInformationContainerIOObject) site);
        } else if (site instanceof IOObjectCollection) {
            @SuppressWarnings("unchecked") IOObjectCollection<ConnectionInformationContainerIOObject> collection =
                    (IOObjectCollection<ConnectionInformationContainerIOObject>) site;
            currentPlatforms = collection.getObjects();
        } else {
            throw new OperatorException(
                    "Provided object at port " + streamingBackendsInputPort.getManagedPorts().get(i).getName()
                            + " is not a connection object or a collection of connection objects.");
        }
        // Check for the supported connection types
        for (ConnectionInformationContainerIOObject conn : currentPlatforms) {
            if (!supportedConnections.containsKey(
                    conn.getConnectionInformation().getConfiguration().getType())) {
                // Throw a User Error if the collection contains non-supported connections.
                throw new PortUserError(streamingBackendsInputPort.getManagedPorts().get(i),
                        "connection.mismatched_type",
                        conn.getConnectionInformation()
                                .getConfiguration()
                                .getType(),
                        StringUtils.join(supportedConnections.keySet(),
                                " or "));
            }
        }
        return currentPlatforms;
    }

    /**
     * This methods performs the optimization of the designed logical workflow.
     * <p>
     * First the response JSON from the INFORE optimizer is retrieved by calling {@link
     * OptimizationHelper#getOptimizerResponse(StreamingOptimizationOperator, Map, boolean, Long)}. Thereby a connection to the
     * Optimizer is created, the necessary configuration files are created and sent to the Optimizer. Also a
     * corresponding entry in the management db is created.
     * <p>
     * The response is used to update the second inner subprocess (the "Optimized Workflow" ; see {@link
     * OptimizationHelper#updateSubprocess(StreamingOptimizationOperator, String, String, Map)}). The Streaming Nest
     * operators are created and the operators of the logical workflow are placed according to the optimizer response.
     * Also splitted connections are updated.
     * <p>
     * Finally, the {@link ProcessPanel} of the GUI is updated, for a better visualization
     *
     * @param availableConnections Map between the name of a computing site and the available platforms ({@link
     *                             ConnectionInformationContainerIOObject}s at this site
     */
    private Pair<String, String> performOptimization(
            Map<String, List<ConnectionInformationContainerIOObject>> availableConnections) throws
            IOException, OperatorException, OperatorCreationException {
        Triple<OptimizerResponse, String, String> result = OptimizationHelper.getOptimizerResponse(this,
                availableConnections,
                getParameterAsBoolean(PARAMETER_CONTINUOUS_OPTIMIZATION), getParameterAsLong(PARAMETER_NUMBER_OF_PLANS));

        AgnosticWorkflow agnosticWorkflow = result.getFirst().getWorkflow();
        Tuple2<HashMap<String, String>, HashMap<String, HashMap<Integer, AWOperator>>> cepOperatorsAndPlatforms = createDecomposedCEPOperators(agnosticWorkflow);

        HashMap<String, String> cepAndFrequentPlatform = cepOperatorsAndPlatforms.f0;
        HashMap<String, HashMap<Integer, AWOperator>> cepOperatorsToAddByName = cepOperatorsAndPlatforms.f1;

        HashMap<String, String> operatorPlatform  = new HashMap<>();
        AgnosticWorkflow copyAgnosticWorkflow = new AgnosticWorkflow(agnosticWorkflow);

        copyAgnosticWorkflow.getPlacementSites().forEach(placementSite -> {
            placementSite.getAvailablePlatforms().forEach(availablePlatform -> {
                availablePlatform.getOperators().forEach(operator -> {
                    operatorPlatform.putIfAbsent(operator.getName(), placementSite.getSiteName());
                });
            });
        });

        HashMap<String, String> rootCepRootDup = new HashMap<>();
        HashMap<String, String> rootCepRootUnion = new HashMap<>();
        // Build the final regex pattern for the root cep operator
        // Add new connections for cep (intermediate) and agnostic workflow
        for (AWOperator operator : copyAgnosticWorkflow.getOperators()) {
            StringBuilder finalRegex = new StringBuilder();
            if (!operator.getClassKey().equals("streaming:cep")) continue;
            HashMap<Integer, AWOperator> cepOperatorsToAdd = cepOperatorsToAddByName.get(operator.getName());
            if (cepOperatorsToAdd == null) continue;
            AWOperator newUnionOperator = createUnionOperator(cepOperatorsToAddByName.get(operator.getName()).size(), operator.getName());
            agnosticWorkflow.getOperators().add(newUnionOperator); // Add important union operators

            AWPlacementOperator newUnionPlacementOperator = new AWPlacementOperator();
            newUnionPlacementOperator.setName(newUnionOperator.getName());
            rootCepRootUnion.put(operator.getName(), newUnionOperator.getName());
            // Add the union operator to sites operators
            agnosticWorkflow
                    .getPlacementSiteByName(cepAndFrequentPlatform.get(operator.getName()))
                        .getAvailablePlatforms().get(0).getOperators().add(newUnionPlacementOperator);

            // building final regex and adding the new operators to agnostic workflow
            for (int i = 0; i < cepOperatorsToAdd.size(); i++) {
                int indexOfRegex = cepOperatorsToAdd.get(i).findIndexOfParameterByKey("Pattern_Name");
                String currentRegex = "\"" + cepOperatorsToAdd.get(i).getParameters().get(indexOfRegex).getValue() + "\"";
                finalRegex.append(currentRegex);

                agnosticWorkflow.getOperators().add(cepOperatorsToAdd.get(i));
            }
            List<String> operatorNames = cepOperatorsToAdd
                    .values()
                    .stream()
                    .map(AWOperator::getName)
                    .collect(Collectors.toList());
            // This is going to be used in connection creation
            String rootDup = createDuplicateConnections((double) cepOperatorsToAddByName.get(operator.getName()).size(),
                    agnosticWorkflow,
                    operator.getName(),
                    operatorNames,
                    cepAndFrequentPlatform.get(operator.getName()));

            rootCepRootDup.put(operator.getName(), rootDup);

            agnosticWorkflow.getOperators().forEach(op -> {
                if (op.getName().equals(operator.getName()))
                {
                    int regexIndex = op.findIndexOfParameterByKey("Regular_Expression");
                    op.getParameters().get(regexIndex).setValue(finalRegex.toString());
                }
            });
        }
        // create new connections union and duplicate operators
        List<AWOperatorConnection> newConnections = new ArrayList<>();
        for (String cepOperator : cepOperatorsToAddByName.keySet()) {
            List<AWOperatorConnection> toConnections = new ArrayList<>(agnosticWorkflow.retrieveToOperators(cepOperator));
            toConnections.forEach(conn -> LOGGER.info("Key size " + cepOperatorsToAddByName.keySet().size() + " CURRENT CONNS " + conn.getFromOperator() + " -> " + conn.getToOperator()));
            List<AWOperatorConnection> updatedConnections = new ArrayList<>(toConnections);
            int counter = 1;
            for (AWOperator decompCep : cepOperatorsToAddByName.get(cepOperator).values()) {
                LOGGER.info(" ----------------- "+ decompCep);
                for (AWOperatorConnection connection : toConnections) {
                    agnosticWorkflow.getOperatorConnections().remove(connection);
                    updatedConnections.remove(connection);
                    toConnections.forEach(conn -> LOGGER.info("CURRENT CONNS " + conn.getFromOperator() + " -> " + conn.getToOperator()));
                    AWOperatorConnection newConnection = new AWOperatorConnection(connection);
                    newConnection.setToOperator(rootCepRootDup.get(cepOperator));
                    newConnections.add(newConnection);
                    LOGGER.info("New connection " + connection.getFromOperator() + " -> " + connection.getToOperator());
                }
                toConnections = updatedConnections;
                AWOperatorConnection newConnection = new AWOperatorConnection();
                newConnection
                        .setFromOperator(decompCep.getName())
                        .setToOperator(rootCepRootUnion.get(cepOperator))
                        .setFromPort("output stream")
                        .setFromPortType(AWPort.PortType.OUTPUT_PORT)
                        .setToPort("in stream "+counter)
                        .setToPortType(AWPort.PortType.INPUT_PORT);
                newConnections.add(newConnection);
                counter++;
            }
            AWOperatorConnection fromUnionConnection = new AWOperatorConnection()
                    .setFromOperator(rootCepRootUnion.get(cepOperator))
                    .setToOperator(cepOperator)
                    .setFromPort("output stream")
                    .setFromPortType(AWPort.PortType.OUTPUT_PORT)
                    .setToPort("input stream")
                    .setToPortType(AWPort.PortType.INPUT_PORT);
            newConnections.add(fromUnionConnection);

        }
        newConnections.forEach(connection -> {
            agnosticWorkflow.getOperatorConnections().add(connection);
        });

        //update agnostic workflow
        result.getFirst().setWorkflow(agnosticWorkflow);
        OptimizationHelper.updateSubprocess(this, result.getFirst(), result.getSecond(),
                availableSites);
        OptimizationHelper.updateProcessPanel(this);
        return new Pair<>(result.getSecond(), result.getThird());
    }

    private String createDuplicateConnections(
            double size,
            AgnosticWorkflow workflow,
            String cepName,
            List<String> cepOperators,
            String siteName
    ) {

        List<AWOperatorConnection> newConnections = new ArrayList<>();
        List<String> toOperatorNames = new ArrayList<>(cepOperators);
        List<String> tempToOperatorNames = new ArrayList<>();
        String duplicateName = "";
        while(size > 1) {
            size = Math.ceil(size/2);
            for (int i = 0; i < size; i++) {
                //Create duplicate operator
                String dupName = "dup_" + cepName + "_" + size + "_" + i;
                duplicateName = dupName;
                List<AWPort> inputPorts = new ArrayList<>();
                List<AWPort> outputPorts = new ArrayList<>();
                AWPort inputPort = new AWPort()
                        .setPortType(AWPort.PortType.INPUT_PORT)
                        .setName("input stream")
                        .setObjectClass(com.rapidminer.extension.streaming.ioobject.StreamDataContainer.class)
                        .setIsConnected(true);
                inputPorts.add(inputPort);
                AWPort outputPort = new AWPort()
                        .setPortType(AWPort.PortType.OUTPUT_PORT)
                        .setName("output stream 1")
                        .setObjectClass(com.rapidminer.extension.streaming.ioobject.StreamDataContainer.class)
                        .setIsConnected(true);
                outputPorts.add(outputPort);
                AWPort outputPort2 = new AWPort(outputPort);
                outputPort2.setName("output stream 2");
                if (cepOperators.size() % 2 != 0 && i == size - 1) { //the second output is empty
                    outputPort2.setIsConnected(false);
                }
                outputPorts.add(outputPort2);

                AWOperator dup = new AWOperator()
                        .setName(dupName)
                        .setClassKey("streaming:duplicate")
                        .setOperatorClass(com.rapidminer.extension.streaming.operator.StreamDuplicate.class)
                        .setIsEnabled(true)
                        .setInputPortsAndSchemas(inputPorts)
                        .setOutputPortsAndSchemas(outputPorts);
                workflow.getOperators().add(dup);
                // Add New duplicate in sites
                workflow.getPlacementSiteByName(siteName)
                        .getAvailablePlatforms().get(0)
                        .getOperators().add(new AWPlacementOperator().setName(dupName));

                // create connections
                AWOperatorConnection newConnection = new AWOperatorConnection()
                        .setFromOperator(dup.getName())
                        .setToOperator(toOperatorNames.remove(0))
                        .setFromPort("output stream 1")
                        .setToPort("input stream")
                        .setToPortType(AWPort.PortType.INPUT_PORT);

                LOGGER.info("new connection:  from "+newConnection.getFromOperator() + " to "+newConnection.getToOperator());
                newConnections.add(newConnection);

                if (cepOperators.size() % 2 != 0 && i == size - 1) continue;
                LOGGER.info("AFTER CONTINUE");
                AWOperatorConnection newConnection2 = new AWOperatorConnection()
                        .setFromOperator(dup.getName())
                        .setToOperator(toOperatorNames.remove(0))
                        .setFromPort("output stream 2")
                        .setToPort("input stream")
                        .setToPortType(AWPort.PortType.INPUT_PORT);
                newConnections.add(newConnection2);
                tempToOperatorNames.add(dupName);

            }
            toOperatorNames.addAll(tempToOperatorNames);
        }
        workflow.getOperatorConnections().addAll(newConnections);
        return duplicateName;
    }
    private AWOperator createUnionOperator(int inputSize, String cepName) {
        AWOperator unionOperator = new AWOperator();
        List<AWPort> inputPortsAndSchemas = new ArrayList<>();
        List<AWPort> outputPortsAndSchemas = new ArrayList<>();

        for (int i = 1; i <= inputSize; i++) {
            AWPort inputPort = new AWPort();
            inputPort
                    .setName("in stream " + i)
                    .setPortType(AWPort.PortType.INPUT_PORT)
                    .setIsConnected(true)
                    .setObjectClass(com.rapidminer.extension.streaming.ioobject.StreamDataContainer.class);
            inputPortsAndSchemas.add(inputPort);
        }
        AWPort inputPort = new AWPort();
        inputPort
                .setName("in stream " + (inputSize+1))
                .setPortType(AWPort.PortType.INPUT_PORT)
                .setIsConnected(false);
        inputPortsAndSchemas.add(inputPort);

        AWPort outputPort = new AWPort();
        outputPort
                .setName("output stream")
                .setPortType(AWPort.PortType.OUTPUT_PORT)
                .setIsConnected(true)
                .setObjectClass(com.rapidminer.extension.streaming.ioobject.StreamDataContainer.class);
        outputPortsAndSchemas.add(outputPort);

        unionOperator
                .setName("Union_" + cepName)
                .setOperatorClass(com.rapidminer.extension.streaming.operator.StreamUnion.class)
                .setClassKey("streaming:union")
                .setIsEnabled(true)
                .setInputPortsAndSchemas(inputPortsAndSchemas)
                .setOutputPortsAndSchemas(outputPortsAndSchemas);

        return unionOperator;
    }
    private Tuple2<HashMap<String, String>, HashMap<String, HashMap<Integer, AWOperator>>> createDecomposedCEPOperators(AgnosticWorkflow agnosticWorkflow) {

        // get cep operators that contain decompositions
        List<AWOperator> flinkCepOperators = new ArrayList<>();
        List<String> flinkCepOperatorNames = new ArrayList<>();
        HashMap<String, List<String>> cepAndPlatforms = new HashMap<>();
        HashMap<String, String> cepAndMostFrequentPlatform = new HashMap<>();
        agnosticWorkflow
                .getOperators()
                .forEach(awOperator -> {
                    AtomicBoolean containsDecompositions = new AtomicBoolean(false);
                    awOperator.getParameters().forEach(awParameter -> {
                        // check if operator contains decompositions
                        if (awParameter.getKey().equals("decompositions")) {
                            if (awParameter.getValue().equals("[]")) {
                                return;
                            }
                            containsDecompositions.set(true);
                        }
                    });
                    if (containsDecompositions.get()) {
                        flinkCepOperators.add(awOperator);
                        flinkCepOperatorNames.add(awOperator.getName());
                    }
                });
        // key is the index of the operator in the level of decomposition
        HashMap<String, HashMap<Integer, AWOperator>> decomposedOperators = new HashMap<>();

        agnosticWorkflow.getPlacementSites().forEach(placementSite -> {
            placementSite.getAvailablePlatforms().forEach(availablePlatform -> {
                availablePlatform.getOperators().forEach(operator -> {
                    int operatorIndex = -1;

                    // format of name is name_regex_index
                    String opName = operator.getName();
                    String[] splitName = opName.split("_");
                    String operatorName = splitName[0];
                    // if is not inside the wanted cep operators
                    if (!flinkCepOperatorNames.contains(operatorName)) return;
                    String decompositionRegex = splitName[1];

                    if (splitName.length != 3) operatorIndex = -1;
                    else operatorIndex = Integer.parseInt(splitName[2]); // in case is not a root

                    // find the root operator
                    AWOperator realRootCepOperator = flinkCepOperators.stream()
                            .filter(operator1 -> operator1.getName().equals(operatorName))
                            .findFirst()
                            .orElse(null);
                    // case not a cep operator
                    if (realRootCepOperator == null) {
                        return;
                    }
                    cepAndPlatforms.putIfAbsent(operatorName, new ArrayList<>());
                    cepAndPlatforms.get(operatorName).add(placementSite.getSiteName());
                    if (operatorIndex == -1) {
                        operator.setName(operatorName);
                        return;
                    }

                    // find the index of the cep operator, in case is the root index=0
                    int regexParameterPos = realRootCepOperator.findIndexOfParameterByKey("Regular_Expression");
                    String rootRegex = realRootCepOperator.getParameters().get(regexParameterPos).getValue();
                    int includeModelIndex = realRootCepOperator.findIndexOfParameterByKey("Include_Model");
                    realRootCepOperator.getParameters().get(includeModelIndex).setValue("false");

                    AWOperator newCEPoperator = new AWOperator(realRootCepOperator);
                    newCEPoperator.setName(opName);
                    List<AWParameter> parameters;
                    parameters = newCEPoperator.getParameters();

                    // in case of a decomposition cep operator
                    AtomicInteger index = new AtomicInteger();
                    List<AWParameter> finalParameters = parameters;
                    parameters.forEach(awParameter -> {
                        if (awParameter.getKey().equals("Pattern_Name"))
                            finalParameters.get(index.get()).setValue(decompositionRegex);
                        if (awParameter.getKey().equals("Regular_Expression"))
                            finalParameters.get(index.get()).setValue(decompositionRegex);
                        index.getAndIncrement();
                    });
                    newCEPoperator.setParameters(finalParameters);
                    // 1) If there isn't already an inner map for this name, put one
                    if (!decomposedOperators.containsKey(operatorName)) {
                        decomposedOperators.put(operatorName, new HashMap<>());
                    }
                    // 2) Now get that inner map and put your operator at operatorIndex
                    Map<Integer, AWOperator> inner = decomposedOperators.get(operatorName);
                    inner.put(operatorIndex, newCEPoperator);
                });
            });
        });

        cepAndPlatforms.forEach((cepName, platforms) -> {
            // 1) turn each platform name into (name,1)
            Optional<Tuple2<String, Integer>> best = platforms.stream()
                    .map(p -> Tuple2.of(p, 1))
                    // 2) reduce by merging two tuples at a time:
                    .reduce((t1, t2) -> {
                        if (t1.f0.equals(t2.f0)) {
                            // same key: sum counts
                            return Tuple2.of(t1.f0, t1.f1 + t2.f1);
                        } else {
                            // different keys: keep the one with higher count
                            return t1.f1 >= t2.f1 ? t1 : t2;
                        }
                    });

            // 3) extract the key (platform name) or null if empty
            String mostFrequent = best.map(t -> t.f0).orElse(null);
            cepAndMostFrequentPlatform.put(cepName, mostFrequent);
        });
        return Tuple2.of(cepAndMostFrequentPlatform, decomposedOperators);
    }

    private void deployOptimizedSubprocess(ConnectionInformationContainerIOObject kafkaConnection, Map<String,
            List<ConnectionInformationContainerIOObject>> availableConnections, Pair<String, String> optimizerDBIds) throws OperatorException {
        // Deliver the KafkaConnection to the (inner) kafkaOutputPort and the connections to
        // the streaming backends to the corresponding inner connection ports
        kafkaOutputPort.deliver(kafkaConnection);
        for (Entry<String, List<ConnectionInformationContainerIOObject>> entry : availableConnections
                .entrySet()) {
            for (ConnectionInformationContainerIOObject connection : entry.getValue()) {
                OutputPort port = getSubprocess(1).getInnerSources().getPortByName(
                        entry.getKey() + "_" + OptimizationHelper.getPlatformNameFromConnection(connection));
                port.deliver(connection);
            }
        }
        updateExecutionOrder();
        getSubprocess(1).execute();
        if (optimizerDBIds != null) {
            ManagementDAO.updateState(optimizerDBIds.getFirst(), optimizerDBIds.getSecond(), Status.DeployingNewPlan,
                    Status.Running);
        }
    }


    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> params = super.getParameterTypes();

        params.addAll(OptimizationHelper.addWriteAndReadParams(this));

        params.add(new ParameterTypeCategory(PARAMETER_ALGORITHM,
                "The algorithm of the optimizer to be used.",
                Arrays.stream(OptimizerAlgorithm.values())
                        .map(OptimizerAlgorithm::getDescription)
                        .toArray(String[]::new), 3, false));

        params.add(new ParameterTypeBoolean(PARAMETER_CONTINUOUS_OPTIMIZATION,
                "Select if the Optimizer Service should perform a continuous optimization.",
                false, false));

        ParameterType type = new ParameterTypeLong(PARAMETER_NUMBER_OF_PLANS,
                "Number of plans the Optimizer Service shall return.",
                1, Long.MAX_VALUE, 1, false);
        type.registerDependencyCondition(new BooleanParameterCondition(
                this, PARAMETER_CONTINUOUS_OPTIMIZATION, false, false));
        params.add(type);

        params.add(new ParameterTypeString(PARAMETER_NETWORK_NAME,
                "The unique name of the dictionary used by the optimizer.",
                "network1", false));

        params.add(new ParameterTypeString(PARAMETER_DICTIONARY_NAME,
                "The unique name of the dictionary used by the optimizer.",
                "dictionary1", false));

        params.add(new ParameterTypeString(PARAMETER_WORKFLOW_NAME,
                "The unique name of the workflow used by the optimizer.",
                "Streaming", false));

        params.add(new ParameterTypeEnumeration(PARAMETER_STREAMING_SITES_NAMES,
                "names of the available streaming sites",
                new ParameterTypeTupel("site", "tupel describing the computing site",
                        new ParameterTypeString("site_name", "name of the streaming site", true),
                        new ParameterTypeEnumeration("platforms", "names of the platforms",
                                new ParameterTypeString("platform_name", "name of the platform")))));

        params.add(new ParameterTypeLong(PARAMETER_CONNECT_TIME_OUT,
                "time out for the connection" + " to the optimizer (in seconds).",
                1, Long.MAX_VALUE, 30));

        params.add(new ParameterTypeInt(PARAMETER_POLLING_TIME_OUT,
                "time out for the polling of " + "the optimizer response (in seconds).",
                1, Integer.MAX_VALUE, 10));

        params.add(new ParameterTypeCategory(PARAMETER_OPERATION_MODE,
                "The mode of the streaming optimization to be used.",
                Arrays.stream(OperationMode.values())
                        .map(OperationMode::getDescription)
                        .toArray(String[]::new), 0, true));

        type = new ParameterTypeList(PARAMETER_CONNECTED_PLATFORMS, "", new ParameterTypeString(
                "port_name", ""), new ParameterTypeString(
                "streaming_nest_name", ""));
        type.setHidden(true);
        params.add(type);

        return params;
    }

    public ConnectionInformationSelector getOptimizerSelector() {
        return optimizerSelector;
    }

    public OutputPort getKafkaOutputPort() {
        return kafkaOutputPort;
    }

    public Map<String, Map<String, OutputPort>> getAvailableSites() {
        return availableSites;
    }

    public void fireUpdatePublic() {
        fireUpdate();
    }

    public void fireUpdatePublic(Operator operator) {
        fireUpdate(operator);
    }

}
