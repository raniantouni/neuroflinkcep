/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator.athena;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;


/**
 * Constants for the SDE operator
 *
 * @author Mate Torok
 * @since 0.3.0
 */
final class SDEConstants {

	/**
	 * Available types of estimations
	 */
	enum EstimationType {

		NORMAL("Normal"),
		WITH_RANDOM_PARTITIONING("With random partitioning"),
		CONTINUOUS("Continuous"),
		ADVANCED("Advanced");

		private final String name;

		EstimationType(String name) {
			this.name = name;
		}

		/**
		 * String representation for the enum value
		 *
		 * @return see above
		 */
		String getName() {
			return name;
		}

		/**
		 * @return string array of types
		 */
		static String[] valueStrings() {
			return Arrays
				.stream(EstimationType.values())
				.map(EstimationType::getName)
				.toArray(String[]::new);
		}

		/**
		 * Given the parameter 'name' this method returns the correct EstimationType belongs to that name
		 *
		 * @param name
		 * @return EstimationType or null (if none found)
		 */
		static EstimationType fromName(String name) {
			return Arrays
				.stream(EstimationType.values())
				.filter(valueType -> StringUtils.equals(name, valueType.getName()))
				.findFirst()
				.orElse(null);
		}

	}

	static final String ITEM_LIST_SEPARATOR = ",";

	static final String PARAMETER_SYNOPSIS_TYPE = "synopsis_type";

	static final String PARAMETER_SYNOPSIS_PARAMS = "synopsis_params";

	static final String PARAMETER_SYNOPSIS_PARALLELISM = "synopsis_parallelism";

	static final String PARAMETER_SYNOPSIS_DATA_SET_KEY = "data_set_key";

	static final String PARAMETER_SYNOPSIS_U_ID = "u_id";

	static final String PARAMETER_SYNOPSIS_STREAM_ID_KEY = "stream_id_key";

	static final String PARAMETER_ESTIMATE_TYPE = "estimate_type";

	static final String PARAMETER_ESTIMATE_FREQUENCY = "estimate_frequency";

	static final String PARAMETER_ESTIMATE_PARAMS = "estimate_params";

	static final String PARAMETER_REQUEST_TOPIC = "request_topic";

	static final String PARAMETER_DATA_TOPIC = "data_topic";

	static final String PARAMETER_OUTPUT_TOPIC = "output_topic";

	private SDEConstants() {}

}