/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.xd.dirt.server.admin.deployment.zk;

import java.util.EnumSet;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorWatcher;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.RuntimeTimeoutException;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentException;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentMessage;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentMessagePublisher;
import org.springframework.xd.dirt.zookeeper.Paths;


/**
 * ZooKeeper based {@link DeploymentMessagePublisher} that publishes
 * {@link DeploymentMessage deployment messages} into a ZooKeeper/Curator
 * distributed queue.
 * <p>
 * The implementation of {@link #poll} blocks the executing thread until
 * the message has been processed by the recipient. This is done by
 * writing the {@link DeploymentMessage#requestId} to ZooKeeper and
 * placing a watch on the children of the node to expect a response
 * by the message handler.
 *
 * @author Ilayaperumal Gopinathan
 * @author Patrick Peralta
 */
public class ZKDeploymentMessagePublisher implements DeploymentMessagePublisher {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * Name of ZooKeeper node that indicates the deployment message
	 * was succesfully processed.
	 */
	public static final String SUCCESS = "success";

	/**
	 * Name of ZooKeeper node that indicates the deployment message
	 * encountered an error during processing.
	 */
	public static final String ERROR = "error";

	/**
	 * Queue for publishing deployment messages.
	 */
	private final DeploymentQueue deploymentQueue;

	/**
	 * Amount of time to wait for a status to be written to all module
	 * deployment request paths.
	 *
	 * @see ModuleDeploymentWriter
	 * @see #getTimeout()
	 */
	@Value("${xd.admin.deploymentTimeout:30000}")
	private long deploymentTimeout;

	/**
	 * Construct the deployment message producer.
	 *
	 * @param deploymentQueue the deployment queue
	 */
	public ZKDeploymentMessagePublisher(DeploymentQueue deploymentQueue) {
		this.deploymentQueue = deploymentQueue;
	}

	private CuratorFramework getClient() {
		return this.deploymentQueue.getClient();
	}

	/**
	 * Return the amount of time in milliseconds to wait for the message
	 * consumer to process the deployment message. This value is 3 times
	 * the value of the configured module deployment timeout.
	 *
	 * @see #deploymentTimeout
	 * @return timeout for deployment message processing
	 */
	private long getTimeout() {
		return deploymentTimeout * 3;
	}

	@Override
	public void publish(DeploymentMessage message) {
		try {
			this.deploymentQueue.getDistributedQueue().put(message);
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void poll(DeploymentMessage message) {
		CuratorFramework client = getClient();
		String requestId = message.getRequestId();
		Assert.hasText(requestId, "requestId for message required");
		ResultWatcher watcher = new ResultWatcher();
		String resultPath = Paths.build(Paths.DEPLOYMENTS, Paths.RESPONSES, requestId);

		try {
			logger.trace("result path: {}", resultPath);
			client.create().creatingParentsIfNeeded().forPath(resultPath);
			client.getChildren().usingWatcher(watcher).forPath(resultPath);
			publish(message);

			long timeout = getTimeout();
			long expiry = System.currentTimeMillis() + timeout;
			synchronized (this) {
				while (watcher.getState() == State.incomplete && System.currentTimeMillis() < expiry) {
					wait(timeout);
				}
			}

			// reading the non volatile "state" and "errorDesc" outside of
			// the synchronized block is safe because they are updated
			// in a synchronized block and read after exiting the "wait"
			// thus enforcing the "happens before" semantics of the JMM
			switch (watcher.getState()) {
				case incomplete:
					throw new RuntimeTimeoutException(String.format("Request %s timed out after %d ms",
							message, expiry));
				case error:
					throw new DeploymentException(watcher.getErrorDesc());
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				client.delete().deletingChildrenIfNeeded().forPath(resultPath);
			}
			catch (Exception e) {
				// logging this as debug; consequence is that the result
				// path isn't deleted...this is not a fatal error
				logger.debug("Exception while removing result path " + resultPath, e);
			}
		}
	}

	/**
	 * Enumeration of deployment message response states.
	 */
	private enum State {

		/**
		 * The message handler has not posted a response to the message.
		 */
		incomplete,

		/**
		 * The message handler successfully processed the message.
		 */
		success,

		/**
		 * The message handler reported an error while processing the message.
		 */
		error
	}

	/**
	 * Watch that is triggered when a child is added to the deployment
	 * message result ZooKeeper node.
	 */
	private class ResultWatcher implements CuratorWatcher {

		/**
		 * State of deployment message handling.
		 */
		private State state = State.incomplete;

		/**
		 * Description of error encountered while handling the deployment
		 * message; this is only populated if an error occurs.
		 */
		private String errorDesc;

		public State getState() {
			return state;
		}

		public String getErrorDesc() {
			return errorDesc;
		}

		@Override
		public void process(WatchedEvent event) throws Exception {
			logger.trace("Event: {}", event);
			if (EnumSet.of(Watcher.Event.KeeperState.SyncConnected,
					Watcher.Event.KeeperState.SaslAuthenticated,
					Watcher.Event.KeeperState.ConnectedReadOnly).contains(event.getState())) {
				if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
					List<String> children = getClient().getChildren().forPath(event.getPath());
					Assert.state(children.size() == 1);
					synchronized (ZKDeploymentMessagePublisher.this) {
						if (children.contains(ERROR)) {
							errorDesc = new String(getClient().getData().forPath(Paths.build(event.getPath(), ERROR)));
							state = State.error;
						}
						else {
							state = State.success;
						}
						ZKDeploymentMessagePublisher.this.notifyAll();
					}
				}
				else {
					logger.debug("Ignoring event: {}", event);
				}
			}
		}
	}

}
