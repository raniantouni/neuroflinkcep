/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator.athena;

import static com.google.common.collect.Lists.newArrayList;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.ATHENA_FLINK_JOB_ARG_FORECAST_ADDR;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.ATHENA_FLINK_JOB_ARG_FORECAST_TOPIC;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.ATHENA_FLINK_JOB_ARG_PARALLELISM;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.ATHENA_FLINK_JOB_ARG_PREDICTION_ADDR;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.ATHENA_FLINK_JOB_ARG_PREDICTION_TOPIC;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.ATHENA_FLINK_JOB_ARG_PSMSG_ADDR;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.ATHENA_FLINK_JOB_ARG_PSMSG_TOPIC;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.ATHENA_FLINK_JOB_ARG_REQ_ADDR;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.ATHENA_FLINK_JOB_ARG_REQ_TOPIC;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.ATHENA_FLINK_JOB_ARG_RESP_ADDR;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.ATHENA_FLINK_JOB_ARG_RESP_TOPIC;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.ATHENA_FLINK_JOB_ARG_TRAIN_ADDR;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.ATHENA_FLINK_JOB_ARG_TRAIN_TOPIC;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_FORECAST_INPUT_TOPIC;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_FORECAST_OUTPUT_TOPIC;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_JOB_JAR;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_LEARNER;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_LEARNER_HYPER;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_LEARNER_HYPER_NAME;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_LEARNER_HYPER_TUPLE;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_LEARNER_HYPER_TYPE;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_LEARNER_HYPER_VALUE;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_LEARNER_PARAM;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_LEARNER_PARAM_NAME;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_LEARNER_PARAM_TUPLE;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_LEARNER_PARAM_TYPE;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_LEARNER_PARAM_VALUE;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_PMESSAGE_TOPIC;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_PREPROCESSOR_HYPER;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_PREPROCESSOR_HYPER_NAME;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_PREPROCESSOR_HYPER_PP_NAME;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_PREPROCESSOR_HYPER_TUPLE;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_PREPROCESSOR_HYPER_TYPE;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_PREPROCESSOR_HYPER_VALUE;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_PREPROCESSOR_PARAM;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_PREPROCESSOR_PARAM_NAME;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_PREPROCESSOR_PARAM_PP_NAME;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_PREPROCESSOR_PARAM_TUPLE;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_PREPROCESSOR_PARAM_TYPE;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_PREPROCESSOR_PARAM_VALUE;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_REQUEST_TOPIC;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_RESPONSE_TOPIC;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_TRAINING_CONFIG;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_TRAINING_CONFIG_NAME;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_TRAINING_CONFIG_TUPLE;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_TRAINING_CONFIG_TYPE;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_TRAINING_CONFIG_VALUE;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.PARAMETER_TRAIN_TOPIC;
import static com.rapidminer.extension.streaming.operator.athena.AthenaOMLConstants.ValueType;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.rapidminer.extension.streaming.utility.api.infore.onlineml.ModelConfiguration;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeFile;
import com.rapidminer.parameter.ParameterTypeList;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.ParameterTypeTupel;
import com.rapidminer.parameter.UndefinedParameterError;


/**
 * Parameter helper utility for the Athena OML operator. This class takes care of the more complex cases of parameter
 * parsing (ParameterTypeList). It also builds the entire ParameterType list for the operator.
 *
 * @author Mate Torok
 * @since 0.1.0
 */
class AthenaOMLParameterHelper {

	private final Operator op;

	/**
	 * Constructs parameter handling functionality for the operator
	 *
	 * @param op
	 */
	AthenaOMLParameterHelper(Operator op) {
		this.op = op;
	}

	/**
	 * Parses the learner parameters (ParameterTypeList-s + strings)
	 *
	 * @return parsed structure
	 */
	ModelConfiguration getLearner() throws UndefinedParameterError {
		Function<String[], String> keyMapper = arr -> arr[0];
		Function<String[], Object> valueMapper = arr -> ValueType.getData(ValueType.fromName(arr[2]), arr[1]);

		return new ModelConfiguration(
			op.getParameterAsString(PARAMETER_LEARNER),
			getListContent(PARAMETER_LEARNER_HYPER).stream().collect(Collectors.toMap(keyMapper, valueMapper)),
			getListContent(PARAMETER_LEARNER_PARAM).stream().collect(Collectors.toMap(keyMapper, valueMapper)));
	}

	/**
	 * Parses the Pre-Processor structures (2 ParameterTypeList-s)
	 *
	 * @return parsed structure
	 */
	List<ModelConfiguration> getPreProcessors() throws UndefinedParameterError {
		Function<String[], String> keyMapper = arr -> arr[1];
		Function<String[], Object> valueMapper = arr -> ValueType.getData(ValueType.fromName(arr[3]), arr[2]);
		Multimap<String, String[]> allHypers = LinkedListMultimap.create();
		Multimap<String, String[]> allParams = LinkedListMultimap.create();

		// Collect hyper-parameters per pre-processor
		getListContent(PARAMETER_PREPROCESSOR_HYPER).forEach(row -> allHypers.put(row[0], row));

		// Collect parameters per pre-processor
		getListContent(PARAMETER_PREPROCESSOR_PARAM).forEach(row -> allParams.put(row[0], row));

		// Iterate through the set of appearing Pre-Processors based on the 2 collections and combine them into Model-s
		return Sets.union(allHypers.keySet(), allParams.keySet())
			.stream()
			.map(name ->
				new ModelConfiguration(
					name,
					allHypers.get(name).stream().collect(Collectors.toMap(keyMapper, valueMapper)),
					allParams.get(name).stream().collect(Collectors.toMap(keyMapper, valueMapper))))
			.collect(Collectors.toList());
	}

	/**
	 * Parses the training configuration
	 *
	 * @return parsed structure
	 */
	Map<String, Object> getTrainingConfiguration() throws UndefinedParameterError {
		return getListContent(PARAMETER_TRAINING_CONFIG)
			.stream()
			.collect(Collectors.toMap(
				strings -> strings[0],
				strings -> ValueType.getData(ValueType.fromName(strings[2]), strings[1])
			));
	}

	/**
	 * Builds the job argument list for submission (Flink), i.e. list of strings, e.g.:
	 * (--input,inTopic,--output,outTopic).
	 *
	 * @param parallelism to use for the job
	 * @param brokers     Kafka broker list to use in the arguments
	 * @return correctly formatted job argument string
	 * @throws UndefinedParameterError
	 */
	List<String> getJobArguments(int parallelism, String brokers) throws UndefinedParameterError {
		return newArrayList(
			ATHENA_FLINK_JOB_ARG_PARALLELISM, String.valueOf(parallelism),
			ATHENA_FLINK_JOB_ARG_TRAIN_TOPIC, op.getParameterAsString(PARAMETER_TRAIN_TOPIC),
			ATHENA_FLINK_JOB_ARG_TRAIN_ADDR, brokers,
			ATHENA_FLINK_JOB_ARG_FORECAST_TOPIC, op.getParameterAsString(PARAMETER_FORECAST_INPUT_TOPIC),
			ATHENA_FLINK_JOB_ARG_FORECAST_ADDR, brokers,
			ATHENA_FLINK_JOB_ARG_REQ_TOPIC, op.getParameterAsString(PARAMETER_REQUEST_TOPIC),
			ATHENA_FLINK_JOB_ARG_REQ_ADDR, brokers,
			ATHENA_FLINK_JOB_ARG_RESP_TOPIC, op.getParameterAsString(PARAMETER_RESPONSE_TOPIC),
			ATHENA_FLINK_JOB_ARG_RESP_ADDR, brokers,
			ATHENA_FLINK_JOB_ARG_PREDICTION_TOPIC, op.getParameterAsString(PARAMETER_FORECAST_OUTPUT_TOPIC),
			ATHENA_FLINK_JOB_ARG_PREDICTION_ADDR, brokers,
			ATHENA_FLINK_JOB_ARG_PSMSG_TOPIC, op.getParameterAsString(PARAMETER_PMESSAGE_TOPIC),
			ATHENA_FLINK_JOB_ARG_PSMSG_ADDR, brokers);
	}

	/**
	 * Builds the parameters for the operator
	 *
	 * @return newly built list of parameters
	 */
	List<ParameterType> buildParameterTypes() {
		List<ParameterType> types = newArrayList();

		addTopicParams(types);
		addLearnerParams(types);
		addPreProcessorParams(types);
		addTrainingConfiguration(types);

		// For the fat-JAR
		types.add(new ParameterTypeFile(PARAMETER_JOB_JAR, "Path to the job fat-JAR", "jar", false));

		return types;
	}

	/**
	 * Parameters (topics) for OML
	 *
	 * @param types
	 */
	private void addTopicParams(List<ParameterType> types) {
		ParameterType trainTopic = new ParameterTypeString(PARAMETER_TRAIN_TOPIC, "Training input topic", false);
		ParameterType predInTopic = new ParameterTypeString(PARAMETER_FORECAST_INPUT_TOPIC, "Input topic", false);
		ParameterType predOutTopic = new ParameterTypeString(PARAMETER_FORECAST_OUTPUT_TOPIC, "Output topic", false);
		ParameterType reqTopic = new ParameterTypeString(PARAMETER_REQUEST_TOPIC, "Request topic", false);
		ParameterType respTopic = new ParameterTypeString(PARAMETER_RESPONSE_TOPIC, "Response topic", false);
		ParameterType psMsgTopic = new ParameterTypeString(PARAMETER_PMESSAGE_TOPIC, "Parameter message topic", false);
		types.addAll(newArrayList(trainTopic, predInTopic, predOutTopic, reqTopic, respTopic, psMsgTopic));
	}

	/**
	 * Parameters for the 'learner'
	 *
	 * @param types
	 */
	private void addLearnerParams(List<ParameterType> types) {
		ParameterType learner = new ParameterTypeString(PARAMETER_LEARNER, "Learner name", false);

		ParameterType hyperParams = buildListParameter(
			PARAMETER_LEARNER_HYPER,
			PARAMETER_LEARNER_HYPER_NAME,
			new ParameterTypeTupel(
				PARAMETER_LEARNER_HYPER_TUPLE, "",
				new ParameterTypeString(PARAMETER_LEARNER_HYPER_VALUE, ""),
				new ParameterTypeCategory(PARAMETER_LEARNER_HYPER_TYPE, "", ValueType.valueStrings(), 0)));

		ParameterType params = buildListParameter(
			PARAMETER_LEARNER_PARAM,
			PARAMETER_LEARNER_PARAM_NAME,
			new ParameterTypeTupel(
				PARAMETER_LEARNER_PARAM_TUPLE, "",
				new ParameterTypeString(PARAMETER_LEARNER_PARAM_VALUE, ""),
				new ParameterTypeCategory(PARAMETER_LEARNER_PARAM_TYPE, "", ValueType.valueStrings(), 0)));

		types.addAll(newArrayList(learner, hyperParams, params));
	}

	/**
	 * Parameters for the 'pre-processors', there are an arbitrary number of pre-processors and for now they will all
	 * live in the same list (there is no easy way to graphically represent this).
	 *
	 * @param types
	 */
	private void addPreProcessorParams(List<ParameterType> types) {
		ParameterType hyperParams = buildListParameter(
			PARAMETER_PREPROCESSOR_HYPER,
			PARAMETER_PREPROCESSOR_HYPER_PP_NAME,
			new ParameterTypeTupel(
				PARAMETER_PREPROCESSOR_HYPER_TUPLE, "",
				new ParameterTypeString(PARAMETER_PREPROCESSOR_HYPER_NAME, ""),
				new ParameterTypeString(PARAMETER_PREPROCESSOR_HYPER_VALUE, ""),
				new ParameterTypeCategory(PARAMETER_PREPROCESSOR_HYPER_TYPE, "", ValueType.valueStrings(), 0)));

		ParameterType params = buildListParameter(
			PARAMETER_PREPROCESSOR_PARAM,
			PARAMETER_PREPROCESSOR_PARAM_PP_NAME,
			new ParameterTypeTupel(
				PARAMETER_PREPROCESSOR_PARAM_TUPLE, "",
				new ParameterTypeString(PARAMETER_PREPROCESSOR_PARAM_NAME, ""),
				new ParameterTypeString(PARAMETER_PREPROCESSOR_PARAM_VALUE, ""),
				new ParameterTypeCategory(PARAMETER_PREPROCESSOR_PARAM_TYPE, "", ValueType.valueStrings(), 0)));

		types.addAll(newArrayList(hyperParams, params));
	}

	/**
	 * Parameters for the "training_configuration" of the request
	 *
	 * @param types
	 */
	private void addTrainingConfiguration(List<ParameterType> types) {
		ParameterType configs = buildListParameter(
			PARAMETER_TRAINING_CONFIG,
			PARAMETER_TRAINING_CONFIG_NAME,
			new ParameterTypeTupel(
				PARAMETER_TRAINING_CONFIG_TUPLE, "",
				new ParameterTypeString(PARAMETER_TRAINING_CONFIG_VALUE, ""),
				new ParameterTypeCategory(PARAMETER_TRAINING_CONFIG_TYPE, "", ValueType.valueStrings(), 0)));

		types.add(configs);
	}

	/**
	 * Builds a ParameterTypeList parameter with the given arguments (Athena OML specific)
	 *
	 * @param listKey to get the entire list later
	 * @param key     left column
	 * @param values  right column
	 * @return newly built parameter
	 */
	private ParameterType buildListParameter(String listKey, String key, ParameterTypeTupel values) {
		ParameterTypeList typeList = new ParameterTypeList(listKey, "", new ParameterTypeString(key, ""), values);
		typeList.setExpert(false);
		return typeList;
	}

	/**
	 * Returns the content of a ParameterTypeList object as a list of string arrays (not limited to 2 elements!).
	 *
	 * @param key
	 * @return see above
	 * @throws UndefinedParameterError
	 */
	private List<String[]> getListContent(String key) throws UndefinedParameterError {
		List<String[]> content = Lists.newArrayList();
		// Iterate through the rows in the list
		for (String[] row : op.getParameterList(key)) {
			// Left column is always a simple string
			String leftCol = row[0];
			// Right "column" is always a tuple for us, either of length 2 or 3
			String[] rightCol = ParameterTypeTupel.transformString2Tupel(row[1]);
			// Flatten it into 1 single array and add it to the result list
			content.add(ArrayUtils.insert(0, rightCol, leftCol));
		}
		return content;
	}

}