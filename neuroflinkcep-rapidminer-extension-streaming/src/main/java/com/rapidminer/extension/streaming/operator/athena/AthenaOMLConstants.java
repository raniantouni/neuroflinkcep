/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator.athena;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;


/**
 * Constants for the Athena operator(s)
 *
 * @author Mate Torok
 * @since 0.1.0
 */
final class AthenaOMLConstants {

	/**
	 * Type of a value
	 */
	enum ValueType {
		NUMERIC("Numeric"), NUMERICLIST("Numeric List"), NOMINAL("Nominal"), NOMINALLIST("Nominal List");

		private final String name;

		ValueType(String name) {
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
		 * Depending on the type it returns (safely!) the data as follows:
		 * <ol>
		 *     <li>Numeric: double value or NaN</li>
		 *     <li>NumericList: double array</li>
		 *     <li>Nominal: string value or EMPTY</li>
		 *     <li>NominalList: string array</li>
		 * </ol>
		 *
		 * @param type
		 * @param str
		 * @return see above
		 */
		static Object getData(ValueType type, String str) throws NumberFormatException {
			switch (type) {
				case NOMINAL:
					return isBlank(str) ? EMPTY : str;
				case NUMERIC:
					return isBlank(str) ? Double.NaN : Double.valueOf(str);
				case NUMERICLIST:
					return Arrays
						.stream(StringUtils.split(str, ","))
						.filter(s -> !isBlank(s))
						.map(Double::valueOf)
						.toArray();
				case NOMINALLIST:
					return StringUtils.split(str, ",");
				default:
					throw new IllegalArgumentException("Type is unknown: " + type);
			}
		}

		/**
		 * @return string array of types
		 */
		static String[] valueStrings() {
			return Arrays
				.stream(ValueType.values())
				.map(ValueType::getName)
				.toArray(String[]::new);
		}

		/**
		 * Given the parameter 'name' this method returns the correct ValueType belongs to that name
		 *
		 * @param name
		 * @return ValueType or null (if none found)
		 */
		static ValueType fromName(String name) {
			return Arrays
				.stream(ValueType.values())
				.filter(valueType -> StringUtils.equals(name, valueType.getName()))
				.findFirst()
				.orElse(null);
		}

	}

	static final String PARAMETER_JOB_JAR = "job_jar";

	static final String PARAMETER_TRAIN_TOPIC = "training_topic";

	static final String PARAMETER_FORECAST_INPUT_TOPIC = "forecast_input_topic";

	static final String PARAMETER_FORECAST_OUTPUT_TOPIC = "forecast_output_topic";

	static final String PARAMETER_REQUEST_TOPIC = "request_topic";

	static final String PARAMETER_RESPONSE_TOPIC = "response_topic";

	static final String PARAMETER_PMESSAGE_TOPIC = "parameter_message_topic";

	static final String PARAMETER_LEARNER = "learner";

	static final String PARAMETER_LEARNER_HYPER = "learner_hyper_parameters";

	static final String PARAMETER_LEARNER_HYPER_NAME = "learner_hyper_parameter_name";

	static final String PARAMETER_LEARNER_HYPER_TUPLE = "learner_hyper_value_tuple";

	static final String PARAMETER_LEARNER_HYPER_VALUE = "learner_hyper_parameter_value";

	static final String PARAMETER_LEARNER_HYPER_TYPE = "learner_hyper_parameter_value_type";

	static final String PARAMETER_LEARNER_PARAM = "learner_parameters";

	static final String PARAMETER_LEARNER_PARAM_NAME = "learner_parameter_name";

	static final String PARAMETER_LEARNER_PARAM_TUPLE = "learner_parameter_value_tuple";

	static final String PARAMETER_LEARNER_PARAM_VALUE = "learner_parameter_value";

	static final String PARAMETER_LEARNER_PARAM_TYPE = "learner_parameter_value_type";

	static final String PARAMETER_PREPROCESSOR_HYPER = "preprocessor_hyper_parameters";

	static final String PARAMETER_PREPROCESSOR_HYPER_PP_NAME = "preprocessor_name_for_hyper_parameter";

	static final String PARAMETER_PREPROCESSOR_HYPER_TUPLE = "preprocessor_hyper_parameter_tuple";

	static final String PARAMETER_PREPROCESSOR_HYPER_NAME = "preprocessor_hyper_parameter_name";

	static final String PARAMETER_PREPROCESSOR_HYPER_VALUE = "preprocessor_hyper_parameter_value";

	static final String PARAMETER_PREPROCESSOR_HYPER_TYPE = "preprocessor_hyper_parameter_value_type";

	static final String PARAMETER_PREPROCESSOR_PARAM = "preprocessor_parameters";

	static final String PARAMETER_PREPROCESSOR_PARAM_PP_NAME = "preprocessor_name_for_parameter";

	static final String PARAMETER_PREPROCESSOR_PARAM_TUPLE = "preprocessor_parameter_tuple";

	static final String PARAMETER_PREPROCESSOR_PARAM_NAME = "preprocessor_parameter_name";

	static final String PARAMETER_PREPROCESSOR_PARAM_VALUE = "preprocessor_parameter_value";

	static final String PARAMETER_PREPROCESSOR_PARAM_TYPE = "preprocessor_parameter_value_type";

	static final String PARAMETER_TRAINING_CONFIG = "training_configuration";

	static final String PARAMETER_TRAINING_CONFIG_NAME = "training_configuration_name";

	static final String PARAMETER_TRAINING_CONFIG_TUPLE = "training_configuration_tuple";

	static final String PARAMETER_TRAINING_CONFIG_VALUE = "training_configuration_value";

	static final String PARAMETER_TRAINING_CONFIG_TYPE = "training_configuration_value_type";

	static final String ATHENA_FLINK_JOB_ARG_PARALLELISM = "--parallelism";

	static final String ATHENA_FLINK_JOB_ARG_TRAIN_TOPIC = "--trainingDataTopic";

	static final String ATHENA_FLINK_JOB_ARG_TRAIN_ADDR = "--trainingDataAddr";

	static final String ATHENA_FLINK_JOB_ARG_FORECAST_TOPIC = "--forecastingDataTopic";

	static final String ATHENA_FLINK_JOB_ARG_FORECAST_ADDR = "--forecastingDataAddr";

	static final String ATHENA_FLINK_JOB_ARG_REQ_TOPIC = "--requestsTopic";

	static final String ATHENA_FLINK_JOB_ARG_REQ_ADDR = "--requestsAddr";

	static final String ATHENA_FLINK_JOB_ARG_RESP_TOPIC = "--responsesTopic";

	static final String ATHENA_FLINK_JOB_ARG_RESP_ADDR = "--responsesAddr";

	static final String ATHENA_FLINK_JOB_ARG_PREDICTION_TOPIC = "--predictionsTopic";

	static final String ATHENA_FLINK_JOB_ARG_PREDICTION_ADDR = "--predictionsAddr";

	static final String ATHENA_FLINK_JOB_ARG_PSMSG_TOPIC = "--psMessagesTopic";

	static final String ATHENA_FLINK_JOB_ARG_PSMSG_ADDR = "--psMessagesAddr";

	private AthenaOMLConstants() {
	}

}