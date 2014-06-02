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

import java.util.Collection;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.cluster.ContainerMatcher;
import org.springframework.xd.dirt.cluster.ContainerRepository;
import org.springframework.xd.dirt.cluster.NoContainerException;
import org.springframework.xd.dirt.cluster.RedeploymentContainerMatcher;
import org.springframework.xd.dirt.core.Job;
import org.springframework.xd.dirt.core.JobDeploymentsPath;
import org.springframework.xd.dirt.core.ModuleDeploymentsPath;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.core.StreamDeploymentsPath;
import org.springframework.xd.dirt.job.JobFactory;
import org.springframework.xd.dirt.stream.StreamFactory;
import org.springframework.xd.dirt.util.DeploymentPropertiesUtility;
import org.springframework.xd.dirt.util.MapBytesUtility;
import org.springframework.xd.dirt.zookeeper.ChildPathIterator;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;

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
	 * Utility to convert maps to byte arrays.
	 */
	private final MapBytesUtility mapBytesUtility = new MapBytesUtility();

	/**
	 * Container matcher for matching modules to containers.
	 */
	private final ContainerMatcher containerMatcher;

	/**
	 * Utility for writing module deployment requests to ZooKeeper.
	 */
	private final ModuleDeploymentWriter moduleDeploymentWriter;

	/**
	 * Utility for loading streams and jobs (including deployment metadata).
	 */
	private final DeploymentLoader deploymentLoader = new DeploymentLoader();

	/**
	 * Stream factory.
	 */
	private final StreamFactory streamFactory;

	/**
	 * Job factory.
	 */
	private final JobFactory jobFactory;

	/**
	 * {@link Converter} from {@link ChildData} in stream deployments to Stream name.
	 *
	 * @see #streamDeployments
	 */
	private final DeploymentNameConverter deploymentNameConverter = new DeploymentNameConverter();

	/**
	 * Cache of children under the job deployment path.
	 */
	private final PathChildrenCache jobDeployments;

	/**
	 * Cache of children under the stream deployment path.
	 */
	private final PathChildrenCache streamDeployments;


	/**
	 * Construct a ContainerListener.
	 *
	 * @param zkConnection ZooKeeper connection
	 * @param streamFactory factory to construct {@link Stream}
	 * @param jobFactory factory to construct {@link Job}
	 * @param streamDeployments cache of children for stream deployments path
	 * @param jobDeployments cache of children for job deployments path
	 * @param containerMatcher matches modules to containers
	 */
	public ContainerListener(ZooKeeperConnection zkConnection,
			ContainerRepository containerRepository,
			StreamFactory streamFactory, JobFactory jobFactory,
			PathChildrenCache streamDeployments, PathChildrenCache jobDeployments,
			ContainerMatcher containerMatcher) {
		this.containerMatcher = containerMatcher;
		this.moduleDeploymentWriter = new ModuleDeploymentWriter(zkConnection,
				containerRepository, containerMatcher);
		this.streamFactory = streamFactory;
		this.jobFactory = jobFactory;
		this.streamDeployments = streamDeployments;
		this.jobDeployments = jobDeployments;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
		ZooKeeperUtils.logCacheEvent(logger, event);
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

		redeployJobs(client, container);
		redeployStreams(client, container);
	}

	/**
	 * Deploy any "orphaned" jobs (jobs that are supposed to be deployed but
	 * do not have any modules deployed in any container).
	 *
	 * @param client curator client
	 * @param container target container for job module deployment
	 * @throws Exception
	 */
	private void redeployJobs(CuratorFramework client, Container container) throws Exception {
		// check for "orphaned" jobs that can be deployed to this new container
		for (Iterator<String> jobDeploymentIterator =
				new ChildPathIterator<String>(deploymentNameConverter, jobDeployments); jobDeploymentIterator.hasNext();) {
			String jobName = jobDeploymentIterator.next();

			// if job is null this means the job was destroyed or undeployed
			Job job = deploymentLoader.loadJob(client, jobName, this.jobFactory);
			if (job != null) {
				ModuleDescriptor descriptor = job.getJobModuleDescriptor();

				// See XD-1777: in order to support partitioning we now include
				// module deployment properties in the module deployment request
				// path. However in this case the original path is not available
				// so this redeployment will simply create a new instance of
				// properties. As a result, if this module had a partition index
				// assigned to it, it will no longer be associated with that
				// partition index. This will be fixed in the future.
				ModuleDeploymentProperties moduleDeploymentProperties = DeploymentPropertiesUtility.createModuleDeploymentProperties(
						job.getDeploymentProperties(), descriptor);
				if (isCandidateForDeployment(container, descriptor, moduleDeploymentProperties)) {
					int moduleCount = moduleDeploymentProperties.getCount();
					if (moduleCount <= 0 || getContainersForJobModule(client, descriptor).size() < moduleCount) {
						// either the module has a count of 0 (therefore it should be deployed everywhere)
						// or the number of containers that have deployed the module is less than the
						// amount specified by the module descriptor
						logger.info("Deploying module {} to {}",
								descriptor.getModuleDefinition().getName(), container);

						try {
							ModuleDeploymentWriter.Result result =
									moduleDeploymentWriter.writeDeployment(descriptor, container);
							moduleDeploymentWriter.validateResult(result);
						}
						catch (NoContainerException e) {
							logger.warn("Could not deploy job {} to container {}; " +
									"this container may have just departed the cluster", job.getName(), container);
						}
					}
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
	 * @param client curator client
	 * @param container target container for stream module deployment
	 * @throws Exception
	 */
	private void redeployStreams(CuratorFramework client, Container container) throws Exception {
		// iterate the cache of stream deployments
		for (Iterator<String> streamDeploymentIterator =
				new ChildPathIterator<String>(deploymentNameConverter, streamDeployments); streamDeploymentIterator.hasNext();) {
			String streamName = streamDeploymentIterator.next();
			Stream stream = deploymentLoader.loadStream(client, streamName, streamFactory);

			// if stream is null this means the stream was destroyed or undeployed
			if (stream != null) {
				for (Iterator<ModuleDescriptor> descriptorIterator = stream.getDeploymentOrderIterator(); descriptorIterator.hasNext();) {
					ModuleDescriptor descriptor = descriptorIterator.next();

					// See XD-1777: in order to support partitioning we now include
					// module deployment properties in the module deployment request
					// path. However in this case the original path is not available
					// so this redeployment will simply create a new instance of
					// properties. As a result, if this module had a partition index
					// assigned to it, it will no longer be associated with that
					// partition index. This will be fixed in the future.
					ModuleDeploymentProperties moduleDeploymentProperties = DeploymentPropertiesUtility.createModuleDeploymentProperties(
							stream.getDeploymentProperties(), descriptor);

					if (isCandidateForDeployment(container, descriptor, moduleDeploymentProperties)) {
						// this container has not deployed this module; determine if it should
						int moduleCount = moduleDeploymentProperties.getCount();
						if (moduleCount <= 0 || getContainersForStreamModule(client, descriptor).size() < moduleCount) {
							// either the module has a count of 0 (therefore it should be deployed everywhere)
							// or the number of containers that have deployed the module is less than the
							// amount specified by the module descriptor
							logger.info("Deploying module {} to {}",
									descriptor.getModuleDefinition().getName(), container);

							try {
								ModuleDeploymentWriter.Result result =
										moduleDeploymentWriter.writeDeployment(descriptor, container);
								moduleDeploymentWriter.validateResult(result);
							}
							catch (NoContainerException e) {
								logger.warn("Could not deploy module {} for stream {} to container {}; " +
										"this container may have just departed the cluster",
										descriptor.getModuleDefinition().getName(),
										stream.getName(), container);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Return true if the {@link org.springframework.xd.dirt.cluster.Container}
	 * is allowed to deploy the {@link org.springframework.xd.module.ModuleDescriptor}
	 * given the criteria present in the
	 * {@link org.springframework.xd.module.ModuleDeploymentProperties}.
	 *
	 * @param container target container
	 * @param descriptor module descriptor
	 * @param properties deployment properties
	 * @return true if the container is allowed to deploy the module
	 */
	private boolean isCandidateForDeployment(final Container container, ModuleDescriptor descriptor,
			ModuleDeploymentProperties properties) {
		return !CollectionUtils.isEmpty(containerMatcher.match(descriptor, properties,
				Collections.singletonList(container)));
	}

	/**
	 * Determine which containers, if any, have deployed a module for a stream.
	 *
	 * @param client curator client
	 * @param descriptor module descriptor
	 *
	 * @return list of containers that have deployed this module; empty
	 * list is returned if no containers have deployed it
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
	 * Determine which containers, if any, have deployed job module of the given job.
	 *
	 * @param client curator client
	 * @param descriptor module descriptor
	 *
	 * @return list of containers that have deployed this module; empty list is returned if no containers have deployed
	 *         it
	 *
	 * @throws Exception thrown by Curator
	 */
	private List<String> getContainersForJobModule(CuratorFramework client, ModuleDescriptor descriptor)
			throws Exception {
		try {
			return client.getChildren().forPath(new JobDeploymentsPath()
					.setJobName(descriptor.getGroup())
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
	 * @param data node data for the container that departed
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

			// reuse the module deployment properties used to deploy
			// this module; this may contain properties specific to
			// the container that just departed (such as partition
			// index in the case of partitioned streams)
			ModuleDeploymentProperties deploymentProperties = new ModuleDeploymentProperties();
			deploymentProperties.putAll(mapBytesUtility.toMap(
					client.getData().forPath(moduleDeploymentsPath.build())));

			String unitName = moduleDeploymentsPath.getStreamName();
			String moduleType = moduleDeploymentsPath.getModuleType();
			String moduleLabel = moduleDeploymentsPath.getModuleLabel();

			if (ModuleType.job.toString().equals(moduleType)) {
				Job job = deploymentLoader.loadJob(client, unitName, this.jobFactory);
				if (job != null) {
					redeployJobModule(client, job, deploymentProperties);
				}
			}
			else {
				Stream stream = streamMap.get(unitName);
				if (stream == null) {
					stream = deploymentLoader.loadStream(client, unitName, this.streamFactory);
					streamMap.put(unitName, stream);
				}
				if (stream != null) {
					redeployStreamModule(client, stream, moduleType, moduleLabel, deploymentProperties);
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
	 * 		<li>the module needs to be redeployed per the deployment properties</li>
	 * 		<li>the stream has not been destroyed</li>
	 * 		<li>the stream has not been undeployed</li>
	 * 		<li>there is a container that can deploy the stream module</li>
	 * </ul>
	 *
	 * @param stream                stream for module
	 * @param moduleType            module type
	 * @param moduleLabel           module label
	 * @param deploymentProperties  deployment properties for stream module
	 * @throws InterruptedException
	 */
	private void redeployStreamModule(CuratorFramework client, final Stream stream, String moduleType,
			String moduleLabel, ModuleDeploymentProperties deploymentProperties) throws Exception {
		ModuleDescriptor moduleDescriptor = stream.getModuleDescriptor(moduleLabel, moduleType);

		// the passed in deploymentProperties were loaded from the
		// deployment path...merge with deployment properties
		// created at the stream level
		ModuleDeploymentProperties mergedProperties =
				DeploymentPropertiesUtility.createModuleDeploymentProperties(
						stream.getDeploymentProperties(), moduleDescriptor);
		mergedProperties.putAll(deploymentProperties);

		if (mergedProperties.getCount() > 0) {
			try {
				ModuleDeploymentWriter.Result result = moduleDeploymentWriter.writeDeployment(
						moduleDescriptor, mergedProperties,
						instantiateContainerMatcher(client, moduleDescriptor));
				moduleDeploymentWriter.validateResult(result);
			}
			catch (NoContainerException e) {
				logger.warn("No containers available for redeployment of {} for stream {}",
						moduleDescriptor.getModuleLabel(),
						stream.getName());
			}
		}
		else {
			logUnwantedRedeployment(mergedProperties.getCriteria(), moduleDescriptor.getModuleLabel());
		}
	}

	/**
	 * Redeploy a module for a job to a container. This redeployment will occur if:
	 * <ul>
	 * 		<li>the job has not been destroyed</li>
	 * 		<li>the job has not been undeployed</li>
	 * 		<li>there is a container that can deploy the job</li>
	 * </ul>
	 *
	 * @param client               curator client
	 * @param job                  job instance to redeploy
	 * @param deploymentProperties deployment properties for job module
	 * @throws Exception
	 */
	private void redeployJobModule(CuratorFramework client, final Job job,
			ModuleDeploymentProperties deploymentProperties) throws Exception {
		ModuleDescriptor moduleDescriptor = job.getJobModuleDescriptor();

		// the passed in deploymentProperties were loaded from the
		// deployment path...merge with deployment properties
		// created at the job level
		ModuleDeploymentProperties mergedProperties =
				DeploymentPropertiesUtility.createModuleDeploymentProperties(
						job.getDeploymentProperties(), moduleDescriptor);
		mergedProperties.putAll(deploymentProperties);

		if (deploymentProperties.getCount() > 0) {
			try {
				ModuleDeploymentWriter.Result result = moduleDeploymentWriter.writeDeployment(
						moduleDescriptor, mergedProperties,
						instantiateContainerMatcher(client, moduleDescriptor));
				moduleDeploymentWriter.validateResult(result);
			}
			catch (NoContainerException e) {
				logger.warn("No containers available for redeployment of {} for job {}",
						moduleDescriptor.getModuleLabel(),
						job.getName());
			}
		}
		else {
			logUnwantedRedeployment(mergedProperties.getCriteria(), moduleDescriptor.getModuleLabel());
		}
	}

	/**
	 * Return a {@link org.springframework.xd.dirt.cluster.ContainerMatcher} that
	 * will <b>exclude</b> containers that are already hosting the module.
	 *
	 * @param client            curator client
	 * @param moduleDescriptor  containers that have deployed this module
	 *                          will be excluded from the results returned
	 *                          by the {@code ContainerMatcher}.
	 * @return container matcher that will return containers not hosting
	 *         the module
	 * @throws Exception
	 */
	protected ContainerMatcher instantiateContainerMatcher(CuratorFramework client,
			ModuleDescriptor moduleDescriptor) throws Exception {
		Collection<String> containers = (moduleDescriptor.getType() == ModuleType.job)
				? getContainersForJobModule(client, moduleDescriptor)
				: getContainersForStreamModule(client, moduleDescriptor);

		return new RedeploymentContainerMatcher(containerMatcher, containers);
	}

	/**
	 * Log unwanted re-deployment of the module if the module count is less
	 * than or equal to zero.
	 * @param criteria the criteria for the module deployment
	 * @param moduleLabel the module label
	 */
	private void logUnwantedRedeployment(String criteria, String moduleLabel) {
		StringBuilder builder = new StringBuilder();
		builder.append("Module '").append(moduleLabel).append("' is targeted to all containers");
		if (StringUtils.hasText(criteria)) {
			builder.append(" matching criteria '").append(criteria).append('\'');
		}
		builder.append("; it does not need to be redeployed");
		logger.info(builder.toString());
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
