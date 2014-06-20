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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.cluster.ContainerMatcher;
import org.springframework.xd.dirt.cluster.NoContainerException;
import org.springframework.xd.dirt.cluster.RedeploymentContainerMatcher;
import org.springframework.xd.dirt.container.store.ContainerRepository;
import org.springframework.xd.dirt.core.DeploymentUnitStateCalculator;
import org.springframework.xd.dirt.core.DeploymentUnitStatus;
import org.springframework.xd.dirt.core.DeploymentUnit;
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
	 * State calculator for stream/job state.
	 */
	private final DeploymentUnitStateCalculator stateCalculator;


	/**
	 * Construct a ContainerListener.
	 *
	 * @param zkConnection ZooKeeper connection
	 * @param streamFactory factory to construct {@link Stream}
	 * @param jobFactory factory to construct {@link Job}
	 * @param streamDeployments cache of children for stream deployments path
	 * @param jobDeployments cache of children for job deployments path
	 * @param containerMatcher matches modules to containers
	 * @param stateCalculator calculator for stream/job state
	 */
	public ContainerListener(ZooKeeperConnection zkConnection,
			ContainerRepository containerRepository,
			StreamFactory streamFactory, JobFactory jobFactory,
			PathChildrenCache streamDeployments, PathChildrenCache jobDeployments,
			ContainerMatcher containerMatcher, DeploymentUnitStateCalculator stateCalculator) {
		this.containerMatcher = containerMatcher;
		this.moduleDeploymentWriter = new ModuleDeploymentWriter(zkConnection,
				containerRepository, containerMatcher);
		this.streamFactory = streamFactory;
		this.jobFactory = jobFactory;
		this.streamDeployments = streamDeployments;
		this.jobDeployments = jobDeployments;
		this.stateCalculator = stateCalculator;
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
				ModuleDeploymentProperties moduleDeploymentProperties =
						DeploymentPropertiesUtility.createModuleDeploymentProperties(
								job.getDeploymentProperties(), descriptor);
				if (isCandidateForDeployment(container, descriptor, moduleDeploymentProperties)) {
					int moduleCount = moduleDeploymentProperties.getCount();
					if (moduleCount <= 0 || getContainersForJobModule(client, descriptor).size() < moduleCount) {
						// either the module has a count of 0 (therefore it should be deployed everywhere)
						// or the number of containers that have deployed the module is less than the
						// amount specified by the module descriptor
						logger.info("Deploying module {} to {}",
								descriptor.getModuleDefinition().getName(), container);

						ModuleDeploymentStatus deploymentStatus = null;
						ModuleDeployment moduleDeployment = new ModuleDeployment(job,
								descriptor, moduleDeploymentProperties);
						try {
							deploymentStatus = deployModule(client, moduleDeployment, container);
						}
						catch (NoContainerException e) {
							logger.warn("Could not deploy job {} to container {}; " +
									"this container may have just departed the cluster", job.getName(), container);
						}
						finally {
							updateDeploymentUnitState(client, moduleDeployment, deploymentStatus);
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
				new ChildPathIterator<String>(deploymentNameConverter, streamDeployments);
						streamDeploymentIterator.hasNext();) {
			String streamName = streamDeploymentIterator.next();
			final Stream stream = deploymentLoader.loadStream(client, streamName, streamFactory);

			// if stream is null this means the stream was destroyed or undeployed
			if (stream != null) {
				for (Iterator<ModuleDescriptor> descriptorIterator =
							stream.getDeploymentOrderIterator(); descriptorIterator.hasNext();) {
					final ModuleDescriptor moduleDescriptor = descriptorIterator.next();

					// See XD-1777: in order to support partitioning we now include
					// module deployment properties in the module deployment request
					// path. However in this case the original path is not available
					// so this redeployment will simply create a new instance of
					// properties. As a result, if this module had a partition index
					// assigned to it, it will no longer be associated with that
					// partition index. This will be fixed in the future.
					final ModuleDeploymentProperties moduleDeploymentProperties =
							DeploymentPropertiesUtility.createModuleDeploymentProperties(
									stream.getDeploymentProperties(), moduleDescriptor);

					if (isCandidateForDeployment(container, moduleDescriptor, moduleDeploymentProperties)) {
						// this container has not deployed this module; determine if it should
						int moduleCount = moduleDeploymentProperties.getCount();
						if (moduleCount <= 0 || getContainersForStreamModule(client, moduleDescriptor).size() < moduleCount) {
							// either the module has a count of 0 (therefore it should be deployed everywhere)
							// or the number of containers that have deployed the module is less than the
							// amount specified by the module descriptor
							logger.info("Deploying module {} to {}",
									moduleDescriptor.getModuleDefinition().getName(), container);

							ModuleDeploymentStatus deploymentStatus = null;
							ModuleDeployment moduleDeployment =  new ModuleDeployment(
									stream, moduleDescriptor, moduleDeploymentProperties);
							try {
								deploymentStatus = deployModule(client, moduleDeployment, container);
							}
							catch (NoContainerException e) {
								logger.warn("Could not deploy module {} for stream {} to container {}; " +
										"this container may have just departed the cluster",
										moduleDescriptor.getModuleDefinition().getName(),
										stream.getName(), container);
							}
							finally {
								updateDeploymentUnitState(client, moduleDeployment, deploymentStatus);
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
		List<String> containers = new ArrayList<String>();
		String moduleType = descriptor.getModuleDefinition().getType().toString();
		String moduleLabel = descriptor.getModuleLabel();
		String moduleDeploymentPath = Paths.build(Paths.STREAM_DEPLOYMENTS, descriptor.getGroup(), Paths.MODULES);
		try {
			List<String> moduleDeployments = client.getChildren().forPath(moduleDeploymentPath);
			for (String moduleDeployment : moduleDeployments) {
				StreamDeploymentsPath path = new StreamDeploymentsPath(
						Paths.build(moduleDeploymentPath, moduleDeployment));
				if (path.getModuleType().equals(moduleType)
						&& path.getModuleLabel().equals(moduleLabel)) {
					containers.add(path.getContainer());
				}
			}
		}
		catch (KeeperException.NoNodeException e) {
			// stream has not been (or is no longer) deployed
		}
		return containers;
	}

	/**
	 * Determine which containers, if any, have deployed job module of the given job.
	 *
	 * @param client curator client
	 * @param descriptor module descriptor
	 *
	 * @return list of containers that have deployed this module; empty
	 * list is returned if no containers have deployed it
	 *
	 * @throws Exception thrown by Curator
	 */
	private List<String> getContainersForJobModule(CuratorFramework client, ModuleDescriptor descriptor)
			throws Exception {
		List<String> containers = new ArrayList<String>();
		String moduleType = descriptor.getModuleDefinition().getType().toString();
		String moduleLabel = descriptor.getModuleLabel();
		String moduleDeploymentPath = Paths.build(Paths.JOB_DEPLOYMENTS, descriptor.getGroup(), Paths.MODULES);
		try {
			List<String> moduleDeployments = client.getChildren().forPath(moduleDeploymentPath);
			for (String moduleDeployment : moduleDeployments) {
				JobDeploymentsPath path = new JobDeploymentsPath(
						Paths.build(moduleDeploymentPath, moduleDeployment));
				if (path.getModuleLabel().equals(moduleLabel)) {
					containers.add(path.getContainer());
				}
			}
		}
		catch (KeeperException.NoNodeException e) {
			// job has not been (or is no longer) deployed
		}
		return containers;
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
			ModuleDeploymentProperties deploymentProperties = new ModuleDeploymentProperties();
			deploymentProperties.putAll(mapBytesUtility.toMap(
					client.getData().forPath(moduleDeploymentsPath.build())));

			String unitName = moduleDeploymentsPath.getStreamName();
			String moduleType = moduleDeploymentsPath.getModuleType();

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
					streamModuleDeployments.add(new ModuleDeployment(stream,
							stream.getModuleDescriptor(moduleDeploymentsPath.getModuleLabel()),
							deploymentProperties));
				}
			}
		}

		for (ModuleDeployment moduleDeployment : streamModuleDeployments) {
			redeployStreamModule(client, moduleDeployment);
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
	 * @param moduleDeployment contains module redeployment details such as
	 *                         stream, module descriptor, and deployment properties
	 * @throws InterruptedException
	 */
	private void redeployStreamModule(CuratorFramework client, ModuleDeployment moduleDeployment)
			throws Exception {
		Stream stream = (Stream) moduleDeployment.deploymentUnit;
		ModuleDescriptor moduleDescriptor = moduleDeployment.moduleDescriptor;
		ModuleDeploymentProperties deploymentProperties = moduleDeployment.deploymentProperties;

		// the passed in deploymentProperties were loaded from the
		// deployment path...merge with deployment properties
		// created at the stream level
		final ModuleDeploymentProperties mergedProperties =
				DeploymentPropertiesUtility.createModuleDeploymentProperties(
						stream.getDeploymentProperties(), moduleDescriptor);
		mergedProperties.putAll(deploymentProperties);

		ModuleDeploymentStatus deploymentStatus = null;
		if (mergedProperties.getCount() > 0) {
			try {
				deploymentStatus = deployModule(client, moduleDeployment,
						instantiateContainerMatcher(client, moduleDescriptor));
			}
			catch (NoContainerException e) {
				logger.warn("No containers available for redeployment of {} for stream {}",
						moduleDescriptor.getModuleLabel(),
						stream.getName());
			}
			finally {
				updateDeploymentUnitState(client, moduleDeployment, deploymentStatus);
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

		ModuleDeploymentStatus deploymentStatus = null;
		ModuleDeployment moduleDeployment = new ModuleDeployment(job, moduleDescriptor, deploymentProperties);
		if (deploymentProperties.getCount() > 0) {
			try {
				deploymentStatus = deployModule(client, moduleDeployment,
						instantiateContainerMatcher(client, moduleDescriptor));
			}
			catch (NoContainerException e) {
				logger.warn("No containers available for redeployment of {} for job {}",
						moduleDescriptor.getModuleLabel(),
						job.getName());
			}
			finally {
				updateDeploymentUnitState(client,
						new ModuleDeployment(job, moduleDescriptor, deploymentProperties),
						deploymentStatus);
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
	 * Issue a module deployment request for the provided module using
	 * the provided container matcher. This also transitions the deployment
	 * unit state to {@link DeploymentUnitStatus.State#deploying}. Once
	 * the deployment attempt completes, the status for the deployment unit
	 * should be updated via {@link #updateDeploymentUnitState} using the
	 * {@link ModuleDeploymentStatus} returned from this method.
	 *
	 * @param client             curator client
	 * @param moduleDeployment   contains module redeployment details such as
	 *                           stream, module descriptor, and deployment properties
	 * @param containerMatcher   matches modules to containers
	 * @return result of module deployment request
	 * @throws Exception
	 * @see #transitionToDeploying
	 */
	private ModuleDeploymentStatus deployModule(CuratorFramework client,
			ModuleDeployment moduleDeployment, ContainerMatcher containerMatcher) throws Exception {
		transitionToDeploying(client, moduleDeployment.deploymentUnit);

		return moduleDeploymentWriter.writeDeployment(moduleDeployment.moduleDescriptor,
				moduleDeployment.deploymentProperties, containerMatcher);
	}

	/**
	 * Issue a module deployment request for the provided module to
	 * the provided container. This also transitions the deployment
	 * unit state to {@link DeploymentUnitStatus.State#deploying}. Once
	 * the deployment attempt completes, the status for the deployment unit
	 * should be updated via {@link #updateDeploymentUnitState} using the
	 * {@link ModuleDeploymentStatus} returned from this method.
	 *
	 * @param client             curator client
	 * @param moduleDeployment   contains module redeployment details such as
	 *                           stream, module descriptor, and deployment properties
	 * @param container          target container for module deployment
	 * @return result of module deployment request
	 * @throws Exception
	 * @see #transitionToDeploying
	 */
	private ModuleDeploymentStatus deployModule(CuratorFramework client,
			ModuleDeployment moduleDeployment, Container container) throws Exception {
		transitionToDeploying(client, moduleDeployment.deploymentUnit);

		return moduleDeploymentWriter.writeDeployment(moduleDeployment.moduleDescriptor, container);
	}

	/**
	 * Transitions the deployment unit state to {@link DeploymentUnitStatus.State#deploying}.
	 * This transition should occur before making a module deployment attempt.
	 *
	 * @param client         curator client
	 * @param deploymentUnit deployment unit that contains a module to be deployed
	 * @throws Exception
	 * @see #deployModule
	 * @see #updateDeploymentUnitState
	 */
	private void transitionToDeploying(CuratorFramework client, DeploymentUnit deploymentUnit) throws Exception {
		String pathPrefix = (deploymentUnit instanceof Stream)
				? Paths.STREAM_DEPLOYMENTS
				: Paths.JOB_DEPLOYMENTS;

		client.setData().forPath(
				Paths.build(pathPrefix, deploymentUnit.getName(), Paths.STATUS),
				ZooKeeperUtils.mapToBytes(new DeploymentUnitStatus(
						DeploymentUnitStatus.State.deploying).toMap()));
	}

	/**
	 * Return a <em>mutable</em> collection of {@link ModuleDeploymentStatus module statuses}
	 * for all of the modules that comprise the provided {@link DeploymentUnit}. This
	 * information is obtained from ZooKeeper via the ephemeral nodes created
	 * by the individual containers that have deployed these modules.
	 * <p />
	 * This collection is used (and modified) in {@link #updateDeploymentUnitState}.
	 *
	 * @param client          curator client
	 * @param deploymentUnit  deployment unit for which to return the individual
	 *                        module statuses
	 * @return mutable collection of status objects
	 * @throws Exception
	 * @see #updateDeploymentUnitState
	 */
	private Collection<ModuleDeploymentStatus> aggregateState(CuratorFramework client,
			DeploymentUnit deploymentUnit) throws Exception {
		Assert.state(deploymentUnit instanceof Stream || deploymentUnit instanceof Job);
		String pathPrefix = (deploymentUnit instanceof Stream)
				? Paths.STREAM_DEPLOYMENTS
				: Paths.JOB_DEPLOYMENTS;

		String path = Paths.build(pathPrefix, deploymentUnit.getName(), Paths.MODULES);
		List<String> modules = client.getChildren().forPath(path);
		Collection<ModuleDeploymentStatus> results = new ArrayList<ModuleDeploymentStatus>();
		for (String module : modules) {
			String deploymentUnitName;
			ModuleType type;
			String label;
			String container;

			if (deploymentUnit instanceof Stream) {
				StreamDeploymentsPath streamDeploymentsPath = new StreamDeploymentsPath(Paths.build(path, module));
				deploymentUnitName = streamDeploymentsPath.getStreamName();
				Assert.state(deploymentUnitName.equals(deploymentUnit.getName()));
				type = ModuleType.valueOf(streamDeploymentsPath.getModuleType());
				label = streamDeploymentsPath.getModuleLabel();
				container = streamDeploymentsPath.getContainer();
			}
			else {
				JobDeploymentsPath jobDeploymentsPath = new JobDeploymentsPath(Paths.build(path, module));
				deploymentUnitName = jobDeploymentsPath.getJobName();
				Assert.state(deploymentUnitName.equals(deploymentUnit.getName()));
				type = ModuleType.job;
				label = jobDeploymentsPath.getModuleLabel();
				container = jobDeploymentsPath.getContainer();
			}

			ModuleDescriptor.Key moduleDescriptorKey = new ModuleDescriptor.Key(deploymentUnitName, type, label);
			results.add(new ModuleDeploymentStatus(container, moduleDescriptorKey,
					ModuleDeploymentStatus.State.deployed, null));
		}

		return results;
	}

	/**
	 * Update the ZooKeeper ephemeral node that indicates the status for the
	 * provided deployment unit.
	 *
	 * @param client            curator client
	 * @param moduleDeployment  module that a redeploy was attempted for
	 * @param deploymentStatus  deployment status for the module; may be null
	 *                          if a module deployment was not attempted
	 *                          (for example if there were no containers
	 *                          available for deployment)
	 * @throws Exception
	 */
	private void updateDeploymentUnitState(CuratorFramework client, ModuleDeployment moduleDeployment,
			ModuleDeploymentStatus deploymentStatus) throws Exception {
		final DeploymentUnit deploymentUnit = moduleDeployment.deploymentUnit;
		final ModuleDescriptor moduleDescriptor = moduleDeployment.moduleDescriptor;

		Collection<ModuleDeploymentStatus> aggregateStatuses = aggregateState(client, deploymentUnit);

		// If the module deployment was successful, it will be present in this
		// list; remove it to avoid duplication since the deployment status
		// that was returned from the deployment request will be used instead.
		// This is especially important if the deployment failed because
		// that deployment status will contain the error condition that
		// caused the failure.
		if (deploymentStatus != null) {
			for (Iterator<ModuleDeploymentStatus> iterator = aggregateStatuses.iterator(); iterator.hasNext();) {
				ModuleDeploymentStatus status = iterator.next();
				if (logger.isTraceEnabled()) {
					logger.trace("module deployment status: {}", status);
					logger.trace("deploymentStatus: {}", deploymentStatus);
				}

				if (status.getKey().getLabel().equals(moduleDescriptor.getModuleLabel())
						&& status.getContainer().equals(deploymentStatus.getContainer())) {
					iterator.remove();
				}
			}
			aggregateStatuses.add(deploymentStatus);
		}

		Assert.state(deploymentUnit instanceof Stream || deploymentUnit instanceof Job);
		boolean isStream = (deploymentUnit instanceof Stream);

		ModuleDeploymentPropertiesProvider provider = (isStream)
				? new StreamDeploymentListener.StreamModuleDeploymentPropertiesProvider((Stream) deploymentUnit)
				: new JobDeploymentListener.JobModuleDeploymentPropertiesProvider((Job) deploymentUnit);

		DeploymentUnitStatus status = stateCalculator.calculate(deploymentUnit, provider, aggregateStatuses);

		logger.info("Deployment state for {} '{}': {}",
				isStream ? "stream" : "job", deploymentUnit.getName(), status);

		client.setData().forPath(
				Paths.build(isStream ? Paths.STREAM_DEPLOYMENTS : Paths.JOB_DEPLOYMENTS,
						deploymentUnit.getName(), Paths.STATUS),
				ZooKeeperUtils.mapToBytes(status.toMap()));
	}


	/**
	 * Converter from {@link ChildData} to deployment name string.
	 */
	public static class DeploymentNameConverter implements Converter<ChildData, String> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String convert(ChildData source) {
			return Paths.stripPath(source.getPath());
		}
	}


	/**
	 * Holds information about a module that needs to be re-deployed.
	 * The {@link Comparable} implementation for this class sorts
	 * first by deployment unit name followed by (reverse) module descriptor
	 * index - i.e., the biggest index appearing first.
	 *
	 * @author Eric Bottard
	 */
	private static class ModuleDeployment implements Comparable<ModuleDeployment> {

		private final DeploymentUnit deploymentUnit;

		private final ModuleDescriptor moduleDescriptor;

		private final ModuleDeploymentProperties deploymentProperties;

		private ModuleDeployment(DeploymentUnit deploymentUnit, ModuleDescriptor moduleDescriptor,
				ModuleDeploymentProperties deploymentProperties) {
			this.deploymentUnit = deploymentUnit;
			this.moduleDescriptor = moduleDescriptor;
			this.deploymentProperties = deploymentProperties;
		}

		@Override
		public int compareTo(ModuleDeployment o) {
			int c = this.deploymentUnit.getName().compareTo(o.deploymentUnit.getName());
			if (c == 0) {
				c = o.moduleDescriptor.getIndex() - this.moduleDescriptor.getIndex();
			}
			return c;
		}

	}

}
