/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import com.google.common.collect.Lists;
import com.rapidminer.extension.streaming.raap.DefaultSettingValue;
import com.rapidminer.extension.streaming.raap.RapidMinerModelApplier;
import com.rapidminer.extension.streaming.raap.RapidMinerModelApplierFactory;
import com.rapidminer.extension.streaming.raap.RapidMinerPluginManager;
import com.rapidminer.extension.streaming.raap.SettingKey;
import com.rapidminer.extension.streaming.raap.data.DataTable;
import com.rapidminer.extension.streaming.raap.data.DataTableColumn;
import com.rapidminer.extension.streaming.raap.data.DataType;


/**
 * Utility class responsible to ease the usage of the RapidMiner-as-a-Plugin common functionalities
 * (e.g.: data conversion, model initialization, model application).
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public final class RaaPHandler {

	private RaaPHandler() {
		// Utility
	}

	/**
	 * Initializes RaaP and gives back a model applier instance that uses the given model, referred by the parameter
	 *
	 * @param rmHome for setting the Home folder for RapidMiner (~installation)
	 * @param rmUserHome for setting the User-Home for .RapidMiner
	 * @param modelFileName artifact name for the model by which the ResourceManager will find it
	 * @return model applier instance
	 * @throws Exception if something goes wrong during initialization
	 */
	public static RapidMinerModelApplier initializeApplier(String rmHome,
														   String rmUserHome,
														   String modelFileName) throws Exception {
		System.setProperty(SettingKey.CONF_RM_HOME, rmHome);
		System.setProperty(SettingKey.CONF_RM_USER_HOME, rmUserHome);
		System.setProperty(SettingKey.CONF_PLUGIN_ID, DefaultSettingValue.RM_PLUGIN_ID);
		System.setProperty(SettingKey.CONF_PLUGIN_PATH, rmHome + "/" + DefaultSettingValue.RM_PLUGIN_ARTIFACT_PATH);

		RapidMinerPluginManager.initialize();
		RapidMinerModelApplierFactory factory = RapidMinerPluginManager.getModelApplierFactory();

		InputStream modelStream = ResourceManager.retrieve(modelFileName);
		return factory.create(modelStream);
	}

	/**
	 * This method applies the model on the input data.
	 *
	 *
	 * @param modelApplier wrapper around RapidMiner model application
	 * @param typeMap to be able to transform the JSON data correctly
	 * @param data input
	 * @return input 'data' extended/transformed
	 * @throws Exception
	 */
	public static JSONObject apply(RapidMinerModelApplier modelApplier,
								   Map<String, DataType> typeMap,
								   JSONObject data) throws Exception {
		DataTable input = json2Table(data, typeMap);
		DataTable result = modelApplier.apply(input);

		JSONObject scored = table2Json(result);

		for (String key : scored.keySet()) {
			data.put(key, scored.get(key));
		}

		return data;
	}

	/**
	 * This method transforms a JSON object into a DataTable (driven by the typeMap parameter).
	 *
	 * @param data JSON data
	 * @param typeMap JSON field to DataType mapping (by field-name)
	 * @return DataTable object from the input JSON
	 */
	public static DataTable json2Table(JSONObject data, Map<String, DataType> typeMap) {
		List<DataTableColumn> columns = Lists.newArrayListWithCapacity(typeMap.size());

		for (Map.Entry<String, DataType> entry : typeMap.entrySet()) {
			String key = entry.getKey();
			DataType type = entry.getValue();
			Object value = data.get(key);

			columns.add(new DataTableColumn(key, type, new Object[]{value}));
		}

		return new DataTable(columns.toArray(new DataTableColumn[0]));
	}

	/**
	 * This method transforms a DataTable into a JSON object using <b>the very first values only</b>.
	 *
	 * @param data input container
	 * @return JSON object built from the first values in the columns
	 */
	public static JSONObject table2Json(DataTable data) {
		JSONObject result = new JSONObject();
		DataTableColumn[] columns = data.getColumns();

		for (DataTableColumn col : columns) {
			String key = col.getName();
			Object value = col.getData().get(0);

			result.put(key, value);
		}

		return result;
	}

}