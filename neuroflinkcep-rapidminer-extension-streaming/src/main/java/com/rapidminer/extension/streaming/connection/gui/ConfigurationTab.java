/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.connection.gui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.rapidminer.connection.gui.AbstractConnectionGUI;
import com.rapidminer.connection.gui.components.InjectedParameterPlaceholderLabel;
import com.rapidminer.connection.gui.model.ConnectionModel;
import com.rapidminer.connection.gui.model.ConnectionParameterGroupModel;
import com.rapidminer.connection.gui.model.ConnectionParameterModel;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.tools.I18N;

import javafx.beans.value.ChangeListener;

import org.apache.commons.lang.StringUtils;


/**
 * Panel for configuring arbitrary properties for connections
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public class ConfigurationTab extends JPanel {

	/**
	 * Class for representing a "property" (= key-value)
	 */
	private static final class Property {

		private String key;
		private String value;

		private Property(String key, String value) {
			this.key = key;
			this.value = value;
		}

		private String getKey() {
			return key;
		}

		private void setKey(String key) {
			this.key = key;
		}

		private String getValue() {
			return value;
		}

		private void setValue(String value) {
			this.value = value;
		}

	}

	/**
	 * Class for holding the "properties" (= key-values)
	 */
	private static final class PropertyModel {

		private final List<Property> properties = Collections.synchronizedList(new ArrayList<>());

		private int getSize() {
			return properties.size();
		}

		private List<Property> getProperties() {
			return properties;
		}

		private Property getProperty(int index) {
			return properties.get(index);
		}

		private void addProperty(String key, String value) {
			properties.add(new Property(key, value));
		}

		private void removeProperty(int index) {
			properties.remove(index);
		}

		private void setKey(int index, String key) {
			properties.get(index).setKey(key);
		}

		private void setValue(int index, String value) {
			properties.get(index).setValue(value);
		}

	}

	private final PropertyModel properties;

	private final ConnectionModel model;

	private final ConnectionParameterGroupModel groupModel;

	private final JScrollPane scrollPane;

	private final JPanel rowPanel;

	private final ChangeListener<String> injectionListener;

	/**
	 * Constructor for creating the UI tab holding key-value properties
	 *
	 * @param groupName
	 * @param model
	 */
	ConfigurationTab(String groupName, ConnectionModel model) {
		super(new BorderLayout());

		this.model = model;
		this.groupModel = model.getOrCreateParameterGroup(groupName);
		this.properties = new PropertyModel();
		this.injectionListener = (observableValue, s, t1) -> updateRows();

		JPanel rootPanel = new JPanel(new GridBagLayout());
		rootPanel.setBorder(AbstractConnectionGUI.DEFAULT_PANEL_BORDER);

		// prepare the property-model
		fillPropertyModel();

		// layout UI
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets(0, 0, 10, 0);
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;

		this.rowPanel = new JPanel(new GridBagLayout());
		updateRows();
		rootPanel.add(rowPanel, gbc);

		// If the view is in EDIT mode
		if (model.isEditable()) {
			// add button
			gbc.gridy += 1;
			gbc.weighty = 0.0;
			gbc.fill = GridBagConstraints.NONE;
			gbc.anchor = GridBagConstraints.EAST;
			rootPanel.add(
				new JButton(
					new ResourceAction(false, "connection.parameter.add") {
						@Override
						protected void loggedActionPerformed(ActionEvent e) {
							properties.addProperty(StringUtils.EMPTY, StringUtils.EMPTY);
							fireStructureChanged();
							scrollPane
								.getViewport()
								.scrollRectToVisible(new Rectangle(1, rootPanel.getHeight() + 50, 1, 1));
						}
					}),
				gbc);
		}

		scrollPane = new ExtendedJScrollPane(rootPanel);
		scrollPane.setBorder(null);
		add(scrollPane, BorderLayout.CENTER);
	}

	/**
	 * Fills the property-model with the connection parameters
	 */
	private void fillPropertyModel() {
		for (ConnectionParameterModel parameter : groupModel.getParameters()) {
			properties.addProperty(parameter.getName(), parameter.getValue());
			parameter.injectorNameProperty().addListener(injectionListener);
		}
	}

	/**
	 * Updates all rows in the UI, based on the property model content
	 */
	private void updateRows() {
		rowPanel.removeAll();
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 5, 0);

		// Create UI rows for properties 1-by-1
		for (int i = 0; i < properties.getSize(); i++) {
			JPanel propRow = createRow(i, properties.getProperty(i));
			rowPanel.add(propRow, gbc);
			gbc.gridy += 1;
		}

		// filler at bottom
		gbc.gridy += 1;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.VERTICAL;
		rowPanel.add(new JLabel(), gbc);

		rowPanel.revalidate();
		rowPanel.repaint();
	}

	/**
	 * Fire if the structure of properties has changed (prop added/removed).
	 * Maps the content of the property model to the actual connection model.
	 */
	private void fireStructureChanged() {
		updateRows();
		fireContentChanged();
	}

	/**
	 * Fire only if the content of the existing property model has changed (no structural change).
	 * This method will map the content of the property model to the actual connection model.
	 * <p>
	 * The reason for this to exist is that in edit state we can have multiple properties with the same name. That is
	 * however strictly forbidden by the connection framework and the group models (for good reason), so we can only
	 * indirectly map to them here.
	 * </p>
	 */
	private void fireContentChanged() {
		// Bring local property model and the parameter group model in sync.
		// Iterate through the properties, filter out empty ones and make sure to 1 key only belongs 1 value (last one)
		for (Property property : properties.getProperties()) {
			String key = property.getKey();
			String value = property.getValue();

			if (StringUtils.trimToNull(key) == null) {
				continue;
			}

			ConnectionParameterModel existingParameter = groupModel.getParameter(key);

			// if we don't have that parameter yet, add it
			if (existingParameter == null) {
				groupModel.addOrSetParameter(key, value, false, null, true);
				groupModel.getParameter(key).injectorNameProperty().addListener(injectionListener);
			} else {
				existingParameter.setValue(value);
			}
		}
	}

	/**
	 * Removes the parameter from the group model specified by the given key.
	 * If it does not exist, nothing happens.
	 * Also removes listeners on the parameter.
	 *
	 * @param key
	 */
	private void removeParameter(String key) {
		ConnectionParameterModel paramToBeRemoved = groupModel.getParameter(key);
		if (paramToBeRemoved != null) {
			paramToBeRemoved.injectorNameProperty().removeListener(injectionListener);
			groupModel.removeParameter(paramToBeRemoved.getName());
		}
	}

	/**
	 * Creates a pre-filled row panel for the given parameter, or creates an empty row.
	 *
	 * @param index    the row index
	 * @param property the Property containing the key and the value for this row
	 * @return the row panel
	 */
	private JPanel createRow(int index, Property property) {
		JComponent keyComp = createKeyComponent(index, property);
		JComponent valueComp = createValueComponent(index, property);

		JPanel rowPanel = new JPanel(new GridBagLayout());
		JPanel innerRowPanel = new JPanel(new GridLayout(1, 2, 10, 0));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(0, 0, 0, 0);

		innerRowPanel.add(keyComp);
		innerRowPanel.add(valueComp);
		rowPanel.add(innerRowPanel, gbc);

		// In EDIT mode there needs to be a DELETE button at the end
		if (model.isEditable()) {
			gbc.gridx += 1;
			gbc.weightx = 0.0;
			gbc.fill = GridBagConstraints.NONE;
			gbc.insets = new Insets(0, 10, 0, 0);
			JButton deleteButton = new JButton(new ResourceAction(false, "connection.parameter.remove") {
				@Override
				protected void loggedActionPerformed(ActionEvent e) {
					// Removes parameter from connection model
					removeParameter(properties.getProperty(index).getKey());
					// Removes parameter from our property model
					properties.removeProperty(index);
					// Re-create rows
					fireStructureChanged();
				}
			});
			deleteButton.setContentAreaFilled(false);
			deleteButton.setBorderPainted(false);
			rowPanel.add(deleteButton, gbc);
		}

		return rowPanel;
	}

	/**
	 * Creates UI component for the "key" field
	 *
	 * @param index
	 * @param property
	 * @return newly created UI component
	 */
	private JComponent createKeyComponent(int index, Property property) {
		String key = property.getKey();

		// If in EDIT mode
		if (model.isEditable()) {
			JTextField keyField = new JTextField(15);
			keyField.setText(key);
			keyField.setEnabled(model.isEditable());
			keyField.setToolTipText(I18N.getGUILabel("connection.property.key.tip"));
			keyField.getDocument().addDocumentListener(
				new DocumentListener() {

					@Override
					public void insertUpdate(DocumentEvent e) {
						updateValue();
					}

					@Override
					public void removeUpdate(DocumentEvent e) {
						updateValue();
					}

					@Override
					public void changedUpdate(DocumentEvent e) {
						updateValue();
					}

					private void updateValue() {
						String newKey = keyField.getText();
						String existingKey = properties.getProperty(index).getKey();
						if (existingKey == null || !existingKey.equals(newKey)) {
							removeParameter(existingKey);
							properties.setKey(index, newKey);
							fireContentChanged();
						}
					}

				});

			return keyField;
		} else {
			return new JLabel(key);
		}
	}

	/**
	 * Creates UI component for the "value" field
	 *
	 * @param index
	 * @param property
	 * @return newly created UI component
	 */
	private JComponent createValueComponent(int index, Property property) {
		String key = property.getKey();
		String value = property.getValue();

		// Parameter injection, special scenario
		ConnectionParameterModel parameter = groupModel.getParameter(key);
		if (parameter != null && parameter.isInjected()) {
			return new InjectedParameterPlaceholderLabel(parameter);
		}

		// Tooltips
		if (model.isEditable()) {
			JTextField valueField = new JTextField(15);
			valueField.setText(value);
			valueField.setEnabled(model.isEditable());
			valueField.setToolTipText(I18N.getGUILabel("connection.property.value.tip"));
			valueField.getDocument().addDocumentListener(
				new DocumentListener() {

					@Override
					public void insertUpdate(DocumentEvent e) {
						updateValue();
					}

					@Override
					public void removeUpdate(DocumentEvent e) {
						updateValue();
					}

					@Override
					public void changedUpdate(DocumentEvent e) {
						updateValue();
					}

					private void updateValue() {
						String newValue = valueField.getText();
						String existingValue = properties.getProperty(index).getValue();
						if (existingValue == null || !existingValue.equals(newValue)) {
							properties.setValue(index, newValue);
							fireContentChanged();
						}
					}

				});

			return valueField;
		} else {
			return new JLabel(value);
		}
	}

}