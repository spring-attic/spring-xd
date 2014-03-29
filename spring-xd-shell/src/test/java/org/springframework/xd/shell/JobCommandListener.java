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

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.zookeeper.data.Stat;

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

	private volatile CuratorFramework client;

	@Override
	public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
		this.client = client;
		JobsPath path = new JobsPath(event.getData().getPath());
		System.out.println("**************** job name:" + path.getJobName() + " event " + event.getType());
	}

	public void waitForCreate(String jobName) {
		this.waitForCreateOrDestroy(jobName, true);
	}

	public void waitForDestroy(String jobName) {
		this.waitForCreateOrDestroy(jobName, false);
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

	private void waitForCreateOrDestroy(String jobName, boolean create) {
		long timeout = System.currentTimeMillis() + TIMEOUT;
		do {
			try {
				boolean exists = exists(jobName);
				if ((create && exists) || (!create && !exists)) {
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
						"Failed while waiting for %s of job %s.", (create ? "creation" : "destruction"), jobName));
			}
		}
		while (System.currentTimeMillis() < timeout);
		throw new IllegalStateException(String.format("Undeployment of job %s timed out.", jobName));
	}

	private boolean exists(String jobName) {
		String path = Paths.build(Paths.JOBS, jobName);
		try {
			if (client.checkExists().forPath(path) != null) {
				return true;
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

	private boolean hasDeployment(String jobName) {
		String parentPath = Paths.build(Paths.JOBS, jobName);
		try {
			Stat stat = client.checkExists().forPath(parentPath);
			if (stat != null) {
				int children = stat.getNumChildren();
				if (children > 0) {
					Assert.state(children == 1, "expected only one child for job: " + jobName);
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
