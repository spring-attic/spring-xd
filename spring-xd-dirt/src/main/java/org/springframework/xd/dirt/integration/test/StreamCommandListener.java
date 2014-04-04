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

package org.springframework.xd.dirt.integration.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.xd.dirt.core.ModuleDescriptor;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.dirt.core.StreamsPath;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamFactory;
import org.springframework.xd.dirt.util.MapBytesUtility;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.module.options.ModuleOptionsMetadataResolver;

/**
 * A {@link PathChildrenCacheListener} that enables waiting for a stream to be created, deployed, undeployed or
 * destroyed.
 *
 * @author David Turanski
 * @author Mark Fisher
 */
public class StreamCommandListener implements PathChildrenCacheListener {

	private final static Logger log = LoggerFactory.getLogger(StreamCommandListener.class);

	private static int TIMEOUT = 5000;

	private ConcurrentMap<String, SettableFuture<Map<String, String>>> streamProperties =
			new ConcurrentHashMap<String, SettableFuture<Map<String, String>>>();

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
		String streamName = new StreamsPath(event.getData().getPath()).getStreamName();
		log.info("event: {} stream: {}", streamName, event.getType());
		if (event.getType().equals(Type.CHILD_ADDED)) {
			streamProperties.putIfAbsent(streamName, new SettableFuture<Map<String, String>>());
			streamProperties.get(streamName).set(mapBytesUtility.toMap(event.getData().getData()));
		}
		else if (event.getType().equals(Type.CHILD_REMOVED)) {
			SettableFuture<Map<String, String>> result = streamProperties.remove(streamName);
			if (result != null) {
				result.cancel(true);
			}
		}
	}

	private Map<String, String> getStreamProperties(String streamName, long timeout) {
		this.streamProperties.putIfAbsent(streamName, new SettableFuture<Map<String, String>>());
		try {
			return this.streamProperties.get(streamName).get(timeout, TimeUnit.MILLISECONDS);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
		catch (Exception e) {
			throw new IllegalStateException("failed while waiting for stream properties", e);
		}
	}

	public void waitForCreate(String streamName) {
		this.waitForCreateOrDestroyEvent(streamName, true);
	}

	public void waitForDestroy(String streamName) {
		this.waitForCreateOrDestroyEvent(streamName, false);
	}

	private void waitForCreateOrDestroyEvent(String streamName, boolean create) {
		String path = Paths.build(Paths.STREAMS, streamName);
		try {
			int attempts = 0;
			Stat stat = client.checkExists().forPath(path);

			while (((create && stat == null) || (!create && stat != null)) && ++attempts < TIMEOUT / 100) {
				Thread.sleep(100);
				stat = client.checkExists().forPath(path);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void waitForDeploy(String streamName) {
		List<String> moduleDeploymentPaths = getModuleDeploymentPaths(streamName);
		long timeout = System.currentTimeMillis() + TIMEOUT;
		do {
			for (ListIterator<String> pathIterator = moduleDeploymentPaths.listIterator(); pathIterator.hasNext();) {
				String path = pathIterator.next();
				try {
					Stat stat = client.checkExists().forPath(path);
					if (stat != null && stat.getNumChildren() > 0) {
						pathIterator.remove();
					}
				}
				catch (RuntimeException e) {
					throw e;
				}
				catch (Exception e) {
					throw new IllegalStateException(String.format(
							"Failed while waiting for deployment of stream %s.", streamName), e);
				}
			}
		}
		while (!moduleDeploymentPaths.isEmpty() && System.currentTimeMillis() < timeout);
		if (!moduleDeploymentPaths.isEmpty()) {
			throw new IllegalStateException(String.format("Deployment of stream %s timed out.", streamName));
		}
	}

	public void waitForUndeploy(String streamName) {
		String path = Paths.build(Paths.STREAMS, streamName);
		long timeout = System.currentTimeMillis() + TIMEOUT;
		do {
			try {
				Stat stat = client.checkExists().forPath(path);
				// stat would be null, if the stream was actually destroyed
				// if it has been completely undeployed but not destroyed, it will have 0 children
				if (stat == null || stat.getNumChildren() == 0) {
					return;
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
						"Failed while waiting for undeployment of stream %s.", streamName), e);
			}
		}
		while (System.currentTimeMillis() < timeout);
		throw new IllegalStateException(String.format("Undeployment of stream %s timed out.", streamName));
	}

	private List<String> getModuleDeploymentPaths(String streamName) {
		List<String> moduleDeploymentPaths = new ArrayList<String>();
		try {
			Stream stream = streamFactory.createStream(streamName, getStreamProperties(streamName, 5000));
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
					String.format("Failed to determine module deployment paths for stream %s", streamName), e);
		}
		return moduleDeploymentPaths;
	}

	private static class SettableFuture<V> implements Future<V> {

		private final AtomicReference<V> result = new AtomicReference<V>();

		private final CountDownLatch latch = new CountDownLatch(1);

		private final AtomicBoolean cancelled = new AtomicBoolean();

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			if (isDone()) {
				return false;
			}
			boolean successful = cancelled.compareAndSet(false, true);
			if (successful) {
				try {
					set(null);
				}
				catch (IllegalStateException e) {
					// value already set, no problem
				}
			}
			return successful;
		}

		@Override
		public boolean isCancelled() {
			return cancelled.get();
		}

		@Override
		public boolean isDone() {
			return cancelled.get() || latch.getCount() == 0;
		}

		public void set(V value) {
			if (latch.getCount() == 0) {
				throw new IllegalStateException("result already set");
			}
			result.set(value);
			latch.countDown();
		}

		@Override
		public V get() throws InterruptedException, ExecutionException {
			if (!cancelled.get()) {
				latch.await();
			}
			return result.get();
		}

		@Override
		public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
				TimeoutException {
			if (cancelled.get()) {
				return null;
			}
			if (!latch.await(timeout, unit)) {
				throw new TimeoutException();
			}
			return result.get();
		}
	}

}
