/*
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.raap.util;

import org.pf4j.CompoundPluginLoader;
import org.pf4j.DevelopmentPluginLoader;
import org.pf4j.PluginLoader;
import org.pf4j.ZipPluginManager;


/**
 * RaaP = "RapidMiner-as-a-Plugin classloader" specialization of the extended class.
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public class RaaPZipPluginManager extends ZipPluginManager {

	@Override
	protected PluginLoader createPluginLoader() {
		return new CompoundPluginLoader()
			.add(new DevelopmentPluginLoader(this), this::isDevelopment)
			.add(new RaaPDefaultPluginLoader(this), this::isNotDevelopment);
	}

}