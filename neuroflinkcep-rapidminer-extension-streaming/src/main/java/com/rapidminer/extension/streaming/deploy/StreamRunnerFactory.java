/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy;

import java.net.URISyntaxException;
import java.util.Properties;
import java.util.logging.Logger;

import com.rapidminer.extension.streaming.deploy.flink.FlinkStreamRunner;
import com.rapidminer.extension.streaming.deploy.spark.SparkStreamRunner;
import com.rapidminer.tools.LogService;


/**
 * Factory class for platform dependent runner creation
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class StreamRunnerFactory {

	private static final Logger LOGGER = LogService.getRoot();

	public StreamRunner createRunner(StreamRunnerType type, Properties configuration) throws StreamRunnerException {
		switch (type) {
			case SPARK:
				LOGGER.fine("Creating Spark runner");
				return createSparkRunner(configuration);
			case FLINK:
				LOGGER.fine("Creating Flink runner");
				return createFlinkRunner(configuration);
			default:
				LOGGER.severe("Invalid runner type selected");
				throw new StreamRunnerException("Invalid runner type: " + type);
		}
	}

	private StreamRunner createSparkRunner(Properties configuration) throws StreamRunnerException {
		try {
			return new SparkStreamRunner(configuration);
		} catch (URISyntaxException e) {
			throw new StreamRunnerException("Could not create Spark runner", e);
		}
	}

	private StreamRunner createFlinkRunner(Properties configuration) {
		return new FlinkStreamRunner(configuration);
	}

}