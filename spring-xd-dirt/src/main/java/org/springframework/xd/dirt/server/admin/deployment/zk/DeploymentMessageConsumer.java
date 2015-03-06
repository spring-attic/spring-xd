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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.queue.QueueConsumer;
import org.apache.curator.framework.state.ConnectionState;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.xd.dirt.core.ResourceDeployer;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentAction;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentMessage;
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.JobDeployer;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.StreamDeployer;

/**
 * Consumer for {@link org.springframework.xd.dirt.server.admin.deployment.DeploymentMessage}
 * which delegates to the corresponding @{@link org.springframework.xd.dirt.core.ResourceDeployer}
 * to process the deployment requests.
 *
 * @author Ilayaperumal Gopinathan
 */
public class DeploymentMessageConsumer implements QueueConsumer<DeploymentMessage> {

	private static final Log logger = LogFactory.getLog(DeploymentMessageConsumer.class);

	@Autowired
	private StreamDeployer streamDeployer;

	@Autowired
	private JobDeployer jobDeployer;

	// for testing only
	public void consumeMessage(DeploymentMessage message, StreamDeployer streamDeployer, JobDeployer jobDeployer) throws Exception {
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
	 * @param deploymentMessage the deployment message
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	private void processDeploymentMessage(ResourceDeployer deployer, DeploymentMessage deploymentMessage) {
		DeploymentAction deploymentAction = deploymentMessage.getDeploymentAction();
		String name = deploymentMessage.getUnitName();
		switch (deploymentAction) {
			case create:
			case createAndDeploy: {
				if (deployer instanceof StreamDeployer) {
					deployer.save(new StreamDefinition(name, deploymentMessage.getDefinition()));
				}
				else if (deployer instanceof JobDeployer) {
					deployer.save(new JobDefinition(name, deploymentMessage.getDefinition()));
				}
				if (DeploymentAction.createAndDeploy.equals(deploymentAction)) {
					deployer.deploy(name, Collections.<String, String>emptyMap());
				}
				break;
			}
			case deploy:
				deployer.deploy(name, deploymentMessage.getDeploymentProperties());
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

	@Override
	public void stateChanged(CuratorFramework client, ConnectionState newState) {
		logger.warn("Deployment Queue consumer state changed: " + newState);
	}
}
