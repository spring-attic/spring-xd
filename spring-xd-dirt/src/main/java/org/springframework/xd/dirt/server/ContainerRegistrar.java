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
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.utils.ThreadUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.KeeperException.NotEmptyException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.BindException;
import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.container.ContainerAttributes;
import org.springframework.xd.dirt.container.store.ContainerRepository;
import org.springframework.xd.dirt.core.Job;
import org.springframework.xd.dirt.core.JobDeploymentsPath;
import org.springframework.xd.dirt.core.ModuleDeploymentsPath;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.core.StreamDeploymentsPath;
import org.springframework.xd.dirt.job.JobFactory;
import org.springframework.xd.dirt.module.ModuleDeployer;
import org.springframework.xd.dirt.stream.StreamFactory;
import org.springframework.xd.dirt.util.MapBytesUtility;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnectionListener;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.CompositeModule;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.SimpleModule;
import org.springframework.xd.module.options.ModuleOptions;
import org.springframework.xd.module.options.ModuleOptionsMetadata;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;
import org.springframework.xd.module.options.PrefixNarrowingModuleOptions;
import org.springframework.xd.module.support.ParentLastURLClassLoader;

/**
 * An instance of this class, registered as a bean in the context for a Container, will handle the registration of that
 * Container's metadata with ZooKeeper by creating an ephemeral node. If the {@link ZooKeeperConnection} used by this
 * registrar is closed, that ephemeral node will be eagerly deleted. Since the {@link ZooKeeperConnection} typically has
 * its lifecycle managed by Spring, that would be the normal behavior when the owning {@link ApplicationContext} is
 * itself closed.
 *
 * @author Mark Fisher
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 */
// todo: Rename ContainerServer or ModuleDeployer since it's driven by callbacks and not really a "server".
public class ContainerRegistrar implements ApplicationListener<ContextRefreshedEvent>, ApplicationContextAware,
		BeanClassLoaderAware {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ContainerRegistrar.class);

	/**
	 * Prefix for tap channels.
	 */
	private static final String TAP_CHANNEL_PREFIX = "tap:";

	/**
	 * Metadata for the current Container.
	 */
	private final ContainerAttributes containerAttributes;

	/**
	 * Repository where {@link Container}s are stored.
	 */
	private final ContainerRepository containerRepository;

	/**
	 * The ZooKeeperConnection.
	 */
	private final ZooKeeperConnection zkConnection;

	/**
	 * A {@link PathChildrenCacheListener} implementation that handles deployment requests (and deployment removals) for
	 * this container.
	 */
	private final DeploymentListener deploymentListener = new DeploymentListener();

	/**
	 * Watcher for modules deployed to this container under the {@link Paths#STREAMS} location.
	 */
	private final StreamModuleWatcher streamModuleWatcher = new StreamModuleWatcher();

	/**
	 * Watcher for modules deployed to this container under the {@link Paths#JOBS} location.
	 */
	private final JobModuleWatcher jobModuleWatcher = new JobModuleWatcher();

	/**
	 * Cache of children under the deployments path.
	 */
	private volatile PathChildrenCache deployments;

	/**
	 * Utility to convert maps to byte arrays.
	 */
	private final MapBytesUtility mapBytesUtility = new MapBytesUtility();

	/**
	 * Module options metadata resolver.
	 */
	private final ModuleOptionsMetadataResolver moduleOptionsMetadataResolver;

	/**
	 * Stream factory.
	 */
	private final StreamFactory streamFactory;

	/**
	 * Job factory.
	 */
	private final JobFactory jobFactory;

	/**
	 * Map of deployed modules.
	 */
	private final Map<ModuleDescriptor.Key, ModuleDescriptor> mapDeployedModules =
			new ConcurrentHashMap<ModuleDescriptor.Key, ModuleDescriptor>();

	/**
	 * The ModuleDeployer this container delegates to when deploying a Module.
	 */
	private final ModuleDeployer moduleDeployer;

	/**
	 * Application context within which this registrar is defined.
	 */
	private volatile ApplicationContext context;

	/**
	 * ClassLoader provided by the ApplicationContext.
	 */
	private volatile ClassLoader parentClassLoader;

	/**
	 * Utility for loading streams and jobs (including deployment metadata).
	 */
	protected final DeploymentLoader deploymentLoader = new DeploymentLoader();

	/**
	 * Create an instance that will register the provided {@link ContainerAttributes} whenever the underlying
	 * {@link ZooKeeperConnection} is established. If that connection is already established at the time this instance
	 * receives a {@link ContextRefreshedEvent}, the attributes will be registered then. Otherwise, registration occurs
	 * within a callback that is invoked for connected events as well as reconnected events.
	 *
	 * @param containerAttributes runtime and configured attributes for the container
	 * @param containerRepository repository for the containers
	 * @param streamFactory factory to construct {@link Stream}
	 * @param jobFactory factory to construct {@link Job}
	 * @param moduleOptionsMetadataResolver resolver for module options metadata
	 * @param moduleDeployer module deployer
	 * @param zkConnection ZooKeeper connection
	 */
	public ContainerRegistrar(ZooKeeperConnection zkConnection, ContainerAttributes containerAttributes,
			ContainerRepository containerRepository,
			StreamFactory streamFactory, JobFactory jobFactory,
			ModuleOptionsMetadataResolver moduleOptionsMetadataResolver, ModuleDeployer moduleDeployer) {
		this.zkConnection = zkConnection;
		this.containerAttributes = containerAttributes;
		this.containerRepository = containerRepository;
		this.streamFactory = streamFactory;
		this.jobFactory = jobFactory;
		this.moduleOptionsMetadataResolver = moduleOptionsMetadataResolver;
		this.moduleDeployer = moduleDeployer;
	}

	/**
	 * Deploy the requested module.
	 *
	 * @param moduleDescriptor descriptor for the module to be deployed
	 */
	private Module deployModule(ModuleDescriptor moduleDescriptor,
			ModuleDeploymentProperties deploymentProperties) {
		logger.info("Deploying module {}", moduleDescriptor);
		ModuleDescriptor.Key key = new ModuleDescriptor.Key(moduleDescriptor.getGroup(), moduleDescriptor.getType(),
				moduleDescriptor.getModuleLabel());
		mapDeployedModules.put(key, moduleDescriptor);
		ModuleOptions moduleOptions = this.safeModuleOptionsInterpolate(moduleDescriptor);
		Module module = (moduleDescriptor.isComposed())
				? createComposedModule(moduleDescriptor, moduleOptions, deploymentProperties)
				: createSimpleModule(moduleDescriptor, moduleOptions, deploymentProperties);

		registerTap(moduleDescriptor);

		// todo: rather than delegate, merge ContainerRegistrar itself into and remove most of ModuleDeployer
		this.moduleDeployer.deployAndStore(module, moduleDescriptor);
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
				zkConnection.getClient().create().creatingParentsIfNeeded().forPath(
						Paths.build(Paths.TAPS, tapChannelName, containerAttributes.getId(),
								descriptor.getGroup()));
			}
			catch (Exception e) {
				// if it already exists, ignore
				ZooKeeperUtils.wrapAndThrowIgnoring(e, NodeExistsException.class);
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
				catch (NoNodeException e) {
					// already deleted, ignore
				}
				try {
					// now try to delete the container node and then if successful, the tap node itself
					client.delete().forPath(Paths.build(Paths.TAPS, tapChannelName, this.containerAttributes.getId()));
					client.delete().forPath(Paths.build(Paths.TAPS, tapChannelName));
				}
				catch (NoNodeException e) {
					// already deleted, ignore
				}
				catch (NotEmptyException e) {
					// attempted to delete a node that still has other children, let it be
				}
			}
			catch (Exception e) {
				throw ZooKeeperUtils.wrapThrowable(e);
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
		return (sourceChannelName != null && sourceChannelName.startsWith(TAP_CHANNEL_PREFIX))
				? sourceChannelName.substring(TAP_CHANNEL_PREFIX.length()) : null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (this.context.equals(event.getApplicationContext())) {
			if (zkConnection.isConnected()) {
				registerWithZooKeeper(zkConnection.getClient());
			}
			zkConnection.addListener(new ContainerAttributesRegisteringZooKeeperConnectionListener());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.parentClassLoader = classLoader;
	}

	/**
	 * Write the Container attributes to ZooKeeper in an ephemeral node under {@code /xd/containers}.
	 */
	private void registerWithZooKeeper(CuratorFramework client) {
		try {
			// Save the container attributes before creating the container
			// deploy path. This is done because the admin leader/supervisor
			// will delete deployment paths for containers that aren't
			// present in the containers path.
			containerRepository.save(new Container(containerAttributes.getId(), containerAttributes));

			String moduleDeploymentPath = Paths.build(Paths.MODULE_DEPLOYMENTS, containerAttributes.getId());
			Paths.ensurePath(client, moduleDeploymentPath);
			deployments = new PathChildrenCache(client, moduleDeploymentPath, true,
					ThreadUtils.newThreadFactory("DeploymentsPathChildrenCache"));
			deployments.getListenable().addListener(deploymentListener);
			deployments.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);

			logger.info("Started container {}", containerAttributes);
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}


	/**
	 * The listener that triggers registration of the container attributes in a ZooKeeper node.
	 */
	private class ContainerAttributesRegisteringZooKeeperConnectionListener implements ZooKeeperConnectionListener {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onConnect(CuratorFramework client) {
			registerWithZooKeeper(client);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onDisconnect(CuratorFramework client) {
			try {
				logger.warn(">>> disconnected container: {}", containerAttributes.getId());
				deployments.getListenable().removeListener(deploymentListener);
				deployments.close();

				for (Iterator<ModuleDescriptor.Key> iterator = mapDeployedModules.keySet().iterator(); iterator.hasNext();) {
					ModuleDescriptor.Key key = iterator.next();
					undeployModule(key.getGroup(), key.getType().name(), key.getLabel());
					iterator.remove();
				}
			}
			catch (Exception e) {
				throw ZooKeeperUtils.wrapThrowable(e);
			}
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
		String unitName = moduleDeploymentsPath.getStreamName();
		String moduleType = moduleDeploymentsPath.getModuleType();
		String moduleLabel = moduleDeploymentsPath.getModuleLabel();
		Module module = null;
		ModuleDeploymentProperties properties = new ModuleDeploymentProperties();
		properties.putAll(mapBytesUtility.toMap(data.getData()));
		Map<String, String> mapStatus = new HashMap<String, String>();
		try {
			module = (ModuleType.job.toString().equals(moduleType))
					? deployJob(client, unitName, moduleLabel, properties)
					: deployStreamModule(client, unitName, moduleType, moduleLabel, properties);
			if (module == null) {
				mapStatus.put(ModuleDeploymentWriter.STATUS_KEY, ModuleDeploymentWriter.Status.error.toString());
				mapStatus.put(ModuleDeploymentWriter.ERROR_DESCRIPTION_KEY, "Module deployment returned null");
			}
			else {
				mapStatus.put(ModuleDeploymentWriter.STATUS_KEY, ModuleDeploymentWriter.Status.deployed.toString());
			}
		}
		catch (Exception e) {
			mapStatus.put(ModuleDeploymentWriter.STATUS_KEY, ModuleDeploymentWriter.Status.error.toString());
			mapStatus.put(ModuleDeploymentWriter.ERROR_DESCRIPTION_KEY, e.toString());
			logger.error("Exception deploying module", e);
		}

		try {
			writeModuleMetadata(module, path, client);
			// update the module deployment node with the deployment status
			client.setData().forPath(Paths.build(path, Paths.STATUS), mapBytesUtility.toByteArray(mapStatus));
		}
		catch (KeeperException.NoNodeException e) {
			logger.warn("During deployment of module {} of type {} for {}, an undeployment request " +
					"was detected; this module will be undeployed.", moduleLabel, moduleType, unitName);
			if (logger.isTraceEnabled()) {
				logger.trace("Path " + path + " was removed", e);
			}
		}
	}

	/**
	 * If the provided module is not null, write its properties to the provided
	 * ZooKeeper path. If the module is null, no action is taken.
	 *
	 * @param module     module for which to write properties; may be null
	 * @param path       path to write properties to
	 * @param client     curator client
	 * @throws Exception
	 */
	private void writeModuleMetadata(Module module, String path, CuratorFramework client) throws Exception {
		if (module != null) {
			Map<String, String> mapMetadata = new HashMap<String, String>();
			CollectionUtils.mergePropertiesIntoMap(module.getProperties(), mapMetadata);
			client.create().withMode(CreateMode.EPHEMERAL).forPath(
					Paths.build(path, "metadata"), mapBytesUtility.toByteArray(mapMetadata));
		}
	}

	/**
	 * Deploy the requested job.
	 *
	 * @param client      curator client
	 * @param jobName     job name
	 * @param jobLabel    job label
	 * @param properties  module deployment properties
	 * @return Module deployed job module
	 */
	private Module deployJob(CuratorFramework client, String jobName, String jobLabel,
			ModuleDeploymentProperties properties) throws Exception {
		logger.info("Deploying job '{}'", jobName);

		String jobDeploymentPath = new JobDeploymentsPath().setJobName(jobName)
				.setModuleLabel(jobLabel)
				.setContainer(containerAttributes.getId()).build();

		Module module = null;
		Job job = deploymentLoader.loadJob(client, jobName, jobFactory);
		if (job != null) {
			ModuleDescriptor moduleDescriptor = job.getJobModuleDescriptor();
			module = deployModule(moduleDescriptor, properties);

			try {
				// this indicates that the container has deployed the module
				client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(jobDeploymentPath,
						getDeployedMessage());

				// set a watch on this module in the job path;
				// if the node is deleted this indicates an undeployment
				client.getData().usingWatcher(jobModuleWatcher).forPath(jobDeploymentPath);
			}
			catch (KeeperException.NodeExistsException e) {
				// todo: review, this should not happen
				logger.info("Module for job {} already deployed", jobName);
			}
		}

		// todo: this will return null if the job doesn't exist...needs review
		return module;
	}

	/**
	 * Deploy the requested module for a stream.
	 *
	 * @param client      curator client
	 * @param streamName  name of the stream for the module
	 * @param moduleType  module type
	 * @param moduleLabel module label
	 * @param properties  module deployment properties
	 * @return Module deployed stream module
	 */
	private Module deployStreamModule(CuratorFramework client, String streamName,
			String moduleType, String moduleLabel, ModuleDeploymentProperties properties) throws Exception {
		logger.info("Deploying module '{}' for stream '{}'", moduleLabel, streamName);

		String streamDeploymentPath = new StreamDeploymentsPath().setStreamName(streamName)
				.setModuleType(moduleType)
				.setModuleLabel(moduleLabel)
				.setContainer(containerAttributes.getId()).build();

		Module module = null;
		Stream stream = deploymentLoader.loadStream(client, streamName, streamFactory);
		if (stream != null) {
			ModuleDescriptor descriptor = stream.getModuleDescriptor(moduleLabel, moduleType);
			module = deployModule(descriptor, properties);

			try {
				// this indicates that the container has deployed the module
				client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(streamDeploymentPath,
						getDeployedMessage());

				// set a watch on this module in the stream path;
				// if the node is deleted this indicates an undeployment
				client.getData().usingWatcher(streamModuleWatcher).forPath(streamDeploymentPath);
			}
			catch (KeeperException.NodeExistsException e) {
				// todo: review, this should not happen
				logger.info("Module {} for stream {} already deployed", moduleLabel, streamName);
			}
		}
		return module;
	}

	/**
	 * Get the byte[] representation of deployed state message.
	 * @return byte array
	 */
	private byte[] getDeployedMessage() {
		return mapBytesUtility.toByteArray(Collections.singletonMap("state", "deployed"));
	}

	/**
	 * Event handler for deployment removals.
	 *
	 * @param client curator client
	 * @param data module data
	 */
	private void onChildRemoved(CuratorFramework client, ChildData data) throws Exception {
		ModuleDeploymentsPath moduleDeploymentsPath = new ModuleDeploymentsPath(data.getPath());
		String streamName = moduleDeploymentsPath.getStreamName();
		String moduleType = moduleDeploymentsPath.getModuleType();
		String moduleLabel = moduleDeploymentsPath.getModuleLabel();

		undeployModule(streamName, moduleType, moduleLabel);

		String path;
		if (ModuleType.job.toString().equals(moduleType)) {
			path = new JobDeploymentsPath().setJobName(streamName)
					.setModuleLabel(moduleLabel)
					.setContainer(containerAttributes.getId()).build();
		}
		else {
			path = new StreamDeploymentsPath().setStreamName(streamName)
					.setModuleType(moduleType)
					.setModuleLabel(moduleLabel)
					.setContainer(containerAttributes.getId()).build();
		}
		if (client.checkExists().forPath(path) != null) {
			logger.trace("Deleting path: {}", path);
			client.delete().forPath(path);
		}
	}

	/**
	 * Create a composed module based on the provided {@link ModuleDescriptor}, {@link ModuleOptions}, and
	 * {@link ModuleDeploymentProperties}.
	 *
	 * @param compositeDescriptor descriptor for the composed module
	 * @param options module options for the composed module
	 * @param deploymentProperties deployment related properties for the composed module
	 *
	 * @return new composed module instance
	 *
	 * @see ModuleDescriptor#isComposed
	 */
	private Module createComposedModule(ModuleDescriptor compositeDescriptor,
			ModuleOptions options, ModuleDeploymentProperties deploymentProperties) {

		List<ModuleDescriptor> children = compositeDescriptor.getChildren();
		Assert.notEmpty(children, "child module list must not be empty");

		List<Module> childrenModules = new ArrayList<Module>(children.size());
		for (ModuleDescriptor childRequest : children) {
			ModuleOptions narrowedOptions = new PrefixNarrowingModuleOptions(options, childRequest.getModuleName());
			// due to parser results being reversed, we add each at index 0
			// todo: is it right to pass the composite deploymentProperties here?
			childrenModules.add(0, createSimpleModule(childRequest, narrowedOptions, deploymentProperties));
		}
		return new CompositeModule(compositeDescriptor, deploymentProperties, childrenModules);
	}

	/**
	 * Create a module based on the provided {@link ModuleDescriptor}, {@link ModuleOptions}, and
	 * {@link ModuleDeploymentProperties}.
	 *
	 * @param descriptor descriptor for the module
	 * @param options module options for the module
	 * @param deploymentProperties deployment related properties for the module
	 *
	 * @return new module instance
	 */
	private Module createSimpleModule(ModuleDescriptor descriptor, ModuleOptions options,
			ModuleDeploymentProperties deploymentProperties) {
		ModuleDefinition definition = descriptor.getModuleDefinition();
		ClassLoader classLoader = (definition.getClasspath() == null) ? null
				: new ParentLastURLClassLoader(definition.getClasspath(), parentClassLoader);
		return new SimpleModule(descriptor, deploymentProperties, classLoader, options);
	}

	/**
	 * Takes a request and returns an instance of {@link ModuleOptions} bound with the request parameters. Binding is
	 * assumed to not fail, as it has already been validated on the admin side.
	 *
	 * @param descriptor module descriptor for which to bind request parameters
	 *
	 * @return module options bound with request parameters
	 */
	private ModuleOptions safeModuleOptionsInterpolate(ModuleDescriptor descriptor) {
		Map<String, String> parameters = descriptor.getParameters();
		ModuleOptionsMetadata moduleOptionsMetadata = moduleOptionsMetadataResolver.resolve(descriptor.getModuleDefinition());
		try {
			return moduleOptionsMetadata.interpolate(parameters);
		}
		catch (BindException e) {
			// Can't happen as parser should have already validated options
			throw new IllegalStateException(e);
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
			if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
				StreamDeploymentsPath streamDeploymentsPath = new StreamDeploymentsPath(event.getPath());

				String streamName = streamDeploymentsPath.getStreamName();
				String moduleType = streamDeploymentsPath.getModuleType();
				String moduleLabel = streamDeploymentsPath.getModuleLabel();

				undeployModule(streamName, moduleType, moduleLabel);

				String deploymentPath = new ModuleDeploymentsPath()
						.setContainer(containerAttributes.getId())
						.setStreamName(streamName)
						.setModuleType(moduleType)
						.setModuleLabel(moduleLabel).build();

				CuratorFramework client = zkConnection.getClient();
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
				if (EnumSet.of(Watcher.Event.KeeperState.SyncConnected,
						Watcher.Event.KeeperState.SaslAuthenticated,
						Watcher.Event.KeeperState.ConnectedReadOnly).contains(event.getState())) {
					// this watcher is only interested in deletes for the purposes of undeploying modules;
					// if any other change occurs the watch needs to be reestablished
					zkConnection.getClient().getData().usingWatcher(this).forPath(event.getPath());
				}
			}
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
			if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
				JobDeploymentsPath jobDeploymentsPath = new JobDeploymentsPath(event.getPath());
				String jobName = jobDeploymentsPath.getJobName();
				String moduleLabel = jobDeploymentsPath.getModuleLabel();

				undeployModule(jobName, ModuleType.job.toString(), moduleLabel);

				String deploymentPath = new ModuleDeploymentsPath()
						.setContainer(containerAttributes.getId())
						.setStreamName(jobName)
						.setModuleType(ModuleType.job.toString())
						.setModuleLabel(moduleLabel).build();

				CuratorFramework client = zkConnection.getClient();
				try {
					if (client.checkExists().forPath(deploymentPath) != null) {
						logger.trace("Deleting path: {}", deploymentPath);
						client.delete().deletingChildrenIfNeeded().forPath(deploymentPath);
					}
				}
				catch (KeeperException e) {
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
				if (EnumSet.of(Watcher.Event.KeeperState.SyncConnected,
						Watcher.Event.KeeperState.SaslAuthenticated,
						Watcher.Event.KeeperState.ConnectedReadOnly).contains(event.getState())) {
					// this watcher is only interested in deletes for the purposes of undeploying modules;
					// if any other change occurs the watch needs to be reestablished
					zkConnection.getClient().getData().usingWatcher(this).forPath(event.getPath());
				}
			}
		}
	}

	/**
	 * Listener for deployment requests for this container under {@link Paths#DEPLOYMENTS}.
	 */
	class DeploymentListener implements PathChildrenCacheListener {

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
	}

}
