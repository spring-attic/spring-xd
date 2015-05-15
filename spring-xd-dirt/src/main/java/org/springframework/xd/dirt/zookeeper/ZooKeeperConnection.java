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

package org.springframework.xd.dirt.zookeeper;

import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;

import org.springframework.context.SmartLifecycle;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A wrapper for a {@link CuratorFramework} instance whose lifecycle is managed as a Spring bean. Accepts
 * {@link ZooKeeperConnectionListener}s to be notified when connection or disconnection events are received.
 *
 * @author Mark Fisher
 * @author David Turanski
 * @author Patrick Peralta
 */
public class ZooKeeperConnection implements SmartLifecycle {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ZooKeeperConnection.class);

	/**
	 * The default client connect string. Port 2181 on localhost.
	 */
	public static final String DEFAULT_CLIENT_CONNECT_STRING = "localhost:2181";

	/**
	 * The default ZooKeeper session timeout in milliseconds.
	 */
	public static final int DEFAULT_SESSION_TIMEOUT = 60000;

	/**
	 * The default ZooKeeper connection timeout in milliseconds.
	 */
	public static final int DEFAULT_CONNECTION_TIMEOUT = 30000;

	/**
	 * The default initial number of milliseconds between connection retries.
	 */
	public static final int DEFAULT_INITIAL_RETRY_WAIT = 1000;

	/**
	 * The default number of connection attempts that will be made after
	 * a failed connection attempt.
	 */
	public static final int DEFAULT_MAX_RETRY_ATTEMPTS = 3;

	/**
	 * The underlying {@link CuratorFramework} instance.
	 */
	private volatile CuratorFramework curatorFramework;

	/**
	 * Curator client retry policy.
	 */
	private volatile RetryPolicy retryPolicy;

	/**
	 * Connection listener for Curator {@link ConnectionState} events.
	 */
	private final DelegatingConnectionStateListener connectionListener = new DelegatingConnectionStateListener();

	/**
	 * The set of {@link ZooKeeperConnectionListener}s that should be notified for connection and disconnection events.
	 */
	private final CopyOnWriteArraySet<ZooKeeperConnectionListener> listeners = new CopyOnWriteArraySet<ZooKeeperConnectionListener>();

	/**
	 * Flag that indicates whether this connection is currently active within a context.
	 */
	private volatile boolean running;

	/**
	 * Flag that indicates whether this connection should be started automatically.
	 */
	private volatile boolean autoStartup = true;

	/**
	 * The current ZooKeeper ConnectionState.
	 */
	private volatile ConnectionState currentState;

	/**
	 * The ZooKeeper connect string.
	 */
	private final String clientConnectString;

	/**
	 * Namespace path within ZooKeeper. Default is {@link Paths#XD_NAMESPACE}.
	 */
	private final String namespace;

	/**
	 * ZooKeeper session timeout in milliseconds.
	 */
	private final int sessionTimeout;

	/**
	 * ZooKeeper connection timeout in milliseconds.
	 */
	private final int connectionTimeout;

	/**
	 * Establish a ZooKeeper connection with the default client connect string: {@value #DEFAULT_CLIENT_CONNECT_STRING}
	 */
	public ZooKeeperConnection() {
		this(DEFAULT_CLIENT_CONNECT_STRING, null);
	}

	/**
	 * Establish a ZooKeeper connection with the provided client connect string.
	 *
	 * @param clientConnectString one or more {@code host:port} strings, comma-delimited if more than one
	 */
	public ZooKeeperConnection(String clientConnectString) {
		this(clientConnectString, null);
	}

	/**
	 * Establish a ZooKeeper connection with the provided client connect string and namespace.
	 *
	 * @param clientConnectString one or more {@code host:port} strings, comma-delimited if more than one
	 * @param namespace the root path namespace in ZooKeeper (or default namespace if null)
	 */
	public ZooKeeperConnection(String clientConnectString, String namespace) {
		this(clientConnectString, namespace, DEFAULT_SESSION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT,
				DEFAULT_INITIAL_RETRY_WAIT, DEFAULT_MAX_RETRY_ATTEMPTS);
	}

	/**
	 * Establish a ZooKeeper connection with the provided client connect string, namespace,
	 * and timing values.
	 *
	 * <p>This class uses {@link ExponentialBackoffRetry} internally.</p>
	 *
	 * @param clientConnectString one or more {@code host:port} strings, comma-delimited if more than one
	 * @param namespace the root path namespace in ZooKeeper (or default namespace if null)
	 * @param sessionTimeout ZooKeeper session timeout in milliseconds
	 * @param connectionTimeout ZooKeeper connection timeout in milliseconds
	 * @param initialRetryWait milliseconds between connection retries (base value)
	 * @param retryMaxAttempts number of connection attempts that will be made after a failed connection attempt
	 */
	public ZooKeeperConnection(String clientConnectString, String namespace, int sessionTimeout,
			int connectionTimeout, int initialRetryWait, int retryMaxAttempts) {
		Assert.hasText(clientConnectString, "clientConnectString is required");
		this.clientConnectString = clientConnectString;
		this.namespace = StringUtils.hasText(namespace) ? namespace : Paths.XD_NAMESPACE;
		this.sessionTimeout = sessionTimeout;
		this.connectionTimeout = connectionTimeout;
		this.retryPolicy = new ExponentialBackoffRetry(initialRetryWait, retryMaxAttempts);
	}

	/**
	 * Checks whether the underlying connection is established.
	 *
	 * @return true if connected
	 */
	public boolean isConnected() {
		return (this.currentState == ConnectionState.CONNECTED || this.currentState == ConnectionState.RECONNECTED);
	}

	/**
	 * Provides access to the underlying {@link CuratorFramework} instance.
	 *
	 * @return the {@link CuratorFramework} instance
	 */
	public CuratorFramework getClient() {
		return this.curatorFramework;
	}

	/**
	 * Add a {@link ZooKeeperConnectionListener}.
	 *
	 * @param listener the listener to add
	 * @return true if the listener was added or false if it was already registered
	 */
	public boolean addListener(ZooKeeperConnectionListener listener) {
		return this.listeners.add(listener);
	}

	/**
	 * Remove a {@link ZooKeeperConnectionListener}.
	 *
	 * @param listener the listener to remove
	 * @return true if the listener was removed or false if it was never registered
	 */
	public boolean removeListener(ZooKeeperConnectionListener listener) {
		return this.listeners.remove(listener);
	}

	// Lifecycle Implementation

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	/**
	 * Set the flag that indicates whether this connection
	 * should be started automatically.
	 *
	 * @param autoStartup if true, indicates this connection should
	 *                    be started automatically
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * Set the Curator retry policy.
	 *
	 * @param retryPolicy Curator client {@link RetryPolicy}
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		Assert.notNull(retryPolicy, "retryPolicy cannot be null");
		this.retryPolicy = retryPolicy;
	}

	/**
	 * Return the Curator retry policy.
	 *
	 * @return the Curator retry policy
	 */
	public RetryPolicy getRetryPolicy() {
		return this.retryPolicy;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getPhase() {
		// start in the last possible phase
		return Integer.MAX_VALUE;
	}

	/**
	 * Check whether this client is running.
	 */
	@Override
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Starts the underlying {@link CuratorFramework} instance.
	 */
	@Override
	public synchronized void start() {
		if (!this.running) {
			this.curatorFramework = CuratorFrameworkFactory.builder()
					.defaultData(new byte[0])
					.namespace(namespace)
					.retryPolicy(this.retryPolicy)
					.connectString(this.clientConnectString)
					.sessionTimeoutMs(sessionTimeout)
					.connectionTimeoutMs(connectionTimeout)
					.build();
			this.curatorFramework.getConnectionStateListenable().addListener(connectionListener);
			curatorFramework.start();
			this.running = true;
		}
	}

	/**
	 * Closes the underlying {@link CuratorFramework} instance.
	 */
	@Override
	public synchronized void stop() {
		if (this.running) {
			if (this.currentState != null) {
				curatorFramework.close();
			}
			this.running = false;
		}
	}

	/**
	 * Closes the underlying {@link CuratorFramework} instance and then invokes the callback.
	 */
	@Override
	public void stop(Runnable callback) {
		this.stop();
		callback.run();
	}

	/**
	 * Listener for Curator {@link ConnectionState} events that delegates to any registered ZooKeeperListeners.
	 */
	private class DelegatingConnectionStateListener implements ConnectionStateListener {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void stateChanged(CuratorFramework client, ConnectionState newState) {
			currentState = newState;
			switch (newState) {
				case CONNECTED:
					logger.info(">>> Curator connected event: " + newState);
					for (ZooKeeperConnectionListener listener : listeners) {
						listener.onConnect(client);
					}
					break;
				case RECONNECTED:
					logger.info(">>> Curator reconnected event: " + newState);
					for (ZooKeeperConnectionListener listener : listeners) {
						listener.onResume(client);
					}
					break;
				case LOST:
					logger.info(">>> Curator disconnected event: " + newState);
					for (ZooKeeperConnectionListener listener : listeners) {
						listener.onDisconnect(client);
					}
					break;
				case SUSPENDED:
					logger.info(">>> Curator suspended event: " + newState);
					for (ZooKeeperConnectionListener listener : listeners) {
						listener.onSuspend(client);
					}
					break;
				case READ_ONLY:
					// todo: ?
					logger.info(">>> Curator read-only event: " + newState);
					break;
			}
		}
	}

}
