/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.shell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;

import org.springframework.xd.dirt.core.ModuleDescriptor;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.core.StreamFactory;
import org.springframework.xd.dirt.core.StreamsPath;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.util.MapBytesUtility;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;

/**
 * A {@link PathChildrenCacheListener} that enables waiting for a stream to be created, deployed, undeployed or
 * destroyed.
 * 
 * @author David Turanski
 * @author Mark Fisher
 */
public class StreamCommandListener implements PathChildrenCacheListener {

	private static int TIMEOUT = 5000;

	private ConcurrentMap<String, LinkedBlockingQueue<PathChildrenCacheEvent>> createQueues = new ConcurrentHashMap<String, LinkedBlockingQueue<PathChildrenCacheEvent>>();

	private ConcurrentMap<String, LinkedBlockingQueue<PathChildrenCacheEvent>> destroyQueues = new ConcurrentHashMap<String, LinkedBlockingQueue<PathChildrenCacheEvent>>();

	private Map<String, Map<String, String>> streamProperties = new HashMap<String, Map<String, String>>();

	private volatile CuratorFramework client;

	private final StreamFactory streamFactory;

	private final MapBytesUtility mapBytesUtility = new MapBytesUtility();

	public StreamCommandListener(StreamDefinitionRepository streamDefinitionRepository,
			ModuleDefinitionRepository moduleDefinitionRepository,
			ModuleOptionsMetadataResolver moduleOptionsMetadataResolver) {
		this.streamFactory = new StreamFactory(streamDefinitionRepository, moduleDefinitionRepository,
				moduleOptionsMetadataResolver);
	}

	@Override
	public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
		this.client = client;
		StreamsPath path = new StreamsPath(event.getData().getPath());
		System.out.println("**************** stream name:" + path.getStreamName() + " event " + event.getType());
		if (event.getType().equals(Type.CHILD_ADDED)) {
			streamProperties.put(path.getStreamName(), mapBytesUtility.toMap(event.getData().getData()));
			createQueues.putIfAbsent(path.getStreamName(), new LinkedBlockingQueue<PathChildrenCacheEvent>());
			LinkedBlockingQueue<PathChildrenCacheEvent> queue = createQueues.get(path.getStreamName());
			queue.put(event);
		}
		else if (event.getType().equals(Type.CHILD_REMOVED)) {
			destroyQueues.putIfAbsent(path.getStreamName(), new LinkedBlockingQueue<PathChildrenCacheEvent>());
			LinkedBlockingQueue<PathChildrenCacheEvent> queue = destroyQueues.get(path.getStreamName());
			queue.put(event);
		}
	}

	public PathChildrenCacheEvent nextCreateEvent(String streamName) {
		try {
			LinkedBlockingQueue<PathChildrenCacheEvent> queue = createQueues.get(streamName);
			return queue != null ? queue.poll(10, TimeUnit.SECONDS) : null;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	public PathChildrenCacheEvent nextDestroyEvent(String streamName) {
		try {
			LinkedBlockingQueue<PathChildrenCacheEvent> queue = destroyQueues.get(streamName);
			return queue != null ? queue.poll(10, TimeUnit.SECONDS) : null;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	public void waitForCreate(String streamName) {
		this.waitForCreateOrDestroyEvent(streamName, true);
	}

	public void waitForDestroy(String streamName) {
		this.waitForCreateOrDestroyEvent(streamName, false);
	}

	private void waitForCreateOrDestroyEvent(String streamName, boolean create) {
		try {
			int attempts = 0;
			PathChildrenCacheEvent event;
			do {
				event = (create) ? this.nextCreateEvent(streamName)
						: this.nextDestroyEvent(streamName);
				Thread.sleep(100);
			}
			while (event == null && ++attempts < 40);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public void waitForDeploy(String streamName) {
		List<String> moduleDeploymentPaths = getModuleDeploymentPaths(streamName);
		long timeout = System.currentTimeMillis() + TIMEOUT;
		do {
			for (ListIterator<String> pathIterator = moduleDeploymentPaths.listIterator(); pathIterator.hasNext();) {
				String path = pathIterator.next();
				try {
					if (client.checkExists().forPath(path) != null) {
						pathIterator.remove();
					}
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				catch (RuntimeException e) {
					throw e;
				}
				catch (Exception e) {
					throw new IllegalStateException(String.format(
							"Failed while waiting for deployment of stream %s.", streamName));
				}
			}
		}
		while (!moduleDeploymentPaths.isEmpty() && System.currentTimeMillis() < timeout);
		if (!moduleDeploymentPaths.isEmpty()) {
			throw new IllegalStateException(String.format("Deployment of stream %s timed out.", streamName));
		}
	}

	public void waitForUndeploy(String streamName) {
		List<String> moduleDeploymentPaths = getModuleDeploymentPaths(streamName);
		Collections.reverse(moduleDeploymentPaths);
		long timeout = System.currentTimeMillis() + TIMEOUT;
		do {
			for (ListIterator<String> pathIterator = moduleDeploymentPaths.listIterator(); pathIterator.hasNext();) {
				String path = pathIterator.next();
				try {
					if (client.checkExists().forPath(path) == null) {
						pathIterator.remove();
					}
					Thread.sleep(100);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				catch (RuntimeException e) {
					throw e;
				}
				catch (Exception e) {
					throw new IllegalStateException(String.format(
							"Failed while waiting for undeployment of stream %s.", streamName));
				}
			}
		}
		while (!moduleDeploymentPaths.isEmpty() && System.currentTimeMillis() < timeout);
		if (!moduleDeploymentPaths.isEmpty()) {
			throw new IllegalStateException(String.format("Undeployment of stream %s timed out.", streamName));
		}
	}

	private List<String> getModuleDeploymentPaths(String streamName) {
		List<String> moduleDeploymentPaths = new ArrayList<String>();
		try {
			Stream stream = streamFactory.createStream(streamName, streamProperties.get(streamName));
			for (Iterator<ModuleDescriptor> iterator = stream.getDeploymentOrderIterator(); iterator.hasNext();) {
				ModuleDescriptor descriptor = iterator.next();
				moduleDeploymentPaths.add(new StreamsPath()
						.setStreamName(stream.getName())
						.setModuleType(descriptor.getModuleDefinition().getType().toString())
						.setModuleLabel(descriptor.getLabel())
						.build());
			}
		}
		catch (Exception e) {
			throw new IllegalStateException(
					String.format("Failed to determine module deployment paths for stream %s", streamName));
		}
		return moduleDeploymentPaths;
	}

}
