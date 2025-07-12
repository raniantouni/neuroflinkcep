/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.deploy.management.db.infore;/*
 * Copyright (C) 2016-2022 RapidMiner GmbH
 */

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rapidminer.extension.streaming.deploy.management.db.StreamingEndpoint;
import com.rapidminer.extension.streaming.optimizer.connection.OptimizerConnection;
import com.rapidminer.extension.streaming.optimizer.settings.Network;
import com.rapidminer.extension.streaming.optimizer.settings.OptimizerResponse;


/**
 * @author Fabian Temme
 * @since 0.6.2
 */
public class InforeOptimizerStreamingEndpoint extends StreamingEndpoint {

	@JsonProperty
	private String connectionLocation;

	@JsonIgnore
	private final OptimizerConnection optimizerConnection;

	@JsonProperty
	private String requestId;

	@JsonProperty
	private long timeOutSessionConnect;

	@JsonProperty
	private int pollingTimeOut;

	@JsonProperty
	private String streamingOptimizationOperatorName;

	@JsonProperty
	private Network network;

	@JsonProperty
	private OptimizerResponse deployedPlan = null;

	@JsonProperty
	private OptimizerResponse newPlan = null;

	public InforeOptimizerStreamingEndpoint() {
		this(null, null, null, 30, 10, null, null);
	}

	public InforeOptimizerStreamingEndpoint(String connectionLocation, OptimizerConnection optimizerConnection,
											String requestId, long timeOutSessionConnect, int pollingTimeOut, String streamingOptimizationOperatorName, Network network) {
		super(null, null);
		this.connectionLocation = connectionLocation;
		this.optimizerConnection = optimizerConnection;
		this.requestId = requestId;
		this.timeOutSessionConnect = timeOutSessionConnect;
		this.pollingTimeOut = pollingTimeOut;
		this.streamingOptimizationOperatorName = streamingOptimizationOperatorName;
		this.network = network;
	}

	public boolean isOptimizationFinished() {
		return optimizerConnection.isOptimizationFinished();
	}

	public boolean newPlanAvailable(){
		checkForNewPlan();
		return newPlan != null;
	}

	public boolean deployedPlanAvailable(){
		return deployedPlan != null;
	}

	public OptimizerResponse waitForNewPlanForDeployment(int pollingTimeOut) throws InterruptedException {
		OptimizerResponse response = optimizerConnection.waitForOptimizerPlan(pollingTimeOut);
		if (response != null){
			deployedPlan = response;
			newPlan = null;
		}
		return response;
	}

	@JsonIgnore
	public OptimizerResponse getNewPlanForDeployment() {
		checkForNewPlan();
		if (newPlan == null){
			return null;
		}
		deployedPlan = newPlan;
		newPlan = null;
		return deployedPlan;
	}

	public OptimizerResponse getDeployedPlan() {
		return deployedPlan;
	}

	public void checkForNewPlan(){
		if (optimizerConnection != null && optimizerConnection.newOptimizationResultsAvailable()){
			OptimizerResponse candidate = optimizerConnection.getLatestOptimizedPlan();
			if (candidate == null){
				return;
			}
			String latestId = null;
			Date latestModified = null;
			if (newPlan != null){
				latestId = newPlan.getId();
				latestModified = newPlan.getModifiedAt();
			} else if (deployedPlan != null){
				latestId = deployedPlan.getId();
				latestModified = deployedPlan.getModifiedAt();
			}
			if (latestId != null){
				if (!latestId.equals(candidate.getId()) && latestModified.before(candidate.getModifiedAt())){
					newPlan = candidate;
				}
			}
		}
	}

	public String getStreamingOptimizationOperatorName() {
		return streamingOptimizationOperatorName;
	}

	public OptimizerConnection getOptimizerConnection() {
		return optimizerConnection;
	}

	public String getConnectionLocation() {
		return connectionLocation;
	}

	public String getRequestId() {
		return requestId;
	}

	public int getPollingTimeOut() {
		return pollingTimeOut;
	}

	public long getTimeOutSessionConnect() {
		return timeOutSessionConnect;
	}

	public Network getNetwork() {
		return network;
	}

	@Override
	public int hashCode() {
		return Objects.hash(connectionLocation);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		InforeOptimizerStreamingEndpoint that = (InforeOptimizerStreamingEndpoint) o;
		return Objects.equals(this.connectionLocation, that.connectionLocation);
	}


	public String newPlanInfos() {
		if (newPlan == null){
			return null;
		}
		return "Deploy new plan (id: " + newPlan.getId() + " ; created: " + new SimpleDateFormat("MMM d, yyyy, " +
			"hh:mm:ss a").format(newPlan.getModifiedAt()) + ")";
	}
}
