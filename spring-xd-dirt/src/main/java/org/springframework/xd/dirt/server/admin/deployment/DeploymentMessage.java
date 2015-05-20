/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.xd.dirt.server.admin.deployment;

import java.lang.String;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The message that wraps the details of a deployment request.
 *
 * @author Ilayaperumal Gopinathan
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class DeploymentMessage {

	/**
	 * String representation of UUID for this request.
	 */
	private String requestId;

	/**
	 * Deployment unit type for the unit
	 */
	private DeploymentUnitType deploymentUnitType;

	/**
	 * Deployment unit name
	 */
	private String unitName;

	/**
	 * The (Stream/Job) definition
	 */
	private String definition;

	/**
	 * Deployment action to perform for this deployment request
	 */
	private DeploymentAction deploymentAction;

	/**
	 * Deployment properties to use while deploying the unit
	 */
	private Map<String, String> deploymentProperties;

	// for serialization
	public DeploymentMessage() {}

	/**
	 * Construct a {@code DeploymentMessage} with a randomly
	 * generated {@link #requestId}.
	 *
	 * @param deploymentUnitType the type of deployment
	 */
	public DeploymentMessage(DeploymentUnitType deploymentUnitType) {
		this.requestId = UUID.randomUUID().toString();
		this.deploymentUnitType = deploymentUnitType;
	}

	public String getRequestId() {
		return requestId;
	}

	public String getUnitName() {
		return unitName;
	}

	public String getDefinition() {
		return definition;
	}

	public DeploymentAction getDeploymentAction() {
		return deploymentAction;
	}

	public Map<String, String> getDeploymentProperties() {
		return deploymentProperties;
	}

	public DeploymentUnitType getDeploymentUnitType() {
		return deploymentUnitType;
	}

	public DeploymentMessage setRequestId(String requestId) {
		this.requestId = requestId;
		return this;
	}

	public DeploymentMessage setUnitName(String unitName) {
		this.unitName = unitName;
		return this;
	}

	public DeploymentMessage setDefinition(String definition) {
		this.definition = definition;
		return this;
	}

	public DeploymentMessage setDeploymentAction(DeploymentAction deploymentAction) {
		this.deploymentAction = deploymentAction;
		return this;
	}

	public DeploymentMessage setDeploymentProperties(Map<String, String> deploymentProperties) {
		this.deploymentProperties = deploymentProperties;
		return this;
	}

	@Override
	public String toString() {
		return "DeploymentMessage{" +
				"requestId='" + requestId + '\'' +
				", deploymentUnitType=" + deploymentUnitType +
				", unitName='" + unitName + '\'' +
				", definition='" + definition + '\'' +
				", deploymentAction=" + deploymentAction +
				", deploymentProperties=" + deploymentProperties +
				'}';
	}
}
