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

package org.springframework.xd.dirt.stream.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;

import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnectionListener;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;

/**
 * ZooKeeper connection listener that ensures path creation for
 * nodes required by the various repository implementations that
 * use ZooKeeper.
 *
 * @author Patrick Peralta
 * @author David Turanski
 */
public class RepositoryConnectionListener implements ZooKeeperConnectionListener {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onConnect(CuratorFramework client) {
		ensurePath(client, Paths.STREAMS);
		ensurePath(client, Paths.JOBS);
		ensurePath(client, Paths.STREAM_DEPLOYMENTS);
		ensurePath(client, Paths.JOB_DEPLOYMENTS);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onResume(CuratorFramework client) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDisconnect(CuratorFramework client) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onSuspend(CuratorFramework client) {
	}

	/**
	 * Ensure that the given path is created if it doesn't already
	 * exist.
	 *
	 * @param client  curator framework client
	 * @param path    path to create
	 */
	private void ensurePath(CuratorFramework client, String path) {
		try {
			client.create().creatingParentsIfNeeded().forPath(path);
		}
		catch (Exception e) {
			//NodeExistsException -already created
			ZooKeeperUtils.wrapAndThrowIgnoring(e, KeeperException.NodeExistsException.class);
		}
	}

}
