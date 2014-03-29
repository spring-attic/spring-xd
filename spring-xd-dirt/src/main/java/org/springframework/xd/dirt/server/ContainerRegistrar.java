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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
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
import org.springframework.validation.BindException;
import org.springframework.xd.dirt.container.ContainerMetadata;
import org.springframework.xd.dirt.container.ContainerStartedEvent;
import org.springframework.xd.dirt.container.ContainerStoppedEvent;
import org.springframework.xd.dirt.core.DeploymentsPath;
import org.springframework.xd.dirt.core.JobsPath;
import org.springframework.xd.dirt.core.ModuleDescriptor;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.core.StreamFactory;
import org.springframework.xd.dirt.core.StreamsPath;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDeployer;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.dirt.module.PrefixNarrowingModuleOptions;
import org.springframework.xd.dirt.stream.ParsingContext;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.XDParser;
import org.springframework.xd.dirt.stream.XDStreamParser;
import org.springframework.xd.dirt.util.MapBytesUtility;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnectionListener;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.CompositeModule;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.SimpleModule;
import org.springframework.xd.module.options.ModuleOptions;
import org.springframework.xd.module.options.ModuleOptionsMetadata;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;
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
 */
// todo: Rename ContainerServer or ModuleDeployer since it's driven by callbacks and not really a "server".
// Likewise consider the AdminServer being renamed to StreamDeployer since that is also callback-driven.
public class ContainerRegistrar implements ApplicationListener<ContextRefreshedEvent>, ApplicationContextAware,
		BeanClassLoaderAware {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(ContainerRegistrar.class);

	/**
	 * Metadata for the current Container.
	 */
	private final ContainerMetadata containerMetadata;

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
	 * ModuleDefinition repository
	 */
	private final ModuleDefinitionRepository moduleDefinitionRepository;

	/**
	 * Module options metadata resolver.
	 */
	private final ModuleOptionsMetadataResolver moduleOptionsMetadataResolver;

	/**
	 * Stream factory.
	 */
	private final StreamFactory streamFactory;

	/**
	 * Map of deployed modules.
	 */
	private final Map<ModuleDescriptor.Key, ModuleDescriptor> mapDeployedModules =
			new ConcurrentHashMap<ModuleDescriptor.Key, ModuleDescriptor>();

	/**
	 * The set of groups this container belongs to.
	 */
	private final Set<String> groups;

	/**
	 * The ModuleDeployer this container delegates to when deploying a Module.
	 */
	private final ModuleDeployer moduleDeployer;

	/**
	 * The parser for streams and jobs.
	 */
	private final XDParser parser;

	/**
	 * Application context within which this registrar is defined.
	 */
	private volatile ApplicationContext context;

	/**
	 * ClassLoader provided by the ApplicationContext.
	 */
	private volatile ClassLoader parentClassLoader;

	/**
	 * Create an instance that will register the provided {@link ContainerMetadata} whenever the underlying
	 * {@link ZooKeeperConnection} is established. If that connection is already established at the time this instance
	 * receives a {@link ContextRefreshedEvent}, the metadata will be registered then. Otherwise, registration occurs
	 * within a callback that is invoked for connected events as well as reconnected events.
	 */
	public ContainerRegistrar(ContainerMetadata metadata,
			StreamDefinitionRepository streamDefinitionRepository,
			ModuleDefinitionRepository moduleDefinitionRepository,
			ModuleOptionsMetadataResolver moduleOptionsMetadataResolver,
			ModuleDeployer moduleDeployer,
			ZooKeeperConnection zkConnection) {
		this.containerMetadata = metadata;
		this.zkConnection = zkConnection;
		// todo: support groups (see the ctor for ContainerServer in xdzk)
		this.groups = Collections.emptySet();
		this.moduleDefinitionRepository = moduleDefinitionRepository;
		this.moduleOptionsMetadataResolver = moduleOptionsMetadataResolver;
		this.moduleDeployer = moduleDeployer;
		// todo: the streamFactory should be injected
		this.streamFactory = new StreamFactory(streamDefinitionRepository, moduleDefinitionRepository,
				moduleOptionsMetadataResolver);
		this.parser = new XDStreamParser(moduleDefinitionRepository, moduleOptionsMetadataResolver);
	}

	/**
	 * Deploy the requested module.
	 * 
	 * @param moduleDescriptor
	 */
	private void deployModule(ModuleDescriptor moduleDescriptor) {
		LOG.info("Deploying module {}", moduleDescriptor);
		mapDeployedModules.put(moduleDescriptor.newKey(), moduleDescriptor);
		ModuleOptions moduleOptions = this.safeModuleOptionsInterpolate(moduleDescriptor);
		Module module = (moduleDescriptor.isComposed())
				? createComposedModule(moduleDescriptor, moduleOptions)
				: createSimpleModule(moduleDescriptor, moduleOptions);
		// todo: rather than delegate, merge ContainerRegistrar itself into and remove most of ModuleDeployer
		this.moduleDeployer.deployAndStore(module, moduleDescriptor);
	}

	/**
	 * Undeploy the requested module. </p> TODO: this is a placeholder
	 * 
	 * @param streamName name of the stream for the module
	 * @param moduleType module type
	 * @param moduleLabel module label
	 */
	protected void undeployModule(String streamName, String moduleType, String moduleLabel) {
		ModuleDescriptor.Key key = new ModuleDescriptor.Key(streamName, ModuleType.valueOf(moduleType), moduleLabel);
		ModuleDescriptor descriptor = mapDeployedModules.get(key);
		if (descriptor == null) {
			LOG.trace("Module {} already undeployed", moduleLabel);
		}
		else {
			LOG.info("Undeploying module {}", descriptor);
			mapDeployedModules.remove(key);
			this.moduleDeployer.undeploy(descriptor);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (this.context.equals(event.getApplicationContext())) {
			if (zkConnection.isConnected()) {
				registerWithZooKeeper(zkConnection.getClient());
				context.publishEvent(new ContainerStartedEvent(containerMetadata));
			}
			zkConnection.addListener(new ContainerMetadataRegisteringZooKeeperConnectionListener());
		}
	}

	/**
	 * Set the bean ClassLoader as the parent ClassLoader.
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.parentClassLoader = classLoader;
	}

	/**
	 * Write the Container metadata to ZooKeeper in an ephemeral node under {@code /xd/containers}.
	 */
	private void registerWithZooKeeper(CuratorFramework client) {
		try {
			Paths.ensurePath(client, Paths.DEPLOYMENTS);
			deployments = new PathChildrenCache(client, Paths.build(Paths.DEPLOYMENTS, containerMetadata.getId()), true);
			deployments.getListenable().addListener(deploymentListener);

			String jvmName = containerMetadata.getJvmName();
			String tokens[] = jvmName.split("@");
			Map<String, String> map = new HashMap<String, String>();
			map.put("pid", tokens[0]);
			map.put("host", tokens[1]);
			map.put("ip", containerMetadata.getIpAddress());

			StringBuilder builder = new StringBuilder();
			Iterator<String> iterator = groups.iterator();
			while (iterator.hasNext()) {
				builder.append(iterator.next());
				if (iterator.hasNext()) {
					builder.append(',');
				}
			}
			map.put("groups", builder.toString());

			// todo: might need creatingParentsIfNeeded here if ensure path is not working
			// (had a similar problem with tests last time we tried to move this code over from proto)
			client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(
					Paths.build(Paths.CONTAINERS, containerMetadata.getId()),
					mapBytesUtility.toByteArray(map));

			deployments.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);

			LOG.info("Started container {} with attributes: {} ", containerMetadata.getId(), map);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	/**
	 * The listener that triggers registration of the container metadata in a ZooKeeper node.
	 */
	private class ContainerMetadataRegisteringZooKeeperConnectionListener implements ZooKeeperConnectionListener {

		@Override
		public void onConnect(CuratorFramework client) {
			registerWithZooKeeper(client);
			context.publishEvent(new ContainerStartedEvent(containerMetadata));
		}

		@Override
		public void onDisconnect(CuratorFramework client) {
			try {
				context.publishEvent(new ContainerStoppedEvent(containerMetadata));
				LOG.warn(">>> disconnected container: {}", containerMetadata.getId());
				deployments.getListenable().removeListener(deploymentListener);
				deployments.close();
				// todo: modules in mapDeployedModules should be undeployed
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Event handler for new module deployments.
	 * 
	 * @param client curator client
	 * @param data module data
	 */
	private void onChildAdded(CuratorFramework client, ChildData data) {
		DeploymentsPath deploymentsPath = new DeploymentsPath(data.getPath());
		String streamName = deploymentsPath.getStreamName();
		String moduleType = deploymentsPath.getModuleType();
		String moduleLabel = deploymentsPath.getModuleLabel();
		if (ModuleType.job.toString().equals(moduleType)) {
			deployJob(client, streamName, moduleLabel);
		}
		else {
			deployStreamModule(client, streamName, moduleType, moduleLabel);
		}
	}

	private void deployJob(CuratorFramework client, String jobName, String jobLabel) {
		LOG.info("Deploying job '{}'", jobName);

		String jobPath = new JobsPath().setJobName(jobName)
				.setModuleLabel(jobLabel)
				.setContainer(containerMetadata.getId()).build();

		Map<String, String> map;
		try {
			map = mapBytesUtility.toMap(client.getData().forPath(Paths.build(Paths.JOBS, jobName)));

			// todo: do we need something like StreamFactory for jobs, or is that overkill?
			String jobModuleName = jobLabel.substring(0, jobLabel.lastIndexOf('-'));
			ModuleDefinition moduleDefinition = this.moduleDefinitionRepository.findByNameAndType(jobModuleName,
					ModuleType.job);
			List<ModuleDeploymentRequest> requests = this.parser.parse(jobName, map.get("definition"),
					ParsingContext.job);
			ModuleDeploymentRequest request = requests.get(0);
			ModuleDescriptor moduleDescriptor = new ModuleDescriptor(moduleDefinition, request.getGroup(), jobLabel,
					request.getIndex(), null, 1);
			moduleDescriptor.addParameters(request.getParameters());
			deployModule(moduleDescriptor);

			// this indicates that the container has deployed the module
			client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
					.forPath(jobPath, mapBytesUtility.toByteArray(Collections.singletonMap("state", "deployed")));

			// set a watch on this module in the job path;
			// if the node is deleted this indicates an undeployment
			client.getData().usingWatcher(jobModuleWatcher).forPath(jobPath);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void deployStreamModule(CuratorFramework client, String streamName, String moduleType, String moduleLabel) {
		LOG.info("Deploying module '{}' for stream '{}'", moduleLabel, streamName);

		String streamPath = new StreamsPath().setStreamName(streamName)
				.setModuleType(moduleType)
				.setModuleLabel(moduleLabel)
				.setContainer(containerMetadata.getId()).build();

		try {
			Stream stream = streamFactory.createStream(streamName,
					mapBytesUtility.toMap(client.getData().forPath(Paths.build(Paths.STREAMS, streamName))));

			deployModule(stream.getModuleDescriptor(moduleLabel, moduleType));

			// this indicates that the container has deployed the module
			client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
					.forPath(streamPath, mapBytesUtility.toByteArray(Collections.singletonMap("state", "deployed")));

			// set a watch on this module in the stream path;
			// if the node is deleted this indicates an undeployment
			client.getData().usingWatcher(streamModuleWatcher).forPath(streamPath);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (KeeperException.NodeExistsException e) {
			// todo: review, this should not happen
			LOG.info("Module for stream {} already deployed", moduleLabel, streamName);
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	/**
	 * Event handler for deployment removals.
	 * 
	 * @param client curator client
	 * @param data module data
	 */
	private void onChildRemoved(CuratorFramework client, ChildData data) throws Exception {
		DeploymentsPath deploymentsPath = new DeploymentsPath(data.getPath());
		String streamName = deploymentsPath.getStreamName();
		String moduleType = deploymentsPath.getModuleType();
		String moduleLabel = deploymentsPath.getModuleLabel();

		undeployModule(streamName, moduleType, moduleLabel);

		String path;
		if (ModuleType.job.toString().equals(moduleType)) {
			path = new JobsPath().setJobName(streamName)
					.setModuleLabel(moduleLabel)
					.setContainer(containerMetadata.getId()).build();
		}
		else {
			path = new StreamsPath().setStreamName(streamName)
					.setModuleType(moduleType)
					.setModuleLabel(moduleLabel)
					.setContainer(containerMetadata.getId()).build();
		}
		if (client.checkExists().forPath(path) != null) {
			LOG.trace("Deleting path: {}", path);
			client.delete().forPath(path);
		}
	}

	private Module createComposedModule(ModuleDescriptor compositeDescriptor, ModuleOptions options) {
		String streamName = compositeDescriptor.getStreamName();
		int index = compositeDescriptor.getIndex();
		String sourceChannelName = compositeDescriptor.getSourceChannelName();
		String sinkChannelName = compositeDescriptor.getSinkChannelName();
		String group = compositeDescriptor.getGroup();
		int count = compositeDescriptor.getCount();
		ModuleDefinition compositeDefinition = compositeDescriptor.getModuleDefinition();
		List<ModuleDeploymentRequest> children = this.parser.parse(
				compositeDefinition.getName(), compositeDefinition.getDefinition(), ParsingContext.module);
		Assert.notEmpty(children, "child module list must not be empty");
		List<Module> childrenModules = new ArrayList<Module>(children.size());
		for (ModuleDeploymentRequest childRequest : children) {
			ModuleOptions narrowedOptions = new PrefixNarrowingModuleOptions(options, childRequest.getModule());
			ModuleDefinition childDefinition = this.moduleDefinitionRepository.findByNameAndType(
					childRequest.getModule(), childRequest.getType());
			String label = ""; // todo: this should be the valid label for each module
			ModuleDescriptor childDescriptor = new ModuleDescriptor(childDefinition,
					childRequest.getGroup(), label, childRequest.getIndex(), group, count);
			// todo: hacky, but due to parser results being reversed, we add each at index 0
			childrenModules.add(0, createSimpleModule(childDescriptor, narrowedOptions));
		}
		DeploymentMetadata metadata = new DeploymentMetadata(streamName, index, sourceChannelName, sinkChannelName);
		return new CompositeModule(compositeDefinition.getName(), compositeDefinition.getType(), childrenModules,
				metadata);
	}

	private Module createSimpleModule(ModuleDescriptor descriptor, ModuleOptions options) {
		String streamName = descriptor.getStreamName();
		int index = descriptor.getIndex();
		String sourceChannelName = descriptor.getSourceChannelName();
		String sinkChannelName = descriptor.getSinkChannelName();
		DeploymentMetadata metadata = new DeploymentMetadata(streamName, index, sourceChannelName, sinkChannelName);
		ModuleDefinition definition = descriptor.getModuleDefinition();
		ClassLoader classLoader = (definition.getClasspath() == null) ? null
				: new ParentLastURLClassLoader(definition.getClasspath(), parentClassLoader);
		Module module = new SimpleModule(definition, metadata, classLoader, options);
		return module;
	}

	/**
	 * Takes a request and returns an instance of {@link ModuleOptions} bound with the request parameters. Binding is
	 * assumed to not fail, as it has already been validated on the admin side.
	 */
	private ModuleOptions safeModuleOptionsInterpolate(ModuleDescriptor descriptor) {
		// todo: this is empty for now
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
	 * Watcher for the modules deployed to this container under the {@link Paths#STREAMS} location. If the node is
	 * deleted, this container will undeploy the module.
	 */
	class StreamModuleWatcher implements CuratorWatcher {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void process(WatchedEvent event) throws Exception {
			if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
				StreamsPath streamsPath = new StreamsPath(event.getPath());

				String streamName = streamsPath.getStreamName();
				String moduleType = streamsPath.getModuleType();
				String moduleLabel = streamsPath.getModuleLabel();

				undeployModule(streamName, moduleType, moduleLabel);

				String deploymentPath = new DeploymentsPath()
						.setContainer(containerMetadata.getId())
						.setStreamName(streamName)
						.setModuleType(moduleType)
						.setModuleLabel(moduleLabel).build();

				CuratorFramework client = zkConnection.getClient();
				if (client.checkExists().forPath(deploymentPath) != null) {
					LOG.trace("Deleting path: {}", deploymentPath);
					client.delete().forPath(deploymentPath);
				}
			}
			else {
				// this watcher is only interested in deletes for the purposes of undeploying modules;
				// if any other change occurs the watch needs to be reestablished
				zkConnection.getClient().getData().usingWatcher(this).forPath(event.getPath());
			}
		}
	}

	/**
	 * Watcher for the modules deployed to this container under the {@link Paths#JOBS} location. If the node is deleted,
	 * this container will undeploy the module.
	 */
	class JobModuleWatcher implements CuratorWatcher {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void process(WatchedEvent event) throws Exception {
			if (event.getType() == Watcher.Event.EventType.NodeDeleted) {
				JobsPath jobsPath = new JobsPath(event.getPath());
				String jobName = jobsPath.getJobName();
				String moduleLabel = jobsPath.getModuleLabel();

				undeployModule(jobName, ModuleType.job.toString(), moduleLabel);

				String deploymentPath = new DeploymentsPath()
						.setContainer(containerMetadata.getId())
						.setStreamName(jobName)
						.setModuleType(ModuleType.job.toString())
						.setModuleLabel(moduleLabel).build();

				CuratorFramework client = zkConnection.getClient();
				if (client.checkExists().forPath(deploymentPath) != null) {
					LOG.trace("Deleting path: {}", deploymentPath);
					client.delete().forPath(deploymentPath);
				}
			}
			else {
				// this watcher is only interested in deletes for the purposes of undeploying modules;
				// if any other change occurs the watch needs to be reestablished
				zkConnection.getClient().getData().usingWatcher(this).forPath(event.getPath());
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
			LOG.debug("Path cache event: {}", event);
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
