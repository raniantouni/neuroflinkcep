/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.connection;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.rapidminer.connection.util.ConnectionInformationSelector;
import com.rapidminer.extension.kafka_connector.connections.KafkaConnectionHandler;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.SimpleProcessSetupError;
import com.rapidminer.operator.ports.PortOwner;
import com.rapidminer.operator.ports.metadata.ConnectionInformationMetaData;
import com.rapidminer.repository.RepositoryException;


/**
 * Logic supporting connection handlers and selectors
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
public class StreamingConnectionHelper {

	private StreamingConnectionHelper() {
	}

	public static final String CONNECTION_MISMATCHED_TYPE = "connection.mismatched_type";

	public static final Map<String, StreamConnectionHandler> CONNECTION_HANDLER_MAP = createConnectionHandlerMap();

	/**
	 * @return newly built connection selector that manages the Kafka connection handler
	 */
	public static ConnectionInformationSelector createKafkaSelector(Operator operator, String portName) {
		return createSelector(operator, portName, KafkaConnectionHandler.getINSTANCE().getType());
	}

	/**
	 * @return newly built connection selector that manages the Flink connection handler
	 */
	public static ConnectionInformationSelector createFlinkSelector(Operator operator, String portName) {
		return createSelector(operator, portName, FlinkConnectionHandler.getINSTANCE().getType());
	}

	/**
	 * @param operator
	 * @param portName
	 * @param type     connection-type
	 * @return newly built connection selector that manages the connection handler
	 */
	public static ConnectionInformationSelector createSelector(Operator operator, String portName, String type) {
		return new ConnectionInformationSelector(operator.getInputPorts().createPort(portName), null, operator, type);
	}

	/**
	 * @return newly built mapping between connection types and actual connection handlers (instance)
	 */
	public static Map<String, StreamConnectionHandler> createConnectionHandlerMap() {
		return new ImmutableMap.Builder<String, StreamConnectionHandler>()
			.put(FlinkConnectionHandler.getINSTANCE().getType(), FlinkConnectionHandler.getINSTANCE())
			.put(SparkConnectionHandler.getINSTANCE().getType(), SparkConnectionHandler.getINSTANCE())
			.build();
	}

	/**
	 * Builds and returns a ConnectionInformationSelector for the operator that is capable of checking whether the
	 * connection is targeting a streaming platform.
	 *
	 * @param operator
	 * @param portName
	 * @return see above
	 */
	public static ConnectionInformationSelector createStreamConnectionSelector(Operator operator, String portName) {
		return new ConnectionInformationSelector(operator.getInputPorts().createPort(portName), null, operator, null) {

			@Override
			public ProcessSetupError checkConnectionTypeMatch(Operator operator) {
				PortOwner portOwner = operator.getPortOwner();

				// Check with parent class
				ProcessSetupError error = super.checkConnectionTypeMatch(operator);
				if (error != null) {
					return error;
				}

				// Check if connection type is one of our own (e.g. Flink)
				try {
					ConnectionInformationMetaData metaData = getMetaDataOrThrow();
					if (metaData == null) {
						return null;
					}

					String foundType = metaData.getConnectionType();
					Set<String> allowedTypes = CONNECTION_HANDLER_MAP.keySet();
					String allowedTypesStr = allowedTypes.toString();

					if (!allowedTypes.contains(metaData.getConnectionType())) {
						return new SimpleProcessSetupError(
							Severity.ERROR, portOwner, CONNECTION_MISMATCHED_TYPE, allowedTypesStr, foundType);
					}
				} catch (RepositoryException e) {
					String errorKey = e.getMessage() == null ? "connection.repository_error" : e.getMessage();
					return new SimpleProcessSetupError(Severity.ERROR, portOwner, errorKey);
				}

				return null;
			}

		};
	}

}