/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.spark;


/**
 * Spark specific constants on RapidMiner Studio side
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public final class SparkConstants {

	public static final String HDFS_FOLDER_JARS = "jars/";

	public static final String RM_CONF_HDFS_URI = "HDFS.URI";

	public static final String RM_CONF_HDFS_PATH = "HDFS.PATH";

	public static final String SPARK_MAIN_CLASS = "com.rapidminer.extension.streaming.spark.StreamGraphProcessor";

	public static final String SPARK_CONFIG_LINE_PATTERN = "%s\t%s\n";

	private SparkConstants() {
	}

}