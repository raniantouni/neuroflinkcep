/*
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.raap;

import static com.rapidminer.extension.streaming.raap.SettingKey.CONF_PLUGIN_ID;
import static com.rapidminer.extension.streaming.raap.SettingKey.CONF_PLUGIN_PATH;

import java.nio.file.Paths;
import java.util.List;

import org.pf4j.PluginManager;

import com.rapidminer.extension.streaming.raap.util.RaaPZipPluginManager;


/**
 * This class (utility) is responsible for setting up the plugin (PF4J) environment
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public final class RapidMinerPluginManager {

	private static boolean initialized = false;

	private static RapidMinerModelApplierFactory modelApplierFactory;

	/**
	 * Synchronized initialization of the entire environment = RapidMiner as a plugin.
	 * Available configurations via system properties: {@link SettingKey}.
	 */
	public static synchronized void initialize() {
		if (!initialized) {
			PluginManager pluginManager = new RaaPZipPluginManager();

			pluginManager.loadPlugin(Paths.get(System.getProperty(CONF_PLUGIN_PATH)));
			pluginManager.startPlugin(System.getProperty(CONF_PLUGIN_ID));

			List<RapidMinerModelApplierFactory> factories = pluginManager.getExtensions(RapidMinerModelApplierFactory.class);
			if (factories.size() == 0) {
				throw new IllegalStateException("RapidMiner Plugin: {" + CONF_PLUGIN_ID + "} could not be found");
			}

			modelApplierFactory = factories.get(0);
			initialized = true;
		}
	}

	/**
	 * @return the factory to build a RapidMiner model applier
	 */
	public static RapidMinerModelApplierFactory getModelApplierFactory() {
		if (initialized) {
			return modelApplierFactory;
		} else {
			throw new IllegalStateException("RapidMinerPluginManager hasn't been successfully initialized yet");
		}
	}

	private RapidMinerPluginManager() {}

}