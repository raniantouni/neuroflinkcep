/*
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.raap.util;

import java.nio.file.Path;

import org.pf4j.DefaultPluginLoader;
import org.pf4j.PluginClassLoader;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginManager;


/**
 * RaaP = "RapidMiner-as-a-Plugin classloader" specialization of the extended class
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public class RaaPDefaultPluginLoader extends DefaultPluginLoader {

	/**
	 * Constructor
	 *
	 * @param pluginManager
	 */
	public RaaPDefaultPluginLoader(PluginManager pluginManager) {
		super(pluginManager);
	}

	@Override
	protected PluginClassLoader createPluginClassLoader(Path pluginPath, PluginDescriptor pluginDescriptor) {
		return new RaaPPluginClassLoader(pluginManager, pluginDescriptor, getClass().getClassLoader());
	}

}