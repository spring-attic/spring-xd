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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.cluster.NoContainerException;
import org.springframework.xd.dirt.container.store.ContainerRepository;
import org.springframework.xd.dirt.core.DeploymentUnitStatus;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.server.admin.deployment.ContainerMatcher;
import org.springframework.xd.dirt.server.admin.deployment.DeploymentUnitStateCalculator;
import org.springframework.xd.dirt.server.admin.deployment.ModuleDeploymentStatus;
import org.springframework.xd.dirt.server.admin.deployment.StreamRuntimePropertiesProvider;
import org.springframework.xd.dirt.stream.StreamFactory;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;

/**
 * Deployment handler that is responsible for deploying Stream.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public class ZKStreamDeploymentHandler extends ZKDeploymentHandler {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ZKStreamDeploymentHandler.class);

	/**
	 * Factory to construct {@link org.springframework.xd.dirt.core.Stream} instance
	 */
	@Autowired
	private StreamFactory streamFactory;

	/**
	 * Matcher that applies container matching criteria
	 */
	@Autowired
	private ContainerMatcher containerMatcher;

	/**
	 * Repository for the containers
	 */
	@Autowired
	private ContainerRepository containerRepository;

	/**
	 * Utility that writes module deployment requests to ZK path
	 */
	@Autowired
	private ModuleDeploymentWriter moduleDeploymentWriter;

	/**
	 * Deployment unit state calculator
	 */
	@Autowired
	private DeploymentUnitStateCalculator stateCalculator;


	/**
	 * Deploy the stream with the given name.
	 * @param streamName the stream name
	 * @throws Exception
	 */
	public void deploy(String streamName) throws Exception {
		CuratorFramework client = zkConnection.getClient();
		deployStream(client, DeploymentLoader.loadStream(client, streamName, streamFactory));
	}

	/**
	 * Issue deployment requests for the modules of the given stream.
	 *
	 * @param stream stream to be deployed
	 *
	 * @throws InterruptedException
	 */
	private void deployStream(CuratorFramework client, Stream stream) throws InterruptedException {
		// Ensure that the path for modules used by the container to write
		// ephemeral nodes exists. The presence of this path is assumed
		// by the supervisor when it calculates stream state when it is
		// assigned leadership. See XD-2170 for details.
		try {
			client.create().creatingParentsIfNeeded().forPath(
					Paths.build(Paths.STREAM_DEPLOYMENTS, stream.getName(), Paths.MODULES));
		}
		catch (Exception e) {
			ZooKeeperUtils.wrapAndThrowIgnoring(e, KeeperException.NodeExistsException.class);
		}

		String statusPath = Paths.build(Paths.STREAM_DEPLOYMENTS, stream.getName(), Paths.STATUS);

		// assert that the deployment status has been correctly set to "deploying"
		DeploymentUnitStatus deployingStatus = null;
		try {
			deployingStatus = new DeploymentUnitStatus(ZooKeeperUtils.bytesToMap(
					client.getData().forPath(statusPath)));
		}
		catch (Exception e) {
			// an exception indicates that the status has not been set
		}
		Assert.state(deployingStatus != null
						&& deployingStatus.getState() == DeploymentUnitStatus.State.deploying,
				String.format("Expected 'deploying' status for stream '%s'; current status: %s",
						stream.getName(), deployingStatus));

		try {
			Collection<ModuleDeploymentStatus> deploymentStatuses = new ArrayList<ModuleDeploymentStatus>();
			DefaultModuleDeploymentPropertiesProvider deploymentPropertiesProvider =
					new DefaultModuleDeploymentPropertiesProvider(stream);
			for (Iterator<ModuleDescriptor> descriptors = stream.getDeploymentOrderIterator(); descriptors.hasNext(); ) {
				ModuleDescriptor descriptor = descriptors.next();
				ModuleDeploymentProperties deploymentProperties = deploymentPropertiesProvider.propertiesForDescriptor(descriptor);

				// write out all of the required modules for this stream (including runtime properties);
				// this does not actually perform a deployment...this data is used in case there are not
				// enough containers to deploy the stream
				StreamRuntimePropertiesProvider partitionPropertiesProvider =
						new StreamRuntimePropertiesProvider(stream, deploymentPropertiesProvider);
				int moduleCount = deploymentProperties.getCount();
				if (moduleCount == 0) {
					createModuleDeploymentRequestsPath(client, descriptor,
							partitionPropertiesProvider.propertiesForDescriptor(descriptor));
				}
				else {
					for (int i = 0; i < moduleCount; i++) {
						createModuleDeploymentRequestsPath(client, descriptor,
								partitionPropertiesProvider.propertiesForDescriptor(descriptor));
					}
				}

				try {
					// find the containers that can deploy these modules
					Collection<Container> containers = containerMatcher.match(descriptor, deploymentProperties,
							containerRepository.findAll());

					// write out the deployment requests targeted to the containers obtained above;
					// a new instance of StreamPartitionPropertiesProvider is created since this
					// object is responsible for generating unique sequence ids for modules
					StreamRuntimePropertiesProvider deploymentRuntimeProvider =
							new StreamRuntimePropertiesProvider(stream, deploymentPropertiesProvider);

					deploymentStatuses.addAll(moduleDeploymentWriter.writeDeployment(
							descriptor, deploymentRuntimeProvider, containers));
				}
				catch (NoContainerException e) {
					logger.warn("No containers available for deployment of module '{}' for stream '{}'",
							descriptor.getModuleLabel(), stream.getName());
				}
			}

			DeploymentUnitStatus status = stateCalculator.calculate(stream, deploymentPropertiesProvider,
					deploymentStatuses);
			logger.info("Deployment status for stream '{}': {}", stream.getName(), status);

			client.setData().forPath(statusPath, ZooKeeperUtils.mapToBytes(status.toMap()));
		}
		catch (InterruptedException e) {
			throw e;
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}
}
