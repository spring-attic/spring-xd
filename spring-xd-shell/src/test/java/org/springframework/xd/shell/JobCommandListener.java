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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;

import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.JobsPath;
import org.springframework.xd.dirt.zookeeper.Paths;

/**
 * A {@link PathChildrenCacheListener} that enables waiting for a job to be created, deployed, undeployed or destroyed.
 * 
 * @author David Turanski
 * @author Mark Fisher
 */
public class JobCommandListener implements PathChildrenCacheListener {

	private static int TIMEOUT = 5000;

	private ConcurrentMap<String, LinkedBlockingQueue<PathChildrenCacheEvent>> createQueues = new ConcurrentHashMap<String, LinkedBlockingQueue<PathChildrenCacheEvent>>();

	private ConcurrentMap<String, LinkedBlockingQueue<PathChildrenCacheEvent>> destroyQueues = new ConcurrentHashMap<String, LinkedBlockingQueue<PathChildrenCacheEvent>>();

	private volatile CuratorFramework client;

	@Override
	public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
		this.client = client;
		JobsPath path = new JobsPath(event.getData().getPath());
		System.out.println("**************** job name:" + path.getJobName() + " event " + event.getType());
		if (event.getType().equals(Type.CHILD_ADDED)) {
			createQueues.putIfAbsent(path.getJobName(), new LinkedBlockingQueue<PathChildrenCacheEvent>());
			LinkedBlockingQueue<PathChildrenCacheEvent> queue = createQueues.get(path.getJobName());
			queue.put(event);
		}
		else if (event.getType().equals(Type.CHILD_REMOVED)) {
			destroyQueues.putIfAbsent(path.getJobName(), new LinkedBlockingQueue<PathChildrenCacheEvent>());
			LinkedBlockingQueue<PathChildrenCacheEvent> queue = destroyQueues.get(path.getJobName());
			queue.put(event);
		}
	}

	public PathChildrenCacheEvent nextCreateEvent(String jobName) {
		try {
			LinkedBlockingQueue<PathChildrenCacheEvent> queue = createQueues.get(jobName);
			return queue != null ? queue.poll(10, TimeUnit.SECONDS) : null;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	public PathChildrenCacheEvent nextDestroyEvent(String jobName) {
		try {
			LinkedBlockingQueue<PathChildrenCacheEvent> queue = destroyQueues.get(jobName);
			return queue != null ? queue.poll(10, TimeUnit.SECONDS) : null;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	public void waitForCreate(String jobName) {
		this.waitForCreateOrDestroyEvent(jobName, true);
	}

	public void waitForDestroy(String jobName) {
		this.waitForCreateOrDestroyEvent(jobName, false);
	}

	private void waitForCreateOrDestroyEvent(String jobName, boolean create) {
		try {
			int attempts = 0;
			PathChildrenCacheEvent event;
			do {
				event = (create) ? this.nextCreateEvent(jobName)
						: this.nextDestroyEvent(jobName);
				Thread.sleep(100);
			}
			while (event == null && ++attempts < 40);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public void waitForDeploy(String jobName) {
		long timeout = System.currentTimeMillis() + TIMEOUT;
		do {
			try {
				if (hasDeployment(jobName)) {
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
						"Failed while waiting for deployment of job %s.", jobName));
			}
		}
		while (System.currentTimeMillis() < timeout);
		throw new IllegalStateException(String.format("Deployment of job %s timed out.", jobName));
	}

	public void waitForUndeploy(String jobName) {
		long timeout = System.currentTimeMillis() + TIMEOUT;
		do {
			try {
				if (!hasDeployment(jobName)) {
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
						"Failed while waiting for undeployment of job %s.", jobName));
			}
		}
		while (System.currentTimeMillis() < timeout);
		throw new IllegalStateException(String.format("Undeployment of job %s timed out.", jobName));
	}

	private boolean hasDeployment(String jobName) {
		String parentPath = Paths.build(Paths.JOBS, jobName);
		try {
			if (client.checkExists().forPath(parentPath) != null) {
				List<String> children = client.getChildren().forPath(parentPath);
				if (children.size() > 0) {
					Assert.state(children.size() == 1, "expected only one child for job: " + jobName);
					return true;
				}
			}
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return false;
	}

}
