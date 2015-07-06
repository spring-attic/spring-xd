/*
 * Copyright 2014-2015 the original author or authors.
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.container.store.ContainerRepository;
import org.springframework.xd.dirt.core.Job;
import org.springframework.xd.dirt.core.JobDeploymentsPath;
import org.springframework.xd.dirt.core.ModuleDeploymentRequestsPath;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.core.StreamDeploymentsPath;
import org.springframework.xd.dirt.job.JobFactory;
import org.springframework.xd.dirt.server.admin.deployment.ContainerMatcher;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentUnitStateCalculator;
import org.springframework.xd.dirt.stream.StreamFactory;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.RuntimeModuleDeploymentProperties;


/**
 * The {@link ModuleRedeployer} that deploys the unallocated stream/job modules.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public class ContainerMatchingModuleRedeployer extends ModuleRedeployer {

	/**
	 * Logger.
	 */
	protected final Logger logger = LoggerFactory.getLogger(ContainerMatchingModuleRedeployer.class);

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
	 * @param moduleDeploymentWriter utility that writes deployment requests to zk path
	 * @param stateCalculator calculator for stream/job state
	 */
	public ContainerMatchingModuleRedeployer(ZooKeeperConnection zkConnection,
			ContainerRepository containerRepository,
			StreamFactory streamFactory, JobFactory jobFactory,
			PathChildrenCache streamDeployments, PathChildrenCache jobDeployments,
			PathChildrenCache moduleDeploymentRequests, ContainerMatcher containerMatcher,
			ModuleDeploymentWriter moduleDeploymentWriter, DeploymentUnitStateCalculator stateCalculator) {
		super(zkConnection, containerRepository, streamFactory, jobFactory, moduleDeploymentRequests, containerMatcher,
				moduleDeploymentWriter, stateCalculator);
		this.streamDeployments = streamDeployments;
		this.jobDeployments = jobDeployments;
	}

	/**
	 * Handle the arrival of a container. This implementation will scan the
	 * existing streams/jobs and determine if any modules should be deployed to
	 * the new container.
	 *
	 * @param container the arriving container
	 */
	@Override
	protected void deployModules(Container container) throws Exception {
		deployUnallocatedStreamModules();
		deployUnallocatedJobModules();
	}

	/**
	 * Deploy any "unallocated" stream modules. These are stream modules that
	 * are supposed to be deployed but currently do not have enough containers
	 * available to meet the deployment criteria (i.e. if a module requires
	 * 3 containers but only 2 are available). Furthermore this will deploy
	 * modules with a count of 0 if this container matches the deployment
	 * criteria.
	 *
	 * @throws Exception
	 */
	private void deployUnallocatedStreamModules() throws Exception {
		List<ModuleDeploymentRequestsPath> requestedModulesPaths = getAllModuleDeploymentRequests();
		CuratorFramework client = getClient();
		// iterate the cache of stream deployments
		for (ChildData data : streamDeployments.getCurrentData()) {
			String streamName = ZooKeeperUtils.stripPathConverter.convert(data);

			try {
				final Stream stream = DeploymentLoader.loadStream(client, streamName, streamFactory);
				// if stream is null this means the stream was destroyed or undeployed
				if (stream != null) {
					List<ModuleDeploymentRequestsPath> requestedModules =
							ModuleDeploymentRequestsPath.getModulesForDeploymentUnit(
									requestedModulesPaths, streamName);
					Set<String> previouslyDeployed = new HashSet<String>();

					for (String deployedModule : client.getChildren().forPath(
							Paths.build(data.getPath(), Paths.MODULES))) {
						previouslyDeployed.add(Paths.stripPath(
								new StreamDeploymentsPath(Paths.build(data.getPath(), Paths.MODULES, deployedModule))
										.getModuleInstanceAsString()));
					}

					for (ModuleDeploymentRequestsPath path : requestedModules) {
						ModuleDescriptor moduleDescriptor = stream.getModuleDescriptor(path.getModuleLabel());
						if (shouldDeploy(moduleDescriptor, path, previouslyDeployed)) {
							RuntimeModuleDeploymentProperties moduleDeploymentProperties =
									new RuntimeModuleDeploymentProperties();
							moduleDeploymentProperties.putAll(ZooKeeperUtils.bytesToMap(
									moduleDeploymentRequests.getCurrentData(path.build()).getData()));
							redeployModule(new ModuleDeployment(stream, moduleDescriptor,
									moduleDeploymentProperties), true);
						}
					}
				}
			}
			catch (Exception e) {
				logger.error(
						String.format("Exception while evaluating module status for stream %s", streamName),
						e);
			}
		}
	}

	/**
	 * Deploy any "unallocated" jobs (jobs that are supposed to be deployed but
	 * do not have any modules deployed in any container).
	 *
	 * @throws Exception
	 */
	private void deployUnallocatedJobModules() throws Exception {
		List<ModuleDeploymentRequestsPath> requestedModulesPaths = getAllModuleDeploymentRequests();
		CuratorFramework client = getClient();
		// check for "orphaned" jobs that can be deployed to this new container
		for (ChildData data : jobDeployments.getCurrentData()) {
			String jobName = ZooKeeperUtils.stripPathConverter.convert(data);

			try {
				final Job job = DeploymentLoader.loadJob(client, jobName, jobFactory);
				// if job is null this means the job was destroyed or undeployed
				if (job != null) {
					List<ModuleDeploymentRequestsPath> requestedModules = ModuleDeploymentRequestsPath.getModulesForDeploymentUnit(
							requestedModulesPaths, jobName);
					Set<String> previouslyDeployed = new HashSet<String>();

					for (String deployedModule : client.getChildren().forPath(Paths.build(data.getPath(), Paths.MODULES))) {
						previouslyDeployed.add(Paths.stripPath(new JobDeploymentsPath(Paths.build(data.getPath(),
								Paths.MODULES,
								deployedModule)).getModuleInstanceAsString()));
					}

					for (ModuleDeploymentRequestsPath path : requestedModules) {
						ModuleDescriptor moduleDescriptor = job.getJobModuleDescriptor();
						if (shouldDeploy(moduleDescriptor, path, previouslyDeployed)) {
							RuntimeModuleDeploymentProperties moduleDeploymentProperties =
									new RuntimeModuleDeploymentProperties();
							moduleDeploymentProperties.putAll(ZooKeeperUtils.bytesToMap(
									moduleDeploymentRequests.getCurrentData(path.build()).getData()));
							redeployModule(new ModuleDeployment(job, moduleDescriptor,
									moduleDeploymentProperties), true);
						}
					}
				}
			}
			catch (Exception e) {
				logger.error(
						String.format("Exception while evaluating module status for job %s", jobName),
						e);
			}
		}
	}

	/**
	 * Evaluate the {@link org.springframework.xd.module.ModuleDescriptor} to
	 * determine if it should be deployed. It should be deployed if:
	 * <ul>
	 *     <li>It contains a module sequence of 0; this indicates it should
	 *         be deployed to every container that otherwise matches its criteria</li>
	 *     <li>The module was not previously deployed to another container</li>
	 * </ul>
	 *
	 * @param moduleDescriptor    module to be evaluated
	 * @param path                path for module deployment request
	 * @param previouslyDeployed  set of modules that have already been
	 *                            allocated to a container, the string
	 *                            format is specified by
	 *                            {@link ModuleDeploymentRequestsPath#getModuleInstanceAsString()}
	 * @return true if this module descriptor should be deployed
	 */
	private boolean shouldDeploy(ModuleDescriptor moduleDescriptor,
			ModuleDeploymentRequestsPath path,
			Set<String> previouslyDeployed) {
		return (path.getModuleSequence().equals("0")
				|| !previouslyDeployed.contains(path.getModuleInstanceAsString()));
	}

}
