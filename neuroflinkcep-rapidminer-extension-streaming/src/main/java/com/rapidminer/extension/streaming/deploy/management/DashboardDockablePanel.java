/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.management;

import static com.rapidminer.extension.streaming.deploy.management.DashboardAPIHandler.WINDOW_KEY;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.rapidminer.extension.browser.BrowserFactory;
import com.rapidminer.extension.browser.gui.BrowserComponent;
import com.rapidminer.extension.browser.gui.HTML5ComponentFactory;
import com.rapidminer.extension.browser.util.BrowserUtilities;
import com.rapidminer.gui.flow.ProcessPanel;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.tools.LogService;
import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.browser.event.ConsoleMessageReceived;
import com.teamdev.jxbrowser.engine.EngineOptions;
import com.teamdev.jxbrowser.engine.RenderingMode;
import com.teamdev.jxbrowser.engine.event.EngineCrashed;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;
import com.vlsolutions.swing.docking.RelativeDockablePosition;


/**
 * Dockable view to show the dashboard panel
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class DashboardDockablePanel extends JPanel implements Dockable {

	private static final Logger LOGGER = LogService.getRoot();

	private static final String KEY_STREAMING_DASHBOARD = "streaming_dashboard";

	private static final String RESOURCE_PREFIX = "resources://";

	private static final String INDEX_PATH = "com/rapidminer/extension/resources/html5/dashboard/dist/index.html";

	private static final String DEBUG_PATH = "http://localhost:3000/";

	private static final DockKey DOCK_KEY = new ResourceDockKey(KEY_STREAMING_DASHBOARD);

	private final DashboardAPIHandler apiHandler = new DashboardAPIHandler();

	private BrowserComponent browserComponent;

	private Browser browser;

	public DashboardDockablePanel() {
		DOCK_KEY.putProperty(ResourceDockKey.PROPERTY_KEY_NEXT_TO_DOCKABLE, ProcessPanel.PROCESS_PANEL_DOCK_KEY);
		DOCK_KEY.putProperty(ResourceDockKey.PROPERTY_KEY_DEFAULT_FALLBACK_LOCATION, RelativeDockablePosition.RIGHT);

		setLayout(new BorderLayout());

		setupBrowser();
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}

	@Override
	public Component getComponent() {
		return this;
	}

	/**
	 * Creates and sets up the browser component (event listeners, etc.)
	 */
	private void setupBrowser() {
		// create and setup browser
		if (BrowserUtilities.WEBAPP_DEVELOPMENT_ACTIVATED) {
			browser = BrowserFactory.createEngine(EngineOptions.newBuilder(RenderingMode.HARDWARE_ACCELERATED), "Developer Tools").newBrowser();
		} else {
			browser = BrowserFactory.createBrowser();
		}

		browser.engine().on(EngineCrashed.class, event -> restoreUI());
		browser.on(ConsoleMessageReceived.class, e -> LOGGER.log(Level.FINE, "Dashboard: " + e.consoleMessage().message()));

		// register API handler
		BrowserUtilities.registerJavaCallbackObject(browser, WINDOW_KEY, apiHandler);

		// set up request handling (we want to allow links to remote dashboards)
		BrowserUtilities.prepareResourceURLLoading(browser.engine(), true, true, Collections.emptyMap());

		// set browser component
		browserComponent = HTML5ComponentFactory.createBrowserComponent(browser);
		add(browserComponent.getComponent(), BorderLayout.CENTER);

		// load
		loadHTMLContent();
	}

	/**
	 * Removes the 'old' browserComponent (if existing) and recreates the appropriate browser components
	 */
	private void restoreUI() {
		// Remove old component
		SwingUtilities.invokeLater(() -> {
			LOGGER.fine("Restoring the dashboard");

			if (browserComponent != null) {
				remove(browserComponent.getComponent());
			}

			// Recreate browser mechanisms
			setupBrowser();

			revalidate();
		});
	}

	/**
	 * Loads the web-content into the browser
	 */
	private void loadHTMLContent() {
		if (browser != null) {
			SwingUtilities.invokeLater(() -> {
				if (BrowserUtilities.WEBAPP_DEVELOPMENT_ACTIVATED) {
					browser.mainFrame().ifPresent(f -> f.loadUrl(DEBUG_PATH));
				} else {
					browser.mainFrame().ifPresent(f -> f.loadUrl(RESOURCE_PREFIX + INDEX_PATH));
				}
			});
		}
	}

}