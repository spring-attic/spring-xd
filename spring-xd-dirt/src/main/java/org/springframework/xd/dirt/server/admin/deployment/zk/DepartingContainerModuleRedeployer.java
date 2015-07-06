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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.container.store.ContainerRepository;
import org.springframework.xd.dirt.core.Job;
import org.springframework.xd.dirt.core.ModuleDeploymentsPath;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.job.JobFactory;
import org.springframework.xd.dirt.server.admin.deployment.ContainerMatcher;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentUnitStateCalculator;
import org.springframework.xd.dirt.stream.StreamFactory;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.RuntimeModuleDeploymentProperties;


/**
 * The {@link ModuleRedeployer} that re-deploys the stream/job modules that were
 * deployed into the departing container.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public class DepartingContainerModuleRedeployer extends ModuleRedeployer {

	/**
	 * Logger.
	 */
	protected final Logger logger = LoggerFactory.getLogger(DepartingContainerModuleRedeployer.class);

	/**
	 * Constructs {@code DepartingContainerModuleRedeployer}
	 *
	 * @param zkConnection ZooKeeper connection
	 * @param containerRepository the repository to find the containers
	 * @param streamFactory factory to construct {@link Stream}
	 * @param jobFactory factory to construct {@link Job}
	 * @param moduleDeploymentRequests cache of children for requested module deployments path
	 * @param containerMatcher matches modules to containers
	 * @param moduleDeploymentWriter utility that writes deployment requests to zk path
	 * @param stateCalculator calculator for stream/job state
	 */
	public DepartingContainerModuleRedeployer(ZooKeeperConnection zkConnection,
			ContainerRepository containerRepository,
			StreamFactory streamFactory, JobFactory jobFactory,
			PathChildrenCache moduleDeploymentRequests, ContainerMatcher containerMatcher,
			ModuleDeploymentWriter moduleDeploymentWriter, DeploymentUnitStateCalculator stateCalculator) {
		super(zkConnection, containerRepository, streamFactory, jobFactory, moduleDeploymentRequests, containerMatcher,
				moduleDeploymentWriter, stateCalculator);
	}

	/**
	 * Handle the departure of a container. This will scan the list of modules
	 * deployed to the departing container and redeploy them if required.
	 *
	 * @param container the container that departed
	 */
	@Override
	protected void deployModules(Container container) throws Exception {
		CuratorFramework client = getClient();
		if (client.getState() == CuratorFrameworkState.STOPPED) {
			return;
		}

		// the departed container may have hosted multiple modules
		// for the same stream; therefore each stream that is loaded
		// will be cached to avoid reloading for each module
		Map<String, Stream> streamMap = new HashMap<String, Stream>();

		String containerDeployments = Paths.build(Paths.MODULE_DEPLOYMENTS, Paths.ALLOCATED, container.getName());
		List<String> deployments = client.getChildren().forPath(containerDeployments);

		// Stream modules need to be deployed in the correct order;
		// this is done in a two-pass operation: gather, then re-deploy.
		Set<ModuleDeployment> streamModuleDeployments = new TreeSet<ModuleDeployment>();
		for (String deployment : deployments) {
			ModuleDeploymentsPath moduleDeploymentsPath =
					new ModuleDeploymentsPath(Paths.build(containerDeployments, deployment));

			// reuse the module deployment properties used to deploy
			// this module; this may contain properties specific to
			// the container that just departed (such as partition
			// index in the case of partitioned streams)
			RuntimeModuleDeploymentProperties deploymentProperties = new RuntimeModuleDeploymentProperties();
			deploymentProperties.putAll(ZooKeeperUtils.bytesToMap(client.getData().forPath(
					moduleDeploymentsPath.build())));

			String unitName = moduleDeploymentsPath.getDeploymentUnitName();
			String moduleType = moduleDeploymentsPath.getModuleType();

			if (ModuleType.job.toString().equals(moduleType)) {
				Job job = null;
				try {
					job = DeploymentLoader.loadJob(client, unitName, jobFactory);
				}
				catch (Exception e) {
					logger.error(String.format("Exception loading the job %s. The job deployment status could be " +
							"unknown. Fix the issue mentioned in the exception and restart the admin. " +
							"The exception is: %s", unitName, ExceptionUtils.getStackTrace(e)));
				}
				if (job != null) {
					redeployModule(new ModuleDeployment(job, job.getJobModuleDescriptor(),
							deploymentProperties), false);
				}
			}
			else {
				Stream stream = streamMap.get(unitName);
				if (stream == null) {
					try {
						stream = DeploymentLoader.loadStream(client, unitName, streamFactory);
					}
					catch (Exception e) {
						logger.error(String.format("Exception loading the stream %s. The stream deployment status " +
								"could be unknown. Fix the issue mentioned in the exception and restart the admin. " +
								"The exception is: %s", unitName, ExceptionUtils.getStackTrace(e)));
					}
					streamMap.put(unitName, stream);
				}
				if (stream != null) {
					streamModuleDeployments.add(new ModuleDeployment(stream,
							stream.getModuleDescriptor(moduleDeploymentsPath.getModuleLabel()),
							deploymentProperties));
				}
			}
		}

		for (ModuleDeployment moduleDeployment : streamModuleDeployments) {
			redeployModule(moduleDeployment, false);
		}

		// remove the deployments from the departed container
		client.delete().deletingChildrenIfNeeded().forPath(
				Paths.build(Paths.MODULE_DEPLOYMENTS, Paths.ALLOCATED, container.getName()));
	}

}
