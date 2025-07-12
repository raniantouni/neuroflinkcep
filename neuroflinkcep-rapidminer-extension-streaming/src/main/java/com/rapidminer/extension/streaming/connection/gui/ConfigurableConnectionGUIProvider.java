/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.connection.gui;

import java.awt.Window;

import com.google.common.collect.Maps;
import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.gui.AbstractConnectionGUI;
import com.rapidminer.connection.gui.ConnectionGUIProvider;
import com.rapidminer.extension.streaming.connection.FlinkConnectionHandler;
import com.rapidminer.extension.streaming.connection.SparkConnectionHandler;
import com.rapidminer.extension.streaming.connection.infore.MaritimeConnectionHandler;
import com.rapidminer.repository.RepositoryLocation;


/**
 * Connection GUI provider
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class ConfigurableConnectionGUIProvider implements ConnectionGUIProvider {

	@Override
	public AbstractConnectionGUI edit(Window parent,
									  ConnectionInformation connection,
									  RepositoryLocation location,
									  boolean editable) {
		switch (connection.getConfiguration().getType()) {
			case SparkConnectionHandler.TYPE:
				return new ConfigurableConnectionGUI(
					parent,
					connection,
					location,
					editable,
					SparkConnectionHandler.getINSTANCE().menuGroups());
			case FlinkConnectionHandler.TYPE:
				return new ConfigurableConnectionGUI(
					parent,
					connection,
					location,
					editable,
					FlinkConnectionHandler.getINSTANCE().menuGroups());
			case MaritimeConnectionHandler.TYPE:
				return new ConfigurableConnectionGUI(
					parent,
					connection,
					location,
					editable,
					MaritimeConnectionHandler.getINSTANCE().menuGroups());
			default:
				return null;
		}
	}

}