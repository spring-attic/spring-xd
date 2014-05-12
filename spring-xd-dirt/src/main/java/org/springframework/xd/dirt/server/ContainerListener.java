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
import org.springframework.xd.dirt.core.JobDeploymentsPath;
import org.springframework.xd.dirt.core.ModuleDeploymentProperties;
import org.springframework.xd.dirt.core.ModuleDeploymentsPath;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.core.StreamDeploymentsPath;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDescriptor;
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.ParsingContext;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamFactory;
import org.springframework.xd.dirt.stream.XDStreamParser;
import org.springframework.xd.dirt.util.MapBytesUtility;
import org.springframework.xd.dirt.zookeeper.ChildPathIterator;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
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
	private final Logger logger = LoggerFactory.getLogger(ContainerListener.class);

	/**
	 * Provides access to the current container list.
	 */
	private final ContainerRepository containerRepository;

	/**
	 * Container matcher for matching modules to containers.
	 */
	private final ContainerMatcher containerMatcher;

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
	 * The parser.
	 */
	private final XDStreamParser parser;

	/**
	 * Utility for writing module deployment requests to containers.
	 */
	private final ModuleDeploymentWriter moduleDeploymentWriter;

	/**
	 * Utility for loading streams and jobs (including deployment metadata).
	 */
	private final DeploymentLoader deploymentLoader = new DeploymentLoader();


	/**
	 * Construct a ContainerListener.
	 *
	 * @param zkConnection ZooKeeper connection
	 * @param containerRepository repository for container data
	 * @param streamDefinitionRepository repository for streams
	 * @param moduleDefinitionRepository repository for module definitions
	 * @param moduleOptionsMetadataResolver resolver for module options metadata
	 * @param streamDeployments cache of children for stream deployments path
	 * @param streamDefinitions cache of children for streams
	 * @param jobDeployments cache of children for job deployments path
	 * @param containerMatcher matches modules to containers
	 */
	public ContainerListener(ZooKeeperConnection zkConnection,
			ContainerRepository containerRepository,
			StreamDefinitionRepository streamDefinitionRepository,
			ModuleDefinitionRepository moduleDefinitionRepository,
			ModuleOptionsMetadataResolver moduleOptionsMetadataResolver,
			PathChildrenCache streamDeployments, PathChildrenCache streamDefinitions,
			PathChildrenCache jobDeployments, ContainerMatcher containerMatcher) {
		this.containerRepository = containerRepository;
		this.streamFactory = new StreamFactory(streamDefinitionRepository, moduleDefinitionRepository,
				moduleOptionsMetadataResolver);
		this.streamDeployments = streamDeployments;
		this.jobDeployments = jobDeployments;
		this.parser = new XDStreamParser(moduleDefinitionRepository, moduleOptionsMetadataResolver);
		this.moduleDeploymentWriter = new ModuleDeploymentWriter(zkConnection,
				containerRepository, containerMatcher);
		this.containerMatcher = containerMatcher;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
		ZooKeeperConnection.logCacheEvent(logger, event);
		switch (event.getType()) {
			case CHILD_ADDED:
				onChildAdded(client, event.getData());
				break;
			case CHILD_UPDATED:
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
	 * Handle the arrival of a container. This implementation will scan the
	 * existing streams/jobs and determine if any modules should be deployed to
	 * the new container.
	 *
	 * @param client curator client
	 * @param data node data for the container that arrived
	 */
	private void onChildAdded(CuratorFramework client, ChildData data) throws Exception {
		Container container = new Container(Paths.stripPath(data.getPath()), mapBytesUtility.toMap(data.getData()));
		logger.info("Container arrived: {}", container.getName());

		deployOrphanedJobs(client, container);
		deployOrphanedStreams(client, container);
	}

	/**
	 * Deploy any "orphaned" jobs (jobs that are supposed to be deployed but
	 * do not have any modules deployed in any container).
	 *
	 * @param client      curator client
	 * @param container   target container for job module deployment
	 * @throws Exception
	 */
	private void deployOrphanedJobs(CuratorFramework client, Container container) throws Exception {
		// check for "orphaned" jobs that can be deployed to this new container
		for (Iterator<String> jobDeploymentIterator =
				new ChildPathIterator<String>(deploymentNameConverter, jobDeployments);
						jobDeploymentIterator.hasNext();) {
			String jobName = jobDeploymentIterator.next();

			// if jobDefinition is null this means the job was destroyed or undeployed
			JobDefinition jobDefinition = deploymentLoader.loadJob(client, jobName);
			if (jobDefinition != null) {
				ModuleDescriptor descriptor = parser.parse(jobName, jobDefinition.getDefinition(),
						ParsingContext.job).iterator().next();
				String moduleLabel = descriptor.getModuleLabel();
				String path = new JobDeploymentsPath().setJobName(jobName).setModuleLabel(moduleLabel).build();
				Stat stat = client.checkExists().forPath(path);
				// if stat is null, this means that the job deployment request was written out
				// to ZK but JobDeploymentListener hasn't picked it up yet; in that case skip
				// this job deployment since JobDeploymentListener will handle it
				if (stat != null && stat.getNumChildren() == 0) {
					// no ephemeral nodes under the job module path; this job should be deployed
					ModuleDeploymentWriter.Result result = moduleDeploymentWriter.writeDeployment(descriptor, container);
					moduleDeploymentWriter.validateResults(Collections.singleton(result));
				}
			}
		}
	}

	/**
	 * Deploy any "orphaned" stream modules. These are stream modules that
	 * are supposed to be deployed but currently do not have enough containers
	 * available to meet the deployment criteria (i.e. if a module requires
	 * 3 containers but only 2 are available). Furthermore this will deploy
	 * modules with a count of 0 if this container matches the deployment
	 * criteria.
	 *
	 * @param client     curator client
	 * @param container  target container for stream module deployment
	 * @throws Exception
	 */
	private void deployOrphanedStreams(CuratorFramework client, Container container) throws Exception {
		// iterate the cache of stream deployments
		for (Iterator<String> streamDeploymentIterator =
					new ChildPathIterator<String>(deploymentNameConverter, streamDeployments);
							streamDeploymentIterator.hasNext();) {
			String streamName = streamDeploymentIterator.next();
			Stream stream = deploymentLoader.loadStream(client, streamName, streamFactory);

			// if stream is null this means the stream was destroyed or undeployed
			if (stream != null) {
				for (Iterator<ModuleDescriptor> descriptorIterator = stream.getDeploymentOrderIterator();
						descriptorIterator.hasNext();) {
					ModuleDescriptor descriptor = descriptorIterator.next();
					ModuleDeploymentProperties moduleDeploymentProperties =
							StreamDeploymentListener.createModuleDeploymentProperties(
									stream.getDeploymentProperties(), descriptor);

					if (isCandidateForDeployment(container, descriptor, moduleDeploymentProperties)) {
						// obtain all of the containers that have deployed this module
						List<String> containersForModule = getContainersForStreamModule(client, descriptor);
						if (!containersForModule.contains(container.getName())) {
							// this container has not deployed this module; determine if it should
							int moduleCount = moduleDeploymentProperties.getCount();
							if (moduleCount <= 0 || containersForModule.size() < moduleCount) {
								// either the module has a count of 0 (therefore it should be deployed everywhere)
								// or the number of containers that have deployed the module is less than the
								// amount specified by the module descriptor
								logger.info("Deploying module {} to {}",
										descriptor.getModuleDefinition().getName(), container);

								ModuleDeploymentWriter.Result result =
										moduleDeploymentWriter.writeDeployment(descriptor, container);
								moduleDeploymentWriter.validateResults(Collections.singleton(result));
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Return true if the {@link org.springframework.xd.dirt.cluster.Container}
	 * is allowed to deploy the {@link org.springframework.xd.dirt.module.ModuleDescriptor}
	 * given the criteria present in the
	 * {@link org.springframework.xd.dirt.core.ModuleDeploymentProperties}.
	 *
	 * @param container   target container
	 * @param descriptor  module descriptor
	 * @param properties  deployment properties
	 * @return true if the container is allowed to deploy the module
	 */
	private boolean isCandidateForDeployment(final Container container, ModuleDescriptor descriptor,
			ModuleDeploymentProperties properties) {
		ContainerRepository containerRepository = new ContainerRepository() {

			@Override
			public Iterator<Container> getContainerIterator() {
				return Collections.singletonList(container).iterator();
			}
		};

		return !CollectionUtils.isEmpty(containerMatcher.match(descriptor, properties, containerRepository));
	}

	/**
	 * Determine which containers, if any, have deployed a module for a stream.
	 *
	 * @param client curator client
	 * @param descriptor module descriptor
	 *
	 * @return list of containers that have deployed this module; empty list is
	 *         returned if no containers have deployed it
	 *
	 * @throws Exception thrown by Curator
	 */
	private List<String> getContainersForStreamModule(CuratorFramework client, ModuleDescriptor descriptor)
			throws Exception {
		try {
			return client.getChildren().forPath(new StreamDeploymentsPath()
					.setStreamName(descriptor.getGroup())
					.setModuleType(descriptor.getModuleDefinition().getType().toString())
					.setModuleLabel(descriptor.getModuleLabel()).build());
		}
		catch (KeeperException.NoNodeException e) {
			return Collections.emptyList();
		}
	}

	/**
	 * Handle the departure of a container. This will scan the list of modules
	 * deployed to the departing container and redeploy them if required.
	 *
	 * @param client curator client
	 * @param data   node data for the container that departed
	 */
	private void onChildLeft(CuratorFramework client, ChildData data) throws Exception {
		String path = data.getPath();
		String container = Paths.stripPath(path);
		logger.info("Container departed: {}", container);
		if (client.getState() == CuratorFrameworkState.STOPPED) {
			return;
		}

		// the departed container may have hosted multiple modules
		// for the same stream; therefore each stream that is loaded
		// will be cached to avoid reloading for each module
		Map<String, Stream> streamMap = new HashMap<String, Stream>();

		String containerDeployments = Paths.build(Paths.MODULE_DEPLOYMENTS, container);
		List<String> deployments = client.getChildren().forPath(containerDeployments);
		for (String deployment : deployments) {
			ModuleDeploymentsPath moduleDeploymentsPath =
					new ModuleDeploymentsPath(Paths.build(containerDeployments, deployment));
			String unitName = moduleDeploymentsPath.getStreamName();
			String moduleType = moduleDeploymentsPath.getModuleType();
			String moduleLabel = moduleDeploymentsPath.getModuleLabel();

			if (ModuleType.job.toString().equals(moduleType)) {
				redeployJob(client, unitName);
			}
			else {
				Stream stream = streamMap.get(unitName);
				if (stream == null) {
					stream = deploymentLoader.loadStream(client, unitName, streamFactory);
					streamMap.put(unitName, stream);
				}
				if (stream != null) {
					redeployStreamModule(stream, moduleType, moduleLabel);
				}
			}
		}

		// remove the deployments from the departed container
		client.delete().deletingChildrenIfNeeded().forPath(Paths.build(Paths.MODULE_DEPLOYMENTS, container));
	}

	/**
	 * Redeploy a module for a stream to a container. This redeployment will occur
	 * if:
	 * <ul>
	 *     <li>the module needs to be redeployed per the deployment properties</li>
	 *     <li>the stream has not been destroyed</li>
	 *     <li>the stream has not been undeployed</li>
	 *     <li>there is a container that can deploy the stream module</li>
	 * </ul>
	 *
	 * @param stream       stream for module
	 * @param moduleType   module type
	 * @param moduleLabel  module label
	 * @throws InterruptedException
	 */
	private void redeployStreamModule(Stream stream, String moduleType, String moduleLabel) throws InterruptedException {
		String streamName = stream.getName();
		ModuleDescriptor moduleDescriptor = stream.getModuleDescriptor(moduleLabel, moduleType);
		ModuleDeploymentProperties moduleDeploymentProperties =
				StreamDeploymentListener.createModuleDeploymentProperties(
						stream.getDeploymentProperties(), moduleDescriptor);
		if (moduleDeploymentProperties.getCount() > 0) {
			Iterator<Container> iterator = containerMatcher.match(moduleDescriptor,
					moduleDeploymentProperties, containerRepository).iterator();
			if (iterator.hasNext()) {
				Container targetContainer = iterator.next();
				ModuleDeploymentWriter.Result result =
						moduleDeploymentWriter.writeDeployment(moduleDescriptor, targetContainer);
				moduleDeploymentWriter.validateResult(result);
			}
			else {
				logger.warn("No containers available for redeployment of {} for stream {}", moduleLabel,
						streamName);
			}
		}
		else {
			StringBuilder builder = new StringBuilder();
			String criteria = moduleDeploymentProperties.getCriteria();
			builder.append("Module '").append(moduleLabel).append("' is targeted to all containers");
			if (StringUtils.hasText(criteria)) {
				builder.append(" matching criteria '").append(criteria).append('\'');
			}
			builder.append("; it does not need to be redeployed");
			logger.info(builder.toString());
		}
	}

	/**
	 * Redeploy a module for a job to a container. This redeployment will occur
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
	private void redeployJob(CuratorFramework client, String jobName) throws Exception {
		JobDefinition jobDefinition = deploymentLoader.loadJob(client, jobName);
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
				logger.warn("No containers available for redeployment of {}", jobName);
			}
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
