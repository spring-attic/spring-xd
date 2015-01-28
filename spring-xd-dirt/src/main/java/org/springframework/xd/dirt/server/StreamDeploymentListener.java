/*
 * Copyright 2014-2015 the original author or authors.
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
import java.util.Iterator;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.Assert;
import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.cluster.NoContainerException;
import org.springframework.xd.dirt.container.store.ContainerRepository;
import org.springframework.xd.dirt.core.DeploymentUnitStatus;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.core.StreamDeploymentsPath;
import org.springframework.xd.dirt.stream.StreamFactory;
import org.springframework.xd.dirt.zookeeper.ChildPathIterator;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;

/**
 * Listener implementation that handles stream deployment requests.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public class StreamDeploymentListener extends InitialDeploymentListener {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(StreamDeploymentListener.class);

	/**
	 * Stream factory.
	 */
	private final StreamFactory streamFactory;

	/**
	 * Construct a StreamDeploymentListener.
	 *
	 * @param zkConnection ZooKeeper connection
	 * @param moduleDeploymentRequests the requested deployment modules
	 * @param containerRepository repository to obtain container data
	 * @param streamFactory factory to construct {@link Stream}
	 * @param containerMatcher matches modules to containers
	 * @param moduleDeploymentWriter utility that writes deployment requests to zk path
	 * @param stateCalculator calculator for stream state
	 */
	public StreamDeploymentListener(ZooKeeperConnection zkConnection,
			PathChildrenCache moduleDeploymentRequests,
			ContainerRepository containerRepository,
			StreamFactory streamFactory,
			ContainerMatcher containerMatcher, ModuleDeploymentWriter moduleDeploymentWriter,
			DeploymentUnitStateCalculator stateCalculator) {
		super(zkConnection, moduleDeploymentRequests, containerRepository, containerMatcher, moduleDeploymentWriter,
				stateCalculator);
		this.streamFactory = streamFactory;
	}

	/**
	 * Handle the creation of a new stream deployment.
	 *
	 * @param client curator client
	 * @param data stream deployment request data
	 */
	@Override
	protected void onChildAdded(CuratorFramework client, ChildData data) throws Exception {
		String streamName = Paths.stripPath(data.getPath());
		Stream stream = DeploymentLoader.loadStream(client, streamName, streamFactory);
		if (stream != null) {
			logger.info("Deploying stream {}", stream);
			deployStream(client, stream);
			logger.info("Stream {} deployment attempt complete", stream);
		}
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
			for (Iterator<ModuleDescriptor> descriptors = stream.getDeploymentOrderIterator(); descriptors.hasNext();) {
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


	/**
	 * Iterate all deployed streams, recalculate the state of each, and create
	 * an ephemeral node indicating the stream state. This is typically invoked
	 * upon leader election.
	 *
	 * @param client             curator client
	 * @param streamDeployments  curator cache of stream deployments
	 * @throws Exception
	 */
	public void recalculateStreamStates(CuratorFramework client, PathChildrenCache streamDeployments) throws Exception {
		for (Iterator<String> iterator =
				new ChildPathIterator<String>(ZooKeeperUtils.stripPathConverter, streamDeployments); iterator.hasNext();) {
			String streamName = iterator.next();
			String definitionPath = Paths.build(Paths.build(Paths.STREAM_DEPLOYMENTS, streamName));
			Stream stream = DeploymentLoader.loadStream(client, streamName, streamFactory);
			if (stream != null) {
				String streamModulesPath = Paths.build(definitionPath, Paths.MODULES);
				List<ModuleDeploymentStatus> statusList = new ArrayList<ModuleDeploymentStatus>();
				try {
					List<String> moduleDeployments = client.getChildren().forPath(streamModulesPath);
					for (String moduleDeployment : moduleDeployments) {
						StreamDeploymentsPath streamDeploymentsPath = new StreamDeploymentsPath(
								Paths.build(streamModulesPath, moduleDeployment));
						statusList.add(new ModuleDeploymentStatus(
								streamDeploymentsPath.getContainer(),
								streamDeploymentsPath.getModuleSequence(),
								new ModuleDescriptor.Key(streamName,
										ModuleType.valueOf(streamDeploymentsPath.getModuleType()),
										streamDeploymentsPath.getModuleLabel()),
								ModuleDeploymentStatus.State.deployed, null));
					}
				}
				catch (KeeperException.NoNodeException e) {
					// indicates there are no modules deployed for this stream;
					// ignore as this will result in an empty statusList
				}

				DeploymentUnitStatus status = stateCalculator.calculate(stream,
						new DefaultModuleDeploymentPropertiesProvider(stream), statusList);

				logger.info("Deployment status for stream '{}': {}", stream.getName(), status);

				String statusPath = Paths.build(Paths.STREAM_DEPLOYMENTS, stream.getName(), Paths.STATUS);
				Stat stat = client.checkExists().forPath(statusPath);
				if (stat != null) {
					logger.trace("Found old status path {}; stat: {}", statusPath, stat);
					client.delete().forPath(statusPath);
				}
				client.create().withMode(CreateMode.EPHEMERAL).forPath(statusPath,
						ZooKeeperUtils.mapToBytes(status.toMap()));
			}
		}
	}
}
