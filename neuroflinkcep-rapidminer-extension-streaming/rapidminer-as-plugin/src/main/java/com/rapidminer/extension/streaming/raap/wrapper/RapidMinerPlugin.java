/*
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.raap.wrapper;

import static com.rapidminer.tools.PlatformUtilities.PROPERTY_RAPIDMINER_HOME;

import java.security.Policy;

import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;

import com.rapidminer.RapidMiner;
import com.rapidminer.extension.streaming.raap.SettingKey;
import com.rapidminer.extension.streaming.raap.util.ClassLoaderSwapper;
import com.rapidminer.gui.license.LicenseTools;
import com.rapidminer.license.LicenseManagerRegistry;
import com.rapidminer.license.internal.DefaultLicenseManager;
import com.rapidminer.security.PluginSandboxPolicy;
import com.rapidminer.security.PluginSecurityManager;
import com.rapidminer.settings.Settings;
import com.rapidminer.settings.SettingsConstants;
import com.rapidminer.tools.FileSystemService;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.PlatformUtilities;
import com.rapidminer.tools.ProxySettings;
import com.rapidminer.tools.net.UserProvidedTLSCertificateLoader;


/**
 * Main class in the "RapidMiner as a plugin" unit.
 * It takes care of the environment initialization (and clean-up if necessary).
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public class RapidMinerPlugin extends Plugin {

	/**
	 * Constructor to be used by plugin manager for plugin instantiation.
	 * Your plugins have to provide constructor with this exact signature to
	 * be successfully loaded by manager.
	 *
	 * @param wrapper
	 */
	public RapidMinerPlugin(PluginWrapper wrapper) {
		super(wrapper);
	}

	@Override
	public void start() {
		try (ClassLoaderSwapper ignored = ClassLoaderSwapper.withContextClassLoader(getClass().getClassLoader())) {
			initializeCore();
		}
	}

	@Override
	public void delete() {
		try (ClassLoaderSwapper ignored = ClassLoaderSwapper.withContextClassLoader(getClass().getClassLoader())) {
			RapidMiner.cleanup();
		}
	}

	/**
	 * Initializes the RapidMiner environment.
	 * For configuration via system properties: see {@link SettingKey}.
	 */
	private static void initializeCore() {
		// Set properties
		System.setProperty(PROPERTY_RAPIDMINER_HOME, System.getProperty(SettingKey.CONF_RM_HOME));
		System.setProperty("user.home", System.getProperty(SettingKey.CONF_RM_USER_HOME));

		Policy.setPolicy(new PluginSandboxPolicy());
		System.setSecurityManager(new PluginSecurityManager());

		Settings.setSetting(SettingsConstants.LOGGING_RESOURCE_FILE, "com.rapidminer.resources.i18n.LogMessages");
		PlatformUtilities.initialize();
		RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.COMMAND_LINE);

		if (RapidMiner.getExecutionMode().canAccessFilesystem()) {
			Settings.setSetting(SettingsConstants.EXECUTION_WORKING_DIRECTORY, FileSystemService.getStandardWorkingDir());
			Settings.setSetting(SettingsConstants.LOGGING_LOG_FILE, FileSystemService.getLogFile().getAbsolutePath());
		}

		UserProvidedTLSCertificateLoader.INSTANCE.init();
		ProxySettings.storeSystemSettings();
		LicenseManagerRegistry.INSTANCE.set(new DefaultLicenseManager());

		ParameterService.init();

		RapidMiner.init();

		LicenseManagerRegistry.INSTANCE.get().getAllActiveLicenses().forEach(LicenseTools::storeActiveLicenseProperties);
	}

}