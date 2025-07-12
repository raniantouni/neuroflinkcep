/**
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.raap.bridge;

import static com.rapidminer.example.table.DataRowFactory.POINT_AS_DECIMAL_CHARACTER;
import static com.rapidminer.example.table.DataRowFactory.TYPE_DOUBLE_ARRAY;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.extension.streaming.raap.data.DataTable;
import com.rapidminer.extension.streaming.raap.data.DataTableColumn;
import com.rapidminer.extension.streaming.raap.data.DataType;


/**
 * Utility class to handle data conversion between the 2 realms' containers (DataTable and ExampleSet):
 * RaaP vs RapidMiner
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public final class DataTableConverter {

	/**
	 * Transforms RapidMiner's ExampleSet object into a data-table (RaaP domain)
	 * @param es ExampleSet to be transformed
	 * @return newly built DataTable
	 */
	public static DataTable exampleSet2DataTable(ExampleSet es) throws TypeConversionException {
		Iterator<Example> iterator = es.iterator();
		Iterator<Attribute> attrIterator = es.getAttributes().allAttributes();

		List<DataTableColumn> columns = new ArrayList<>();
		List<Attribute> attributes = new ArrayList<>();

		// Create columns
		while (attrIterator.hasNext()) {
			Attribute attribute = attrIterator.next();
			attributes.add(attribute);
			columns.add(new DataTableColumn(attribute.getName(), DataTypeConverter.getDataType(attribute.getValueType())));
		}

		// Fill columns (with data)
		while (iterator.hasNext()) {
			Example example = iterator.next();
			int i = 0;

			for (Attribute attr : attributes) {
				if (attr.isNominal()) {
					columns.get(i).addData(example.getValueAsString(attr));
				} else {
					columns.get(i).addData(example.getValue(attr));
				}

				i++;
			}
		}

		return new DataTable(columns.toArray(new DataTableColumn[0]));
	}

	/**
	 * Transforms the RaaP domain object (table) into a RapidMiner ExampleSet to make the data model compliant
	 * @param dt data-table to be transformed
	 * @return newly built ExampleSet
	 */
	public static ExampleSet dataTable2ExampleSet(DataTable dt) throws TypeConversionException {
		DataRowFactory rowFactory = new DataRowFactory(TYPE_DOUBLE_ARRAY, POINT_AS_DECIMAL_CHARACTER);

		DataTableColumn[] columns = dt.getColumns();
		List<Attribute> attributeList = new ArrayList<>();
		int rows = 0, cols = columns.length;

		// Create attributes
		for (DataTableColumn col : columns) {
			rows = col.getData().size();
			attributeList.add(AttributeFactory.createAttribute(col.getName(), DataTypeConverter.getOntology(col.getType())));
		}

		Attribute[] attributes = attributeList.toArray(new Attribute[0]);
		ExampleSetBuilder esBuilder = ExampleSets.from(attributes);

		// Create rows (with data)
		for (int i = 0; i < rows; i++) {
			Object[] rowData = new Object[cols];
			for (int j = 0; j < cols; j++) {
				Object raw = columns[j].getData().get(i);
				rowData[j] = readAsString(columns[j].getType()) ? raw.toString() : raw;
			}
			esBuilder.addDataRow(rowFactory.create(rowData, attributes));
		}

		return esBuilder.build();
	}

	/**
	 * @param type of the column
	 * @return true if the value based on the column type should be read as a string (plays a role in how ExampleSet
	 * rows get built)
	 */
	private static boolean readAsString(DataType type) {
		switch (type) {
			case STRING:
			case NOMINAL:
			case BINOMINAL:
			case POLYNOMINAL:
				return true;
			default:
				return false;
		}
	}

	private DataTableConverter() {}

}