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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.xd.dirt.container.ContainerMetadata;
import org.springframework.xd.dirt.container.ContainerStartedEvent;
import org.springframework.xd.dirt.container.ContainerStoppedEvent;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnectionListener;

/**
 * An instance of this class, registered as a bean in the context for a Container, will handle the registration of that
 * Container's metadata with ZooKeeper by creating an ephemeral node. It will eagerly delete that node upon a clean
 * shutdown.
 * 
 * @author Mark Fisher
 */
public class ContainerRegistrar implements ApplicationListener<ContextRefreshedEvent> {

	/**
	 * Logger.
	 */
	private static final Log logger = LogFactory.getLog(ContainerRegistrar.class);

	/**
	 * Metadata for the current Container.
	 */
	private final ContainerMetadata containerMetadata;

	/**
	 * The ZooKeeperConnection.
	 */
	private final ZooKeeperConnection zkConnection;

	/**
	 * Application context within which this registrar is defined.
	 */
	private ApplicationContext context;

	/**
	 * Create an instance of the registrar. Establishes the Container metadata and creates a ZooKeeper client.
	 */
	public ContainerRegistrar(ContainerMetadata metadata, ZooKeeperConnection zkConnection) {
		this.containerMetadata = metadata;
		this.zkConnection = zkConnection;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		this.context = event.getApplicationContext();
		if (zkConnection.isConnected()) {
			registerWithZooKeeper(zkConnection.getClient());
			context.publishEvent(new ContainerStartedEvent(containerMetadata));
		}
		zkConnection.addListener(new ContainerMetadataRegisteringZooKeeperConnectionListener());
	}

	/**
	 * Write the Container metadata to ZooKeeper in an ephemeral node under {@code /xd/containers}.
	 */
	private void registerWithZooKeeper(CuratorFramework client) {
		try {
			// todo:
			// Paths.ensurePath(client, Paths.DEPLOYMENTS);
			// deployments = new PathChildrenCache(client, Paths.DEPLOYMENTS + "/" + this.getId(), false);
			// deployments.getListenable().addListener(deploymentListener);

			String jvmName = containerMetadata.getJvmName();
			String tokens[] = jvmName.split("@");
			StringBuilder builder = new StringBuilder()
					.append("pid=").append(tokens[0])
					.append(System.getProperty("line.separator"))
					.append("host=").append(tokens[1]);

			client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(
					Paths.CONTAINERS + "/" + containerMetadata.getId(), builder.toString().getBytes("UTF-8"));

			// todo:
			// deployments.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);

			logger.info("Started container " + containerMetadata.getId() + " with attributes: " + builder);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


	/**
	 * The listener that triggers registration of the container metadata in a ZooKeeper node.
	 */
	private class ContainerMetadataRegisteringZooKeeperConnectionListener implements ZooKeeperConnectionListener {

		@Override
		public void onConnect(CuratorFramework client) {
			registerWithZooKeeper(client);
			context.publishEvent(new ContainerStartedEvent(containerMetadata));
		}

		@Override
		public void onDisconnect(CuratorFramework client) {
			context.publishEvent(new ContainerStoppedEvent(containerMetadata));
		}
	}

}
