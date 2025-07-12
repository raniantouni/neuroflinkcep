/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.settings;/*
 * Copyright (C) 2016-2022 RapidMiner GmbH
 */

/**
 * @author Fabian Temme
 * @since 0.6.2
 */
public class OptimizerMessage {
	public static final String OPTIMIZATION_COMPLETED_ACTION = "OPTIMIZATION_COMPLETED";

	private String action;

	private String id;

	private OptimizerMessage(){
		id = null;
		action = null;
	}

	public OptimizerMessage(String action, String id) {
		this.action = action;
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public String getAction() {
		return action;
	}
}
