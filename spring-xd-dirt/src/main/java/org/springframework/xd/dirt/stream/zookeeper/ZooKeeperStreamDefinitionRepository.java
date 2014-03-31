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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.BackgroundPathAndBytesable;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.xd.dirt.module.ModuleDependencyRepository;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamDefinitionRepositoryUtils;
import org.springframework.xd.dirt.util.MapBytesUtility;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnectionListener;

/**
 * @author Mark Fisher
 */
// todo: the StreamDefinitionRepository abstraction can be removed once we are fully zk-enabled since we do not need to
// support multiple impls at that point
public class ZooKeeperStreamDefinitionRepository implements StreamDefinitionRepository, InitializingBean {

	private final Logger LOG = LoggerFactory.getLogger(ZooKeeperStreamDefinitionRepository.class);

	private final ZooKeeperConnection zkConnection;

	private final ModuleDependencyRepository moduleDependencyRepository;

	private final MapBytesUtility mapBytesUtility = new MapBytesUtility();

	private final StreamPathEnsuringConnectionListener connectionListener = new StreamPathEnsuringConnectionListener();

	@Autowired
	public ZooKeeperStreamDefinitionRepository(ZooKeeperConnection zkConnection,
			ModuleDependencyRepository moduleDependencyRepository) {
		this.zkConnection = zkConnection;
		this.moduleDependencyRepository = moduleDependencyRepository;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		zkConnection.addListener(connectionListener);
		if (zkConnection.isConnected()) {
			// already connected, invoke the callback directly
			connectionListener.onConnect(zkConnection.getClient());
		}
	}

	@Override
	public Iterable<StreamDefinition> findAll(Sort sort) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Page<StreamDefinition> findAll(Pageable pageable) {
		List<StreamDefinition> all = findAll();
		if (CollectionUtils.isEmpty(all)) {
			return new PageImpl<StreamDefinition>(all);
		}
		Collections.sort(all);

		int offSet = pageable.getOffset();
		int size = pageable.getPageSize();

		List<StreamDefinition> page = new ArrayList<StreamDefinition>();
		for (int i = offSet; i < Math.min(all.size(), offSet + size); i++) {
			page.add(all.get(i));
		}

		return new PageImpl<StreamDefinition>(page, pageable, all.size());
	}

	@Override
	public <S extends StreamDefinition> Iterable<S> save(Iterable<S> entities) {
		List<S> results = new ArrayList<S>();
		for (S entity : entities) {
			results.add(this.save(entity));
		}
		return results;
	}

	@Override
	public <S extends StreamDefinition> S save(S entity) {
		try {
			Map<String, String> map = new HashMap<String, String>();
			map.put("definition", entity.getDefinition());
			map.put("deploy", Boolean.toString(entity.isDeploy()));

			CuratorFramework client = zkConnection.getClient();
			String path = Paths.build(Paths.STREAMS, entity.getName());
			byte[] binary = mapBytesUtility.toByteArray(map);

			BackgroundPathAndBytesable<?> op = client.checkExists().forPath(path) == null
					? client.create() : client.setData();

			op.forPath(path, binary);

			LOG.trace("Saved stream {} with properties {}", path, map);

			StreamDefinitionRepositoryUtils.saveDependencies(moduleDependencyRepository, entity);
		}
		catch (NodeExistsException e) {
			// this exception indicates that we tried to create the
			// path just after another thread/jvm successfully created it
		}
		catch (Exception e) {
			throw e instanceof RuntimeException ? (RuntimeException) e : new RuntimeException(e);
		}
		return entity;
	}

	@Override
	public StreamDefinition findOne(String id) {
		try {
			byte[] bytes = zkConnection.getClient().getData().forPath(Paths.build(Paths.STREAMS, id));
			if (bytes == null) {
				return null;
			}
			Map<String, String> map = this.mapBytesUtility.toMap(bytes);
			return new StreamDefinition(id, map.get("definition"), Boolean.parseBoolean(map.get("deploy")));
		}
		catch (NoNodeException e) {
			return null;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean exists(String id) {
		try {
			return (null != zkConnection.getClient().checkExists().forPath(Paths.build(Paths.STREAMS, id)));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<StreamDefinition> findAll() {
		try {
			return this.findAll(zkConnection.getClient().getChildren().forPath(Paths.STREAMS));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<StreamDefinition> findAll(Iterable<String> ids) {
		List<StreamDefinition> results = new ArrayList<StreamDefinition>();
		for (String id : ids) {
			StreamDefinition sd = this.findOne(id);
			if (sd != null) {
				results.add(sd);
			}
		}
		return results;
	}

	@Override
	public long count() {
		try {
			Stat stat = zkConnection.getClient().checkExists().forPath(Paths.STREAMS);
			return stat == null ? 0 : stat.getNumChildren();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void delete(String id) {
		try {
			zkConnection.getClient().delete().deletingChildrenIfNeeded().forPath(Paths.build(Paths.STREAMS, id));
		}
		catch (NoNodeException e) {
			// ignore
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void delete(StreamDefinition entity) {
		this.delete(entity.getName());
		StreamDefinitionRepositoryUtils.deleteDependencies(moduleDependencyRepository, entity);
	}

	@Override
	public void delete(Iterable<? extends StreamDefinition> entities) {
		for (StreamDefinition streamDefinition : entities) {
			this.delete(streamDefinition);
		}
	}

	@Override
	public void deleteAll() {
		delete(findAll());
	}

	@Override
	public Iterable<StreamDefinition> findAllInRange(String from, boolean fromInclusive, String to, boolean toInclusive) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}


	/**
	 * A {@link ZooKeeperConnectionListener} that ensures the {@code /xd/streams} path exists.
	 */
	private static class StreamPathEnsuringConnectionListener implements ZooKeeperConnectionListener {

		@Override
		public void onDisconnect(CuratorFramework client) {
		}

		@Override
		public void onConnect(CuratorFramework client) {
			try {
				client.create().creatingParentsIfNeeded().forPath(Paths.STREAMS);
			}
			catch (NodeExistsException e) {
				// already created
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

}
