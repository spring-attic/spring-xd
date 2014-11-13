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

package org.springframework.xd.distributed.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import com.oracle.tools.runtime.java.JavaApplication;
import com.oracle.tools.runtime.java.SimpleJavaApplication;
import org.apache.curator.test.TestingServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.integration.test.util.SocketUtils;
import org.springframework.util.Assert;
import org.springframework.xd.rest.client.impl.SpringXDTemplate;

/**
 * @author Patrick Peralta
 */
public class DefaultDistributedTestSupport implements DistributedTestSupport {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(DefaultDistributedTestSupport.class);

	/**
	 * ZooKeeper server.
	 *
	 * @see org.springframework.xd.distributed.util.ServerProcessUtils#startZooKeeper
	 */
	private TestingServer zooKeeper;

	/**
	 * HSQL server.
	 *
	 * @see org.springframework.xd.distributed.util.ServerProcessUtils#startHsql
	 */
	private JavaApplication<SimpleJavaApplication> hsqlServer;

	/**
	 * Admin server.
	 *
	 * @see org.springframework.xd.distributed.util.ServerProcessUtils#startAdmin
	 */
	private JavaApplication<SimpleJavaApplication> adminServer;

	/**
	 * System properties to pass to new container JVMs. These
	 * properties contain connection info for ZooKeeper, HSQL, etc.
	 */
	private Properties containerSystemProperties;

	/**
	 * The admin server URL generated at runtime.
	 */
	private String adminUrl;

	/**
	 * Template used to execute REST commands against the admin server.
	 */
	private SpringXDTemplate template;

	/**
	 * Map of container process IDs to processes.
	 */
	private final Map<Long, JavaApplication<SimpleJavaApplication>> mapPidContainers =
			new HashMap<Long, JavaApplication<SimpleJavaApplication>>();

	/**
	 * Atomic boolean indicating if {@link #startup} has been invoked.
	 */
	private static final AtomicBoolean started = new AtomicBoolean(false);


	/**
	 * {@inheritDoc}
	 * <p />
	 * Also populates the following for use in testing:
	 * <ul>
	 *     <li>{@link #containerSystemProperties} for use in creating
	 *         new container processes via
	 *         {@link org.springframework.xd.distributed.util.ServerProcessUtils#startContainer}</li>
	 *     <li>{@link #adminUrl} for connecting to the admin server</li>
	 * </ul>
	 * This implementation may only be invoked once for a test suite execution.
	 * This restriction is intended to ensure that tests execute as quickly as
	 * possible. Starting up the test infrastructure is expensive and multiple
	 * invocations will slow test execution down.
	 */
	@Override
	public void startup() {
		Assert.state(started.compareAndSet(false, true), "startup should only be invoked once");

		try {
			int zooKeeperPort = SocketUtils.findAvailableServerSocket();
			logger.info("ZooKeeper port: {}", zooKeeperPort);
			zooKeeper = ServerProcessUtils.startZooKeeper(zooKeeperPort);

			int hsqlPort = SocketUtils.findAvailableServerSocket();
			logger.info("HSQL port: {}", hsqlPort);

			// create system properties to pass to HSQL starter
			Properties systemProperties = new Properties();
			systemProperties.setProperty("hsql.server.port", String.valueOf(hsqlPort));
			hsqlServer = ServerProcessUtils.startHsql(systemProperties);

			// add the ZooKeeper connect string to the properties
			// passed to HSQL; these properties will be used
			// by containers and the admin
			systemProperties.setProperty("zk.client.connect", "localhost:" + zooKeeperPort);

			// set up the system properties to be used by
			// containers created in tests
			containerSystemProperties = new Properties();
			containerSystemProperties.putAll(systemProperties);

			// generate a port for the admin and include this
			// port in system properties passed to the admin
			int adminPort = SocketUtils.findAvailableServerSocket();
			logger.info("Admin port: {}", adminPort);

			adminUrl = "http://localhost:" + adminPort;
			systemProperties.setProperty("server.port", String.valueOf(adminPort));
			adminServer = ServerProcessUtils.startAdmin(systemProperties);
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SpringXDTemplate ensureTemplate() {
		if (template == null) {
			try {
				template = new SpringXDTemplate(new URI(adminUrl));
			}
			catch (URISyntaxException e) {
				throw new IllegalArgumentException(e);
			}
		}
		return template;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JavaApplication<SimpleJavaApplication> startContainer(Properties properties) {
		Properties systemProperties = new Properties();
		systemProperties.putAll(containerSystemProperties);
		if (properties != null) {
			systemProperties.putAll(properties);
		}

		try {
			JavaApplication<SimpleJavaApplication> containerServer =
					ServerProcessUtils.startContainer(systemProperties);
			mapPidContainers.put(containerServer.getId(), containerServer);
			return containerServer;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JavaApplication<SimpleJavaApplication> startContainer() {
		return startContainer(null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<Long, String> waitForContainers() throws InterruptedException {
		return ServerProcessUtils.waitForContainers(ensureTemplate(), mapPidContainers.keySet());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdownContainer(long pid) {
		logger.info("Killing container with pid {}", pid);
		Assert.state(mapPidContainers.containsKey(pid),
				String.format("Container pid %d not found", pid));
		mapPidContainers.remove(pid).close();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdownContainers() throws InterruptedException {
		for (Iterator<Map.Entry<Long, JavaApplication<SimpleJavaApplication>>> iterator =
					mapPidContainers.entrySet().iterator(); iterator.hasNext();) {
			iterator.next().getValue().close();
			iterator.remove();
		}
		waitForContainers();
		logger.info("All containers shutdown");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void shutdownAll() throws InterruptedException {
		shutdownContainers();
		if (adminServer != null) {
			try {
				adminServer.close();
			}
			catch (Exception e) {
				// ignore exceptions on shutdown
			}
		}
		if (hsqlServer != null) {
			try {
				hsqlServer.close();
			}
			catch (Exception e) {
				// ignore exceptions on shutdown
			}
		}
		if (zooKeeper != null) {
			try {
				zooKeeper.stop();
			}
			catch (Exception e) {
				// ignore exceptions on shutdown
			}
		}
	}

}
