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

import java.util.Iterator;
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
import org.springframework.xd.dirt.core.ModuleDeploymentProperties;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDescriptor;
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.ParsingContext;
import org.springframework.xd.dirt.stream.XDStreamParser;
import org.springframework.xd.dirt.util.MapBytesUtility;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;

/**
 * Listener implementation that handles job deployment requests.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class JobDeploymentListener implements PathChildrenCacheListener {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(JobDeploymentListener.class);

	/**
	 * Provides access to the current container list.
	 */
	private final ContainerRepository containerRepository;

	/**
	 * Utility to convert maps to byte arrays.
	 */
	private final MapBytesUtility mapBytesUtility = new MapBytesUtility();

	/**
	 * Container matcher for matching modules to containers.
	 */
	private final ContainerMatcher containerMatcher;

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
	 * Utility for writing module deployment requests to containers.
	 */
	private final ModuleDeploymentWriter moduleDeploymentWriter;


	/**
	 * Construct a JobDeploymentListener.
	 *
	 * @param zkConnection                  ZooKeeper connection
	 * @param containerRepository           repository to obtain container data
	 * @param moduleDefinitionRepository    repository to obtain module data
	 * @param moduleOptionsMetadataResolver resolver for module options metadata
	 * @param containerMatcher              matches modules to containers
	 */
	public JobDeploymentListener(ZooKeeperConnection zkConnection,
			ContainerRepository containerRepository,
			ModuleDefinitionRepository moduleDefinitionRepository,
			ModuleOptionsMetadataResolver moduleOptionsMetadataResolver,
			ContainerMatcher containerMatcher) {
		this.containerRepository = containerRepository;
		this.parser = new XDStreamParser(moduleDefinitionRepository, moduleOptionsMetadataResolver);
		this.moduleDeploymentWriter = new ModuleDeploymentWriter(zkConnection,
				containerRepository, containerMatcher);
		this.containerMatcher = containerMatcher;
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
	 * @param data   job deployment request data
	 */
	private void onChildAdded(CuratorFramework client, ChildData data) throws Exception {
		String jobName = Paths.stripPath(data.getPath());
		deployJob(client, jobName);
	}

	/**
	 * Issue deployment requests for a job. This deployment will occur
	 * if:
	 * <ul>
	 *     <li>the job has not been destroyed</li>
	 *     <li>the job has not been undeployed</li>
	 *     <li>there is a container that can deploy the job</li>
	 * </ul>
	 *
	 * @param client      curator client
	 * @param jobName     name of job to redeploy
	 * @throws Exception
	 */
	private void deployJob(CuratorFramework client, String jobName) throws Exception {
		JobDefinition jobDefinition = loadJob(client, jobName);
		if (jobDefinition != null) {
			ModuleDescriptor descriptor = parser.parse(jobName, jobDefinition.getDefinition(),
					ParsingContext.job).get(0);

			Iterator<Container> iterator = containerMatcher.match(descriptor,
					ModuleDeploymentProperties.defaultInstance, containerRepository).iterator();
			if (iterator.hasNext()) {
				Container targetContainer = iterator.next();
				ModuleDeploymentWriter.Result result =
						moduleDeploymentWriter.writeDeployment(descriptor, targetContainer);
				moduleDeploymentWriter.validateResult(result);
			}
			else {
				logger.warn("No containers available for deployment of {}", jobName);
			}
		}
	}

	/**
	 * Load the {@link org.springframework.xd.dirt.stream.JobDefinition}
	 * instance for a given job name if the job definition is present <i>and the
	 * job is deployed</i>.
	 *
	 * @param client   curator client
	 * @param jobName  the name of the job to load
	 * @return the job instance, or {@code null} if the job does not exist or is not deployed
	 * @throws Exception
	 */
	private JobDefinition loadJob(CuratorFramework client, String jobName) throws Exception {
		try {
			if (client.checkExists().forPath(Paths.build(Paths.JOB_DEPLOYMENTS, jobName)) != null) {
				byte[] data = client.getData().forPath(Paths.build(Paths.JOBS, jobName));
				Map<String, String> map = mapBytesUtility.toMap(data);
				return new JobDefinition(jobName, map.get("definition"));
			}
		}
		catch (KeeperException.NoNodeException e) {
			// job is not deployed
		}
		return null;
	}

}
