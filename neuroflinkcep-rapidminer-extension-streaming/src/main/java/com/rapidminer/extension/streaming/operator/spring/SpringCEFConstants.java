/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator.spring;


/**
 * Constants for Spring CEF operators
 *
 * @author Mate Torok
 * @since 0.2.0
 */
final class SpringCEFConstants {

	static final String PARAMETER_JOB_JAR = "job_jar";

	static final String PARAMETER_PATTERN_LENGTH = "pattern_length";

	static final String PARAMETER_STEP_LENGTH = "step_length";

	static final String PARAMETER_FORECAST_HORIZON = "forecast_horizon";

	static final String PARAMETER_PRECISION = "precision";

	static final String PARAMETER_LENGTH = "length";

	static final String PARAMETER_PARALLELISM = "parallelism";

	static final String PARAMETER_INPUT_TOPIC = "input_topic";

	static final String PARAMETER_OUTPUT_TOPIC = "output_topic";

	static final String SPRING_FLINK_JOB_ARG_LENGTH = "-length";

	static final String SPRING_FLINK_JOB_ARG_STEP = "-step";

	static final String SPRING_FLINK_JOB_ARG_PL = "-pl";

	static final String SPRING_FLINK_JOB_ARG_FH = "-fh";

	static final String SPRING_FLINK_JOB_ARG_PR = "-pr";

	static final String SPRING_FLINK_JOB_ARG_BST = "-bst";

	static final String SPRING_FLINK_JOB_ARG_INPUT_TOPIC = "-i";

	static final String SPRING_FLINK_JOB_ARG_OUTPUT_TOPIC = "-o";

	private SpringCEFConstants() {
	}

}