/*
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.raap.util;

import org.pf4j.ClassLoadingStrategy;
import org.pf4j.PluginClassLoader;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginManager;


/**
 * This class, RaaP = "RapidMiner-as-a-Plugin classloader", adjusts the functionality provided by
 * {@link PluginClassLoader}.
 * Why is this adjustment necessary ?
 * RapidMiner's own plugin-class-loader overrides the following method {@link ClassLoader#loadClass(String, boolean)}.
 * That method, at some point delegates to it's parent's method with the same signature.
 * This parent will ideally be our plugin-class-loader (since we are the ones who loaded RapidMiner in the first place).
 * However, PF4J's implementation overrides {@link ClassLoader#loadClass(String)} only.
 * This means, as soon as RapidMiner's plugin-class-loader delegates to its parent (our plugin-class-loader), we
 * lose control over class-loading.
 * This is not good, since we want to have a reversed / child-first policy implemented.
 * The reason for that is that we want to enable RapidMiner to use any library it desires, even if the host system
 * already uses a different version of the same library (e.g.: Flink using apache.commons3 but a non-compatible version).
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public class RaaPPluginClassLoader extends PluginClassLoader {

	private static final String _JAVA_PACKAGE_PREFIX = "java.";
	private static final String _PLUGIN_PACKAGE_PREFIX = "org.pf4j.";

	/**
	 * Constructor
	 *
	 * @param pluginManager
	 * @param pluginDescriptor
	 * @param parent
	 */
	public RaaPPluginClassLoader(PluginManager pluginManager, PluginDescriptor pluginDescriptor, ClassLoader parent) {
		super(pluginManager, pluginDescriptor, parent);
	}

	@Override
	public Class<?> loadClass(String className) throws ClassNotFoundException {
		return loadClass(className, false);
	}

	@Override
	public Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException {
		synchronized (getClassLoadingLock(className)) {
			// first check whether it's a system class, delegate to the system loader
			if (className.startsWith(_JAVA_PACKAGE_PREFIX)) {
				return findSystemClass(className);
			}

			// if the class is part of the plugin engine use parent class loader
			if (className.startsWith(_PLUGIN_PACKAGE_PREFIX)) {
				return super.loadClass(className, resolve);
			}

			// second check whether it's already been loaded
			Class<?> loadedClass = findLoadedClass(className);
			if (loadedClass != null) {
				return loadedClass;
			}

			for (ClassLoadingStrategy.Source classLoadingSource : ClassLoadingStrategy.PDA.getSources()) {
				try {
					Class<?> c = null;
					switch (classLoadingSource) {
						case APPLICATION:
							c = super.loadClass(className, resolve);
							break;
						case PLUGIN:
							c = findClass(className);
							break;
						case DEPENDENCIES:
							c = loadClassFromDependencies(className);
							break;
					}

					if (c != null) {
						return c;
					}
				} catch (ClassNotFoundException ignored) {}
			}

			throw new ClassNotFoundException(className);
		}
	}

}