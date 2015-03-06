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

import org.apache.curator.framework.recipes.queue.DistributedQueue;

import org.springframework.xd.dirt.server.admin.deployment.DeploymentMessage;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentMessagePublisher;

/**
 * ZooKeeper based {@link org.springframework.xd.dirt.server.admin.deployment.DeploymentMessagePublisher} that sends the {@link org.springframework.xd.dirt.server.admin.deployment.DeploymentMessage} into
 * ZK distributed queue.
 *
 * @author Ilayaperumal Gopinathan
 */
public class ZKDeploymentMessagePublisher implements DeploymentMessagePublisher {

	private final DistributedQueue<DeploymentMessage> distributedQueue;

	/**
	 * Construct the deployment message producer.
	 *
	 * @param deploymentQueue the deployment queue
	 */
	public ZKDeploymentMessagePublisher(DeploymentQueue deploymentQueue) {
		this.distributedQueue = deploymentQueue.getDistributedQueue();
	}

	/**
	 * Produces the deployment message into ZK distributed queue.
	 *
	 * @param deploymentMessage the deployment message
	 */
	@Override
	public void publishDeploymentMessage(DeploymentMessage deploymentMessage) {
		try {
			this.distributedQueue.put(deploymentMessage);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
