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

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.cluster.NoContainerException;
import org.springframework.xd.dirt.core.ModuleDeploymentsPath;
import org.springframework.xd.dirt.server.admin.deployment.ModuleDeploymentPropertiesProvider;
import org.springframework.xd.dirt.server.admin.deployment.ModuleDeploymentStatus;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.RuntimeModuleDeploymentProperties;

/**
 * Utility class to write module deployment requests under {@code /xd/deployments/modules}.
 * There are several {@code writeDeployment} methods that allow for targeting
 * a deployment to a specific container or a collection of containers.
 * <p/>
 * General usage is to invoke {@code writeDeployment} and examine the {@link org.springframework.xd.dirt.server.admin.deployment.ModuleDeploymentStatus}
 * object that is returned. This invocation will block until:
 * <ul>
 *     <li>all containers have "responded" by updating the ZooKeeper nodes</li>
 *     <li>the timeout period elapses without all containers responding.</li>
 *     <li>the waiting thread is interrupted</li>
 * </ul>
 * The results may be examined to obtain detailed information about each deployment
 * attempt and its result.
 *
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 *
 * @see org.springframework.xd.dirt.server.admin.deployment.DeploymentUnitStateCalculator
 */
public class ModuleDeploymentWriter {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ModuleDeploymentWriter.class);

	@Autowired
	private ZooKeeperConnection zkConnection;

	/**
	 * Amount of time to wait for a status to be written to all module
	 * deployment request paths.
	 *
	 */
	@Value("${xd.admin.deploymentTimeout:30000}")
	private long deploymentTimeout;

	/**
	 * Write a module deployment request for the provided module descriptor
	 * using the provided properties to the given matched container.
	 *
	 * @param moduleDescriptor      descriptor for module to deploy
	 * @param deploymentProperties  deployment properties for module
	 * @param container     		the container to deploy
	 * @return result of request
	 * @throws InterruptedException if the executing thread is interrupted
	 * @throws NoContainerException if there are no containers that match the criteria
	 *                              for module deployment
	 */
	protected ModuleDeploymentStatus writeDeployment(ModuleDescriptor moduleDescriptor,
			RuntimeModuleDeploymentProperties deploymentProperties, Container container)
			throws InterruptedException, NoContainerException {
		ResultCollector collector = new ResultCollector();
		writeDeployment(moduleDescriptor, deploymentProperties, container, collector);
		Collection<ModuleDeploymentStatus> statuses = processResults(collector);
		if (statuses.isEmpty()) {
			throw new NoContainerException();
		}
		return statuses.iterator().next();
	}


	/**
	 * Write module deployment requests for the provided module descriptor
	 * using the runtime deployment properties provided by the provider
	 * to the given matched containers.
	 *
	 * @param moduleDescriptor      descriptor for module to deploy
	 * @param provider  			runtime deployment properties provider for the module
	 * @param containers     		the matched containers to deploy
	 * @return result of request
	 * @throws InterruptedException if the executing thread is interrupted
	 * @throws NoContainerException if there are no containers that match the criteria
	 *                              for module deployment
	 */
	protected Collection<ModuleDeploymentStatus> writeDeployment(ModuleDescriptor moduleDescriptor,
			ModuleDeploymentPropertiesProvider<RuntimeModuleDeploymentProperties> provider,
			Collection<Container> containers)
			throws InterruptedException, NoContainerException {
		ResultCollector collector = new ResultCollector();
		for (Container container : containers) {
			writeDeployment(moduleDescriptor, provider.propertiesForDescriptor(moduleDescriptor), container, collector);
		}
		Collection<ModuleDeploymentStatus> statuses = processResults(collector);
		if (statuses.isEmpty()) {
			throw new NoContainerException();
		}
		return statuses;
	}

	/**
	 * Writes the module deployment to the container.
	 *
	 * @param moduleDescriptor			descriptor for module to deploy
	 * @param runtimeProperties			runtime deployment properties provider for the module
	 * @param container					the container to deploy
	 * @param collector					the result collector
	 * @throws InterruptedException 	if the executing thread is interrupted
	 * @throws NoContainerException 	if there are no containers that match the criteria
	 *                              	for module deployment
	 */
	private void writeDeployment(ModuleDescriptor moduleDescriptor,
			RuntimeModuleDeploymentProperties runtimeProperties,
			Container container, ResultCollector collector)
			throws InterruptedException, NoContainerException {
		int moduleSequence = runtimeProperties.getSequence();
		String containerName = container.getName();
		String deploymentPath = new ModuleDeploymentsPath()
				.setContainer(containerName)
				.setDeploymentUnitName(moduleDescriptor.getGroup())
				.setModuleType(moduleDescriptor.getType().toString())
				.setModuleLabel(moduleDescriptor.getModuleLabel())
				.setModuleSequence(String.valueOf(moduleSequence)).build();
		String statusPath = Paths.build(deploymentPath, Paths.STATUS);
		collector.addPending(containerName, moduleSequence, moduleDescriptor.createKey());
		try {
			ensureModuleDeploymentPath(deploymentPath, statusPath, moduleDescriptor,
					runtimeProperties, container);

			// set the collector as a watch; it is possible that
			// a. that the container has already updated this node (unlikely)
			// b. the deployment was previously written; in this case read
			//    the status written by the container
			byte[] data = zkConnection.getClient().getData().usingWatcher(collector).forPath(statusPath);
			if (data != null && data.length > 0) {
				collector.addResult(createResult(deploymentPath, data));
			}
		}
		catch (InterruptedException e) {
			throw e;
		}
		catch (Exception e) {
			collector.addResult(createResult(deploymentPath, e));
		}
	}

	/**
	 * Block the calling thread until all expected results are returned
	 * or until a timeout occurs. Additionally, remove any module deployment
	 * paths for deployments that failed or timed out.
	 *
	 * @param collector  ZooKeeper watch used to collect results
	 * @return collection of results for module deployment requests
	 * @throws InterruptedException
	 */
	protected Collection<ModuleDeploymentStatus> processResults(ResultCollector collector) throws InterruptedException {
		Collection<ModuleDeploymentStatus> statuses = collector.getResults();

		// remove the ZK path for any failed deployments
		for (ModuleDeploymentStatus deploymentStatus : statuses) {
			if (deploymentStatus.getState() != ModuleDeploymentStatus.State.deployed) {
				String path = new ModuleDeploymentsPath()
						.setContainer(deploymentStatus.getContainer())
						.setDeploymentUnitName(deploymentStatus.getKey().getGroup())
						.setModuleType(deploymentStatus.getKey().getType().toString())
						.setModuleLabel(deploymentStatus.getKey().getLabel())
						.setModuleSequence(deploymentStatus.getModuleSequenceAsString()).build();
				logger.debug("Unsuccessful deployment: {}; removing path {}", deploymentStatus, path);
				try {
					zkConnection.getClient().delete().deletingChildrenIfNeeded().forPath(path);
				}
				catch (InterruptedException e) {
					throw e;
				}
				catch (KeeperException.NoNodeException e) {
					// this node was already removed (perhaps by the supervisor
					// as a result of the target container departing the cluster);
					// this is safe to ignore
				}
				catch (Exception e) {
					logger.warn("Error while cleaning up failed deployment " + path, e);
				}
			}
		}
		return statuses;
	}

	/**
	 * Ensure the creation of the provided path to deploy the module.
	 * If the path already exists, the {@link org.apache.zookeeper.KeeperException.NodeExistsException}
	 * is swallowed. All other exceptions are rethrown.
	 *
	 * @param deploymentPath ZooKeeper path to create for module deployment
	 * @param statusPath     ZooKeeper path for module deployment status
	 * @param descriptor     module descriptor for module to be deployed
	 * @param properties     module deployment properties
	 * @param container      target container for deployment
	 *
	 * @throws Exception if an exception is thrown during path creation
	 */
	private void ensureModuleDeploymentPath(String deploymentPath, String statusPath, ModuleDescriptor descriptor,
			ModuleDeploymentProperties properties, Container container)
			throws Exception {
		try {
			zkConnection.getClient().inTransaction()
					.create().forPath(deploymentPath, ZooKeeperUtils.mapToBytes(properties)).and()
					.create().forPath(statusPath).and().commit();
		}
		catch (KeeperException.NodeExistsException e) {
			logger.info("Module {} is already deployed to container {}", descriptor, container);
		}
		catch (KeeperException.NoNodeException e) {
			logger.error(String.format("Error creating the following deployment paths: %s, %s",
					deploymentPath, statusPath), e);
			throw e;
		}
	}

	/**
	 * Create a {@link ModuleDeploymentStatus} from a ZooKeeper path and data.
	 *
	 * @param pathString  ZooKeeper module deployment path
	 * @param data        data for the path
	 * @return result based on data
	 */
	private ModuleDeploymentStatus createResult(String pathString, byte[] data) {
		return createResult(pathString, ZooKeeperUtils.bytesToMap(data));
	}

	/**
	 * Create a {@link ModuleDeploymentStatus} from a ZooKeeper path and status map.
	 *
	 * @param pathString  ZooKeeper module deployment path
	 * @param statusMap   status map
	 * @return result based on status map
	 */
	private ModuleDeploymentStatus createResult(String pathString, Map<String, String> statusMap) {
		ModuleDeploymentsPath path = new ModuleDeploymentsPath(pathString);
		ModuleDescriptor.Key key = new ModuleDescriptor.Key(
				path.getDeploymentUnitName(),
				ModuleType.valueOf(path.getModuleType()),
				path.getModuleLabel());
		return new ModuleDeploymentStatus(path.getContainer(), path.getModuleSequence(), key,
				statusMap);
	}

	/**
	 * Create a {@link ModuleDeploymentStatus} from a ZooKeeper path and a {@code Throwable}.
	 *
	 * @param pathString  ZooKeeper module deployment path
	 * @param t           exception thrown while attempting deployment
	 * @return result based on exception
	 */
	private ModuleDeploymentStatus createResult(String pathString, Throwable t) {
		ModuleDeploymentsPath path = new ModuleDeploymentsPath(pathString);
		ModuleDescriptor.Key key = new ModuleDescriptor.Key(
				path.getDeploymentUnitName(),
				ModuleType.valueOf(path.getModuleType()),
				path.getModuleLabel());

		return new ModuleDeploymentStatus(path.getContainer(), path.getModuleSequence(), key,
				ModuleDeploymentStatus.State.failed, ZooKeeperUtils.getStackTrace(t));
	}


	/**
	 * Key used to track results of module deployments to a container.
	 */
	private class ContainerModuleKey {

		/**
		 * Container name.
		 */
		private String container;

		/**
		 * Module sequence number.
		 */
		private int moduleSequence;

		/**
		 * Module descriptor key.
		 */
		private ModuleDescriptor.Key moduleDescriptorKey;

		/**
		 * Construct a {@code ContainerModuleKey}.
		 *
		 * @param container             container name
		 * @param moduleSequence        module sequence number
		 * @param moduleDescriptorKey   module descriptor key
		 */
		private ContainerModuleKey(String container, int moduleSequence, ModuleDescriptor.Key moduleDescriptorKey) {
			this.container = container;
			this.moduleSequence = moduleSequence;
			this.moduleDescriptorKey = moduleDescriptorKey;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}

			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			ContainerModuleKey that = (ContainerModuleKey) o;
			return this.container.equals(that.container) &&
					(this.moduleSequence == that.moduleSequence) &&
					this.moduleDescriptorKey.equals(that.moduleDescriptorKey);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int hashCode() {
			int result = container.hashCode();
			result = 31 * result + moduleSequence;
			result = 31 * result + moduleDescriptorKey.hashCode();
			return result;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return "ContainerModuleKey{" +
					"container='" + container + '\'' +
					"moduleSequence'" + moduleSequence + '\'' +
					", moduleDescriptorKey=" + moduleDescriptorKey +
					'}';
		}
	}

	/**
	 * Implementation of {@link org.apache.curator.framework.api.CuratorWatcher}
	 * used to collect results from the target containers updating the
	 * module deployment paths.
	 */
	private class ResultCollector implements CuratorWatcher {

		/**
		 * Pending requests for module deployments to containers.
		 */
		private final Set<ContainerModuleKey> pending = new HashSet<ContainerModuleKey>();

		/**
		 * Deployment request results for modules and containers.
		 */
		private final Map<ContainerModuleKey, ModuleDeploymentStatus> results =
				new HashMap<ContainerModuleKey, ModuleDeploymentStatus>();


		/**
		 * {@inheritDoc}
		 */
		@Override
		public void process(WatchedEvent event) throws Exception {
			logger.trace("EventCollector received event: {}", event);
			if (EnumSet.of(Watcher.Event.KeeperState.SyncConnected,
					Watcher.Event.KeeperState.SaslAuthenticated,
					Watcher.Event.KeeperState.ConnectedReadOnly).contains(event.getState())) {
				if (event.getType() == Watcher.Event.EventType.NodeDataChanged) {
					byte[] data = zkConnection.getClient().getData().forPath(event.getPath());
					addResult(createResult(event.getPath(), data));
				}
				else {
					logger.debug("Ignoring event: {}", event);
				}
			}
		}

		/**
		 * Indicate that a reply is expected for a module deployment request
		 * to the container for the module indicated by the module descriptor key.
		 *
		 * @param container       container name
		 * @param moduleSequence  module sequence
		 * @param key             module descriptor key
		 */
		public synchronized void addPending(String container, int moduleSequence, ModuleDescriptor.Key key) {
			pending.add(new ContainerModuleKey(container, moduleSequence, key));
		}

		/**
		 * Add an incoming result for a module deployment request.
		 *
		 * @param deploymentStatus incoming result
		 */
		public synchronized void addResult(ModuleDeploymentStatus deploymentStatus) {
			ContainerModuleKey key = new ContainerModuleKey(deploymentStatus.getContainer(),
					deploymentStatus.getModuleSequence(),
					deploymentStatus.getKey());
			pending.remove(key);
			results.put(key, deploymentStatus);
			notifyAll();
		}

		/**
		 * Block until all:
		 * <ul>
		 *     <li>All pending requests have been responded to.</li>
		 *     <li>A timeout occurs; in this case a collection of
		 *     {@link ModuleDeploymentStatus}
		 *     is returned. These results can be examined to see which container(s)
		 *     timed out.</li>
		 *     <li>The thread invoking this method is interrupted.</li>
		 * </ul>
		 *
		 * @return collection of results
		 * @throws InterruptedException
		 */
		public synchronized Collection<ModuleDeploymentStatus> getResults() throws InterruptedException {
			long now = System.currentTimeMillis();
			long expiryTime = now + deploymentTimeout;
			while (pending.size() > 0 && now < expiryTime) {
				wait(expiryTime - now);
				now = System.currentTimeMillis();
			}

			// if there are any module descriptors in the pending set,
			// this means the ZooKeeper node for that module deployment
			// was never updated
			for (ContainerModuleKey key : pending) {
				results.put(key,
						new ModuleDeploymentStatus(key.container, Integer.valueOf(key.moduleSequence),
								key.moduleDescriptorKey,
								ModuleDeploymentStatus.State.failed,
								String.format("Deployment of module '%s' to container '%s' timed out after %d ms",
										key.moduleDescriptorKey, key.container, deploymentTimeout)));
			}
			return results.values();
		}

	}

}
