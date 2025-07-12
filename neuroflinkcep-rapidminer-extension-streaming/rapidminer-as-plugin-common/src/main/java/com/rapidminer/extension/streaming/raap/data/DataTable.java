/*
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.raap.data;

import java.io.Serializable;
import java.util.Arrays;


/**
 * Our domain-specific DTO level root-class for holding and passing around data in a RapidMiner independent way
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public class DataTable implements Serializable {

	private static final long serialVersionUID = -2106018020386906983L;

	private final DataTableColumn[] columns;

	/**
	 * Constructor for wrapping the columns
	 *
	 * @param columns holding the data
	 */
	public DataTable(DataTableColumn[] columns) {
		this.columns = columns;

		// Sanity check
		checkColumns();
	}

	/**
	 * @return columns holding the data
	 */
	public DataTableColumn[] getColumns() {
		return columns;
	}

	/**
	 * Sanity checks on the columns
	 */
	private void checkColumns() {
		// Check names
		if (Arrays.stream(columns).anyMatch(col -> col.getName() == null)) {
			throw new IllegalArgumentException("Every column should have a name");
		}

		// Check types
		if (Arrays.stream(columns).anyMatch(col -> col.getType() == null)) {
			throw new IllegalArgumentException("Every column should have a type");
		}

		// Check data
		if (Arrays.stream(columns).anyMatch(col -> col.getData() == null)) {
			throw new IllegalArgumentException("Every column should have data in it");
		}

		// Check data lengths
		if (1 < Arrays.stream(columns).map(col -> col.getData().size()).distinct().count()) {
			throw new IllegalArgumentException("Every column should have the same length");
		}
	}

}