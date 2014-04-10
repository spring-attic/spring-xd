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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.cluster.ContainerMatcher;
import org.springframework.xd.dirt.cluster.ContainerRepository;
import org.springframework.xd.dirt.cluster.DefaultContainerMatcher;
import org.springframework.xd.dirt.core.JobsPath;
import org.springframework.xd.dirt.core.ModuleDeploymentsPath;
import org.springframework.xd.dirt.core.ModuleDescriptor;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.core.StreamsPath;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.ParsingContext;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamFactory;
import org.springframework.xd.dirt.stream.XDStreamParser;
import org.springframework.xd.dirt.util.MapBytesUtility;
import org.springframework.xd.dirt.zookeeper.ChildPathIterator;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;

/**
 * Listener implementation that is invoked when containers are added/removed/modified.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class ContainerListener implements PathChildrenCacheListener {

	/**
	 * Logger.
	 */
	private final Logger LOG = LoggerFactory.getLogger(ContainerListener.class);

	/**
	 * Provides access to the current container list.
	 */
	private final ContainerRepository containerRepository;

	// todo: make this pluggable and/or refactor to avoid duplication with StreamListener
	private final ContainerMatcher containerMatcher = new DefaultContainerMatcher();

	/**
	 * Utility to convert maps to byte arrays.
	 */
	private final MapBytesUtility mapBytesUtility = new MapBytesUtility();

	/**
	 * Stream factory.
	 */
	private final StreamFactory streamFactory;

	/**
	 * {@link Converter} from {@link ChildData} in stream deployments to Stream name.
	 *
	 * @see #streamDeployments
	 */
	private final DeploymentNameConverter deploymentNameConverter = new DeploymentNameConverter();

	/**
	 * Cache of children under the stream deployment path.
	 */
	private final PathChildrenCache jobDeployments;

	/**
	 * Cache of children under the job deployment path.
	 */
	private final PathChildrenCache streamDeployments;

	/**
	 * Cache of children under the streams path.
	 */
	private final PathChildrenCache streamDefinitions;

	/**
	 * The parser.
	 */
	private final XDStreamParser parser;


	/**
	 * Construct a ContainerListener.
	 *
	 * @param containerRepository           repository for container data
	 * @param streamDefinitionRepository    repository for streams
	 * @param moduleDefinitionRepository    repository for module definitions
	 * @param moduleOptionsMetadataResolver resolver for module options metadata
	 * @param streamDeployments             cache of children for stream deployments path
	 * @param streamDefinitions             cache of children for streams
	 * @param jobDeployments                cache of children for job deployments path
	 */
	public ContainerListener(ContainerRepository containerRepository,
			StreamDefinitionRepository streamDefinitionRepository,
			ModuleDefinitionRepository moduleDefinitionRepository,
			ModuleOptionsMetadataResolver moduleOptionsMetadataResolver,
			PathChildrenCache streamDeployments, PathChildrenCache streamDefinitions, PathChildrenCache jobDeployments) {
		this.containerRepository = containerRepository;
		this.streamFactory = new StreamFactory(streamDefinitionRepository, moduleDefinitionRepository,
				moduleOptionsMetadataResolver);
		this.streamDeployments = streamDeployments;
		this.streamDefinitions = streamDefinitions;
		this.jobDeployments = jobDeployments;
		this.parser = new XDStreamParser(moduleDefinitionRepository, moduleOptionsMetadataResolver);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
		switch (event.getType()) {
			case CHILD_ADDED:
				onChildAdded(client, event.getData());
				break;
			case CHILD_UPDATED:
				LOG.info("Container updated: {}", Paths.stripPath(event.getData().getPath()));
				break;
			case CHILD_REMOVED:
				onChildLeft(client, event.getData());
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
	 * Handle the arrival of a container. This implementation will scan the existing streams and determine if any
	 * modules should be deployed to the new container.
	 *
	 * @param client curator client
	 * @param data node data for the container that arrived
	 */
	private void onChildAdded(CuratorFramework client, ChildData data) throws Exception {
		// TODO: there is duplicate code here and JobListener/StreamListener. These
		// should be refactored into a JobDeployer/StreamDeployer class

		final Container container = new Container(Paths.stripPath(data.getPath()), mapBytesUtility.toMap(data.getData()));
		String containerName = container.getName();
		LOG.info("Container arrived: {}", containerName);

		// check for "orphaned" jobs that can be deployed to this new container
		for (Iterator<String> jobDeploymentIterator =
					 new ChildPathIterator<String>(deploymentNameConverter, jobDeployments);
						jobDeploymentIterator.hasNext();) {
			String jobName = jobDeploymentIterator.next();

			byte[] bytes = client.getData().forPath(Paths.build(Paths.JOBS, jobName));
			Map<String, String> map = mapBytesUtility.toMap(bytes);
			JobDefinition jobDefinition = new JobDefinition(jobName, map.get("definition"));

			List<ModuleDeploymentRequest> results = parser.parse(jobName, jobDefinition.getDefinition(),
					ParsingContext.job);
			ModuleDeploymentRequest mdr = results.get(0);
			String moduleLabel = mdr.getModule() + "-0";
			String moduleType = ModuleType.job.toString();

			Stat stat = client.checkExists().forPath(new JobsPath().setJobName(jobName).setModuleLabel(moduleLabel).build());
			// if stat is null, this means that the job deployment request was written out
			// to ZK but JobListener hasn't picked it up yet; in that case skip this job
			// deployment since JobListener will handle it
			if (stat != null && stat.getNumChildren() == 0) {
				// no ephemeral nodes under the job module path; this job should be deployed
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

					// todo: make timeout configurable
					long timeout = System.currentTimeMillis() + 30000;
					boolean deployed;
					do {
						Thread.sleep(10);
						deployed = client.checkExists().forPath(deploymentPath) != null;
					}
					while (!deployed && System.currentTimeMillis() < timeout);

					if (!deployed) {
						throw new IllegalStateException(String.format(
								"Deployment of module %s to container %s timed out", jobName, containerName));
					}
				}
				catch (KeeperException.NodeExistsException e) {
					LOG.info("Job {} is already deployed to container {}", jobDefinition, container);
				}
			}
		}

		// check for "orphaned" stream modules that can be deployed to this new container
		for (Iterator<String> streamDeploymentIterator =
					 new ChildPathIterator<String>(deploymentNameConverter, streamDeployments);
						streamDeploymentIterator.hasNext();) {
			String streamName = streamDeploymentIterator.next();
			Stream stream = loadStream(client, streamName);

			for (Iterator<ModuleDescriptor> descriptorIterator = stream.getDeploymentOrderIterator(); descriptorIterator.hasNext();) {
				ModuleDescriptor descriptor = descriptorIterator.next();
				ContainerRepository containerRepository = new ContainerRepository() {

					@Override
					public Iterator<Container> getContainerIterator() {
						return Collections.singletonList(container).iterator();
					}
				};
				if (!CollectionUtils.isEmpty(containerMatcher.match(descriptor, containerRepository))) {
					String moduleType = descriptor.getModuleDefinition().getType().toString();
					String moduleName = descriptor.getModuleDefinition().getName();
					String moduleLabel = descriptor.getLabel();

					// obtain all of the containers that have deployed this module
					List<String> containersForModule = getContainersForStreamModule(client, descriptor);
					if (!containersForModule.contains(containerName)) {
						// this container has not deployed this module; determine if it should
						int moduleCount = descriptor.getDeploymentProperties().getCount();
						if (moduleCount <= 0 || containersForModule.size() < moduleCount) {
							// either the module has a count of 0 (therefore it should be deployed everywhere)
							// or the number of containers that have deployed the module is less than the
							// amount specified by the module descriptor
							LOG.info("Deploying module {} to {}", moduleName, container);

							client.create().creatingParentsIfNeeded().forPath(new ModuleDeploymentsPath()
									.setContainer(containerName)
									.setStreamName(streamName)
									.setModuleType(moduleType)
									.setModuleLabel(moduleLabel).build());

							String path = new StreamsPath()
									.setStreamName(streamName)
									.setModuleType(moduleType)
									.setModuleLabel(moduleLabel)
									.setContainer(containerName).build();

							// todo: make timeout configurable
							long timeout = System.currentTimeMillis() + 10000;
							boolean deployed;
							do {
								Thread.sleep(10);
								deployed = client.checkExists().forPath(path) != null;
							}
							while (!deployed && System.currentTimeMillis() < timeout);

							if (!deployed) {
								throw new IllegalStateException(String.format(
										"Deployment of module %s to container %s timed out", moduleName, containerName));
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Determine which containers, if any, have deployed a module for a stream.
	 *
	 * @param client      curator client
	 * @param descriptor  module descriptor
	 *
	 * @return list of containers that have deployed this module; empty list
	 *         is returned if no containers have deployed it
	 *
	 * @throws Exception  thrown by Curator
	 */
	private List<String> getContainersForStreamModule(CuratorFramework client, ModuleDescriptor descriptor)
			throws Exception {
		try {
			return client.getChildren().forPath(new StreamsPath()
					.setStreamName(descriptor.getStreamName())
					.setModuleType(descriptor.getModuleDefinition().getType().toString())
					.setModuleLabel(descriptor.getLabel()).build());
		}
		catch (KeeperException.NoNodeException e) {
			return Collections.emptyList();
		}
	}

	/**
	 * This will load the {@link Stream} instance for a given stream name. It will include the
	 * stream definition as well as any deployment properties data for the stream deployment.
	 *
	 * @param client {@link CuratorFramework} instance used to retrieve data for this stream
	 * @param streamName the name of the stream to load
	 * @return the stream instance
	 * @throws Exception if ZooKeeper access fails for any reason
	 */
	private Stream loadStream(CuratorFramework client, String streamName) throws Exception {
		Map<String, String> map = mapBytesUtility.toMap(streamDefinitions.getCurrentData(
				new StreamsPath().setStreamName(streamName).build()).getData());
		byte[] deploymentPropertiesData = client.getData().forPath(Paths.build(Paths.STREAM_DEPLOYMENTS, streamName));
		if (deploymentPropertiesData != null && deploymentPropertiesData.length > 0) {
			map.put("deploymentProperties", new String(deploymentPropertiesData, "UTF-8"));
		}
		return streamFactory.createStream(streamName, map);
	}

	/**
	 * Handle the departure of a container. This will scan the list of modules deployed to the departing container and
	 * redeploy them if required.
	 *
	 * @param client curator client
	 * @param data node data for the container that departed
	 */
	// todo: break this method up into undeployJob and undeployStreamModule
	private void onChildLeft(CuratorFramework client, ChildData data) {
		String path = data.getPath();
		String container = Paths.stripPath(path);
		LOG.info("Container departed: {}", container);
		if (client.getState() == CuratorFrameworkState.STOPPED) {
			return;
		}
		try {
			Map<String, Stream> streamMap = new HashMap<String, Stream>();
			String containerDeployments = Paths.build(Paths.MODULE_DEPLOYMENTS, container);
			List<String> deployments = client.getChildren().forPath(containerDeployments);

			for (String deployment : deployments) {
				ModuleDeploymentsPath moduleDeploymentsPath = new ModuleDeploymentsPath(containerDeployments + '/' + deployment);
				String streamName = moduleDeploymentsPath.getStreamName();
				String moduleType = moduleDeploymentsPath.getModuleType();
				String moduleLabel = moduleDeploymentsPath.getModuleLabel();

				if (ModuleType.job.toString().equals(moduleType)) {
					Iterator<Container> iterator = containerMatcher.match(JobListener.createJobModuleDescriptor(streamName),
							containerRepository).iterator();
					if (iterator.hasNext()) {
						Container target = iterator.next();
						String targetName = target.getName();
						LOG.info("Redeploying job {} to container {}", streamName, targetName);
						client.create().creatingParentsIfNeeded().forPath(new ModuleDeploymentsPath()
								.setContainer(targetName)
								.setStreamName(streamName)
								.setModuleType(moduleType)
								.setModuleLabel(moduleLabel).build()
								);
					}
					else {
						LOG.warn("No containers available for redeployment of job {}", streamName);
					}
				}
				else {
					Stream stream = streamMap.get(streamName);
					if (stream == null) {
						stream = loadStream(client, streamName);
						streamMap.put(streamName, stream);
					}
					ModuleDescriptor moduleDescriptor = stream.getModuleDescriptor(moduleLabel, moduleType);
					if (moduleDescriptor.getDeploymentProperties().getCount() > 0) {
						Iterator<Container> iterator = containerMatcher.match(moduleDescriptor, containerRepository).iterator();
						if (iterator.hasNext()) {
							Container targetContainer = iterator.next();
							String targetName = targetContainer.getName();

							LOG.info("Redeploying module {} for stream {} to container {}",
									moduleLabel, streamName, targetName);

							client.create().creatingParentsIfNeeded().forPath(new ModuleDeploymentsPath()
									.setContainer(targetName)
									.setStreamName(streamName)
									.setModuleType(moduleType)
									.setModuleLabel(moduleLabel).build()
									);

							// todo: consider redeployment verification
						}
						else {
							LOG.warn("No containers available for redeployment of {} for stream {}", moduleLabel,
									streamName);
						}
					}
					else {
						StringBuilder builder = new StringBuilder();
						String criteria = moduleDescriptor.getDeploymentProperties().getCriteria();
						builder.append("Module '").append(moduleLabel).append("' is targeted to all containers");
						if (StringUtils.hasText(criteria)) {
							builder.append(" matching criteria '").append(criteria).append('\'');
						}
						builder.append("; it does not need to be redeployed");
						LOG.info(builder.toString());
					}
				}
			}

			// remove the deployments from the departed container
			client.delete().deletingChildrenIfNeeded().forPath(Paths.build(Paths.MODULE_DEPLOYMENTS, container));
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
		}
	}


	/**
	 * Converter from {@link ChildData} to deployment name string.
	 */
	public class DeploymentNameConverter implements Converter<ChildData, String> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String convert(ChildData source) {
			return Paths.stripPath(source.getPath());
		}
	}

}
