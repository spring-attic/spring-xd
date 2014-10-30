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

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.xd.dirt.cluster.ContainerMatcher;
import org.springframework.xd.dirt.container.store.ContainerRepository;
import org.springframework.xd.dirt.core.ModuleDeploymentRequestsPath;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.RuntimeModuleDeploymentProperties;

/**
 * Abstract deployment listener that handles fresh stream/job deployment requests.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public abstract class InitialDeploymentListener implements PathChildrenCacheListener {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(InitialDeploymentListener.class);

	/**
	 * Cache of children under the module deployment requests path.
	 */
	private final PathChildrenCache moduleDeploymentRequests;

	/**
	 * Container matcher for matching modules to containers.
	 */
	protected final ContainerMatcher containerMatcher;

	/**
	 * Repository from which to obtain containers in the cluster.
	 */
	protected final ContainerRepository containerRepository;

	/**
	 * Utility for writing module deployment requests to ZooKeeper.
	 */
	protected final ModuleDeploymentWriter moduleDeploymentWriter;

	/**
	 * State calculator for stream state.
	 */
	protected final DeploymentUnitStateCalculator stateCalculator;


	/**
	 * Construct a {@code PrimaryDeploymentListener}.
	 *
	 * @param zkConnection ZooKeeper connection
	 * @param moduleDeploymentRequests the requested module deployments
	 * @param containerRepository repository to obtain container data
	 * @param containerMatcher matches modules to containers
	 * @param stateCalculator calculator for stream state
	 */
	public InitialDeploymentListener(ZooKeeperConnection zkConnection,
			PathChildrenCache moduleDeploymentRequests,
			ContainerRepository containerRepository,
			ContainerMatcher containerMatcher, DeploymentUnitStateCalculator stateCalculator) {
		this.moduleDeploymentRequests = moduleDeploymentRequests;
		this.containerMatcher = containerMatcher;
		this.containerRepository = containerRepository;
		this.moduleDeploymentWriter = new ModuleDeploymentWriter(zkConnection, containerMatcher);
		this.stateCalculator = stateCalculator;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Handle child events for the {@link Paths#STREAMS} path.
	 */
	@Override
	public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
		ZooKeeperUtils.logCacheEvent(logger, event);
		switch (event.getType()) {
			case CHILD_ADDED:
				onChildAdded(client, event.getData());
				break;
			case CHILD_REMOVED:
				onChildRemoved(client, event.getData());
				break;
			default:
				break;
		}
	}

	/**
	 * Handle the new deployment requests.
	 *
	 * @param client the curator client
	 * @param data the data that represents the module deployments
	 * @throws Exception
	 */
	protected abstract void onChildAdded(CuratorFramework client, ChildData data) throws Exception;

	/**
	 * Handle the removal of module deployment requests.
	 *
	 * @param client the curator client
	 * @param data the module deployment data
	 * @throws Exception
	 */
	protected void onChildRemoved(CuratorFramework client, ChildData data) throws Exception {
		String deploymentUnitName = Paths.stripPath(data.getPath());
		ModuleDeploymentRequestsPath path;
		for (ChildData requestedModulesData : moduleDeploymentRequests.getCurrentData()) {
			path = new ModuleDeploymentRequestsPath(requestedModulesData.getPath());
			if (path.getDeploymentUnitName().equals(deploymentUnitName)) {
				client.delete().deletingChildrenIfNeeded().forPath(path.build());
			}
		}
	}

	/**
	 * Create {@link ModuleDeploymentRequestsPath} for the given {@link ModuleDescriptor} and
	 * the {@link RuntimeModuleDeploymentProperties}.
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

}
