/*
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.raap;


/**
 * This class holds the (default) setting values to be used for configuring the plugin (i.e. RapidMiner as a plugin)
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public final class DefaultSettingValue {

	/**
	 * Default path to the home folder of RapidMiner (RM specific plugins, configurations live there)
	 */
	public static final String RM_HOME = "/rm-home";

	/**
	 * Default path to the <i>user.home</i> which also contains <b>.RapidMiner</b>
	 */
	public static final String RM_USER_HOME = RM_HOME;

	/**
	 * Default plugin ID for the RaaP plugin
	 */
	public static final String RM_PLUGIN_ID = "rm-as-plugin";

	/**
	 * Default path to the RaaP plugin artifact (ZIP) (relative to for instance RM_HOME)
	 */
	public static final String RM_PLUGIN_ARTIFACT_PATH =  "lib/" + RM_PLUGIN_ID + "/" + RM_PLUGIN_ID + ".zip";

	private DefaultSettingValue() {}

}