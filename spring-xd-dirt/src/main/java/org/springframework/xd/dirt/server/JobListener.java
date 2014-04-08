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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.cluster.ContainerMatcher;
import org.springframework.xd.dirt.cluster.ContainerRepository;
import org.springframework.xd.dirt.cluster.DefaultContainerMatcher;
import org.springframework.xd.dirt.core.ModuleDeploymentsPath;
import org.springframework.xd.dirt.core.JobsPath;
import org.springframework.xd.dirt.core.ModuleDescriptor;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.ParsingContext;
import org.springframework.xd.dirt.stream.XDStreamParser;
import org.springframework.xd.dirt.util.MapBytesUtility;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;

/**
 * Listener implementation that handles job deployment requests.
 * 
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class JobListener implements PathChildrenCacheListener {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(JobListener.class);

	/**
	 * Provides access to the current container list.
	 */
	private final ContainerRepository containerRepository;

	/**
	 * Utility to convert maps to byte arrays.
	 */
	private final MapBytesUtility mapBytesUtility = new MapBytesUtility();

	/**
	 * todo: make this pluggable
	 */
	private final ContainerMatcher containerMatcher = new DefaultContainerMatcher();

	/**
	 * Stream factory.
	 */
	// todo: something similar for Jobs (both should actually be the result of parseStream/parseJob)
	// private final StreamFactory streamFactory;

	/**
	 * The parser.
	 */
	private final XDStreamParser parser;

	/**
	 * Construct a JobListener.
	 * 
	 * @param containerRepository repository to obtain container data
	 * @param moduleDefinitionRepository repository to obtain module data
	 * @param moduleOptionsMetadataResolver resolver for module options metadata
	 */
	public JobListener(ContainerRepository containerRepository,
			ModuleDefinitionRepository moduleDefinitionRepository,
			ModuleOptionsMetadataResolver moduleOptionsMetadataResolver) {
		this.containerRepository = containerRepository;
		// this.moduleDefinitionRepository = moduleDefinitionRepository;
		this.parser = new XDStreamParser(moduleDefinitionRepository, moduleOptionsMetadataResolver);
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Handle child events for the {@link Paths#JOBS} path.
	 */
	@Override
	public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
		switch (event.getType()) {
			case CHILD_ADDED:
				onChildAdded(client, event.getData());
				break;
			case CHILD_UPDATED:
				break;
			case CHILD_REMOVED:
				onChildRemoved(client, event.getData());
				break;
			case CONNECTION_SUSPENDED:
				break;
			case CONNECTION_RECONNECTED:
				break;
			case CONNECTION_LOST:
				break;
			case INITIALIZED:
				// TODO!!
				// when this admin is first elected leader and there are
				// jobs, it needs to verify that the jobs have been
				// deployed
				// for (ChildData childData : event.getInitialData()) {
				// LOG.info("Existing job: {}", Paths.stripPath(childData.getPath()));
				// }
				break;
		}
	}

	/**
	 * Handle the creation of a new job deployment.
	 * 
	 * @param client curator client
	 * @param data   job deployment request data
	 */
	private void onChildAdded(CuratorFramework client, ChildData data) throws Exception {
		String jobName = Paths.stripPath(data.getPath());
		byte[] bytes = client.getData().forPath(Paths.build(Paths.JOBS, jobName));
		Map<String, String> map = mapBytesUtility.toMap(bytes);
		JobDefinition jobDefinition = new JobDefinition(jobName, map.get("definition"));

		LOG.info("Deploying job {}", jobDefinition);
		deployJob(client, jobDefinition);
	}

	/**
	 * Handle the deletion of a job deployment.
	 * 
	 * @param client curator client
	 * @param data   job deployment request data
	 */
	private void onChildRemoved(CuratorFramework client, ChildData data) throws Exception {
		String jobName = Paths.stripPath(data.getPath());
		LOG.info("Undeploying job {}", jobName);

		try {
			byte[] bytes = client.getData().forPath(Paths.build(Paths.JOBS, jobName));
			Map<String, String> map = mapBytesUtility.toMap(bytes);
			JobDefinition jobDefinition = new JobDefinition(jobName, map.get("definition"));
			undeployJob(client, jobDefinition);
		}
		catch (KeeperException.NoNodeException e) {
			LOG.debug("Job definition {} has already been removed", jobName);
		}
	}

	/**
	 * Issue deployment requests for the job.
	 * 
	 * @param client         curator client
	 * @param jobDefinition  job to be deployed
	 */
	private void deployJob(CuratorFramework client, JobDefinition jobDefinition) throws Exception {
		Map<Container, String> mapDeploymentStatus = new HashMap<Container, String>();
		String jobName = jobDefinition.getName();

		// create a ModuleDescriptor for the job using the static helper method;
		// eventually ModuleDescriptor will be part of the Job object model
		Iterator<Container> containerIterator = containerMatcher.match(
				createJobModuleDescriptor(jobName), containerRepository).iterator();
		if (containerIterator.hasNext()) {
			Container container = containerIterator.next();
			String containerName = container.getName();

			List<ModuleDeploymentRequest> results = this.parser.parse(jobName, jobDefinition.getDefinition(),
					ParsingContext.job);
			ModuleDeploymentRequest mdr = results.get(0);
			String moduleLabel = mdr.getModule() + "-0";
			String moduleType = ModuleType.job.toString();
			try {
				// todo: consider something more abstract for stream name
				// OR separate path builders for stream-modules and jobs
				client.create().creatingParentsIfNeeded().forPath(new ModuleDeploymentsPath().setContainer(containerName)
						.setStreamName(jobName)
						.setModuleType(moduleType)
						.setModuleLabel(moduleLabel).build());

				String deploymentPath = new JobsPath().setJobName(jobName)
						.setModuleLabel(moduleLabel)
						.setContainer(containerName).build();
				mapDeploymentStatus.put(container, deploymentPath);
			}
			catch (KeeperException.NodeExistsException e) {
				LOG.info("Job {} is already deployed to container {}", jobDefinition, container);
			}

			// wait for all deployments to succeed
			// todo: make timeout configurable
			long timeout = System.currentTimeMillis() + 30000;

			do {
				for (Iterator<Map.Entry<Container, String>> iteratorStatus =
							 mapDeploymentStatus.entrySet().iterator(); iteratorStatus.hasNext();) {
					Map.Entry<Container, String> entry =
							iteratorStatus.next();
					if (client.checkExists().forPath(entry.getValue()) != null) {
						iteratorStatus.remove();
					}
					Thread.sleep(10);
				}
			}
			while (!mapDeploymentStatus.isEmpty() && System.currentTimeMillis() < timeout);

			if (!mapDeploymentStatus.isEmpty()) {
				// todo: if the container went away we should select another one to deploy to;
				// otherwise this reflects a bug in the container or some kind of network
				// error in which case the state of deployment is "unknown"
				throw new IllegalStateException(String.format(
						"Deployment of job %s to the following containers timed out: %s", jobName,
						mapDeploymentStatus.keySet()));
			}
		}
		else {
			LOG.info("No containers available to deploy job {}", jobName);
		}

	}

	/**
	 * Issue undeployment requests for the job.
	 * 
	 * @param client         curator client
	 * @param jobDefinition  job to be undeployed
	 * 
	 * @throws Exception
	 */
	private void undeployJob(CuratorFramework client, JobDefinition jobDefinition) throws Exception {
		String jobPath = Paths.build(Paths.JOBS, jobDefinition.getName());
		List<String> children = client.getChildren().forPath(jobPath);
		for (String child : children) {
			String path = jobPath + "/" + child;
			try {
				client.delete().deletingChildrenIfNeeded().forPath(path);
			}
			catch (KeeperException.NoNodeException e) {
				LOG.trace("Path {} already deleted", path);
			}
		}
	}

	/**
	 * Create an instance of {@link ModuleDescriptor} for a given job name.
	 * This helper method is intended for use in {@link ContainerMatcher#match(ModuleDescriptor, ContainerRepository)}
	 * when deploying jobs. This is intended to be temporary; future revisions of Jobs will include
	 * ModuleDescriptors.
	 *
	 * @param jobName job name
	 *
	 * @return a ModuleDescriptor for the given job
	 */
	public static ModuleDescriptor createJobModuleDescriptor(String jobName) {
		return new ModuleDescriptor(new ModuleDefinition(jobName, ModuleType.job),
				jobName, jobName, 0, /*group*/ null, 1);
	}

}
