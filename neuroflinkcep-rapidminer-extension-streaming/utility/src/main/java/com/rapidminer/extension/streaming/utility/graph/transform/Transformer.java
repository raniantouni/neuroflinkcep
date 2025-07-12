/*
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2019-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.utility.graph.transform;

import com.rapidminer.extension.streaming.utility.graph.StreamConsumer;
import com.rapidminer.extension.streaming.utility.graph.StreamProducer;


/**
 * Base class for all stream transformers
 *
 * @author Mate Torok
 * @since 0.1.0
 */
public abstract class Transformer implements StreamProducer, StreamConsumer {

	private final long id;

	protected Transformer(long id) {
		this.id = id;
	}

	@Override
	public long getId() {
		return id;
	}

}