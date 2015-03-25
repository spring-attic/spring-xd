/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.xd.dirt.server.admin.deployment.zk;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.curator.utils.ThreadUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.cluster.Admin;
import org.springframework.xd.dirt.cluster.AdminAttributes;
import org.springframework.xd.dirt.container.store.AdminRepository;
import org.springframework.xd.dirt.container.store.ContainerRepository;
import org.springframework.xd.dirt.job.JobFactory;
import org.springframework.xd.dirt.server.admin.deployment.ContainerMatcher;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentUnitStateCalculator;
import org.springframework.xd.dirt.stream.StreamFactory;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnectionListener;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;

/**
 * Process that watches ZooKeeper for Container arrivals and departures from
 * the XD cluster. Each {@code DeploymentSupervisor} instance will attempt
 * to request leadership, but at any given time only one {@code DeploymentSupervisor}
 * instance in the cluster will have leadership status.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 *
 * @see org.apache.curator.framework.recipes.leader.LeaderSelector
 */
public class DeploymentSupervisor implements ApplicationListener<ApplicationEvent>, DisposableBean {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(DeploymentSupervisor.class);

	/**
	 * ZooKeeper connection.
	 */
	@Autowired
	private ZooKeeperConnection zkConnection;

	/**
	 * Repository to load the admins.
	 */
	@Autowired
	private AdminRepository adminRepository;

	/**
	 * Consumer for {@link org.springframework.xd.dirt.server.admin.deployment.DeploymentMessage}s.
	 */
	@Autowired
	private DeploymentMessageConsumer deploymentMessageConsumer;

	/**
	 * Factory to construct {@link org.springframework.xd.dirt.core.Stream} instance
	 */
	@Autowired
	private StreamFactory streamFactory;

	/**
	 * Factory to construct {@link org.springframework.xd.dirt.core.Job} instance
	 */
	@Autowired
	private JobFactory jobFactory;

	/**
	 * Matcher that applies container matching criteria
	 */
	@Autowired
	private ContainerMatcher containerMatcher;

	/**
	 * Repository for the containers
	 */
	@Autowired
	private ContainerRepository containerRepository;

	/**
	 * Utility that writes module deployment requests to ZK path
	 */
	@Autowired
	private ModuleDeploymentWriter moduleDeploymentWriter;

	/**
	 * Deployment unit state calculator
	 */
	@Autowired
	private DeploymentUnitStateCalculator stateCalculator;

	/**
	 * Attributes for admin stored in admin repository.
	 */
	private final AdminAttributes adminAttributes;

	/**
	 * ZK distributed queue for the {@link DeploymentMessageConsumer}
	 * to use.
	 */
	private volatile DeploymentQueue deploymentQueueForConsumer = null;

	/**
	 * {@link ApplicationContext} for this admin server. This reference is updated
	 * via an application context event and read via {@link #getId()}.
	 */
	private volatile ApplicationContext applicationContext;

	/**
	 * Leader selector to elect admin server that will handle stream deployment requests. Marked volatile because this
	 * reference is written and read by the Curator event dispatch threads - there is no guarantee that the same thread
	 * will do the reading and writing.
	 */
	private volatile LeaderSelector leaderSelector;

	/**
	 * Listener that is invoked when this admin server is elected leader.
	 */
	private final LeaderSelectorListener leaderListener = new LeaderListener();

	/**
	 * ZooKeeper connection listener that attempts to obtain leadership when
	 * the ZooKeeper connection is established.
	 */
	private final ConnectionListener connectionListener = new ConnectionListener();

	/**
	 * Executor service used to execute Curator path cache events.
	 *
	 * @see #instantiatePathChildrenCache
	 */
	private final ScheduledExecutorService executorService =
			Executors.newSingleThreadScheduledExecutor(ThreadUtils.newThreadFactory("DeploymentSupervisor"));

	/**
	 * Namespace for management context in the container's application context.
	 */
	private final static String MGMT_CONTEXT_NAMESPACE = "management";

	/**
	 * The amount of time that must elapse after the newest container arrives
	 * before deployments to new containers are initiated.
	 */
	private final AtomicLong quietPeriod = new AtomicLong(15000);

	/**
	 * Property for specifying the {@link #quietPeriod quiet period}
	 * for deployments to new containers.
	 */
	public static final String QUIET_PERIOD_PROPERTY = "xd.admin.quietPeriod";

	/**
	 * Construct Deployment Supervisor
	 * @param adminAttributes the admin attributes
	 */
	public DeploymentSupervisor(AdminAttributes adminAttributes) {
		this.adminAttributes = adminAttributes;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContextRefreshedEvent) {
			String namespace = ((EmbeddedWebApplicationContext) event.getSource()).getNamespace();

			// If a custom management port is selected, a child application context
			// for management will be created (see EndpointWebMvcAutoConfiguration
			// in Spring Boot). Since the management context does not contain ZooKeeper
			// beans, ZooKeeper related initialization should not take place.
			// See https://jira.spring.io/browse/XD-2861.
			if (!MGMT_CONTEXT_NAMESPACE.equals(namespace)) {
				this.applicationContext = ((ContextRefreshedEvent) event).getApplicationContext();
				String delay = this.applicationContext.getEnvironment().getProperty(QUIET_PERIOD_PROPERTY);
				if (StringUtils.hasText(delay)) {
					quietPeriod.set(Long.parseLong(delay));
					logger.info("Set container quiet period to {} ms", delay);
				}
				if (this.zkConnection.isConnected()) {
					// initial registration, we don't yet have a port info
					registerWithZooKeeper(zkConnection.getClient());
					requestLeadership(this.zkConnection.getClient());
				}
				this.zkConnection.addListener(connectionListener);
			}
		}
		else if (event instanceof ContextStoppedEvent) {
			if (this.leaderSelector != null) {
				this.leaderSelector.close();
			}
		}
		else if (event instanceof EmbeddedServletContainerInitializedEvent) {
			String namespace = ((EmbeddedServletContainerInitializedEvent) event).getApplicationContext().getNamespace();
			int port = ((EmbeddedServletContainerInitializedEvent) event).getEmbeddedServletContainer().getPort();
			synchronized (adminAttributes) {
				if (MGMT_CONTEXT_NAMESPACE.equals(namespace)) {
					adminAttributes.setManagementPort(port);
				}
				else {
					adminAttributes.setPort(port);
				}
				if (zkConnection.isConnected() && adminRepository.exists(adminAttributes.getId())) {
					adminRepository.update(new Admin(adminAttributes.getId(), adminAttributes));
				}
			}
		}
	}

	/**
	 * Return the UUID for this admin server.
	 *
	 * @return id for this admin server
	 */
	private String getId() {
		return this.applicationContext.getId();
	}

	/**
	 * Register the {@link LeaderListener} if not already registered. This method is {@code synchronized} because it may
	 * be invoked either by the thread starting the {@link ApplicationContext} or the thread that raises the ZooKeeper
	 * connection event.
	 *
	 * @param client the {@link CuratorFramework} client
	 */
	@SuppressWarnings("rawtypes")
	private synchronized void requestLeadership(CuratorFramework client) {
		try {
			Paths.ensurePath(client, Paths.MODULE_DEPLOYMENTS);
			Paths.ensurePath(client, Paths.STREAM_DEPLOYMENTS);
			Paths.ensurePath(client, Paths.JOB_DEPLOYMENTS);
			Paths.ensurePath(client, Paths.ADMINS);
			Paths.ensurePath(client, Paths.CONTAINERS);
			Paths.ensurePath(client, Paths.STREAMS);
			Paths.ensurePath(client, Paths.JOBS);
			if (leaderSelector == null) {
				leaderSelector = new LeaderSelector(client, Paths.build(Paths.ADMINELECTION), leaderListener);
				leaderSelector.setId(getId());
				leaderSelector.start();
			}
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void destroy() {
		if (leaderSelector != null) {
			leaderSelector.close();
			leaderSelector = null;
		}
	}

	/**
	 * Instantiate a Curator {@link PathChildrenCache} for the provided path with
	 * the following parameters:
	 * <ul>
	 *     <li>node cache enabled</li>
	 *     <li>data compression disabled</li>
	 *     <li>executor service {@link #executorService} for invoking event handlers</li>
	 * </ul>
	 *
	 * @param client the {@link CuratorFramework} client
	 * @param path   the path for the cache
	 * @return Curator path children cache
	 */
	private PathChildrenCache instantiatePathChildrenCache(CuratorFramework client, String path) {
		return new PathChildrenCache(client, path, true, false, executorService);
	}

	/**
	 * Write the Container runtime attributes to ZooKeeper in an ephemeral node under {@code /xd/admins}.
	 */
	private void registerWithZooKeeper(CuratorFramework client) {
		try {
			String containerId = adminAttributes.getId();
			String containerPath = Paths.build(Paths.ADMINS, containerId);
			Stat containerPathStat = client.checkExists().forPath(containerPath);

			if (containerPathStat != null) {
				long prevSession = containerPathStat.getEphemeralOwner();
				long currSession = client.getZookeeperClient().getZooKeeper().getSessionId();
				if (prevSession == currSession) {
					// the current session still exists on the server; skip the
					// rest of the registration process
					logger.info(String.format(
							"Existing registration for admin runtime %s with session 0x%x detected",
							containerId, currSession));
					return;
				}

				logger.info(String.format("Trying to delete previous registration for admin runtime %s with " +
								"session %x detected; current session: 0x%x; path: %s",
						containerId, prevSession, currSession, containerPath));
				try {
					client.delete().forPath(containerPath);
				}
				catch (Exception e) {
					// NoNodeException - nothing to delete
					ZooKeeperUtils.wrapAndThrowIgnoring(e, NoNodeException.class);
				}
			}

			synchronized (adminAttributes) {
				// reading the container runtime attributes and writing them to
				// the container node must be an atomic operation; see
				// the handling of EmbeddedServletContainerInitializedEvent
				// in onApplicationEvent
				adminRepository.save(new Admin(containerId, adminAttributes));
			}
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
	 * {@link ZooKeeperConnectionListener} implementation that requests leadership
	 * upon connection to ZooKeeper.
	 */
	private class ConnectionListener implements ZooKeeperConnectionListener {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onConnect(CuratorFramework client) {
			logger.info("Admin {} connection established", getId());
			registerWithZooKeeper(client);
			requestLeadership(client);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onResume(CuratorFramework client) {
			logger.info("Admin {} connection resumed, client state: {}", getId(), client.getState());
			registerWithZooKeeper(client);
			requestLeadership(client);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onDisconnect(CuratorFramework client) {
			logger.info("Admin {} connection terminated", getId());
			try {
				destroy();
			}
			catch (Exception e) {
				logger.warn("exception occurred while closing leader selector", e);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onSuspend(CuratorFramework client) {
			logger.info("Admin {} connection suspended", getId());
			try {
				destroy();
			}
			catch (Exception e) {
				logger.warn("exception occurred while closing leader selector", e);
			}
		}

	}

	/**
	 * Listener implementation that is invoked when this server becomes the leader.
	 */
	class LeaderListener extends LeaderSelectorListenerAdapter {

		/**
		 * {@inheritDoc}
		 * <p/>
		 * Upon leadership election, this Admin server will create a {@link PathChildrenCache}
		 * for {@link Paths#STREAMS} and {@link Paths#JOBS}. These caches will have
		 * {@link PathChildrenCacheListener PathChildrenCacheListeners} attached to them
		 * that will react to stream and job creation and deletion. Upon leadership
		 * relinquishment, the listeners will be removed and the caches shut down.
		 */
		@Override
		@SuppressWarnings("rawtypes")
		public void takeLeadership(CuratorFramework client) throws Exception {
			logger.info("Leader Admin {} is watching for stream/job deployment requests.", getId());
			cleanupDeployments(client);
			PathChildrenCache containers = null;
			PathChildrenCache streamDeployments = null;
			PathChildrenCache jobDeployments = null;
			PathChildrenCache moduleDeploymentRequests = null;
			ContainerListener containerListener;

			try {
				String requestedModulesPath = Paths.build(Paths.MODULE_DEPLOYMENTS, Paths.REQUESTED);
				Paths.ensurePath(client, requestedModulesPath);
				String allocatedModulesPath = Paths.build(Paths.MODULE_DEPLOYMENTS, Paths.ALLOCATED);
				Paths.ensurePath(client, allocatedModulesPath);
				moduleDeploymentRequests = instantiatePathChildrenCache(client, requestedModulesPath);
				moduleDeploymentRequests.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);

				streamDeployments = instantiatePathChildrenCache(client, Paths.STREAM_DEPLOYMENTS);
//				// using BUILD_INITIAL_CACHE so that all known streams are populated
//				// in the cache before invoking recalculateStreamStates; same for
//				// jobs below
				streamDeployments.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);

				jobDeployments = instantiatePathChildrenCache(client, Paths.JOB_DEPLOYMENTS);
				jobDeployments.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);

				SupervisorElectedEvent supervisorElectedEvent = new SupervisorElectedEvent(moduleDeploymentRequests,
						streamDeployments, jobDeployments);

				Map<String, SupervisorElectionListener> listenersMap =
						applicationContext.getBeansOfType(SupervisorElectionListener.class);
				for (Map.Entry<String, SupervisorElectionListener> entry : listenersMap.entrySet()) {
					entry.getValue().onSupervisorElected(supervisorElectedEvent);
				}

				containerListener = new ContainerListener(zkConnection,
						containerRepository,
						streamFactory,
						jobFactory,
						streamDeployments,
						jobDeployments,
						moduleDeploymentRequests,
						containerMatcher,
						moduleDeploymentWriter,
						stateCalculator,
						executorService,
						quietPeriod);

				containers = instantiatePathChildrenCache(client, Paths.CONTAINERS);
				containers.getListenable().addListener(containerListener);
				containers.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);

				deploymentQueueForConsumer = new DeploymentQueue(client, deploymentMessageConsumer, Paths.DEPLOYMENT_QUEUE,
						executorService);
				deploymentQueueForConsumer.start();

				Thread.sleep(Long.MAX_VALUE);
			}
			catch (InterruptedException e) {
				logger.info("Leadership canceled due to thread interrupt");
				Thread.currentThread().interrupt();
			}
			finally {
				if (containers != null) {
					containers.close();
				}

				if (streamDeployments != null) {
					streamDeployments.close();
				}

				if (jobDeployments != null) {
					jobDeployments.close();
				}
				if (moduleDeploymentRequests != null) {
					moduleDeploymentRequests.close();
				}
				if (deploymentQueueForConsumer != null) {
					try {
						deploymentQueueForConsumer.destroy();
					}
					catch (IOException e) {
						logger.warn("Exception closing the distributed queue producer " + e);
					}
				}
			}
		}

		/**
		 * Remove module deployments targeted to containers that are no longer running.
		 *
		 * @param client the {@link CuratorFramework} client
		 *
		 * @throws Exception
		 */
		private void cleanupDeployments(CuratorFramework client) throws Exception {
			Set<String> containerDeployments = new HashSet<String>();

			try {
				containerDeployments.addAll(client.getChildren().forPath(
						Paths.build(Paths.MODULE_DEPLOYMENTS, Paths.ALLOCATED)));
				containerDeployments.removeAll(client.getChildren().forPath(Paths.build(Paths.CONTAINERS)));
			}
			catch (KeeperException.NoNodeException e) {
				// ignore
			}

			for (String oldContainer : containerDeployments) {
				try {
					client.delete().deletingChildrenIfNeeded().forPath(
							Paths.build(Paths.MODULE_DEPLOYMENTS, Paths.ALLOCATED, oldContainer));
				}
				catch (KeeperException.NoNodeException e) {
					// ignore
				}
			}
		}
	}

}
