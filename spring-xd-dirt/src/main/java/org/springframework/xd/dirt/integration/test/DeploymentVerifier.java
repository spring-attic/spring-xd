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

package org.springframework.xd.dirt.integration.test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.xd.dirt.core.RuntimeTimeoutException;
import org.springframework.xd.dirt.integration.test.source.DeploymentPathProvider;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;

/**
 * Verifies deployments or undeployments via inspection of ZooKeeper
 * paths. Each of the {@code wait...} methods block for a specified
 * amount of time and throw {@link RuntimeTimeoutException} if the
 * expected path is not present (in the case of create/deploy) or
 * is still present (in the case of undeploy/destroy) in the
 * time allotted. Deployment paths are determined by the
 * {@link DeploymentPathProvider} passed into the constructor.
 *
 * @author Patrick Peralta
 */
public class DeploymentVerifier {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(DeploymentVerifier.class);

	/**
	 * Default timeout in milliseconds.
	 */
	private static final int DEFAULT_TIMEOUT = 10000;

	/**
	 * ZooKeeper connection.
	 */
	private final ZooKeeperConnection zkConnection;

	/**
	 * Provides expected paths for definition, deployments, etc.
	 */
	private final DeploymentPathProvider provider;

	/**
	 * Timeout value in milliseconds.
	 */
	private final int verifyTimeout;

	/**
	 * Construct a {@code DeploymentVerifier} using the default timeout indicated
	 * by {@link #DEFAULT_TIMEOUT}.
	 *
	 * @param zkConnection  ZooKeeper connection
	 * @param provider      definition/deployment path provider
	 */
	public DeploymentVerifier(ZooKeeperConnection zkConnection, DeploymentPathProvider provider) {
		this(zkConnection, provider, DEFAULT_TIMEOUT);
	}

	/**
	 * Construct a {@code DeploymentVerifier} using the provided timeout.
	 *
	 * @param zkConnection   ZooKeeper connection
	 * @param provider       definition/deployment path provider
	 * @param verifyTimeout  timeout value in milliseconds
	 */
	public DeploymentVerifier(ZooKeeperConnection zkConnection, DeploymentPathProvider provider, int verifyTimeout) {
		this.zkConnection = zkConnection;
		this.provider = provider;
		this.verifyTimeout = verifyTimeout;
	}

	/**
	 * Return the ZooKeeper connection.
	 *
	 * @return the ZooKeeper connection
	 */
	protected ZooKeeperConnection getZooKeeperConnection() {
		return zkConnection;
	}

	/**
	 * Block the executing thread until the named deployment definition
	 * has been created.
	 *
	 * @param name deployment name
	 * @throws RuntimeTimeoutException if the definition isn't created in the
	 *                                 allotted time
	 */
	public void waitForCreate(String name) throws RuntimeTimeoutException {
		String path = provider.getDefinitionPath(name);
		long timeout = System.currentTimeMillis() + verifyTimeout;
		boolean exists = pathExists(path);

		try {
			while (!exists && System.currentTimeMillis() < timeout) {
				Thread.sleep(100);
				exists = pathExists(path);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		if (!exists) {
			throw new RuntimeTimeoutException(String.format("Creation of %s timed out", name));
		}
	}

	/**
	 * Block the executing thread until the named deployment definition
	 * has been destroyed.
	 *
	 * @param name deployment name
	 * @throws RuntimeTimeoutException if the definition isn't destroyed in the
	 *                                 allotted time
	 */
	public void waitForDestroy(String name) throws RuntimeTimeoutException {
		logger.trace("Waiting to destroy {}", name);

		String path = provider.getDefinitionPath(name);
		long timeout = System.currentTimeMillis() + verifyTimeout;
		boolean exists = pathExists(path);

		try {
			while (exists && System.currentTimeMillis() < timeout) {
				Thread.sleep(100);
				exists = pathExists(path);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		if (exists) {
			throw new RuntimeTimeoutException(String.format("Destruction of %s timed out", name));
		}
	}

	/**
	 * Return true if the given path is present in ZooKeeper.
	 *
	 * @param path path for which to check for existence
	 * @return return true if the path exists in ZooKeeper
	 */
	private boolean pathExists(String path) throws RuntimeTimeoutException {
		try {
			return zkConnection.getClient().checkExists().forPath(path) != null;
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Return the number of children for a given ZooKeeper path.
	 * If the path does not exist, 0 is returned.
	 *
	 * @param path path for which to return the number of children for
	 * @return number of children for the given path
	 */
	private int pathChildrenCount(String path) throws RuntimeTimeoutException {
		try {
			Stat stat = zkConnection.getClient().checkExists().forPath(path);
			return stat == null ? 0 : stat.getNumChildren();
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Block the executing thread until the named deployment has been deployed.
	 *
	 * @param name deployment name
	 * @throws RuntimeTimeoutException if the deployment isn't deployed in the
	 *                                 allotted time
	 */
	public void waitForDeploy(String name) throws RuntimeTimeoutException {
		waitForCreate(name);

		Set<String> paths = new HashSet<String>(provider.getModuleDeploymentPaths(name));
		long timeout = System.currentTimeMillis() + verifyTimeout;

		try {
			while (!paths.isEmpty() && System.currentTimeMillis() < timeout) {
				for (Iterator<String> iterator = paths.iterator(); iterator.hasNext();) {
					String path = iterator.next();
					if (pathChildrenCount(path) > 0) {
						iterator.remove();
					}
				}
				Thread.sleep(100);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		if (!paths.isEmpty()) {
			throw new RuntimeTimeoutException(
					String.format("Creation of the following module paths timed out for %s: %s", name, paths));
		}
	}

	/**
	 * Block the executing thread until the named deployment has been undeployed.
	 *
	 * @param name deployment name
	 * @throws RuntimeTimeoutException if the deployment isn't undeployed in the
	 *                                 allotted time
	 */
	public void waitForUndeploy(String name) throws RuntimeTimeoutException {
		String path = provider.getDeploymentPath(name);
		long timeout = System.currentTimeMillis() + verifyTimeout;
		int children = pathChildrenCount(path);

		try {
			while (children > 0 && System.currentTimeMillis() < timeout) {
				Thread.sleep(100);
				children = pathChildrenCount(path);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		if (children > 0) {
			throw new RuntimeTimeoutException(String.format("Undeploy of %s timed out", name));
		}
	}

}
