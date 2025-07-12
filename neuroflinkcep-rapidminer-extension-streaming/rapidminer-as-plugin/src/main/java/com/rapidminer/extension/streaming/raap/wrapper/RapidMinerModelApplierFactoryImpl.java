/*
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.raap.wrapper;

import java.io.InputStream;

import org.pf4j.Extension;

import com.rapidminer.extension.streaming.raap.RapidMinerModelApplier;
import com.rapidminer.extension.streaming.raap.RapidMinerModelApplierFactory;


/**
 * Implementation for the functionality.
 * This class does actually create RapidMiner model applier(s).
 *
 * @author Mate Torok
 * @since 0.4.0
 */
@Extension
public class RapidMinerModelApplierFactoryImpl implements RapidMinerModelApplierFactory {

	@Override
	public RapidMinerModelApplier create(InputStream modelStream) throws Exception {
		return new RapidMinerModelApplierImpl(modelStream);
	}

}