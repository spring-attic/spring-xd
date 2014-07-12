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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.cluster.ContainerMatcher;
import org.springframework.xd.dirt.container.store.ContainerRepository;
import org.springframework.xd.dirt.core.Job;
import org.springframework.xd.dirt.core.JobDeploymentsPath;
import org.springframework.xd.dirt.core.ModuleDeploymentRequestsPath;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.core.StreamDeploymentsPath;
import org.springframework.xd.dirt.job.JobFactory;
import org.springframework.xd.dirt.stream.StreamFactory;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.RuntimeModuleDeploymentProperties;


/**
 * The {@link ModuleRedeployer} that deploys the unallocated stream/job modules upon a new container arrival.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public class ArrivingContainerModuleRedeployer extends ModuleRedeployer {

	/**
	 * Logger.
	 */
	protected final Logger logger = LoggerFactory.getLogger(ArrivingContainerModuleRedeployer.class);

	/**
	 * Cache of children under the stream deployment path.
	 */
	private final PathChildrenCache streamDeployments;

	/**
	 * Cache of children under the job deployment path.
	 */
	protected final PathChildrenCache jobDeployments;

	/**
	 * Constructs {@code ArrivingContainerModuleRedeployer}
	 *
	 * @param zkConnection ZooKeeper connection
	 * @param containerRepository the repository to find the containers
	 * @param streamFactory factory to construct {@link Stream}
	 * @param jobFactory factory to construct {@link Job}
	 * @param streamDeployments cache of children for stream deployments path
	 * @param jobDeployments cache of children for job deployments path
	 * @param moduleDeploymentRequests cache of children for requested module deployments path
	 * @param containerMatcher matches modules to containers
	 * @param stateCalculator calculator for stream/job state
	 */
	public ArrivingContainerModuleRedeployer(ZooKeeperConnection zkConnection,
			ContainerRepository containerRepository,
			StreamFactory streamFactory, JobFactory jobFactory,
			PathChildrenCache streamDeployments, PathChildrenCache jobDeployments,
			PathChildrenCache moduleDeploymentRequests, ContainerMatcher containerMatcher,
			DeploymentUnitStateCalculator stateCalculator) {
		super(zkConnection, containerRepository, streamFactory, jobFactory, moduleDeploymentRequests, containerMatcher,
				stateCalculator);
		this.streamDeployments = streamDeployments;
		this.jobDeployments = jobDeployments;
	}

	/**
	 * Handle the arrival of a container. This implementation will scan the
	 * existing streams/jobs and determine if any modules should be deployed to
	 * the new container.
	 *
	 * @param client curator client
	 * @param container the arriving container
	 */
	@Override
	protected void deployModules(CuratorFramework client, Container container) throws Exception {
		String containerName = container.getName();
		logger.info("Container arrived: {}", containerName);
		if (hasDeployments(client, containerName)) {
			// The only reason when the arriving container would already have module deployments is
			// when a new deployment supervisor takes the leadership and the container in this context
			// already exist with the deployments. Since this container has already been matched
			// against the stream/job deployments, there is no need to perform the deployment of
			// unallocated modules here.
			logger.info(String.format("Arriving container '%s' already has module deployments", containerName));
		}
		else {
			deployUnallocatedStreamModules(client, container);
			deployUnallocatedJobModules(client, container);
		}
	}

	/**
	 * Check if the container has deployed any modules.
	 *
	 * @param client curator client
	 * @param containerName the container name
	 * @return boolean true if the container has deployments.
	 * @throws Exception
	 */
	private boolean hasDeployments(CuratorFramework client, String containerName) throws Exception {
		Stat stat = client.checkExists().forPath(
				Paths.build(Paths.MODULE_DEPLOYMENTS, Paths.ALLOCATED, containerName));
		return (stat != null && stat.getNumChildren() > 0);
	}

	/**
	 * Deploy any "unallocated" stream modules. These are stream modules that
	 * are supposed to be deployed but currently do not have enough containers
	 * available to meet the deployment criteria (i.e. if a module requires
	 * 3 containers but only 2 are available). Furthermore this will deploy
	 * modules with a count of 0 if this container matches the deployment
	 * criteria.
	 *
	 * @param client curator client
	 * @param container target container for stream module deployment
	 * @throws Exception
	 */
	private void deployUnallocatedStreamModules(CuratorFramework client, Container container) throws Exception {
		List<ModuleDeploymentRequestsPath> requestedModulesPaths = getAllModuleDeploymentRequests();
		// iterate the cache of stream deployments
		for (ChildData data : streamDeployments.getCurrentData()) {
			String streamName = ZooKeeperUtils.stripPathConverter.convert(data);
			final Stream stream = DeploymentLoader.loadStream(client, streamName, streamFactory);
			// if stream is null this means the stream was destroyed or undeployed
			if (stream != null) {
				List<ModuleDeploymentRequestsPath> requestedModules =
						ModuleDeploymentRequestsPath.getModulesForDeploymentUnit(
								requestedModulesPaths, streamName);
				Set<String> deployedModules = new HashSet<String>();

				try {
					for (String deployedModule : client.getChildren().forPath(
							Paths.build(data.getPath(), Paths.MODULES))) {
						deployedModules.add(Paths.stripPath(new StreamDeploymentsPath(Paths.build(data.getPath(),
								Paths.MODULES,
								deployedModule)).getModuleInstanceAsString()));
					}
				}
				catch (KeeperException.NoNodeException e) {
					// the stream does not have any modules deployed; this can be
					// ignored as it will result in an empty deployedModules set
				}

				Set<ModuleDescriptor> deployedDescriptors = new HashSet<ModuleDescriptor>();
				for (ModuleDeploymentRequestsPath path : requestedModules) {
					ModuleDescriptor moduleDescriptor = stream.getModuleDescriptor(path.getModuleLabel());
					if ((path.getModuleSequence().equals("0") || !deployedModules.contains(path.getModuleInstanceAsString()))
							&& !deployedDescriptors.contains(moduleDescriptor)) {
						deployedDescriptors.add(moduleDescriptor);
						RuntimeModuleDeploymentProperties moduleDeploymentProperties = new RuntimeModuleDeploymentProperties();
						moduleDeploymentProperties.putAll(ZooKeeperUtils.bytesToMap(moduleDeploymentRequests.getCurrentData(
								path.build()).getData()));
						deployModule(container, new ModuleDeployment(stream, moduleDescriptor,
								moduleDeploymentProperties));
					}
				}
			}
		}
	}

	/**
	 * Deploy any "unallocated" jobs (jobs that are supposed to be deployed but
	 * do not have any modules deployed in any container).
	 *
	 * @param client curator client
	 * @param container target container for job module deployment
	 * @throws Exception
	 */
	private void deployUnallocatedJobModules(CuratorFramework client, Container container) throws Exception {
		List<ModuleDeploymentRequestsPath> requestedModulesPaths = getAllModuleDeploymentRequests();
		// check for "orphaned" jobs that can be deployed to this new container
		for (ChildData data : jobDeployments.getCurrentData()) {
			String jobName = ZooKeeperUtils.stripPathConverter.convert(data);

			// if job is null this means the job was destroyed or undeployed
			Job job = DeploymentLoader.loadJob(client, jobName, this.jobFactory);
			if (job != null) {
				List<ModuleDeploymentRequestsPath> requestedModules = ModuleDeploymentRequestsPath.getModulesForDeploymentUnit(
						requestedModulesPaths,
						jobName);
				Set<String> deployedModules = new HashSet<String>();
				for (String deployedModule : client.getChildren().forPath(Paths.build(data.getPath(), Paths.MODULES))) {
					deployedModules.add(Paths.stripPath(new JobDeploymentsPath(Paths.build(data.getPath(),
							Paths.MODULES,
							deployedModule)).getModuleInstanceAsString()));
				}
				Set<ModuleDescriptor> deployedDescriptors = new HashSet<ModuleDescriptor>();
				for (ModuleDeploymentRequestsPath path : requestedModules) {
					ModuleDescriptor moduleDescriptor = job.getJobModuleDescriptor();
					if ((path.getModuleSequence().equals("0") || !deployedModules.contains(path.getModuleInstanceAsString()))
							&& !deployedDescriptors.contains(moduleDescriptor)) {
						deployedDescriptors.add(moduleDescriptor);
						RuntimeModuleDeploymentProperties moduleDeploymentProperties = new RuntimeModuleDeploymentProperties();
						moduleDeploymentProperties.putAll(ZooKeeperUtils.bytesToMap(moduleDeploymentRequests.getCurrentData(
								path.build()).getData()));
						deployModule(container, new ModuleDeployment(job, moduleDescriptor, moduleDeploymentProperties));
					}
				}
			}
		}
	}
}
