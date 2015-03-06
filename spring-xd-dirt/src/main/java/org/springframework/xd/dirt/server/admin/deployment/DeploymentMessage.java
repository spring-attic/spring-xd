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

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The message that wraps the details of a deployment request.
 *
 * @author Ilayaperumal Gopinathan
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public class DeploymentMessage {

	private DeploymentUnitType deploymentUnitType;

	private String unitName;

	private String definition;

	private DeploymentAction deploymentAction;

	private Map<String, String> deploymentProperties;

	// for serialization
	public DeploymentMessage() {}

	public DeploymentMessage(DeploymentUnitType deploymentUnitType) {
		this.deploymentUnitType = deploymentUnitType;
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
}
