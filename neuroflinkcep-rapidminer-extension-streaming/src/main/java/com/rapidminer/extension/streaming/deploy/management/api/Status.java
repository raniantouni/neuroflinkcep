/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.management.api;


/**
 * Job/Workflow status/state
 *
 * @author Mate Torok
 * @since 0.2.0
 */
public enum Status {

	Unknown, Running, Stopping, Finished, Failed, NewPlanAvailable, DeployingNewPlan

}