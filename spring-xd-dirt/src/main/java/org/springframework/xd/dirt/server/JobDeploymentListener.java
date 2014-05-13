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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.xd.dirt.cluster.ContainerMatcher;
import org.springframework.xd.dirt.cluster.ContainerRepository;
import org.springframework.xd.dirt.core.Job;
import org.springframework.xd.dirt.core.ModuleDeploymentProperties;
import org.springframework.xd.dirt.job.JobFactory;
import org.springframework.xd.dirt.module.ModuleDescriptor;
import org.springframework.xd.dirt.util.DeploymentUtility;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.module.ModuleType;

/**
 * Listener implementation that handles job deployment requests.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public class JobDeploymentListener extends DeploymentHandler implements PathChildrenCacheListener {

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
	 * @param containerRepository repository to obtain container data
	 * @param jobFactory factory to construct {@link Job}
	 * @param containerMatcher matches modules to containers
	 */
	public JobDeploymentListener(ZooKeeperConnection zkConnection, ContainerRepository containerRepository,
			JobFactory jobFactory, ContainerMatcher containerMatcher) {
		super(zkConnection, containerRepository, containerMatcher);
		this.jobFactory = jobFactory;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Handle child events for the {@link Paths#JOBS} path.
	 */
	@Override
	public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
		ZooKeeperConnection.logCacheEvent(logger, event);
		switch (event.getType()) {
			case CHILD_ADDED:
				onChildAdded(client, event.getData());
				break;
			default:
				break;
		}
	}

	/**
	 * Handle the creation of a new job deployment.
	 *
	 * @param client curator client
	 * @param data job deployment request data
	 */
	private void onChildAdded(CuratorFramework client, ChildData data) throws Exception {
		String jobName = Paths.stripPath(data.getPath());
		Job job = deploymentLoader.loadJob(client, jobName, jobFactory);
		deployJob(job);
	}

	/**
	 * Issue deployment requests for a job. This deployment will occur if:
	 * <ul>
	 *     <li>the job has not been destroyed</li>
	 *     <li>the job has not been undeployed</li>
	 *     <li>there is a container that can deploy the job</li>
	 * </ul>
	 *
	 * @param client curator client
	 * @param job the job instance to redeploy
	 * @throws Exception
	 */
	private void deployJob(final Job job) throws Exception {
		if (job != null) {
			ModuleDescriptor descriptor = createJobModuleDescriptor(job.getName(),
					job.getJobModuleDescriptor().getModuleName());
			ModuleDeploymentWriter.ModuleDeploymentPropertiesProvider provider =
					new ModuleDeploymentWriter.ModuleDeploymentPropertiesProvider() {

						@Override
						public ModuleDeploymentProperties propertiesForDescriptor(ModuleDescriptor descriptor) {
							return DeploymentUtility.createModuleDeploymentProperties(job.getDeploymentProperties(),
									descriptor);
						}
					};

			List<ModuleDescriptor> descriptors = new ArrayList<ModuleDescriptor>();
			descriptors.add(descriptor);
			Collection<ModuleDeploymentWriter.Result> results =
					moduleDeploymentWriter.writeDeployment(descriptors.iterator(), provider);
			moduleDeploymentWriter.validateResults(results);
		}
	}

	/**
	 * Create an instance of {@link ModuleDescriptor} for a given job name.
	 *
	 * @param jobName job name
	 * @param moduleName job module name
	 *
	 * @return a ModuleDescriptor for the given job
	 */
	public static ModuleDescriptor createJobModuleDescriptor(String jobName, String moduleName) {
		return new ModuleDescriptor.Builder()
				.setGroup(jobName)
				.setType(ModuleType.job)
				.setModuleName(moduleName)
				// By default, module label is null for job module, explicitly set to use "modulename-index" pattern
				// here.
				.setModuleLabel(moduleName + "-0")
				.setIndex(0)
				.build();
	}


}
