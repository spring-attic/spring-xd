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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.Assert;
import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.cluster.ContainerMatcher;
import org.springframework.xd.dirt.cluster.ContainerRepository;
import org.springframework.xd.dirt.core.ModuleDeploymentsPath;
import org.springframework.xd.dirt.util.MapBytesUtility;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;

/**
 * Utility class to write module deployment requests under {@code /xd/deployments/modules}.
 * There are several {@code writeDeployment} methods that allow for targeting
 * a deployment to a specific container or to a collection of containers indicated
 * by the {@link org.springframework.xd.dirt.cluster.ContainerMatcher} passed into
 * the constructor.
 * <p/>
 * General usage is to invoke {@code writeDeployment} and examine the {@link Result}
 * object that is returned. This invocation will block until:
 * <ul>
 *     <li>all containers have "responded" by updating the ZooKeeper nodes</li>
 *     <li>the timeout period elapses without all containers responding
 *         (default value is {@link #DEFAULT_TIMEOUT}.</li>
 *     <li>the waiting thread is interrupted</li>
 * </ul>
 * Convenience methods {@link #validateResult} and {@link #validateResults} can
 * be used to examine the returned result and throw an {@link java.lang.IllegalStateException}
 * if a module deployment timed out. Otherwise the results may be examined to
 * obtain detailed information about each deployment attempt and its result.
 *
 * @author Patrick Peralta
 */
public class ModuleDeploymentWriter {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ModuleDeploymentWriter.class);

	/**
	 * Default timeout in milliseconds.
	 *
	 * @see #timeout
	 */
	private final static long DEFAULT_TIMEOUT = 30000;

	/**
	 * ZooKeeper connection.
	 */
	private final ZooKeeperConnection zkConnection;

	/**
	 * Utility to convert byte arrays to maps of strings.
	 */
	private final MapBytesUtility mapBytesUtility = new MapBytesUtility();

	/**
	 * Container matcher for matching modules to containers.
	 */
	private final ContainerMatcher containerMatcher;

	/**
	 * Implementation of {@link ModuleDeploymentWriter.ModuleDeploymentPropertiesProvider}
	 * that returns {@link ModuleDeploymentProperties#defaultInstance}.
	 */
	private static final ModuleDeploymentPropertiesProvider defaultProvider = new ModuleDeploymentPropertiesProvider() {

		@Override
		public ModuleDeploymentProperties propertiesForDescriptor(ModuleDescriptor descriptor) {
			return ModuleDeploymentProperties.defaultInstance;
		}
	};

	/**
	 * Repository from which to obtain containers in the cluster.
	 */
	private final ContainerRepository containerRepository;

	/**
	 * Amount of time to wait for a status to be written to all module
	 * deployment request paths.
	 */
	private final long timeout;

	/**
	 * Key used in status map to indicate the module deployment status.
	 * The status map is written by the container deploying the module.
	 *
	 * @see org.springframework.xd.dirt.server.ContainerRegistrar
	 */
	public static final String STATUS_KEY = "status";

	/**
	 * Key used in status map to provide an error description.
	 * The status map is written by the container deploying the module.
	 * This value will be written if there is an error deploying the module.
	 *
	 * @see org.springframework.xd.dirt.server.ContainerRegistrar
	 */
	public static final String ERROR_DESCRIPTION_KEY = "errorDescription";

	/**
	 * Module deployment statuses.
	 */
	enum Status {
		/**
		 * Module was successfully deployed.
		 */
		deployed,

		/**
		 * Module deployment request timed out; a response was never received
		 * from the container.
		 */
		timedOut,

		/**
		 * An error occurred during module deployment (usually on the container side).
		 */
		error
	}

	/**
	 * Construct a {@code ModuleDeploymentWriter} with the default timeout
	 * value indicated by {@link #DEFAULT_TIMEOUT}.
	 *
	 * @param zkConnection         ZooKeeper connection
	 * @param containerRepository  repository for containers in the cluster
	 * @param containerMatcher     matcher for modules to containers
	 */
	public ModuleDeploymentWriter(ZooKeeperConnection zkConnection,
			ContainerRepository containerRepository, ContainerMatcher containerMatcher) {
		this(zkConnection, containerRepository, containerMatcher, DEFAULT_TIMEOUT);
	}

	/**
	 * Construct a {@code ModuleDeploymentWriter}.
	 *
	 * @param zkConnection         ZooKeeper connection
	 * @param containerRepository  repository for containers in the cluster
	 * @param containerMatcher     matcher for modules to containers
	 * @param timeout    amount of time to wait for module deployments
	 */
	public ModuleDeploymentWriter(ZooKeeperConnection zkConnection,
			ContainerRepository containerRepository, ContainerMatcher containerMatcher,
			long timeout) {
		this.zkConnection = zkConnection;
		this.containerRepository = containerRepository;
		this.timeout = timeout;
		this.containerMatcher = containerMatcher;
	}

	/**
	 * Write a deployment request for the module to the container.
	 *
	 * @param descriptor  module descriptor for module to be deployed
	 * @param container   target container for deployment
	 * @return result of request
	 * @throws InterruptedException
	 */
	public Result writeDeployment(ModuleDescriptor descriptor, final Container container) throws InterruptedException {
		ContainerMatcher matcher = new ContainerMatcher() {

			@Override
			public Collection<Container> match(ModuleDescriptor moduleDescriptor,
					ModuleDeploymentProperties deploymentProperties,
					Iterable<Container> containers) {
				return Collections.singleton(container);
			}
		};

		return writeDeployment(Collections.singletonList(descriptor).iterator(),
				defaultProvider, matcher).iterator().next();
	}

	/**
	 * Write module deployment requests for the modules returned by the {@code descriptors}
	 * iterator. The target containers are indicated by {@link #containerMatcher} and
	 * the {@link org.springframework.xd.module.ModuleDeploymentProperties} provided
	 * by the {@link ModuleDeploymentPropertiesProvider}.
	 *
	 * @param descriptors  descriptors for modules to deploy
	 * @param provider     callback to obtain the deployment properties for a module
	 * @return result of request
	 * @throws InterruptedException
	 */
	public Collection<Result> writeDeployment(Iterator<ModuleDescriptor> descriptors,
			ModuleDeploymentPropertiesProvider provider) throws InterruptedException {
		return writeDeployment(descriptors, provider, containerMatcher);
	}

	/**
	 * Write a module deployment request for the provided module descriptor
	 * using the provided properties. Since one module descriptor and one
	 * instance of {@link ModuleDeploymentProperties} are provided,  it is
	 * assumed that the provided {@link ContainerMatcher} will only return
	 * one container. This method should be used for module redeployment
	 * when a container exits the cluster.
	 *
	 * @param moduleDescriptor      descriptor for module to deploy
	 * @param deploymentProperties  deployment properties for module
	 * @param containerMatcher      matcher for modules to containers
	 * @return result of request
	 * @throws InterruptedException
	 */
	public Result writeDeployment(ModuleDescriptor moduleDescriptor,
			final ModuleDeploymentProperties deploymentProperties,
			ContainerMatcher containerMatcher) throws InterruptedException {
		Collection<Result> results = writeDeployment(Collections.singleton(moduleDescriptor).iterator(),
				new ModuleDeploymentPropertiesProvider() {
					@Override
					public ModuleDeploymentProperties propertiesForDescriptor(ModuleDescriptor descriptor) {
						return deploymentProperties;
					}
				}, containerMatcher);

		if (results.size() > 1) {
			throw new IllegalStateException("Expected to deploy to one container; " +
					"deployment results: " + results);
		}

		return results.iterator().next();
	}

	/**
	 * Write module deployment requests for the modules returned by the {@code descriptors}
	 * iterator. The target containers are indicated by the provided {@code containerMatcher}
	 * and the {@link org.springframework.xd.module.ModuleDeploymentProperties} provided
	 * by the {@link ModuleDeploymentPropertiesProvider}.
	 *
	 * @param descriptors       descriptors for modules to deploy
	 * @param provider          callback to obtain the deployment properties for a module
	 * @param containerMatcher  matcher for modules to containers
	 * @return result of request
	 * @throws InterruptedException
	 */
	public Collection<Result> writeDeployment(Iterator<ModuleDescriptor> descriptors,
			ModuleDeploymentPropertiesProvider provider, ContainerMatcher containerMatcher)
			throws InterruptedException {
		Collection<Result> results = new ArrayList<Result>();
		CuratorFramework client = zkConnection.getClient();
		while (descriptors.hasNext()) {
			ResultCollector collector = new ResultCollector();
			ModuleDescriptor descriptor = descriptors.next();
			ModuleDeploymentProperties deploymentProperties = provider.propertiesForDescriptor(descriptor);
			for (Container container : containerMatcher.match(descriptor, deploymentProperties,
					wrapAsIterable(containerRepository.getContainerIterator()))) {
				String containerName = container.getName();
				String deploymentPath = new ModuleDeploymentsPath()
						.setContainer(containerName)
						.setStreamName(descriptor.getGroup())
						.setModuleType(descriptor.getType().toString())
						.setModuleLabel(descriptor.getModuleLabel()).build();
				String statusPath = Paths.build(deploymentPath, Paths.STATUS);
				collector.addPending(containerName, descriptor.createKey());
				try {
					if (provider instanceof ContainerAwareModuleDeploymentPropertiesProvider) {
						deploymentProperties.putAll(((ContainerAwareModuleDeploymentPropertiesProvider) provider)
								.propertiesForDescriptor(descriptor, container));
					}

					ensureModuleDeploymentPath(deploymentPath, statusPath, descriptor,
							deploymentProperties, container);

					// set the collector as a watch; it is possible that
					// a. that the container has already updated this node (unlikely)
					// b. the deployment was previously written; in this case read
					//    the status written by the container
					byte[] data = client.getData().usingWatcher(collector).forPath(statusPath);
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
			// for each individual module, block until all containers
			// have responded to (or timed out) the module deployment request;
			// the blocking has to occur for each individual module in
			// order to ensure that modules for streams are deployed
			// in the correct order
			results.addAll(processResults(client, collector));
		}

		return results;
	}

	/**
	 * Block the calling thread until all expected results are returned
	 * or until a timeout occurs. Additionally, remove any module deployment
	 * paths for deployments that failed or timed out.
	 *
	 * @param client     Curator client
	 * @param collector  ZooKeeper watch used to collect results
	 * @return collection of results for module deployment requests
	 * @throws InterruptedException
	 */
	private Collection<Result> processResults(CuratorFramework client,
			ResultCollector collector) throws InterruptedException {
		Collection<Result> results = collector.getResults();

		// remove the ZK path for any failed deployments
		for (Result result : results) {
			if (result.status != Status.deployed) {
				logger.trace("Unsuccessful deployment: {}", result);
				String path = new ModuleDeploymentsPath()
						.setContainer(result.container)
						.setStreamName(result.key.getGroup())
						.setModuleType(result.key.getType().toString())
						.setModuleLabel(result.key.getLabel()).build();
				try {
					client.delete().forPath(path);
				}
				catch (InterruptedException e) {
					throw e;
				}
				catch (Exception e) {
					logger.warn("Error while cleaning up failed deployment " + path, e);
				}
			}
		}
		return results;
	}

	/**
	 * Wrap an {@link Iterator} of {@link Container} in an instance
	 * of {@link Iterable}.
	 *
	 * @param containers iterator of containers to wrap
	 *
	 * @return instance of {@code Iterable} for the provided
	 *         {@code Iterator} of {@code Container}s.
	 */
	private Iterable<Container> wrapAsIterable(final Iterator<Container> containers) {
		return new Iterable<Container>() {

			@Override
			public Iterator<Container> iterator() {
				return containers;
			}
		};
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
					.create().forPath(deploymentPath, mapBytesUtility.toByteArray(properties)).and()
					.create().forPath(statusPath).and().commit();
		}
		catch (KeeperException.NodeExistsException e) {
			logger.info("Module {} is already deployed to container {}", descriptor, container);
		}
	}

	/**
	 * Create a {@link Result} from a ZooKeeper path and data.
	 *
	 * @param pathString  ZooKeeper module deployment path
	 * @param data        data for the path
	 * @return result based on data
	 */
	private Result createResult(String pathString, byte[] data) {
		return createResult(pathString, mapBytesUtility.toMap(data));
	}

	/**
	 * Create a {@link Result} from a ZooKeeper path and status map.
	 *
	 * @param pathString  ZooKeeper module deployment path
	 * @param statusMap   status map
	 * @return result based on status map
	 *
	 * @see #STATUS_KEY
	 * @see #ERROR_DESCRIPTION_KEY
	 */
	private Result createResult(String pathString, Map<String, String> statusMap) {
		ModuleDeploymentsPath path = new ModuleDeploymentsPath(pathString);

		ModuleDescriptor.Key key = new ModuleDescriptor.Key(
				path.getStreamName(),
				ModuleType.valueOf(path.getModuleType()),
				path.getModuleLabel());
		String status = statusMap.get(STATUS_KEY);
		Assert.hasText(status, "Expected a '" + STATUS_KEY + "' key in result map");
		return new Result(path.getContainer(), key,
				Status.valueOf(status),
				statusMap.get(ERROR_DESCRIPTION_KEY));
	}

	/**
	 * Create a {@link Result} from a ZooKeeper path and a {@code Throwable}.
	 *
	 * @param pathString  ZooKeeper module deployment path
	 * @param t           exception thrown while attempting deployment
	 * @return result based on exception
	 */
	private Result createResult(String pathString, Throwable t) {
		ModuleDeploymentsPath path = new ModuleDeploymentsPath(pathString);
		ModuleDescriptor.Key key = new ModuleDescriptor.Key(
				path.getStreamName(),
				ModuleType.valueOf(path.getModuleType()),
				path.getModuleLabel());

		return new Result(path.getContainer(), key, Status.error, t.toString());
	}

	/**
	 * Validate the result. An exception is thrown if the request
	 * did not succeed.
	 *
	 * @param result result to validate
	 * @throws IllegalStateException if the result does not indicate
	 *                               a successful deployment
	 */
	public void validateResult(Result result) {
		validateResults(Collections.singleton(result));
	}

	/**
	 * Validate the results. An exception is thrown if a request
	 * in the collection did not succeed. The error message will
	 * contain a summary of all failed deployments.
	 *
	 * @param results results to validate
	 * @throws IllegalStateException if the result does not indicate
	 *                               a successful deployment
	 */
	public void validateResults(Collection<Result> results) throws IllegalStateException {
		StringBuilder error = new StringBuilder();
		for (ModuleDeploymentWriter.Result result : results) {
			switch (result.getStatus()) {
				case deployed:
					break;
				case error:
					if (error.length() > 0) {
						error.append("; ");
					}
					error.append("Container ")
							.append(result.getContainer())
							.append(" experienced the following error deploying module ")
							.append(result.getKey().getLabel())
							.append(" of type ")
							.append(result.getKey().getType())
							.append(": ")
							.append(result.getErrorDescription());
					break;
				case timedOut:
					if (error.length() > 0) {
						error.append("; ");
					}
					error.append("Container ")
							.append(result.getContainer())
							.append(" timed out deploying module ")
							.append(result.getKey().getLabel())
							.append(" of type ")
							.append(result.getKey().getType())
							.append(": ")
							.append(result.getErrorDescription());
					break;
				default:
					break;
			}
		}
		if (error.length() > 0) {
			throw new IllegalStateException(error.toString());
		}
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
		 * Module descriptor key.
		 */
		private ModuleDescriptor.Key moduleDescriptorKey;

		/**
		 * Construct a {@code ContainerModuleKey}.
		 *
		 * @param container             container name
		 * @param moduleDescriptorKey   module descriptor key
		 */
		private ContainerModuleKey(String container, ModuleDescriptor.Key moduleDescriptorKey) {
			this.container = container;
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
					this.moduleDescriptorKey.equals(that.moduleDescriptorKey);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int hashCode() {
			int result = container.hashCode();
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
		private final Map<ContainerModuleKey, Result> results =
				new HashMap<ContainerModuleKey, Result>();


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
		 * @param container  container name
		 * @param key        module descriptor key
		 */
		public synchronized void addPending(String container, ModuleDescriptor.Key key) {
			pending.add(new ContainerModuleKey(container, key));
		}

		/**
		 * Add an incoming result for a module deployment request.
		 *
		 * @param result incoming result
		 */
		public synchronized void addResult(Result result) {
			ContainerModuleKey key = new ContainerModuleKey(result.container, result.key);
			pending.remove(key);
			results.put(key, result);
			notifyAll();
		}

		/**
		 * Block until all:
		 * <ul>
		 *     <li>All pending requests have been responded to.</li>
		 *     <li>A timeout occurs; in this case a collection of
		 *     {@link org.springframework.xd.dirt.server.ModuleDeploymentWriter.Result}
		 *     is returned. These results can be examined to see which container(s)
		 *     timed out.</li>
		 *     <li>The thread invoking this method is interrupted.</li>
		 * </ul>
		 *
		 * @return collection of results
		 * @throws InterruptedException
		 */
		public synchronized Collection<Result> getResults() throws InterruptedException {
			long now = System.currentTimeMillis();
			long expiryTime = now + timeout;
			while (pending.size() > 0 && now < expiryTime) {
				wait(expiryTime - now);
				now = System.currentTimeMillis();
			}

			// if there are any module descriptors in the pending set,
			// this means the ZooKeeper node for that module deployment
			// was never updated
			for (ContainerModuleKey key : pending) {
				results.put(key,
						new Result(key.container, key.moduleDescriptorKey,
								Status.timedOut, /*errorDescription*/null));
			}
			return results.values();
		}
	}


	/**
	 * Result of a module deployment request.
	 */
	public static class Result {

		/**
		 * Target container for module deployment request.
		 */
		private final String container;

		/**
		 * Module descriptor key for the module being deployed.
		 */
		private final ModuleDescriptor.Key key;

		/**
		 * Status of module deployment.
		 */
		private final Status status;

		/**
		 * Error description; will be null unless there was an error
		 * deploying the module.
		 */
		private final String errorDescription;

		/**
		 * Construct a {@code Result}.
		 *
		 * @param container         target container name
		 * @param key               module descriptor key
		 * @param status            deployment status
		 * @param errorDescription  error description (may be null)
		 */
		public Result(String container, ModuleDescriptor.Key key, Status status, String errorDescription) {
			this.container = container;
			this.key = key;
			this.status = status;
			this.errorDescription = errorDescription;
		}

		/**
		 * @return target container name
		 *
		 * @see #container
		 */
		public String getContainer() {
			return container;
		}

		/**
		 * @return module descriptor key
		 *
		 * @see #key
		 */
		public ModuleDescriptor.Key getKey() {
			return key;
		}

		/**
		 * @return module deployment status
		 *
		 * @see #status
		 */
		public Status getStatus() {
			return status;
		}

		/**
		 * @return error description; may be null
		 *
		 * @see #errorDescription
		 */
		public String getErrorDescription() {
			return errorDescription;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return "Result{" +
					"container='" + container + '\'' +
					", key=" + key +
					", status=" + status +
					", errorDescription='" + errorDescription + '\'' +
					'}';
		}
	}


	/**
	 * Callback interface to obtain {@link ModuleDeploymentProperties}
	 * for a {@link ModuleDescriptor}.
	 */
	public interface ModuleDeploymentPropertiesProvider {

		/**
		 * Return the deployment properties for the module descriptor.
		 *
		 * @param descriptor module descriptor for module to be deployed
		 * @return deployment properties for module
		 */
		ModuleDeploymentProperties propertiesForDescriptor(ModuleDescriptor descriptor);
	}

	/**
	 * Callback interface to obtain {@link ModuleDeploymentProperties}
	 * for a {@link ModuleDescriptor} within the context of deployment
	 * to the provided {@link Container}.
	 */
	public interface ContainerAwareModuleDeploymentPropertiesProvider
			extends ModuleDeploymentPropertiesProvider {

		/**
		 * Return the deployment properties for the module descriptor
		 * that are specific for deployment to the provided {@link Container}.
		 *
		 * @param descriptor module descriptor for module to be deployed
		 * @param container  target container for deployment
		 * @return deployment properties for module
		 */
		ModuleDeploymentProperties propertiesForDescriptor(ModuleDescriptor descriptor,
				Container container);
	}

}
