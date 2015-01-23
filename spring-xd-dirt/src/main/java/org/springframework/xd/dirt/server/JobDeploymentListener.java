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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.Assert;
import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.cluster.NoContainerException;
import org.springframework.xd.dirt.container.store.ContainerRepository;
import org.springframework.xd.dirt.core.DeploymentUnitStatus;
import org.springframework.xd.dirt.core.Job;
import org.springframework.xd.dirt.core.JobDeploymentsPath;
import org.springframework.xd.dirt.job.JobFactory;
import org.springframework.xd.dirt.zookeeper.ChildPathIterator;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.RuntimeModuleDeploymentProperties;


/**
 * Listener implementation that handles job deployment requests.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public class JobDeploymentListener extends InitialDeploymentListener {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(JobDeploymentListener.class);

	/**
	 * Job factory.
	 */
	private final JobFactory jobFactory;

	/**
	 * Construct a JobDeploymentListener.
	 *
	 * @param zkConnection ZooKeeper connection
	 * @param moduleDeploymentRequests the requested module deployments
	 * @param containerRepository repository to obtain container data
	 * @param jobFactory factory to construct {@link Job}
	 * @param containerMatcher matches modules to containers
	 * @param stateCalculator calculator for job state
	 */
	public JobDeploymentListener(ZooKeeperConnection zkConnection, PathChildrenCache moduleDeploymentRequests,
			ContainerRepository containerRepository, JobFactory jobFactory,
			ContainerMatcher containerMatcher, DeploymentUnitStateCalculator stateCalculator) {
		super(zkConnection, moduleDeploymentRequests, containerRepository, containerMatcher, stateCalculator);
		this.jobFactory = jobFactory;
	}

	/**
	 * Handle the creation of a new job deployment.
	 *
	 * @param client curator client
	 * @param data job deployment request data
	 */
	@Override
	protected void onChildAdded(CuratorFramework client, ChildData data) throws Exception {
		String jobName = Paths.stripPath(data.getPath());
		Job job = DeploymentLoader.loadJob(client, jobName, jobFactory);
		deployJob(client, job);
	}

	/**
	 * Issue deployment requests for a job. This deployment will occur if:
	 * <ul>
	 *     <li>the job has not been destroyed</li>
	 *     <li>the job has not been undeployed</li>
	 *     <li>there is a container that can deploy the job</li>
	 * </ul>
	 *
	 * @param job the job instance to redeploy
	 * @throws InterruptedException
	 */
	private void deployJob(CuratorFramework client, final Job job) throws InterruptedException {
		if (job != null) {
			// Ensure that the path for modules used by the container to write
			// ephemeral nodes exists. The presence of this path is assumed
			// by the supervisor when it calculates stream state when it is
			// assigned leadership. See XD-2170 for details.
			try {
				client.create().creatingParentsIfNeeded().forPath(Paths.build(
						Paths.JOB_DEPLOYMENTS, job.getName(), Paths.MODULES));
			}
			catch (Exception e) {
				ZooKeeperUtils.wrapAndThrowIgnoring(e, KeeperException.NodeExistsException.class);
			}

			String statusPath = Paths.build(Paths.JOB_DEPLOYMENTS, job.getName(), Paths.STATUS);

			DeploymentUnitStatus deployingStatus = null;
			try {
				deployingStatus = new DeploymentUnitStatus(ZooKeeperUtils.bytesToMap(
						client.getData().forPath(statusPath)));
			}
			catch (Exception e) {
				// an exception indicates that the status has not been set
			}
			Assert.state(deployingStatus != null
					&& deployingStatus.getState() == DeploymentUnitStatus.State.deploying,
					String.format("Expected 'deploying' status for job '%s'; current status: %s",
							job.getName(), deployingStatus));

			ModuleDeploymentPropertiesProvider<ModuleDeploymentProperties> provider =
					new DefaultModuleDeploymentPropertiesProvider(job);

			try {
				Collection<ModuleDeploymentStatus> deploymentStatuses = new ArrayList<ModuleDeploymentStatus>();
				for (ModuleDescriptor descriptor : job.getModuleDescriptors()) {
					RuntimeModuleDeploymentProperties deploymentProperties = new RuntimeModuleDeploymentProperties();
					deploymentProperties.putAll(provider.propertiesForDescriptor(descriptor));
					Deque<Container> matchedContainers = new ArrayDeque<Container>(containerMatcher.match(descriptor,
							deploymentProperties,
							containerRepository.findAll()));
					// Modules count == 0
					if (deploymentProperties.getCount() == 0) {
						deploymentProperties.setSequence(0);
						createModuleDeploymentRequestsPath(client, descriptor, deploymentProperties);
					}
					// Modules count > 0
					else {
						for (int i = 1; i <= deploymentProperties.getCount(); i++) {
							deploymentProperties.setSequence(i);
							createModuleDeploymentRequestsPath(client, descriptor, deploymentProperties);
						}
					}
					RuntimeModuleDeploymentPropertiesProvider deploymentRuntimeProvider =
							new RuntimeModuleDeploymentPropertiesProvider(provider);

					try {
						deploymentStatuses.addAll(moduleDeploymentWriter.writeDeployment(
								descriptor, deploymentRuntimeProvider, matchedContainers));
					}
					catch (NoContainerException e) {
						logger.warn("No containers available for deployment of job {}", job.getName());
					}

					DeploymentUnitStatus status = stateCalculator.calculate(job, provider, deploymentStatuses);

					logger.info("Deployment status for job '{}': {}", job.getName(), status);

					client.setData().forPath(statusPath, ZooKeeperUtils.mapToBytes(status.toMap()));
				}
			}
			catch (InterruptedException e) {
				throw e;
			}
			catch (Exception e) {
				throw ZooKeeperUtils.wrapThrowable(e);
			}
		}
	}

	/**
	 * Iterate all deployed jobs, recalculate the deployment status of each, and
	 * create an ephemeral node indicating the job state. This is typically invoked
	 * upon leader election.
	 *
	 * @param client          curator client
	 * @param jobDeployments  curator cache of job deployments
	 * @throws Exception
	 */
	public void recalculateJobStates(CuratorFramework client, PathChildrenCache jobDeployments) throws Exception {
		for (Iterator<String> iterator = new ChildPathIterator<String>(ZooKeeperUtils.stripPathConverter,
				jobDeployments); iterator.hasNext();) {
			String jobName = iterator.next();
			Job job = DeploymentLoader.loadJob(client, jobName, jobFactory);
			if (job != null) {
				String jobModulesPath = Paths.build(Paths.JOB_DEPLOYMENTS, jobName, Paths.MODULES);
				List<ModuleDeploymentStatus> statusList = new ArrayList<ModuleDeploymentStatus>();
				List<String> moduleDeployments = client.getChildren().forPath(jobModulesPath);
				for (String moduleDeployment : moduleDeployments) {
					JobDeploymentsPath jobDeploymentsPath = new JobDeploymentsPath(
							Paths.build(jobModulesPath, moduleDeployment));
					statusList.add(new ModuleDeploymentStatus(
							jobDeploymentsPath.getContainer(),
							jobDeploymentsPath.getModuleSequence(),
							new ModuleDescriptor.Key(jobName, ModuleType.job, jobDeploymentsPath.getModuleLabel()),
							ModuleDeploymentStatus.State.deployed, null));
				}
				DeploymentUnitStatus status = stateCalculator.calculate(job,
						new DefaultModuleDeploymentPropertiesProvider(job), statusList);

				logger.info("Deployment status for job '{}': {}", job.getName(), status);

				String statusPath = Paths.build(Paths.JOB_DEPLOYMENTS, job.getName(), Paths.STATUS);
				Stat stat = client.checkExists().forPath(statusPath);
				if (stat != null) {
					logger.trace("Found old status path {}; stat: {}", statusPath, stat);
					client.delete().forPath(statusPath);
				}
				client.create().withMode(CreateMode.EPHEMERAL).forPath(statusPath,
						ZooKeeperUtils.mapToBytes(status.toMap()));
			}
		}
	}

}
