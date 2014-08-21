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

import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.cluster.ContainerMatcher;
import org.springframework.xd.dirt.container.store.ContainerRepository;
import org.springframework.xd.dirt.core.Job;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.job.JobFactory;
import org.springframework.xd.dirt.stream.StreamFactory;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;

/**
 * Listener implementation that is invoked when containers are added/removed/modified.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public class ContainerListener implements PathChildrenCacheListener {

	/**
	 * Logger.
	 */
	private final Logger logger = LoggerFactory.getLogger(ContainerListener.class);

	/**
	 * The {@link ModuleRedeployer} that deploys unallocated stream/job modules
	 * upon new container arrival.
	 */
	private final ModuleRedeployer arrivingContainerModuleRedeployer;

	/**
	 * The {@link ModuleRedeployer} that re-deploys the stream/job modules
	 * that were deployed at the departing container.
	 */
	private final ModuleRedeployer departingContainerModuleRedeployer;

	/**
	 * Construct a ContainerListener.
	 *
	 * @param zkConnection ZooKeeper connection
	 * @param streamFactory factory to construct {@link Stream}
	 * @param jobFactory factory to construct {@link Job}
	 * @param streamDeployments cache of children for stream deployments path
	 * @param jobDeployments cache of children for job deployments path
	 * @param moduleDeploymentRequests cache of children for requested module deployments path
	 * @param containerMatcher matches modules to containers
	 * @param stateCalculator calculator for stream/job state
	 */
	public ContainerListener(ZooKeeperConnection zkConnection,
			ContainerRepository containerRepository,
			StreamFactory streamFactory, JobFactory jobFactory,
			PathChildrenCache streamDeployments, PathChildrenCache jobDeployments,
			PathChildrenCache moduleDeploymentRequests, ContainerMatcher containerMatcher,
			DeploymentUnitStateCalculator stateCalculator) {
		this.arrivingContainerModuleRedeployer = new ArrivingContainerModuleRedeployer(zkConnection,
				containerRepository, streamFactory, jobFactory, streamDeployments, jobDeployments,
				moduleDeploymentRequests, containerMatcher, stateCalculator);
		this.departingContainerModuleRedeployer = new DepartingContainerModuleRedeployer(zkConnection,
				containerRepository, streamFactory, jobFactory, moduleDeploymentRequests, containerMatcher,
				stateCalculator);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
		ZooKeeperUtils.logCacheEvent(logger, event);
		Container container;
		switch (event.getType()) {
			case CHILD_ADDED:
				container = getContainer(event.getData());
				logger.info("Container arrived: {}", container);
				this.arrivingContainerModuleRedeployer.deployModules(container);
				break;
			case CHILD_UPDATED:
				break;
			case CHILD_REMOVED:
				container = getContainer(event.getData());
				logger.info("Container departed: {}", container);
				this.departingContainerModuleRedeployer.deployModules(container);
				break;
			case CONNECTION_SUSPENDED:
				break;
			case CONNECTION_RECONNECTED:
				break;
			case CONNECTION_LOST:
				break;
			case INITIALIZED:
				break;
		}
	}

	/**
	 * Get the {@link Container} from the data.
	 *
	 * @param data the ChildData that the ContainerListener event receives.
	 * @return the container that represents name and attributes.
	 */
	private Container getContainer(ChildData data) {
		return new Container(Paths.stripPath(data.getPath()), ZooKeeperUtils.bytesToMap(data.getData()));
	}

}
