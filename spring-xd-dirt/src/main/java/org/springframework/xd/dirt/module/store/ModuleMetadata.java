/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.module.store;

import java.util.Properties;

import org.springframework.xd.dirt.core.DeploymentUnitStatus;
import org.springframework.xd.module.ModuleType;

/**
 * Represents runtime module model info.
 * 
 * @author Ilayaperumal Gopinathan
 */
public class ModuleMetadata implements Comparable<ModuleMetadata> {

	private final String id;

	private final String name;

	private final String unitName;

	private final ModuleType moduleType;

	private final String containerId;

	private final Properties moduleOptions;

	private final Properties deploymentProperties;

	private DeploymentUnitStatus.State deploymentStatus;


	public ModuleMetadata(String id, String name, String unitName, ModuleType moduleType, String containerId,
			Properties moduleOptions,
			Properties deploymentProperties) {
		this.id = id;
		this.name = name;
		this.unitName = unitName;
		this.moduleType = moduleType;
		this.containerId = containerId;
		this.moduleOptions = moduleOptions;
		this.deploymentProperties = deploymentProperties;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getUnitName() {
		return this.unitName;
	}

	public ModuleType getModuleType() {
		return this.moduleType;
	}

	public String getContainerId() {
		return containerId;
	}

	public Properties getModuleOptions() {
		return moduleOptions;
	}

	public Properties getDeploymentProperties() {
		return deploymentProperties;
	}

	public DeploymentUnitStatus.State getDeploymentStatus() {
		return deploymentStatus;
	}

	public void setDeploymentStatus(DeploymentUnitStatus.State deploymentStatus) {
		this.deploymentStatus = deploymentStatus;
	}

	/**
	 * Compares ModuleMetadata using module id.
	 */
	@Override
	public int compareTo(ModuleMetadata other) {
		return this.getId().compareTo(other.getId());
	}
}
