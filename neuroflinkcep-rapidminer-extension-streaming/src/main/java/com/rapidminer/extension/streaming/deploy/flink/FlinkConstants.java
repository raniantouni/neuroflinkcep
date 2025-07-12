/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.flink;


/**
 * Flink specific constants on RapidMiner Studio side
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public final class FlinkConstants {

	public static final String FLINK_MAIN_CLASS = "com.rapidminer.extension.streaming.flink.StreamGraphProcessor";

	public static final String RM_CONF_FLINK_PARALLELISM = "FLINK.CLUSTER.PARALLELISM";

	private FlinkConstants() {
	}

}