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

import java.net.Socket;

import javax.net.SocketFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.xd.dirt.container.ContainerMetadata;
import org.springframework.xd.dirt.container.ContainerStartedEvent;
import org.springframework.xd.dirt.container.ContainerStoppedEvent;
import org.springframework.xd.dirt.curator.Paths;

/**
 * An instance of this class, registered as a bean in the context for a Container, will handle the registration of that
 * Container's metadata with ZooKeeper by creating an ephemeral node. It will eagerly delete that node upon a clean
 * shutdown.
 * 
 * @author Mark Fisher
 */
public class ContainerRegistrar implements SmartLifecycle, ApplicationContextAware {

	/**
	 * Logger.
	 */
	private static final Log logger = LogFactory.getLog(ContainerRegistrar.class);

	/**
	 * ZooKeeper client connect string.
	 * 
	 * todo: make this pluggable
	 */
	private final String clientConnectString = "localhost:2181";

	/**
	 * Curator client.
	 */
	private final CuratorFramework client;

	/**
	 * Curator client retry policy.
	 * 
	 * todo: make pluggable
	 */
	private final RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);

	/**
	 * Connection listener for Curator client.
	 */
	private final ConnectionListener connectionListener = new ConnectionListener();

	/**
	 * Metadata for the current Container.
	 */
	private final ContainerMetadata containerMetadata;

	/**
	 * Application context within which this registrar is defined.
	 */
	private ApplicationContext context;

	/**
	 * Flag that indicates whether this registrar is currently active within a context.
	 */
	private volatile boolean running;

	/**
	 * The current ZooKeeper ConnectionState.
	 */
	private volatile ConnectionState currentState;

	/**
	 * Create an instance of the registrar. Establishes the Container metadata and creates a ZooKeeper client.
	 */
	public ContainerRegistrar(ContainerMetadata metadata) {
		this.containerMetadata = metadata;
		client = CuratorFrameworkFactory.builder()
				.namespace(Paths.XD_NAMESPACE)
				.retryPolicy(retryPolicy)
				.connectString(clientConnectString).build();
		client.getConnectionStateListenable().addListener(connectionListener);
	}

	/**
	 * Sets the ApplicationContext id to match the Container metadata.
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}

	/**
	 * Temporary method to check if ZooKeeper appears to be running for the provided clientConnectString.
	 * 
	 * @return whether the client string appears to be able to establish a connection
	 */
	private boolean testClientConnectString() {
		for (String address : this.clientConnectString.split(",")) {
			String[] hostAndPort = address.trim().split(":");
			try {
				Socket socket = SocketFactory.getDefault().createSocket(
						hostAndPort[0], Integer.parseInt(hostAndPort[1]));
				socket.close();
				return true;
			}
			catch (Exception e) {
				// keep trying, will return false if all fail
			}
		}
		return false;
	}

	/**
	 * Write the Container metadata to ZooKeeper in an ephemeral node under {@code /xd/containers}.
	 */
	private void registerWithZooKeeper() {
		try {
			Paths.ensurePath(client, Paths.CONTAINERS);

			// todo:
			// Paths.ensurePath(client, Paths.DEPLOYMENTS);
			// deployments = new PathChildrenCache(client, Paths.DEPLOYMENTS + "/" + this.getId(), false);
			// deployments.getListenable().addListener(deploymentListener);

			String jvmName = containerMetadata.getJvmName();
			String tokens[] = jvmName.split("@");
			StringBuilder builder = new StringBuilder()
					.append("pid=").append(tokens[0])
					.append(System.lineSeparator())
					.append("host=").append(tokens[1]);

			client.create().withMode(CreateMode.EPHEMERAL).forPath(
					Paths.CONTAINERS + "/" + containerMetadata.getId(), builder.toString().getBytes("UTF-8"));

			// todo:
			// deployments.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);

			logger.info("Started container " + containerMetadata.getId() + " with attributes: " + builder);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// Lifecycle Implementation

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public int getPhase() {
		// start in the last possible phase
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public void start() {
		if (testClientConnectString()) {
			client.start();
		}
		else {
			logger.warn("ZooKeeper does not appear to be running for client connect string: " + clientConnectString);
		}
		this.context.publishEvent(new ContainerStartedEvent(this.containerMetadata));
		this.running = true;
	}

	@Override
	public void stop() {
		if (this.running) {
			if (this.currentState != null) {
				client.close();
			}
			this.context.publishEvent(new ContainerStoppedEvent(this.containerMetadata));
			this.running = false;
		}
	}

	@Override
	public void stop(Runnable callback) {
		this.stop();
		callback.run();
	}

	/**
	 * Curator connection listener.
	 */
	private class ConnectionListener implements ConnectionStateListener {

		@Override
		public void stateChanged(CuratorFramework client, ConnectionState newState) {
			currentState = newState;
			switch (newState) {
				case CONNECTED:
				case RECONNECTED:
					logger.info(">>> Curator connected event: " + newState);
					registerWithZooKeeper();
					break;
				case LOST:
				case SUSPENDED:
					logger.info(">>> Curator disconnected event: " + newState);
					break;
				case READ_ONLY:
					// todo: ???
			}
		}
	}

}
