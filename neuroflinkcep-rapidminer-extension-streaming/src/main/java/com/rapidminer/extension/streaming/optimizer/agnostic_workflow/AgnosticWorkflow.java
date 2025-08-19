/**
 * RapidMiner Streaming Extension
 *
 * Copyright (C) 2020-2022 RapidMiner GmbH
 */
package com.rapidminer.extension.streaming.optimizer.agnostic_workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * This is a container class for an agnostic workflow. It contains the information for an inner
 * process of an operator. It is used to serialize the information to JSON, so that the INFORE
 * optimizer can process it.
 *
 * @author Fabian Temme
 * @since 0.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgnosticWorkflow {
	
	private String workflowName = null;
	private String enclosingOperatorName = null;
	private List<AWPort> innerSourcesPortsAndSchemas = null;
	private List<AWPort> innerSinksPortsAndSchemas = null;
	
	private List<AWOperatorConnection> operatorConnections = new ArrayList<>();
	private List<AWOperator> operators = new ArrayList<>();
	
	private List<AWPlacementSite> placementSites = new ArrayList<>();
	
	/**
	 * Creates a new {@link AgnosticWorkflow} instance with the fields set to {@code null} or new
	 * {@link ArrayList}s.
	 */
	public AgnosticWorkflow() {
	}
	/**
	 * Copy‑constructor: deep‑copies all fields from another AgnosticWorkflow.
	 *
	 * @param other the AgnosticWorkflow to copy from
	 */
	public AgnosticWorkflow(AgnosticWorkflow other) {
		this.workflowName = other.workflowName;
		this.enclosingOperatorName = other.enclosingOperatorName;

		// deep‑copy inner source ports
		if (other.innerSourcesPortsAndSchemas != null) {
			this.innerSourcesPortsAndSchemas = new ArrayList<>();
			for (AWPort port : other.innerSourcesPortsAndSchemas) {
				this.innerSourcesPortsAndSchemas.add(new AWPort(port));
			}
		}

		// deep‑copy inner sink ports
		if (other.innerSinksPortsAndSchemas != null) {
			this.innerSinksPortsAndSchemas = new ArrayList<>();
			for (AWPort port : other.innerSinksPortsAndSchemas) {
				this.innerSinksPortsAndSchemas.add(new AWPort(port));
			}
		}

		// deep‑copy operator connections
		this.operatorConnections = new ArrayList<>();
		for (AWOperatorConnection conn : other.operatorConnections) {
			this.operatorConnections.add(new AWOperatorConnection(conn));
		}

		// deep‑copy operators
		this.operators = new ArrayList<>();
		for (AWOperator op : other.operators) {
			this.operators.add(new AWOperator(op));
		}

		// deep‑copy placement sites
		this.placementSites = new ArrayList<>();
		for (AWPlacementSite site : other.placementSites) {
			this.placementSites.add(new AWPlacementSite(site));
		}
	}
	/**
	 * Returns the list of {@link AWOperator}s in this {@link AgnosticWorkflow}.
	 *
	 * @return list of {@link AWOperator}s in this {@link AgnosticWorkflow}
	 */
	public List<AWOperator> getOperators() {
		return operators;
	}

	public List<AWOperatorConnection> retrieveFromOperators(String name) {
		List<AWOperatorConnection> result = new ArrayList<>();
		for (AWOperatorConnection operatorConnection : operatorConnections) {
			if (operatorConnection.getFromOperator().equals(name)) {
				result.add(operatorConnection);
			}
		}
		return result;
	}
	public List<AWOperatorConnection> retrieveToOperators(String name) {
		List<AWOperatorConnection> result = new ArrayList<>();
		for (AWOperatorConnection operatorConnection : operatorConnections) {
			if (operatorConnection.getToOperator().equals(name)) {
				result.add(operatorConnection);
			}
		}
		return result;
	}

	/**
	 * Sets the list of {@link AWOperator}s to the provided one. Returns itself, so that set methods
	 * can be chained.
	 *
	 * @param operators
	 * 		new list of {@link AWOperator}s
	 * @return this {@link AgnosticWorkflow}
	 */
	public AgnosticWorkflow setOperators(List<AWOperator> operators) {
		this.operators = operators;
		return this;
	}

	/**
	 * Returns the list of {@link AWOperatorConnection}s in this {@link AgnosticWorkflow}.
	 *
	 * @return list of {@link AWOperatorConnection}s in this {@link AgnosticWorkflow}
	 */
	public List<AWOperatorConnection> getOperatorConnections() {
		return operatorConnections;
	}
	
	/**
	 * Sets the list of {@link AWOperatorConnection}s to the provided one. Returns itself, so that
	 * set methods can be chained.
	 *
	 * @param operatorConnections
	 * 		new list of {@link AWOperatorConnection}s
	 * @return this {@link AgnosticWorkflow}
	 */
	public AgnosticWorkflow setOperatorConnections(List<AWOperatorConnection> operatorConnections) {
		this.operatorConnections = operatorConnections;
		return this;
	}
	
	/**
	 * Returns the name of the workflow of this {@link AgnosticWorkflow}.
	 *
	 * @return name of the workflow of this {@link AgnosticWorkflow}
	 */
	public String getWorkflowName() {
		return workflowName;
	}
	
	/**
	 * Sets the name of the workflow to the provided one. Returns itself, so that set methods can be
	 * chained.
	 *
	 * @param workflowName
	 * 		new name of the workflow
	 * @return this {@link AgnosticWorkflow}
	 */
	public AgnosticWorkflow setWorkflowName(String workflowName) {
		this.workflowName = workflowName;
		return this;
	}

	public AWPlacementSite getPlacementSiteByName(String name) {
		if (placementSites == null) {
			return null;
		}
		return placementSites.stream()
				.filter(p -> p.getSiteName() != null && p.getSiteName().equals(name))
				.findFirst()
				.orElse(null);
	}
	/**
	 * Returns the name of the enclosing operator of this {@link AgnosticWorkflow}.
	 *
	 * @return name of the enclosing operator of this {@link AgnosticWorkflow}
	 */
	public String getEnclosingOperatorName() {
		return enclosingOperatorName;
	}
	
	/**
	 * Sets the name of the enclosing operator to the provided one. Returns itself, so that set
	 * methods can be chained.
	 *
	 * @param enclosingOperatorName
	 * 		new name of the enclosing operator
	 * @return this {@link AgnosticWorkflow}
	 */
	public AgnosticWorkflow setEnclosingOperatorName(String enclosingOperatorName) {
		this.enclosingOperatorName = enclosingOperatorName;
		return this;
	}
	
	/**
	 * Returns the list of inner source ports and their schemas (represented by a list of {@link
	 * AWPort}s) in this {@link AgnosticWorkflow}.
	 *
	 * @return list of inner source ports and their schemas (represented by a list of {@link
	 * AWPort}s) in this {@link AgnosticWorkflow}
	 */
	public List<AWPort> getInnerSourcesPortsAndSchemas() {
		return innerSourcesPortsAndSchemas;
	}
	
	/**
	 * Sets the list of inner source ports and their schemas (represented by a list of {@link
	 * AWPort}s) to the provided one. Returns itself, so that set methods can be chained.
	 *
	 * @param innerSourcesPortsAndSchemas
	 * 		list of inner source ports and their schemas (represented by a list of {@link AWPort}s)
	 * @return this {@link AgnosticWorkflow}
	 */
	public AgnosticWorkflow setInnerSourcesPortsAndSchemas(
			List<AWPort> innerSourcesPortsAndSchemas) {
		this.innerSourcesPortsAndSchemas = innerSourcesPortsAndSchemas;
		return this;
	}
	
	/**
	 * Returns the list of inner sink ports and their schemas (represented by a list of {@link
	 * AWPort}s) in this {@link AgnosticWorkflow}.
	 *
	 * @return list of list of inner sink ports and their schemas (represented by a list of {@link
	 * AWPort}s) in this {@link AgnosticWorkflow}
	 */
	public List<AWPort> getInnerSinksPortsAndSchemas() {
		return innerSinksPortsAndSchemas;
	}
	
	/**
	 * Sets the list of inner sink ports and their schemas (represented by a list of {@link
	 * AWPort}s) to the provided one. Returns itself, so that set methods can be chained.
	 *
	 * @param innerSinksPortsAndSchemas
	 * 		list of inner sink ports and their schemas (represented by a list of {@link AWPort}s)
	 * @return this {@link AgnosticWorkflow}
	 */
	public AgnosticWorkflow setInnerSinksPortsAndSchemas(List<AWPort> innerSinksPortsAndSchemas) {
		this.innerSinksPortsAndSchemas = innerSinksPortsAndSchemas;
		return this;
	}
	
	/**
	 * Returns the list of {@link AWPlacementSite}s in this {@link AgnosticWorkflow}.
	 *
	 * @return list of {@link AWPlacementSite}s in this {@link AgnosticWorkflow}
	 */
	public List<AWPlacementSite> getPlacementSites() {
		return placementSites;
	}
	
	/**
	 * Sets the list of {@link AWPlacementSite}s to the provided one. Returns itself, so that set
	 * methods can be chained.
	 *
	 * @param placementSites
	 * 		new list of {@link AWPlacementSite}s
	 * @return this {@link AgnosticWorkflow}
	 */
	public AgnosticWorkflow setPlacementSites(List<AWPlacementSite> placementSites) {
		this.placementSites = placementSites;
		return this;
	}
	
	/**
	 * Returns a copy of this {@link AgnosticWorkflow}. All field are copied.
	 *
	 * @return copy of this {@link AgnosticWorkflow}
	 */
	public AgnosticWorkflow copy() {
		return new AgnosticWorkflow().setEnclosingOperatorName(enclosingOperatorName)
									 .setWorkflowName(workflowName)
									 .setOperators(new ArrayList<>(operators))
									 .setOperatorConnections(new ArrayList<>(operatorConnections))
									 .setInnerSinksPortsAndSchemas(
											 new ArrayList<>(innerSinksPortsAndSchemas))
									 .setInnerSourcesPortsAndSchemas(
											 new ArrayList<>(innerSourcesPortsAndSchemas))
									 .setPlacementSites(new ArrayList<>(placementSites));
	}
	
}
