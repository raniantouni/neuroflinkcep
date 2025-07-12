/**
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.raap.bridge;

import com.rapidminer.extension.streaming.raap.data.DataType;
import com.rapidminer.tools.Ontology;


/**
 * Utility class to handle conversion between the 2 realms' ontology/types: RaaP vs RapidMiner
 *
 * @author Mate Torok
 * @since 0.4.0
 */
public class DataTypeConverter {

	/**
	 * Translates our RapidMiner type into ours
	 * @param type to be translated
	 * @return equivalent in our realm/domain
	 */
	public static DataType getDataType(int type) throws TypeConversionException {
		switch (type) {
			case Ontology.NOMINAL:
				return DataType.NOMINAL;
			case Ontology.STRING:
				return DataType.STRING;
			case Ontology.POLYNOMINAL:
				return DataType.POLYNOMINAL;
			case Ontology.INTEGER:
				return DataType.INTEGER;
			case Ontology.NUMERICAL:
				return DataType.NUMERICAL;
			case Ontology.REAL:
				return DataType.REAL;
			case Ontology.BINOMINAL:
				return DataType.BINOMINAL;
			default:
				throw new TypeConversionException(String.format("The ontology '%d' is not supported.", type));
		}
	}

	/**
	 * Translates our domain type into RapidMiner's
	 * @param type to be translated
	 * @return equivalent in the RapidMiner realm/domain
	 */
	public static int getOntology(DataType type) throws TypeConversionException {
		switch (type) {
			case NUMERICAL:
				return Ontology.NUMERICAL;
			case INTEGER:
				return Ontology.INTEGER;
			case REAL:
				return Ontology.REAL;
			case NOMINAL:
				return Ontology.NOMINAL;
			case STRING:
				return Ontology.STRING;
			case POLYNOMINAL:
				return Ontology.POLYNOMINAL;
			case BINOMINAL:
				return Ontology.BINOMINAL;
			default:
				throw new TypeConversionException(String.format("The type '%s' is not supported.", type));
		}
	}

	private DataTypeConverter() {}

}