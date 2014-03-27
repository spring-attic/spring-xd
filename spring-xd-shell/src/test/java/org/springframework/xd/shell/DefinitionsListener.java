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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;

import org.springframework.xd.dirt.core.StreamsPath;


/**
 * 
 * @author David Turanski
 */
public class DefinitionsListener implements PathChildrenCacheListener {

	private ConcurrentMap<String, LinkedBlockingQueue<PathChildrenCacheEvent>> createQueues = new ConcurrentHashMap<String, LinkedBlockingQueue<PathChildrenCacheEvent>>();

	private ConcurrentMap<String, LinkedBlockingQueue<PathChildrenCacheEvent>> destroyQueues = new ConcurrentHashMap<String, LinkedBlockingQueue<PathChildrenCacheEvent>>();

	@Override
	public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
		StreamsPath path = new StreamsPath(event.getData().getPath());
		System.out.println("**************** stream name:" + path.getStreamName() + " event " + event.getType());
		if (event.getType().equals(Type.CHILD_ADDED)) {
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

	public void waitForCreateOrDestroyEvent(String streamOrJobName, boolean create) {
		try {
			int attempts = 0;
			PathChildrenCacheEvent event;
			do {
				event = (create) ? this.nextCreateEvent(streamOrJobName)
						: this.nextDestroyEvent(streamOrJobName);
				Thread.sleep(100);
			}
			while (event == null && ++attempts < 40);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
