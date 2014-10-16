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

package org.springframework.xd.dirt.container.store;

import java.util.ArrayList;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.xd.dirt.cluster.Admin;
import org.springframework.xd.dirt.cluster.NoSuchContainerException;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;

/**
 * ZooKeeper backed repository for runtime info about Admins.
 *
 * @author Janne Valkealahti
 */
public class ZooKeeperAdminRepository implements AdminRepository {

	/**
	 * ZooKeeper connection.
	 */
	private final ZooKeeperConnection zkConnection;

	/**
	 * Construct a {@code ZooKeeperRuntimeRepository}.
	 *
	 * @param zkConnection the ZooKeeper connection
	 */
	@Autowired
	public ZooKeeperAdminRepository(ZooKeeperConnection zkConnection) {
		this.zkConnection = zkConnection;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Admin save(Admin entity) {
		CuratorFramework client = zkConnection.getClient();
		String path = Paths.build(Paths.ADMINS, entity.getName());

		try {
			client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
					.forPath(path, ZooKeeperUtils.mapToBytes(entity.getAttributes()));
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
		return entity;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void update(Admin entity) {
		CuratorFramework client = zkConnection.getClient();
		String path = Paths.build(Paths.ADMINS, entity.getName());

		try {
			Stat stat = client.checkExists().forPath(path);
			if (stat == null) {
				throw new NoSuchContainerException("Could not find admin with id " + entity.getName());
			}
			client.setData().forPath(path, ZooKeeperUtils.mapToBytes(entity.getAttributes()));
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e, e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean exists(String id) {
		try {
			return null != zkConnection.getClient().checkExists()
					.forPath(path(id));
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Admin> findAll() {
		List<Admin> results = new ArrayList<Admin>();
		try {
			List<String> children = zkConnection.getClient().getChildren().forPath(Paths.build(Paths.ADMINS));
			for (String child : children) {
				byte[] data = zkConnection.getClient().getData().forPath(
						Paths.build(Paths.ADMINS, child));
				if (data != null && data.length > 0) {
					results.add(new Admin(child, ZooKeeperUtils.bytesToMap(data)));
				}
			}

		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
		return results;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Admin findOne(String id) {
		try {
			byte[] data = zkConnection.getClient().getData().forPath(
					Paths.build(Paths.ADMINS, id));
			if (data != null && data.length > 0) {
				return new Admin(id, ZooKeeperUtils.bytesToMap(data));
			}
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
		return null;
	}

	/**
	 * Return the path for a admin runtime.
	 *
	 * @param id container runtime id
	 * @return path for the container
	 * @see Paths#build
	 */
	private String path(String id) {
		return Paths.build(Paths.ADMINS, id);
	}

}
