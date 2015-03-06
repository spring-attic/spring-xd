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

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.utils.ThreadUtils;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.cluster.ContainerAttributes;
import org.springframework.xd.dirt.container.store.ContainerRepository;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnectionListener;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;

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
public class ContainerRegistrar implements ApplicationListener<ApplicationEvent>,
		ApplicationContextAware {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ContainerRegistrar.class);


	/**
	 * Metadata for the current Container.
	 */
	private final ContainerAttributes containerAttributes;


	/**
	 * Cache of children under the deployments path.
	 */
	private volatile PathChildrenCache deployments;

	/**
	 * Repository where {@link Container}s are stored.
	 */
	private final ContainerRepository containerRepository;

	/**
	 * The ZooKeeperConnection.
	 */
	private final ZooKeeperConnection zkConnection;

	/**
	 * Application context within which this registrar is defined.
	 */
	private volatile ApplicationContext context;

	/*
	 * Deployment listener.
	 */
	private final DeploymentListener deploymentListener;

	/**
	 * Namespace for management context in the container's application context.
	 */
	private final static String MGMT_CONTEXT_NAMESPACE = "management";


	/**
	 * Create an instance that will register the provided {@link ContainerAttributes} whenever the underlying
	 * {@link ZooKeeperConnection} is established. If that connection is already established at the time this instance
	 * receives a {@link ContextRefreshedEvent}, the attributes will be registered then. Otherwise, registration occurs
	 * within a callback that is invoked for connected events as well as reconnected events.
	 *
	 * @param containerAttributes runtime and configured attributes for the container
	 * @param containerRepository repository for the containers
	 * @param zkConnection ZooKeeper connection
	 */
	public ContainerRegistrar(ZooKeeperConnection zkConnection, ContainerAttributes containerAttributes,
			ContainerRepository containerRepository, DeploymentListener deploymentListener) {
		this.zkConnection = zkConnection;
		this.containerAttributes = containerAttributes;
		this.containerRepository = containerRepository;
		this.deploymentListener = deploymentListener;
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
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContextRefreshedEvent) {
			if (this.context.equals(((ContextRefreshedEvent) event).getApplicationContext())) {
				if (zkConnection.isConnected()) {
					registerWithZooKeeper(zkConnection.getClient());
				}
				zkConnection.addListener(new ContainerConnectionListener());
			}
		}
		else if (event instanceof EmbeddedServletContainerInitializedEvent) {
			String namespace = ((EmbeddedServletContainerInitializedEvent) event).getApplicationContext().getNamespace();
			// Make sure management port is updated from the ManagementServer context.
			if (MGMT_CONTEXT_NAMESPACE.equals(namespace)) {
				int managementPort = ((EmbeddedServletContainerInitializedEvent) event).getEmbeddedServletContainer().getPort();
				synchronized (containerAttributes) {
					// if the container has already been registered, update
					// the attributes in the container node; the read and
					// write operation (check if exists, attribute update)
					// must be atomic since it is possible for another thread
					// executing registerWithZooKeeper to read the attributes
					// before this thread sets the management port and therefore
					// create the node before this thread checks for its existence

					containerAttributes.setManagementPort(String.valueOf(managementPort));
					String containerId = containerAttributes.getId();

					if (zkConnection.isConnected() && containerRepository.exists(containerId)) {
						containerRepository.update(new Container(containerId, containerAttributes));
					}
				}
			}
		}
	}


	/**
	 * Write the Container attributes to ZooKeeper in an ephemeral node under {@code /xd/containers}.
	 */
	private void registerWithZooKeeper(CuratorFramework client) {
		try {
			String containerId = containerAttributes.getId();
			String containerPath = Paths.build(Paths.CONTAINERS, containerId);
			Stat containerPathStat = client.checkExists().forPath(containerPath);

			// If the ephemeral node for this container exists, there are two
			// possibilities:
			//   1. The ZooKeeper session for this container was temporarily
			//      suspended and has now resumed. This is usually the case
			//      when a RECONNECTED event is raised after a SUSPENDED
			//      event.
			//   2. A new ZooKeeper session has been created and the existing
			//      ephemeral node has not been cleaned up yet because the
			//      previous session has not expired yet. This may happen
			//      when a RECONNECTED event follows a SUSPENDED event
			//      if Curator decides to recreate the ZooKeeper connection
			//      in the background. This may also happen when ContainerRegistar
			//      forces a new ZooKeeper connection when a RECONNECTED event
			//      follows a LOST event; see ContainerConnectionListener.onResume.
			if (containerPathStat != null) {
				long prevSession = containerPathStat.getEphemeralOwner();
				long currSession = client.getZookeeperClient().getZooKeeper().getSessionId();
				if (prevSession == currSession) {
					// the current session still exists on the server; skip the
					// rest of the registration process
					logger.info(String.format("Existing registration for container %s with session 0x%x detected",
							containerId, currSession));
					return;
				}

				logger.info(String.format("Previous registration for container %s with " +
								"session %x detected; current session: 0x%x",
						containerId, prevSession, currSession));

				// Before proceeding, wait for the server to remove the ephemeral
				// node from the previous session
				int i = 1;
				long startTime = System.currentTimeMillis();
				while (client.checkExists().forPath(containerPath) != null) {
					logger.info("Waiting for container registration cleanup (elapsed time {} seconds)...",
							((System.currentTimeMillis() - startTime) / 1000));
					Thread.sleep(exponentialDelay(i++, 60) * 1000);
				}
			}

			String moduleDeploymentPath = Paths.build(Paths.MODULE_DEPLOYMENTS,
					Paths.ALLOCATED, containerId);

			// if the module deployment path exists, it means this container
			// was previously connected to ZooKeeper and the admin supervisor
			// has yet to complete processing of this container's exit event;
			// therefore the container startup process will block until the
			// admin has cleaned up the module deployment path (see XD-2004)
			int i = 1;
			long startTime = System.currentTimeMillis();
			while (client.checkExists().forPath(moduleDeploymentPath) != null) {
				logger.info("Waiting for supervisor to clean up prior deployments (elapsed time {} seconds)...",
						((System.currentTimeMillis() - startTime) / 1000));
				Thread.sleep(exponentialDelay(i++, 60) * 1000);
			}

			// create the module deployment path before the ephemeral node
			// (which is created via containerRepository) - the latter
			// makes the container "available" for module deployment
			client.create().creatingParentsIfNeeded().forPath(moduleDeploymentPath);

			synchronized (containerAttributes) {
				// reading the container attributes and writing them to
				// the container node must be an atomic operation; see
				// the handling of EmbeddedServletContainerInitializedEvent
				// in onApplicationEvent
				containerRepository.save(new Container(containerId, containerAttributes));
			}

			// if this container is rejoining, clear out the old cache and recreate
			if (deployments != null) {
				try {
					deployments.close();
				}
				catch (Exception e) {
					// this exception can be ignored; logging at trace level
					logger.trace("Exception while closing deployments cache", e);
				}
			}

			deployments = new PathChildrenCache(client, moduleDeploymentPath, true,
					ThreadUtils.newThreadFactory("DeploymentsPathChildrenCache"));
			deployments.getListenable().addListener(deploymentListener);
			deployments.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);

			logger.info("Container {} joined cluster", containerAttributes);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw ZooKeeperUtils.wrapThrowable(e);
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	/**
	 * Calculate an exponential delay per the algorithm described
	 * in http://en.wikipedia.org/wiki/Exponential_backoff.
	 *
	 * @param attempts number of failed attempts
	 * @param max      the maximum amount of time to wait
	 *
	 * @return the amount of time to wait
	 */
	private long exponentialDelay(int attempts, long max) {
		long delay = ((long) Math.pow(2, attempts) - 1) / 2;
		return Math.min(delay, max);
	}

	/**
	 * The listener that triggers registration of the container attributes in a ZooKeeper node.
	 */
	private class ContainerConnectionListener implements ZooKeeperConnectionListener {

		/**
		 * The last known connection state. This will be {@code null} prior to the
		 * initial ZooKeeper connection.
		 */
		private ConnectionState lastKnownState;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onConnect(CuratorFramework client) {
			lastKnownState = ConnectionState.CONNECTED;
			registerWithZooKeeper(client);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onResume(CuratorFramework client) {
			if (lastKnownState == ConnectionState.LOST) {
				if (client.getState() == CuratorFrameworkState.STARTED) {
					// this condition (connection lost but framework started)
					// may occur in single node mode when the node disconnects
					// and reconnects to ZK (see XD-2331) - note that
					// DeploymentSupervisor.ConnectionListener.onResume
					// may be invoked before this method
					logger.info("ZooKeeper connection lost and restarted; registering container");
					registerWithZooKeeper(client);
				}
				else {
					// force a shutdown and restart of the client;
					// this will create a new ZooKeeper session id
					// and cause expiration of the previous session
					// after which ZooKeeper will clean up the previous
					// ephemeral nodes for this container
					logger.info("ZooKeeper connection lost; restarting connection");
					zkConnection.stop();
					zkConnection.start();
				}
			}
			else if (lastKnownState == ConnectionState.SUSPENDED) {
				logger.info("ZooKeeper connection resumed");
				registerWithZooKeeper(client);
			}

			lastKnownState = ConnectionState.RECONNECTED;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onDisconnect(CuratorFramework client) {
			logger.warn("ZooKeeper connection terminated: {}", containerAttributes.getId());
			lastKnownState = ConnectionState.LOST;
			try {
				deployments.getListenable().removeListener(deploymentListener);
				deployments.close();
			}
			catch (Exception e) {
				logger.debug("Exception closing deployments cache", e);
			}

			deploymentListener.undeployAllModules();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onSuspend(CuratorFramework client) {
			lastKnownState = ConnectionState.SUSPENDED;
			logger.info("ZooKeeper connection suspended: {}", containerAttributes.getId());
		}
	}

}
