/*
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.raap;


/**
 * This class holds setting keys to be used for configuring the plugin (i.e. RapidMiner as a plugin)
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public final class SettingKey {

	/**
	 * Path to the home folder of RapidMiner (RM specific plugins, configurations live there)
	 */
	public static final String CONF_RM_HOME = "CONF_RMPLUGIN_RM_HOME";

	/**
	 * Name/id of the plugin, e.g.: rm-as-plugin
	 */
	public static final String CONF_PLUGIN_ID = "CONF_RMPLUGIN_PLUGIN_ID";

	/**
	 * Path to the plugin zip file
	 */
	public static final String CONF_PLUGIN_PATH = "CONF_RMPLUGIN_PLUGIN_PATH";

	/**
	 * Path to the <i>user.home</i> which also contains <b>.RapidMiner</b>
	 */
	public static final String CONF_RM_USER_HOME = "CONF_RMPLUGIN_RM_USER_HOME";

	private SettingKey() {}

}