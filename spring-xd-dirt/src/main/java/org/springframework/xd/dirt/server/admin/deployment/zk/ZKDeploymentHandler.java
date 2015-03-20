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
package org.springframework.xd.dirt.server.admin.deployment.zk;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.ModuleDeploymentRequestsPath;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentHandler;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.RuntimeModuleDeploymentProperties;

/**
 * Abstract class that has common implementations of ZK based deployments.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public abstract class ZKDeploymentHandler implements DeploymentHandler, SupervisorElectionListener {

	/**
	 * ZooKeeper connection
	 */
	@Autowired
	protected ZooKeeperConnection zkConnection;

	/**
	 * Cache of children under the module deployment requests path.
	 */
	protected PathChildrenCache moduleDeploymentRequests;


	/**
	 * Create {@link org.springframework.xd.dirt.core.ModuleDeploymentRequestsPath} for the given
	 * {@link org.springframework.xd.module.ModuleDescriptor} and
	 * the {@link org.springframework.xd.module.RuntimeModuleDeploymentProperties}.
	 *
	 * @param client the curator client
	 * @param descriptor the module descriptor
	 * @param deploymentProperties the runtime deployment properties
	 */
	protected void createModuleDeploymentRequestsPath(CuratorFramework client, ModuleDescriptor descriptor,
			RuntimeModuleDeploymentProperties deploymentProperties) {
		// Create and set the data for the requested modules path
		String requestedModulesPath = new ModuleDeploymentRequestsPath()
				.setDeploymentUnitName(descriptor.getGroup())
				.setModuleType(descriptor.getType().toString())
				.setModuleLabel(descriptor.getModuleLabel())
				.setModuleSequence(deploymentProperties.getSequenceAsString())
				.build();
		try {
			client.create().creatingParentsIfNeeded().forPath(requestedModulesPath,
					ZooKeeperUtils.mapToBytes(deploymentProperties));
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	@Override
	public final void undeploy(String deploymentUnitName) throws Exception {
		Assert.notNull(moduleDeploymentRequests, "Module deployment request path cache shouldn't be null.");
		ModuleDeploymentRequestsPath path;
		for (ChildData requestedModulesData : moduleDeploymentRequests.getCurrentData()) {
			path = new ModuleDeploymentRequestsPath(requestedModulesData.getPath());
			if (path.getDeploymentUnitName().equals(deploymentUnitName)) {
				zkConnection.getClient().delete().deletingChildrenIfNeeded().forPath(path.build());
			}
		}
	}

	@Override
	public void onSupervisorElected(SupervisorElectedEvent supervisorElectedEvent) {
		this.moduleDeploymentRequests = supervisorElectedEvent.getModuleDeploymentRequests();
	}

}
