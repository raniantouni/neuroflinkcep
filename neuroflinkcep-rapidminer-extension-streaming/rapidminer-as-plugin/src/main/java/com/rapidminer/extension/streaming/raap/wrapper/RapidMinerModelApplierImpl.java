/*
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.raap.wrapper;

import java.io.InputStream;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.extension.streaming.raap.RapidMinerModelApplier;
import com.rapidminer.extension.streaming.raap.bridge.DataTableConverter;
import com.rapidminer.extension.streaming.raap.data.DataTable;
import com.rapidminer.extension.streaming.raap.util.ClassLoaderSwapper;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.tools.IOObjectSerializer;


/**
 * Implementation for the functionality.
 * This class takes care of data transformation (there and back) and model application.
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public class RapidMinerModelApplierImpl implements RapidMinerModelApplier {

	private static final long serialVersionUID = -6285384128298435305L;

	private final Model model;

	public RapidMinerModelApplierImpl(InputStream modelStream) throws Exception {
		try (ClassLoaderSwapper ignored = ClassLoaderSwapper.withContextClassLoader(getClass().getClassLoader())) {
			model = (Model) IOObjectSerializer.getInstance().deserialize(modelStream);
		}
	}

	@Override
	public DataTable apply(DataTable input) throws Exception {
		try (ClassLoaderSwapper ignored = ClassLoaderSwapper.withContextClassLoader(getClass().getClassLoader())) {
			ExampleSet esInput = DataTableConverter.dataTable2ExampleSet(input);
			ExampleSet esResult = model.apply(esInput);

			return DataTableConverter.exampleSet2DataTable(esResult);
		}
	}

}