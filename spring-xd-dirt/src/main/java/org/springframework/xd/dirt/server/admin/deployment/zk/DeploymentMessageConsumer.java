/*
 * Copyright 2015 the original author or authors.
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

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.queue.QueueConsumer;
import org.apache.curator.framework.state.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.core.ResourceDeployer;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentAction;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentMessage;
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.JobDeployer;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.StreamDeployer;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;

/**
 * Consumer for {@link org.springframework.xd.dirt.server.admin.deployment.DeploymentMessage}
 * which delegates to the corresponding @{@link org.springframework.xd.dirt.core.ResourceDeployer}
 * to process the deployment requests.
 *
 * @author Ilayaperumal Gopinathan
 */
public class DeploymentMessageConsumer implements QueueConsumer<DeploymentMessage> {

	private static final Logger logger = LoggerFactory.getLogger(DeploymentMessageConsumer.class);

	@Autowired
	private StreamDeployer streamDeployer;

	@Autowired
	private JobDeployer jobDeployer;

	@Autowired
	private ZooKeeperConnection zkConnection;

	// todo: for testing only; this will be removed eventually
	public void consumeMessage(DeploymentMessage message, StreamDeployer streamDeployer, JobDeployer jobDeployer)
			throws Exception {
		this.streamDeployer = streamDeployer;
		this.jobDeployer = jobDeployer;
		this.consumeMessage(message);
	}

	/**
	 * Consume the deployment message and delegate to the deployer.
	 *
	 * @param deploymentMessage the deployment message
	 * @throws Exception
	 */
	@Override
	public void consumeMessage(DeploymentMessage deploymentMessage) throws Exception {
		switch (deploymentMessage.getDeploymentUnitType()) {
			case Stream:
				processDeploymentMessage(streamDeployer, deploymentMessage);
				break;
			case Job:
				processDeploymentMessage(jobDeployer, deploymentMessage);
				break;
		}
	}

	/**
	 * Processes the deployment message based on the deployment action.
	 *
	 * @param deployer the deployer to use for processing
	 * @param message the deployment message
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void processDeploymentMessage(ResourceDeployer deployer, DeploymentMessage message) {
		DeploymentAction deploymentAction = message.getDeploymentAction();
		String name = message.getUnitName();
		String errorDesc = null;

		try {
			switch (deploymentAction) {
				case create:
				case createAndDeploy: {
					if (deployer instanceof StreamDeployer) {
						deployer.save(new StreamDefinition(name, message.getDefinition()));
					}
					else if (deployer instanceof JobDeployer) {
						deployer.save(new JobDefinition(name, message.getDefinition()));
					}
					if (DeploymentAction.createAndDeploy.equals(deploymentAction)) {
						deployer.deploy(name, Collections.<String, String> emptyMap());
					}
					break;
				}
				case deploy:
					deployer.deploy(name, message.getDeploymentProperties());
					break;
				case undeploy:
					deployer.undeploy(name);
					break;
				case undeployAll:
					deployer.undeployAll();
					break;
				case destroy:
					deployer.delete(name);
					break;
				case destroyAll:
					deployer.deleteAll();
					break;
			}
		}
		catch (Throwable t) {//NOSONAR - ok since it's re-thrown
			errorDesc = ZooKeeperUtils.getStackTrace(t);
			throw t;
		}
		finally {
			writeResponse(message, errorDesc);
		}

	}

	/**
	 * Write the deployment message response.
	 *
	 * @param message    deployment message
	 * @param errorDesc  error description if an error occurred processing
	 *                   the request
	 */
	private void writeResponse(DeploymentMessage message, String errorDesc) {
		try {
			String requestId = message.getRequestId();
			if (StringUtils.hasText(requestId)) {
				logger.debug("Processed deployment request " + requestId);
				String resultPath = Paths.build(Paths.DEPLOYMENTS, Paths.RESPONSES, requestId,
						errorDesc == null
								? ZKDeploymentMessagePublisher.SUCCESS
								: ZKDeploymentMessagePublisher.ERROR);
				logger.debug("creating result path {}", resultPath);
				zkConnection.getClient().create().forPath(resultPath,
						errorDesc == null ? null : errorDesc.getBytes());
			}
		}
		catch (Exception e) {
			logger.info("Could not publish response to deployment message", e);
		}
	}

	@Override
	public void stateChanged(CuratorFramework client, ConnectionState newState) {
		logger.debug("Deployment Queue consumer state changed: " + newState);
	}
}
