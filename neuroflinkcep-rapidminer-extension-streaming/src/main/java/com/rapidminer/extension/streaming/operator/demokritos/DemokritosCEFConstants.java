/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.operator.demokritos;


/**
 * Constants for CEF operators
 *
 * @author Mate Torok
 * @since 0.1.0
 */
final class DemokritosCEFConstants {

	static final String PARAMETER_DOMAIN_STREAM = "domain_stream";

	static final String PARAMETER_TIMESTAMP_KEY = "timestamp_key";

	static final String PARAMETER_CONFIG_TOPIC = "config_topic";

	static final String PARAMETER_JOB_JAR = "job_jar";

	static final String PARAMETER_THRESHOLD = "threshold";

	static final String PARAMETER_MAX_SPREAD = "max_spread";

	static final String PARAMETER_HORIZON = "horizon";

	static final String PARAMETER_PATTERNS = "patterns";

	static final String PARAMETER_DECLARATIONS = "declarations";
	
	static final String PARAMETER_K = "k";

	static final String PARAMETER_PARALLELISM = "parallelism";
	
	static final String PARAMETER_OFFSET_RESET = "offset_reset";

	static final String PARAMETER_INPUT_TOPIC = "input_topic";

	static final String PARAMETER_OUTPUT_TOPIC = "output_topic";

	static final String MLSTM_CONFIGURATION_TOPIC = "ETS_CONFIG";

	static final String CEF_JOB_MAIN_CLASS = "ui.WayebCLI";

	static final String CEF_FLINK_JOB_ARG_DOMAIN_STREAM = "--domainSpecificStream";

	static final String CEF_FLINK_JOB_ARG_TIMESTAMP_KEY = "--timestampKey";

	static final String CEF_FLINK_JOB_ARG_CONFIG_TOPIC = "--configTopic";

	static final String CEF_FLINK_JOB_ARG_THRESHOLD = "--threshold";

	static final String CEF_FLINK_JOB_ARG_MAX_SPREAD = "--maxSpread";

	static final String CEF_FLINK_JOB_ARG_HORIZON = "--horizon";

	static final String CEF_FLINK_JOB_ARG_PATTERNS = "--patterns";

	static final String CEF_FLINK_JOB_ARG_DECLARATIONS = "--declarations";
	
	static final String CEF_FLINK_JOB_ARG_K = "--k";

	static final String CEF_FLINK_JOB_ARG_PARALLELISM = "--parallelism";
	
	static final String CEF_FLINK_JOB_ARG_OFFSET_RESET = "--offsetReset";

	static final String CEF_FLINK_JOB_ARG_INPUT_TOPIC = "--inputTopic";

	static final String CEF_FLINK_JOB_ARG_OUTPUT_TOPIC = "--outputTopic";

	static final String CEF_FLINK_JOB_ARG_BOOTSTRAPS = "--bootstrapServers";

	static final String CEF_FLINK_JOB_ARG_ONLINE_FORECASTING = "onlineForecasting";

	private DemokritosCEFConstants() {
	}

}