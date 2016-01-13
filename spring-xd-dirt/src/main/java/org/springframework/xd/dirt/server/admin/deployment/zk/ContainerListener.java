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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.container.store.ContainerRepository;
import org.springframework.xd.dirt.job.JobFactory;
import org.springframework.xd.dirt.server.admin.deployment.ContainerMatcher;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentUnitStateCalculator;
import org.springframework.xd.dirt.stream.StreamFactory;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;

/**
 * Listener implementation that is invoked when containers are added/removed/modified.
 * In addition to reacting to container arrivals and departures, this listener can
 * be scheduled to detect departed containers on a regular basis via
 * {@link #scheduleDepartedContainerDeployer()}. This ensures that departed containers are
 * handled in case a container departing event is missed.
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
	 * ZooKeeper connection.
	 */
	private final ZooKeeperConnection zkConnection;

	/**
	 * The {@link ModuleRedeployer} that deploys unallocated stream/job modules
	 * upon new container arrival.
	 */
	private final ContainerMatchingModuleRedeployer containerMatchingModuleRedeployer;

	/**
	 * The {@link ModuleRedeployer} that re-deploys the stream/job modules
	 * that were deployed at the departed container.
	 */
	private final ModuleRedeployer departedContainerModuleRedeployer;

	/**
	 * Runnable that deploys modules to new containers.
	 */
	private final ArrivingContainerDeployer arrivingContainerDeployer = new ArrivingContainerDeployer();

	/**
	 * Runnable that handles departed containers.
	 */
	private final DepartedContainerDeployer departedContainerDeployer = new DepartedContainerDeployer();

	/**
	 * The amount of time that must elapse after the newest container arrives
	 * before deployments to new containers are initiated.
	 */
	private final AtomicLong quietPeriod;

	/**
	 * Executor service for module deployments
	 */
	private final ScheduledExecutorService executorService;

	/**
	 * Container and timestamp info for newest container.
	 */
	private final AtomicReference<ContainerArrival> latestContainer = new AtomicReference<ContainerArrival>();

	/**
	 * Construct a ContainerListener.
	 *
	 * @param zkConnection ZooKeeper connection
	 * @param streamFactory factory to construct {@link org.springframework.xd.dirt.stream.Stream}
	 * @param jobFactory factory to construct {@link org.springframework.xd.dirt.stream.Job}
	 * @param streamDeployments cache of children for stream deployments path
	 * @param jobDeployments cache of children for job deployments path
	 * @param moduleDeploymentRequests cache of children for requested module deployments path
	 * @param containerMatcher matches modules to containers
	 * @param moduleDeploymentWriter utility that writes deployment requests to zk path
	 * @param stateCalculator calculator for stream/job state
	 * @param quietPeriod AtomicLong indicating quiet period for new container module deployments
	 */
	public ContainerListener(ZooKeeperConnection zkConnection,
			ContainerRepository containerRepository,
			StreamFactory streamFactory, JobFactory jobFactory,
			PathChildrenCache streamDeployments, PathChildrenCache jobDeployments,
			PathChildrenCache moduleDeploymentRequests, ContainerMatcher containerMatcher,
			ModuleDeploymentWriter moduleDeploymentWriter, DeploymentUnitStateCalculator stateCalculator,
			ScheduledExecutorService executorService, AtomicLong quietPeriod) {
		this.zkConnection = zkConnection;
		this.containerMatchingModuleRedeployer = new ContainerMatchingModuleRedeployer(zkConnection,
				containerRepository, streamFactory, jobFactory, streamDeployments, jobDeployments,
				moduleDeploymentRequests, containerMatcher, moduleDeploymentWriter, stateCalculator);
		this.departedContainerModuleRedeployer = new DepartedContainerModuleRedeployer(zkConnection,
				containerRepository, streamFactory, jobFactory, moduleDeploymentRequests, containerMatcher,
				moduleDeploymentWriter, stateCalculator);
		this.quietPeriod = quietPeriod;
		this.executorService = executorService;
	}

	/**
	 * Schedules execution of
	 * {@link org.springframework.xd.dirt.server.admin.deployment.zk.DepartedContainerDeployer}
	 * on a regular basis.
	 * This ensures that departed containers are properly handled in case a container
	 * departs without raising an event.
	 */
	public void scheduleDepartedContainerDeployer() {
		departedContainerDeployer.scheduleOngoing();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
		ZooKeeperUtils.logCacheEvent(logger, event);
		Container container;
		switch (event.getType()) {
			case CHILD_ADDED:
				container = getContainer(event.getData());
				logger.info("Container arrived: {}", container);
				latestContainer.set(new ContainerArrival(container, System.currentTimeMillis()));
				arrivingContainerDeployer.schedule();
				break;
			case CHILD_UPDATED:
				break;
			case CHILD_REMOVED:
				container = getContainer(event.getData());
				logger.info("Container departed: {}", container);
				departedContainerDeployer.scheduleImmediately();
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
	 * Get the {@link Container} from the data.
	 *
	 * @param data the ChildData that the ContainerListener event receives.
	 * @return the container that represents name and attributes.
	 */
	private Container getContainer(ChildData data) {
		return new Container(Paths.stripPath(data.getPath()), ZooKeeperUtils.bytesToMap(data.getData()));
	}


	/**
	 * Holder to keep track of the latest arrived container and
	 * the time it arrived.
	 */
	private class ContainerArrival {

		final Container container;

		final long timestamp;

		private ContainerArrival(Container container, long timestamp) {
			this.container = container;
			this.timestamp = timestamp;
		}
	}


	/**
	 * Runnable that performs new container module deployments. It schedules
	 * itself for execution on {@link #executorService} based on the last
	 * container arrival time and the configured {@link #quietPeriod}.
	 *
	 * To request new container module deployments, invoke {@link #schedule}.
	 */
	private class ArrivingContainerDeployer implements Runnable {

		/**
		 * Flag to indicate whether this deployer has already been
		 * scheduled for execution on {@link #executorService}.
		 */
		private final AtomicBoolean scheduled = new AtomicBoolean(false);

		/**
		 * Schedule new container module deployments. This is scheduled
		 * for execution {@link #quietPeriod} milliseconds after the
		 * latest container arrival. If this runnable has already been
		 * scheduled, invoking this method has no effect.
		 */
		void schedule() {
			if (scheduled.compareAndSet(false, true)) {
				long delay = Math.max(0, quietPeriod.get() -
						(System.currentTimeMillis() - latestContainer.get().timestamp));
				logger.info("Scheduling deployments to new container(s) in {} ms ", delay);
				executorService.schedule(this, delay, TimeUnit.MILLISECONDS);
			}
			else {
				logger.trace("Container deployment already scheduled");
			}
		}

		/**
		 * If {@link #quietPeriod} milliseconds have passed since the
		 * newest container arrived (see {@link #latestContainer}),
		 * deploy new modules using {@link #arrivingContainerDeployer}.
		 * If the quiet period has not elapsed, reschedule execution
		 * of this runnable.
		 */
		@Override
		public void run() {
			scheduled.set(false);
			ContainerArrival containerArrival = latestContainer.get();
			if (containerArrival != null) {
				if (System.currentTimeMillis() >= containerArrival.timestamp + quietPeriod.get()) {
					try {
						containerMatchingModuleRedeployer.deployModules(containerArrival.container);
						latestContainer.compareAndSet(containerArrival, null);
					}
					catch (Exception e) {
						logger.error("Error deploying to container " + containerArrival.container, e);
					}
				}
				else {
					logger.trace("Quiet period not over yet; rescheduling container deployment");
					schedule();
				}
			}
			else {
				logger.trace("Arrived container already processed");
			}
		}
	}


	/**
	 * Runnable that handles departed containers. Modules that were
	 * previously deployed on departed containers are re-deployed,
	 * and any paths associated with departed containers are cleaned up.
	 */
	private class DepartedContainerDeployer implements Runnable {

		/**
		 * Schedule execution of this runnable on a regular basis.
		 */
		public void scheduleOngoing() {
			executorService.scheduleWithFixedDelay(this, 0, 5, TimeUnit.SECONDS);
		}

		/**
		 * Schedule immediate execution of this runnable.
		 */
		public void scheduleImmediately() {
			executorService.schedule(this, 0, TimeUnit.MILLISECONDS);
		}

		@Override
		public void run() {
			try {
				CuratorFramework client = zkConnection.getClient();
				Set<String> containerDeployments = new HashSet<String>();

				try {
					containerDeployments.addAll(client.getChildren().forPath(
							Paths.build(Paths.MODULE_DEPLOYMENTS, Paths.ALLOCATED)));
					containerDeployments.removeAll(client.getChildren().forPath(Paths.build(Paths.CONTAINERS)));
				}
				catch (KeeperException.NoNodeException e) {
					// ignore
				}

				for (String containerName : containerDeployments) {
					Container container = new Container(containerName, Collections.<String, String> emptyMap());
					departedContainerModuleRedeployer.deployModules(container);
				}
			}
			catch (Exception e) {
				logger.error("Exception while handling departed containers", e);
			}
		}
	}

}
