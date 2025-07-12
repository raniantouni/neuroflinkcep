/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.connection.gui;

import java.awt.Window;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JComponent;

import com.rapidminer.connection.ConnectionInformation;
import com.rapidminer.connection.gui.DefaultConnectionGUI;
import com.rapidminer.connection.gui.model.ConnectionModel;
import com.rapidminer.connection.gui.model.ConnectionParameterGroupModel;
import com.rapidminer.repository.RepositoryLocation;

import static java.util.Objects.isNull;


/**
 * GUI for arbitrarily configurable connections
 *
 * @author Mate Torok
 * @since 0.1.0
 */
class ConfigurableConnectionGUI extends DefaultConnectionGUI {

	/**
	 * Key: i18n key for the menu tab name, Value: true if this is a configurable property tab
	 */
	private final LinkedHashMap<String, Boolean> menuGroups;

	/**
	 * Constructor for the connection window
	 *
	 * @param parent
	 * @param connection
	 * @param location
	 * @param editable
	 * @param menuGroups
	 */
	ConfigurableConnectionGUI(Window parent,
							  ConnectionInformation connection,
							  RepositoryLocation location,
							  boolean editable,
							  LinkedHashMap<String, Boolean> menuGroups) {
		super(parent, connection, location, editable);
		this.menuGroups = menuGroups;
	}

	@Override
	protected List<ConnectionParameterGroupModel> getDisplayedGroups() {
		return menuGroups
			.keySet()
			.stream()
			.map(getConnectionModel()::getOrCreateParameterGroup)
			.collect(Collectors.toList());
	}

	@Override
	public JComponent getComponentForGroup(ConnectionParameterGroupModel groupModel, ConnectionModel connection) {
		String groupName = groupModel.getName();
		Boolean configurable = menuGroups.get(groupName);

		// If this menu-group was not pre-configured OR it is not configurable
		if (isNull(configurable) || !configurable) {
			return super.getComponentForGroup(groupModel, connection);
		} else {
			return new ConfigurationTab(groupName, connectionModel);
		}
	}

}