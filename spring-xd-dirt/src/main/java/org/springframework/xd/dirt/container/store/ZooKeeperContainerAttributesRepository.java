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
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.xd.dirt.container.ContainerAttributes;
import org.springframework.xd.dirt.util.MapBytesUtility;
import org.springframework.xd.dirt.util.PagingUtility;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;

/**
 * ZooKeeper backed repository for runtime info about Containers.
 *
 * @author Mark Fisher
 * @author David Turanski
 */
public class ZooKeeperContainerAttributesRepository implements ContainerAttributesRepository {


	private final ZooKeeperConnection zkConnection;

	private final MapBytesUtility mapBytesUtility = new MapBytesUtility();

	private final PagingUtility<ContainerAttributes> pagingUtility = new PagingUtility<ContainerAttributes>();

	@Autowired
	public ZooKeeperContainerAttributesRepository(ZooKeeperConnection zkConnection) {
		this.zkConnection = zkConnection;
	}

	@Override
	public Iterable<ContainerAttributes> findAll(Sort sort) {
		// todo: add support for sort
		return findAll();
	}

	@Override
	public Page<ContainerAttributes> findAll(Pageable pageable) {
		return pagingUtility.getPagedData(pageable, findAll());
	}

	@Override
	public <S extends ContainerAttributes> S save(S entity) {
		CuratorFramework client = zkConnection.getClient();
		String path = Paths.build(Paths.CONTAINERS, entity.getId());

		try {
			if (client.checkExists().forPath(path) != null) {
				// if this container disconnected from ZooKeeper
				// and reconnects, the ephemeral node may not
				// have been cleaned up yet...this can happen
				// in cases where the machine running both the
				// container and ZooKeeper goes to sleep
				client.delete().forPath(path);
			}
		}
		catch (Exception e) {
			// trapping the case where the ephemeral node exists
			// but is removed by ZK before this container gets
			// the chance to remove it
			ZooKeeperUtils.wrapAndThrowIgnoring(e, KeeperException.NoNodeException.class);
		}

		try {
			client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL)
					.forPath(path, mapBytesUtility.toByteArray(entity));
			return entity;
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	@Override
	public <S extends ContainerAttributes> Iterable<S> save(Iterable<S> entities) {
		List<S> results = new ArrayList<S>();
		for (S entity : entities) {
			results.add(save(entity));
		}
		return results;
	}

	@Override
	public ContainerAttributes findOne(String id) {
		ContainerAttributes containerAttributes = null;
		try {
			byte[] data = zkConnection.getClient().getData().forPath(path(id));
			if (data != null) {
				Map<String, String> map = mapBytesUtility.toMap(data);
				containerAttributes = new ContainerAttributes(map);
			}
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
		return containerAttributes;
	}

	@Override
	public boolean exists(String id) {
		try {
			return null != zkConnection.getClient().checkExists().forPath(path(id));
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	@Override
	public List<ContainerAttributes> findAll() {
		List<ContainerAttributes> results = new ArrayList<ContainerAttributes>();
		try {
			List<String> children = zkConnection.getClient().getChildren().forPath(Paths.CONTAINERS);
			for (String id : children) {
				results.add(findOne(id));
			}
			return results;
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	@Override
	public Iterable<ContainerAttributes> findAll(Iterable<String> ids) {
		List<ContainerAttributes> results = new ArrayList<ContainerAttributes>();
		for (String id : ids) {
			ContainerAttributes entity = findOne(id);
			if (entity != null) {
				results.add(entity);
			}
		}
		return results;
	}

	@Override
	public long count() {
		try {
			Stat stat = zkConnection.getClient().checkExists().forPath(Paths.CONTAINERS);
			return (stat != null) ? stat.getNumChildren() : 0;
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	@Override
	public void delete(String id) {
		// Container metadata is "deleted" when a Container departs
	}

	@Override
	public void delete(ContainerAttributes entity) {
		// Container metadata is "deleted" when a Container departs
	}

	@Override
	public void delete(Iterable<? extends ContainerAttributes> entities) {
		// Container metadata is "deleted" when a Container departs
	}

	@Override
	public void deleteAll() {
		// Container metadata is "deleted" when a Container departs
	}

	@Override
	public Iterable<ContainerAttributes> findAllInRange(String from, boolean fromInclusive, String to,
			boolean toInclusive) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	private String path(String id) {
		return Paths.build(Paths.CONTAINERS, id);
	}

}
