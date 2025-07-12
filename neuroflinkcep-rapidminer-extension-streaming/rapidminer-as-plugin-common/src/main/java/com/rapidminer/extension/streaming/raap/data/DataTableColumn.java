/*
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.raap.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Our domain-specific DTO level class for holding and passing around data in a RapidMiner independent way.
 * This class represents a column.
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public class DataTableColumn implements Serializable {

	private static final long serialVersionUID = 4097593597881299919L;

	private final String name;

	private final DataType type;

	private final List<Object> data;

	/**
	 * Constructor for creating an empty column
	 *
	 * @param name
	 * @param type
	 */
	public DataTableColumn(String name, DataType type) {
		this.name = name;
		this.type = type;
		this.data = new ArrayList<>();
	}

	/**
	 * Constructor for creating a potentially non-empty column (i.e. with data)
	 *
	 * @param name
	 * @param type
	 * @param data
	 */
	public DataTableColumn(String name, DataType type, Object[] data) {
		this.name = name;
		this.type = type;
		this.data = Arrays.asList(data);
	}

	/**
	 * @return name of the column
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return type of the column
	 */
	public DataType getType() {
		return type;
	}

	/**
	 * @return unmodifiable view of the data
	 */
	public List<Object> getData() {
		return Collections.unmodifiableList(data);
	}

	/**
	 * @param obj will be appended to the data list
	 */
	public void addData(Object obj) {
		data.add(obj);
	}

}