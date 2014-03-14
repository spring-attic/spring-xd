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

import org.apache.zookeeper.data.Stat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.xd.dirt.util.MapBytesUtility;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;

/**
 * ZooKeeper backed repository for runtime info about Containers.
 * 
 * @author Mark Fisher
 */
public class ZooKeeperRuntimeContainerInfoRepository implements RuntimeContainerInfoRepository {

	private final ZooKeeperConnection zkConnection;

	private final MapBytesUtility mapBytesUtility = new MapBytesUtility();

	@Autowired
	public ZooKeeperRuntimeContainerInfoRepository(ZooKeeperConnection zkConnection) {
		this.zkConnection = zkConnection;
	}

	@Override
	public Iterable<RuntimeContainerInfoEntity> findAll(Sort sort) {
		// todo: add support for sort
		return findAll();
	}

	@Override
	public Page<RuntimeContainerInfoEntity> findAll(Pageable pageable) {
		// todo: add support for paging
		return new PageImpl<RuntimeContainerInfoEntity>(findAll());
	}

	@Override
	public <S extends RuntimeContainerInfoEntity> S save(S entity) {
		// Container metadata is "saved" when each Container registers itself with ZK
		return entity;
	}

	@Override
	public <S extends RuntimeContainerInfoEntity> Iterable<S> save(Iterable<S> entities) {
		// Container metadata is "saved" when each Container registers itself with ZK
		return entities;
	}

	@Override
	public RuntimeContainerInfoEntity findOne(String id) {
		try {
			byte[] data = zkConnection.getClient().getData().forPath(path(id));
			if (data != null) {
				Map<String, String> map = mapBytesUtility.toMap(data);
				return new RuntimeContainerInfoEntity(id,
						map.get("pid") + "@" + map.get("host"),
						map.get("host"),
						map.get("ip"));
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	@Override
	public boolean exists(String id) {
		try {
			return null != zkConnection.getClient().checkExists().forPath(path(id));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<RuntimeContainerInfoEntity> findAll() {
		List<RuntimeContainerInfoEntity> results = new ArrayList<RuntimeContainerInfoEntity>();
		try {
			List<String> children = zkConnection.getClient().getChildren().forPath(Paths.CONTAINERS);
			for (String id : children) {
				results.add(findOne(id));
			}
			return results;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Iterable<RuntimeContainerInfoEntity> findAll(Iterable<String> ids) {
		List<RuntimeContainerInfoEntity> results = new ArrayList<RuntimeContainerInfoEntity>();
		for (String id : ids) {
			RuntimeContainerInfoEntity entity = findOne(id);
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
			throw new RuntimeException(e);
		}
	}

	@Override
	public void delete(String id) {
		// Container metadata is "deleted" when a Container departs
	}

	@Override
	public void delete(RuntimeContainerInfoEntity entity) {
		// Container metadata is "deleted" when a Container departs
	}

	@Override
	public void delete(Iterable<? extends RuntimeContainerInfoEntity> entities) {
		// Container metadata is "deleted" when a Container departs
	}

	@Override
	public void deleteAll() {
		// Container metadata is "deleted" when a Container departs
	}

	@Override
	public Iterable<RuntimeContainerInfoEntity> findAllInRange(String from, boolean fromInclusive, String to,
			boolean toInclusive) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	private String path(String id) {
		return Paths.build(Paths.CONTAINERS, id);
	}

}
