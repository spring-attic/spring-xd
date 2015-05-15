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

package org.springframework.xd.dirt.server.container;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.CollectionUtils;
import org.springframework.xd.dirt.cluster.ContainerAttributes;
import org.springframework.xd.dirt.core.Job;
import org.springframework.xd.dirt.core.JobDeploymentsPath;
import org.springframework.xd.dirt.core.ModuleDeploymentsPath;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.core.StreamDeploymentsPath;
import org.springframework.xd.dirt.job.JobFactory;
import org.springframework.xd.dirt.module.ModuleDeployer;
import org.springframework.xd.dirt.server.admin.deployment.zk.DeploymentLoader;
import org.springframework.xd.dirt.server.admin.deployment.ModuleDeploymentStatus;
import org.springframework.xd.dirt.stream.StreamFactory;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.RuntimeModuleDeploymentProperties;
import org.springframework.xd.module.core.Module;

/**
 * @author Mark Fisher
 * @author David Turanski
 * @author Ilayaperumal Gopinathan Listener for deployment requests for a container instance under {@link
 *         org.springframework.xd.dirt.zookeeper.Paths#DEPLOYMENTS}.
 */
class DeploymentListener implements PathChildrenCacheListener {

	private static final Logger logger = LoggerFactory.getLogger(DeploymentListener.class);

	/**
	 * The ZooKeeperConnection.
	 */
	private final ZooKeeperConnection zkConnection;

	/**
	 * Watcher for modules deployed to this container under the {@link Paths#STREAMS} location.
	 */
	private final StreamModuleWatcher streamModuleWatcher;

	/**
	 * Watcher for modules deployed to this container under the {@link Paths#JOBS} location.
	 */
	private final JobModuleWatcher jobModuleWatcher;

	/**
	 * The ModuleDeployer this container delegates to when deploying a Module.
	 */
	private final ModuleDeployer moduleDeployer;

	/**
	 * Metadata for the current Container.
	 */
	private final ContainerAttributes containerAttributes;

	/**
	 * Job factory.
	 */
	private final JobFactory jobFactory;

	/**
	 * Stream factory.
	 */
	private final StreamFactory streamFactory;

	/**
	 * Prefix for tap channels.
	 */
	private static final String TAP_CHANNEL_PREFIX = "tap:";

	/**
	 * Map of deployed modules.
	 */
	private final Map<ModuleDescriptor.Key, ModuleDescriptor> mapDeployedModules =
			new ConcurrentHashMap<ModuleDescriptor.Key, ModuleDescriptor>();

	/**
	 * Create an instance that will register the provided {@link ContainerAttributes} whenever the underlying {@link
	 * ZooKeeperConnection} is established. If that connection is already established at the time this instance receives
	 * a {@link org.springframework.context.event.ContextRefreshedEvent}, the attributes will be registered then.
	 * Otherwise, registration occurs within a callback that is invoked for connected events as well as reconnected
	 * events.
	 *
	 * @param containerAttributes runtime and configured attributes for the container
	 * @param streamFactory factory to construct {@link Stream}
	 * @param jobFactory factory to construct {@link Job}
	 * @param moduleDeployer module deployer
	 * @param zkConnection ZooKeeper connection
	 */
	public DeploymentListener(ZooKeeperConnection zkConnection, ModuleDeployer moduleDeployer,
			ContainerAttributes containerAttributes, JobFactory jobFactory, StreamFactory streamFactory) {
		this.zkConnection = zkConnection;
		this.jobModuleWatcher = new JobModuleWatcher();
		this.streamModuleWatcher = new StreamModuleWatcher();
		this.moduleDeployer = moduleDeployer;
		this.containerAttributes = containerAttributes;
		this.jobFactory = jobFactory;
		this.streamFactory = streamFactory;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
		ZooKeeperUtils.logCacheEvent(logger, event);
		switch (event.getType()) {
			case INITIALIZED:
				break;
			case CHILD_ADDED:
				onChildAdded(client, event.getData());
				break;
			case CHILD_REMOVED:
				onChildRemoved(client, event.getData());
				break;
			default:
				break;
		}
	}

	/**
	 * Event handler for new module deployments.
	 *
	 * @param client curator client
	 * @param data module data
	 */
	private void onChildAdded(CuratorFramework client, ChildData data) throws Exception {
		String path = data.getPath();
		ModuleDeploymentsPath moduleDeploymentsPath = new ModuleDeploymentsPath(path);
		String unitName = moduleDeploymentsPath.getDeploymentUnitName();
		String moduleType = moduleDeploymentsPath.getModuleType();
		String moduleLabel = moduleDeploymentsPath.getModuleLabel();
		int moduleSequence = moduleDeploymentsPath.getModuleSequence();
		ModuleDescriptor.Key key = new ModuleDescriptor.Key(unitName, ModuleType.valueOf(moduleType), moduleLabel);
		String container = moduleDeploymentsPath.getContainer();
		Module module = null;
		ModuleDeploymentStatus status;

		RuntimeModuleDeploymentProperties properties = new RuntimeModuleDeploymentProperties();
		properties.putAll(ZooKeeperUtils.bytesToMap(data.getData()));

		try {
			module = (ModuleType.job.toString().equals(moduleType)) ?
					deployJobModule(client, unitName, moduleLabel, properties) :
					deployStreamModule(client, unitName, moduleType, moduleLabel, properties);
			if (module == null) {
				status = new ModuleDeploymentStatus(container, moduleSequence, key, ModuleDeploymentStatus.State.failed,
						"Module deployment returned null");
			}
			else {
				status = new ModuleDeploymentStatus(container, moduleSequence, key,
						ModuleDeploymentStatus.State.deployed, null);
			}
		}
		catch (Exception e) {
			status = new ModuleDeploymentStatus(container, moduleSequence, key, ModuleDeploymentStatus.State.failed,
					ZooKeeperUtils.getStackTrace(e));
			logger.error("Exception deploying module", e);
		}

		try {
			writeModuleMetadata(client, module, path);
			client.setData().forPath(status.buildPath(), ZooKeeperUtils.mapToBytes(status.toMap()));
		}
		catch (KeeperException.NoNodeException e) {
			logger.warn("During deployment of module {} of type {} for {} with sequence number {}," +
							"an undeployment request was detected; this module will be undeployed.", moduleLabel,
					moduleType, unitName, moduleSequence);
			if (logger.isTraceEnabled()) {
				logger.trace("Path " + path + " was removed", e);
			}
		}
	}

	/**
	 * Event handler for deployment removals.
	 *
	 * @param client curator client
	 * @param data module data
	 */
	private void onChildRemoved(CuratorFramework client, ChildData data) throws Exception {
		ModuleDeploymentsPath moduleDeploymentsPath = new ModuleDeploymentsPath(data.getPath());
		String streamName = moduleDeploymentsPath.getDeploymentUnitName();
		String moduleType = moduleDeploymentsPath.getModuleType();
		String moduleLabel = moduleDeploymentsPath.getModuleLabel();
		String moduleSequence = moduleDeploymentsPath.getModuleSequenceAsString();

		undeployModule(streamName, moduleType, moduleLabel);

		String path;
		if (ModuleType.job.toString().equals(moduleType)) {
			path = new JobDeploymentsPath().setJobName(streamName).setModuleLabel(moduleLabel)
					.setContainer(containerAttributes.getId()).build();
		}
		else {
			path = new StreamDeploymentsPath().setStreamName(streamName).setModuleType(moduleType)
					.setModuleLabel(moduleLabel).setModuleSequence(moduleSequence)
					.setContainer(this.containerAttributes.getId()).build();
		}
		if (client.checkExists().forPath(path) != null) {
			logger.trace("Deleting path: {}", path);
			client.delete().forPath(path);
		}
	}

	/**
	 * If the provided module is not null, write its properties to the provided ZooKeeper path. If the module is null,
	 * no action is taken.
	 *
	 * @param client curator client
	 * @param module module for which to write properties; may be null
	 * @param path path to write properties to
	 * @throws Exception
	 */
	private void writeModuleMetadata(CuratorFramework client, Module module, String path) throws Exception {
		if (module != null) {
			Map<String, String> mapMetadata = new HashMap<String, String>();
			CollectionUtils.mergePropertiesIntoMap(module.getProperties(), mapMetadata);
			try {
				client.create().withMode(CreateMode.EPHEMERAL)
						.forPath(Paths.build(path, Paths.METADATA), ZooKeeperUtils.mapToBytes(mapMetadata));
			}
			catch (KeeperException.NodeExistsException ne) {
				// This is likely to happen when the container disconnects and reconnects but the admin leader
				// was not available to handle the container leaving event.
				ModuleDescriptor descriptor = module.getDescriptor();
				logger.info("The module metadata path for the module {} of type {} for {}" + "already exists.",
						descriptor.getModuleLabel(), descriptor.getType().toString(), descriptor.getGroup());
			}
		}
	}

	/**
	 * Deploy the requested job.
	 *
	 * @param client curator client
	 * @param jobName job name
	 * @param jobLabel job label
	 * @param properties module deployment properties
	 * @return Module deployed job module
	 */
	private Module deployJobModule(CuratorFramework client, String jobName, String jobLabel,
			RuntimeModuleDeploymentProperties properties) throws Exception {
		logger.info("Deploying job '{}'", jobName);

		String jobDeploymentPath = new JobDeploymentsPath().setJobName(jobName).setModuleLabel(jobLabel)
				.setModuleSequence(properties.getSequenceAsString()).setContainer(containerAttributes.getId()).build();

		Module module = null;
		Job job = DeploymentLoader.loadJob(client, jobName, jobFactory);
		if (job != null) {
			ModuleDescriptor moduleDescriptor = job.getJobModuleDescriptor();
			module = deployModule(moduleDescriptor, properties);

			try {
				// this indicates that the container has deployed the module
				client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(jobDeploymentPath);

				// set a watch on this module in the job path;
				// if the node is deleted this indicates an undeployment
				client.getData().usingWatcher(jobModuleWatcher).forPath(jobDeploymentPath);
			}
			catch (KeeperException.NodeExistsException e) {
				// This could possibly happen when the container disconnects and reconnects but
				// admin leader was not available to process container removed event.
				logger.info("Module for job {} already deployed", jobName);
			}
		}

		// todo: this will return null if the job doesn't exist...needs review
		return module;
	}

	/**
	 * Deploy the requested module for a stream.
	 *
	 * @param client curator client
	 * @param streamName name of the stream for the module
	 * @param moduleType module type
	 * @param moduleLabel module label
	 * @param properties module deployment properties
	 * @return Module deployed stream module
	 */
	private Module deployStreamModule(CuratorFramework client, String streamName, String moduleType, String moduleLabel,
			RuntimeModuleDeploymentProperties properties) throws Exception {
		logger.info("Deploying module '{}' for stream '{}'", moduleLabel, streamName);

		String streamDeploymentPath = new StreamDeploymentsPath().setStreamName(streamName).setModuleType(moduleType)
				.setModuleLabel(moduleLabel).setModuleSequence(properties.getSequenceAsString())
				.setContainer(this.containerAttributes.getId()).build();

		Module module = null;
		Stream stream = DeploymentLoader.loadStream(client, streamName, streamFactory);
		if (stream != null) {
			ModuleDescriptor descriptor = stream.getModuleDescriptor(moduleLabel);
			module = deployModule(descriptor, properties);

			try {
				// this indicates that the container has deployed the module
				client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(streamDeploymentPath);

				// set a watch on this module in the stream path;
				// if the node is deleted this indicates an undeployment
				client.getData().usingWatcher(streamModuleWatcher).forPath(streamDeploymentPath);
			}
			catch (KeeperException.NodeExistsException e) {
				// This could possibly happen when the container disconnects and reconnects but
				// admin leader was not available to process container removed event.
				logger.info("Module {} for stream {} already deployed", moduleLabel, streamName);
			}
		}
		return module;
	}

	/**
	 * Deploy the requested module.
	 *
	 * @param moduleDescriptor descriptor for the module to be deployed
	 */
	private Module deployModule(ModuleDescriptor moduleDescriptor, ModuleDeploymentProperties deploymentProperties) {
		logger.info("Deploying module {}", moduleDescriptor);
		ModuleDescriptor.Key key = new ModuleDescriptor.Key(moduleDescriptor.getGroup(), moduleDescriptor.getType(),
				moduleDescriptor.getModuleLabel());
		mapDeployedModules.put(key, moduleDescriptor);
		Module module = moduleDeployer.createModule(moduleDescriptor, deploymentProperties);
		registerTap(moduleDescriptor);
		this.moduleDeployer.deploy(module, moduleDescriptor);
		return module;
	}

	/**
	 * Undeploy the requested module.
	 *
	 * @param streamName name of the stream for the module
	 * @param moduleType module type
	 * @param moduleLabel module label
	 */
	protected void undeployModule(String streamName, String moduleType, String moduleLabel) {
		ModuleDescriptor.Key key = new ModuleDescriptor.Key(streamName, ModuleType.valueOf(moduleType), moduleLabel);
		ModuleDescriptor descriptor = mapDeployedModules.get(key);
		if (descriptor == null) {
			// This is logged at trace level because every module undeployment
			// will cause this to be logged. This is because there is a listener
			// on both streams and modules, and the listener implementation for
			// each will remove the module from the other, thus causing
			// this method to be invoked twice per module undeployment.
			logger.trace("Module {} already undeployed", moduleLabel);
		}
		else {
			logger.info("Undeploying module {}", descriptor);
			mapDeployedModules.remove(key);
			this.moduleDeployer.undeploy(descriptor);
			unregisterTap(descriptor);
		}
	}

	/**
	 * Register the existence of a tap subscriber if the provided module has a tap channel as its source channel.
	 *
	 * @param descriptor {@link ModuleDescriptor} for the module
	 */
	private void registerTap(ModuleDescriptor descriptor) {
		String tapChannelName = determineTapChannel(descriptor);
		if (tapChannelName != null) {
			try {
				zkConnection.getClient().create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(
						Paths.build(Paths.TAPS, tapChannelName, containerAttributes.getId(), descriptor.getGroup()));
			}
			catch (Exception e) {
				// if it already exists, ignore
				ZooKeeperUtils.wrapAndThrowIgnoring(e, KeeperException.NodeExistsException.class);
			}
		}
	}

	/**
	 * Unregister the tap subscriber if the provided module has a tap channel as its source channel.
	 *
	 * @param descriptor {@link ModuleDescriptor} for the module
	 */
	private void unregisterTap(ModuleDescriptor descriptor) {
		CuratorFramework client = zkConnection.getClient();
		String tapChannelName = determineTapChannel(descriptor);
		if (tapChannelName != null) {
			try {
				try {
					client.delete().forPath(Paths.build(Paths.TAPS, tapChannelName, this.containerAttributes.getId(),
							descriptor.getGroup()));
				}
				catch (KeeperException.NoNodeException e) {
					// already deleted, ignore
				}
				try {
					// now try to delete the container node and then if successful, the tap node itself
					client.delete().forPath(Paths.build(Paths.TAPS, tapChannelName, this.containerAttributes.getId()));
					client.delete().forPath(Paths.build(Paths.TAPS, tapChannelName));
				}
				catch (KeeperException.NoNodeException e) {
					// already deleted, ignore
				}
				catch (KeeperException.NotEmptyException e) {
					// attempted to delete a node that still has other children, let it be
				}
			}
			catch (Exception e) {
				if (client.getState() == CuratorFrameworkState.STARTED) {
					throw ZooKeeperUtils.wrapThrowable(e);
				}
				else {
					logger.debug("Ignoring exception for tap un-registration due to closed ZooKeeper connection", e);
				}
			}
		}
	}

	/**
	 * Determine whether the provided descriptor has a tap channel as its "source", and if so return the unqualified tap
	 * channel name. If not, return {@code null}.
	 *
	 * @param descriptor ModuleDescriptor whose source channel is checked
	 * @return unqualified tap channel name or {@code null}
	 */
	private String determineTapChannel(ModuleDescriptor descriptor) {
		String sourceChannelName = descriptor.getSourceChannelName();
		return (sourceChannelName != null && sourceChannelName.startsWith(TAP_CHANNEL_PREFIX)) ?
				sourceChannelName.substring(TAP_CHANNEL_PREFIX.length()) : null;
	}

	void undeployAllModules() {
		for (Iterator<ModuleDescriptor.Key> iterator = mapDeployedModules.keySet().iterator(); iterator.hasNext(); ) {
			ModuleDescriptor.Key key = iterator.next();
			try {
				undeployModule(key.getGroup(), key.getType().name(), key.getLabel());
			}
			catch (Exception e) {
				logger.warn("Exception while undeploying " + key, e);
			}
			iterator.remove();
		}
	}

	/**
	 * Watcher for the modules deployed to this container under the {@link Paths#JOB_DEPLOYMENTS} location. If the node
	 * is deleted, this container will undeploy the module.
	 */
	class JobModuleWatcher implements CuratorWatcher {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void process(WatchedEvent event) throws Exception {
			CuratorFramework client = zkConnection.getClient();

			if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
				JobDeploymentsPath jobDeploymentsPath = new JobDeploymentsPath(event.getPath());
				String jobName = jobDeploymentsPath.getJobName();
				String moduleLabel = jobDeploymentsPath.getModuleLabel();
				String moduleSequence = jobDeploymentsPath.getModuleSequenceAsString();

				undeployModule(jobName, ModuleType.job.toString(), moduleLabel);

				String deploymentPath = new ModuleDeploymentsPath().setContainer(containerAttributes.getId())
						.setDeploymentUnitName(jobName).setModuleType(ModuleType.job.toString())
						.setModuleLabel(moduleLabel).setModuleSequence(moduleSequence).build();

				try {
					if (client.checkExists().forPath(deploymentPath) != null) {
						logger.trace("Deleting path: {}", deploymentPath);
						client.delete().deletingChildrenIfNeeded().forPath(deploymentPath);
					}
				}
				catch (Exception e) {
					// it is common for a process shutdown to trigger this
					// event; therefore any exception thrown while attempting
					// to delete a deployment path will only be rethrown
					// if the client is in a connected/started state
					if (client.getState() == CuratorFrameworkState.STARTED) {
						throw ZooKeeperUtils.wrapThrowable(e);
					}
				}
			}
			else {
				logger.debug("Unexpected event {}, ZooKeeper state: {}", event.getType(), event.getState());
				if (EnumSet.of(Watcher.Event.KeeperState.SyncConnected, Watcher.Event.KeeperState.SaslAuthenticated,
						Watcher.Event.KeeperState.ConnectedReadOnly).contains(event.getState())) {
					// this watcher is only interested in deletes for the purposes of
					// undeploying modules;if any other change occurs the watch needs to be
					// reestablished
					try {
						client.getData().usingWatcher(this).forPath(event.getPath());
					}
					catch (Exception e) {
						logger.error("Exception setting up watch for path '{}': {}; ZooKeeper state: {}",
								event.getPath(), e,
								zkConnection.getClient().getZookeeperClient().getZooKeeper().getState());
						if (logger.isDebugEnabled()) {
							logger.debug("Full stack trace", e);
						}
						if (client.getState() == CuratorFrameworkState.STARTED) {
							throw ZooKeeperUtils.wrapThrowable(e);
						}
					}
				}
			}
		}
	}

	/**
	 * Watcher for the modules deployed to this container under the {@link Paths#STREAM_DEPLOYMENTS} location. If the
	 * node is deleted, this container will undeploy the module.
	 */
	class StreamModuleWatcher implements CuratorWatcher {
		/**
		 * {@inheritDoc}
		 */
		@Override
		public void process(WatchedEvent event) throws Exception {
			CuratorFramework client = zkConnection.getClient();

			if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
				StreamDeploymentsPath streamDeploymentsPath = new StreamDeploymentsPath(event.getPath());

				String streamName = streamDeploymentsPath.getStreamName();
				String moduleType = streamDeploymentsPath.getModuleType();
				String moduleLabel = streamDeploymentsPath.getModuleLabel();
				String moduleSequence = streamDeploymentsPath.getModuleSequenceAsString();

				undeployModule(streamName, moduleType, moduleLabel);

				String deploymentPath = new ModuleDeploymentsPath().setContainer(containerAttributes.getId())
						.setDeploymentUnitName(streamName).setModuleType(moduleType).setModuleLabel(moduleLabel)
						.setModuleSequence(moduleSequence).build();

				try {
					if (client.checkExists().forPath(deploymentPath) != null) {
						logger.trace("Deleting path: {}", deploymentPath);
						client.delete().deletingChildrenIfNeeded().forPath(deploymentPath);
					}
				}
				catch (Exception e) {
					// it is common for a process shutdown to trigger this
					// event; therefore any exception thrown while attempting
					// to delete a deployment path will only be rethrown
					// if the client is in a connected/started state
					if (client.getState() == CuratorFrameworkState.STARTED) {
						throw ZooKeeperUtils.wrapThrowable(e);
					}
				}
			}
			else {
				logger.debug("Unexpected event {}, ZooKeeper state: {}", event.getType(), event.getState());
				if (EnumSet.of(Watcher.Event.KeeperState.SyncConnected, Watcher.Event.KeeperState.SaslAuthenticated,
						Watcher.Event.KeeperState.ConnectedReadOnly).contains(event.getState())) {
					// this watcher is only interested in deletes for the purposes of undeploying modules;
					// if any other change occurs the watch needs to be reestablished
					try {
						client.getData().usingWatcher(this).forPath(event.getPath());
					}
					catch (Exception e) {
						logger.error("Exception setting up watch for path '{}': {}; ZooKeeper state: {}",
								event.getPath(), e,
								zkConnection.getClient().getZookeeperClient().getZooKeeper().getState());
						if (logger.isDebugEnabled()) {
							logger.debug("Full stack trace", e);
						}
						if (client.getState() == CuratorFrameworkState.STARTED) {
							throw ZooKeeperUtils.wrapThrowable(e);
						}
					}
				}
			}
		}
	}
}
