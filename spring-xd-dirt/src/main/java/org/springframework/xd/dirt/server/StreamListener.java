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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.cluster.ContainerMatcher;
import org.springframework.xd.dirt.cluster.ContainerRepository;
import org.springframework.xd.dirt.cluster.DefaultContainerMatcher;
import org.springframework.xd.dirt.core.DeploymentsPath;
import org.springframework.xd.dirt.core.Module;
import org.springframework.xd.dirt.core.ModuleDescriptor;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.core.StreamFactory;
import org.springframework.xd.dirt.core.StreamsPath;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.util.MapBytesUtility;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;

/**
 * Listener implementation that handles stream deployment requests.
 * 
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class StreamListener implements PathChildrenCacheListener {

	/**
	 * Logger.
	 */
	private final Logger LOG = LoggerFactory.getLogger(StreamListener.class);

	/**
	 * Provides access to the current container list.
	 */
	private final ContainerRepository containerRepository;

	/**
	 * Utility to convert maps to byte arrays.
	 */
	private final MapBytesUtility mapBytesUtility = new MapBytesUtility();

	/**
	 * Stream factory.
	 */
	private final StreamFactory streamFactory;

	/**
	 * todo: make this pluggable
	 */
	private final ContainerMatcher containerMatcher = new DefaultContainerMatcher();

	/**
	 * Construct a StreamListener.
	 * 
	 * @param containerRepository repository to obtain container data
	 * @param moduleDefinitionRepository repository to obtain module data
	 * @param moduleOptionsMetadataResolver resolver for module options metadata
	 */
	public StreamListener(ContainerRepository containerRepository,
			ModuleDefinitionRepository moduleDefinitionRepository,
			ModuleOptionsMetadataResolver moduleOptionsMetadataResolver) {
		this.containerRepository = containerRepository;
		this.streamFactory = new StreamFactory(moduleDefinitionRepository, moduleOptionsMetadataResolver);
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Handle child events for the {@link Paths#STREAMS} path.
	 */
	@Override
	public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
		switch (event.getType()) {
			case CHILD_ADDED:
				onChildAdded(client, event.getData());
				break;
			case CHILD_UPDATED:
				onChildUpdated(client, event.getData());
				break;
			case CHILD_REMOVED:
				onChildRemoved(client, event.getData());
				break;
			case CONNECTION_SUSPENDED:
				break;
			case CONNECTION_RECONNECTED:
				break;
			case CONNECTION_LOST:
				break;
			case INITIALIZED:
				// TODO!!
				// when this admin is first elected leader and there are
				// streams, it needs to verify that the streams have been
				// deployed
				// for (ChildData childData : event.getInitialData()) {
				// LOG.info("Existing stream: {}", Paths.stripPath(childData.getPath()));
				// }
				break;
		}
	}

	/**
	 * Handle the creation of a new stream.
	 * 
	 * @param client curator client
	 * @param data stream data
	 */
	private void onChildAdded(CuratorFramework client, ChildData data) throws Exception {
		String streamName = Paths.stripPath(data.getPath());
		Stream stream = streamFactory.createStream(streamName, mapBytesUtility.toMap(data.getData()));

		LOG.info("Stream definition added for {}", stream);

		if (stream.isDeploy()) {
			LOG.info("Deploying stream {}", stream);
			prepareStream(client, stream);
			deployStream(client, stream);
		}
	}

	/**
	 * Handle the updating of an existing stream.
	 *
	 * @param client curator client
	 * @param data   stream data
	 */
	private void onChildUpdated(CuratorFramework client, ChildData data) throws Exception {
		String streamName = Paths.stripPath(data.getPath());
		Stream stream = streamFactory.createStream(streamName, mapBytesUtility.toMap(data.getData()));

		if (stream.isDeploy()) {
			LOG.info("Deploying stream {}", stream);
			prepareStream(client, stream);
			deployStream(client, stream);
		}
		else {
			LOG.info("Undeploying stream {}", stream);

			// build the paths of the modules to be undeployed
			// in the stream processing order; as each path
			// is deleted each container will undeploy
			// its individual deployed modules
			List<String> paths = new ArrayList<String>();
			paths.add(new StreamsPath()
					.setStreamName(streamName)
					.setModuleType(Module.Type.SOURCE.toString())
					.build());
			for (ModuleDescriptor descriptor : stream.getProcessors()) {
				paths.add(new StreamsPath()
						.setStreamName(streamName)
						.setModuleType(Module.Type.PROCESSOR.toString())
						.setModuleLabel(descriptor.getLabel())
						.build());
			}
			paths.add(new StreamsPath()
					.setStreamName(streamName)
					.setModuleType(Module.Type.PROCESSOR.toString())
					.build());
			paths.add(new StreamsPath()
					.setStreamName(streamName)
					.setModuleType(Module.Type.SINK.toString())
					.build());

			for (String path : paths) {
				try {
					client.delete().deletingChildrenIfNeeded().forPath(path);
				}
				catch (KeeperException.NoNodeException e) {
					LOG.trace("Path {} already deleted", path);
				}
			}
		}
	}

	/**
	 * Handle the deletion of a stream.
	 *
	 * @param client curator client
	 * @param data stream data
	 */
	private void onChildRemoved(CuratorFramework client, ChildData data) throws Exception {
		String streamName = Paths.stripPath(data.getPath());
		LOG.info("Stream removed: {}", streamName);

		// nothing to do there as each container will handle its own
		// modules in the stream that was removed
	}

	/**
	 * Prepare the new stream for deployment. This updates the ZooKeeper znode for the stream by adding the following
	 * under {@code /xd/streams/[stream-name]}:
	 * <ul>
	 * <li>{@code .../source/[module-name.module-label]}</li>
	 * <li>{@code .../processor/[module-name.module-label]}</li>
	 * <li>{@code .../sink/[module-name.module-label]}</li>
	 * </ul>
	 * The children of these nodes will be ephemeral nodes written by the containers that accept deployment of the
	 * modules.
	 * 
	 * @param client curator client
	 * @param stream stream to be prepared
	 */
	private void prepareStream(CuratorFramework client, Stream stream) throws Exception {
		for (Iterator<ModuleDescriptor> iterator = stream.getDeploymentOrderIterator(); iterator.hasNext();) {
			ModuleDescriptor descriptor = iterator.next();
			String streamName = stream.getName();
			String moduleType = descriptor.getModuleDefinition().getType().toString();
			String moduleName = descriptor.getModuleDefinition().getName();
			String moduleLabel = descriptor.getLabel();

			String path = new StreamsPath()
					.setStreamName(streamName)
					.setModuleType(moduleType)
					.setModuleLabel(moduleLabel).build();

			try {
				client.create().creatingParentsIfNeeded().forPath(path);
			}
			catch (KeeperException.NodeExistsException e) {
				// todo: this would be somewhat unexpected
				LOG.info("Path {} already exists", path);
			}
		}
	}

	/**
	 * Issue deployment requests for the modules of the given stream.
	 * 
	 * @param client curator client
	 * @param stream stream to be deployed
	 * 
	 * @throws Exception
	 */
	private void deployStream(CuratorFramework client, Stream stream) throws Exception {
		for (Iterator<ModuleDescriptor> iterator = stream.getDeploymentOrderIterator(); iterator.hasNext();) {
			ModuleDescriptor descriptor = iterator.next();
			String streamName = stream.getName();
			String moduleType = descriptor.getModuleDefinition().getType().toString();
			String moduleName = descriptor.getModuleDefinition().getName();
			String moduleLabel = descriptor.getLabel();
			Map<Container, String> mapDeploymentStatus = new HashMap<Container, String>();

			for (Container container : containerMatcher.match(descriptor, containerRepository)) {
				String containerName = container.getName();
				try {
					client.create().creatingParentsIfNeeded().forPath(new DeploymentsPath()
							.setContainer(containerName)
							.setStreamName(streamName)
							.setModuleType(moduleType)
							.setModuleLabel(moduleLabel).build());

					mapDeploymentStatus.put(container, new StreamsPath()
							.setStreamName(streamName)
							.setModuleType(moduleType)
							.setModuleLabel(moduleLabel)
							.setContainer(containerName).build());
				}
				catch (KeeperException.NodeExistsException e) {
					LOG.info("Module {} is already deployed to container {}", descriptor, container);
				}
			}

			// wait for all deployments to succeed
			// todo: make timeout configurable
			long timeout = System.currentTimeMillis() + 30000;
			do {
				for (Iterator<Map.Entry<Container, String>> iteratorStatus = mapDeploymentStatus.entrySet().iterator(); iteratorStatus.hasNext();) {
					Map.Entry<Container, String> entry = iteratorStatus.next();
					if (client.checkExists().forPath(entry.getValue()) != null) {
						iteratorStatus.remove();
					}
					Thread.sleep(10);
				}
			}
			while (!mapDeploymentStatus.isEmpty() && System.currentTimeMillis() < timeout);

			if (!mapDeploymentStatus.isEmpty()) {
				// todo: if the container went away we should select another one to deploy to;
				// otherwise this reflects a bug in the container or some kind of network
				// error in which case the state of deployment is "unknown"
				throw new IllegalStateException(String.format(
						"Deployment of module %s to the following containers timed out: %s",
						moduleName, mapDeploymentStatus.keySet()));
			}
		}
	}

}
