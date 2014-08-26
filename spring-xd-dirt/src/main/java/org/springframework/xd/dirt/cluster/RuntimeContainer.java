/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.cluster;

import java.util.ArrayList;
import java.util.List;

import org.springframework.xd.dirt.module.store.ModuleMetadata;

/**
 * Domain object for an XD runtime container.
 *
 * @author Ilayaperumal Gopinathan
 */
public class RuntimeContainer extends Container {

	private int deploymentSize;

	private List<ModuleMetadata> deployedModules = new ArrayList<ModuleMetadata>();

	/**
	 * Construct a RuntimeContainer object.
	 *
	 * @param container the {@link Container}
	 */
	public RuntimeContainer(Container container) {
		super(container.getName(), container.getAttributes());
	}

	/**
	 * @return the number of deployed modules.
	 */
	public int getDeploymentSize() {
		return this.deploymentSize;
	}

	/**
	 * Set the number of deployed modules.
	 *
	 * @param deploymentSize
	 */
	public void setDeploymentSize(int deploymentSize) {
		this.deploymentSize = deploymentSize;
	}

	/**
	 * @return the modules deployed into this container
	 */
	public List<ModuleMetadata> getDeployedModules() {
		return this.deployedModules;
	}

	/**
	 * Set the deployed modules for this runtime container.
	 *
	 * @param deployedModules the modules deployed into this containers.
	 */
	public void setDeployedModules(List<ModuleMetadata> deployedModules) {
		this.deployedModules = deployedModules;
	}
}
