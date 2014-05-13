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

package org.springframework.xd.dirt.server;

import org.springframework.xd.dirt.cluster.ContainerMatcher;
import org.springframework.xd.dirt.cluster.ContainerRepository;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;

/**
 * Abstract class that any listener which handles the stream/job deployments could extend.
 *
 * @author Ilayaperumal Gopinathan
 */
public abstract class DeploymentHandler {

	/**
	 * Provides access to the current container list.
	 */
	protected final ContainerRepository containerRepository;

	/**
	 * Container matcher for matching modules to containers.
	 */
	protected final ContainerMatcher containerMatcher;

	/**
	 * Utility for writing module deployment requests to ZooKeeper.
	 */
	protected final ModuleDeploymentWriter moduleDeploymentWriter;

	/**
	 * Utility for loading streams and jobs (including deployment metadata).
	 */
	protected final DeploymentLoader deploymentLoader = new DeploymentLoader();


	/**
	 *
	 * @param containerRepository repository to obtain container data
	 * @param moduleDefinitionRepository repository to obtain module data
	 * @param moduleOptionsMetadataResolver resolver for module options metadata
	 * @param containerMatcher matches modules to containers
	 */
	public DeploymentHandler(ZooKeeperConnection zkConnection, ContainerRepository containerRepository,
			ContainerMatcher containerMatcher) {
		this.containerMatcher = containerMatcher;
		this.containerRepository = containerRepository;
		this.moduleDeploymentWriter = new ModuleDeploymentWriter(zkConnection,
				containerRepository, containerMatcher);
	}

}
