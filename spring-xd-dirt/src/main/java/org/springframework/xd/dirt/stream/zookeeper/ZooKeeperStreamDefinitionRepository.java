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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.xd.dirt.module.ModuleDependencyRepository;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamDefinitionRepositoryUtils;
import org.springframework.xd.dirt.util.PagingUtility;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.ModuleDefinition;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * @author Mark Fisher
 */
// todo: the StreamDefinitionRepository abstraction can be removed once we are fully zk-enabled since we do not need to
// support multiple impls at that point
public class ZooKeeperStreamDefinitionRepository implements StreamDefinitionRepository, InitializingBean {

	/**
	 * The key used in serialized properties to hold the raw definition of a stream.
	 */
	private static final String DEFINITION_KEY = "definition";

	/**
	 * The key used in serialized properties to hold the parsed module definitions that make up a stream.
	 */
	private static final String MODULE_DEFINITIONS_KEY = "moduleDefinitions";

	private final static TypeReference<List<ModuleDefinition>> MODULE_DEFINITIONS_LIST = new TypeReference<List<ModuleDefinition>>() {};

	private final Logger logger = LoggerFactory.getLogger(ZooKeeperStreamDefinitionRepository.class);

	private final ZooKeeperConnection zkConnection;

	private final ModuleDependencyRepository moduleDependencyRepository;

	private final PagingUtility<StreamDefinition> pagingUtility = new PagingUtility<StreamDefinition>();

	private final RepositoryConnectionListener connectionListener = new RepositoryConnectionListener();

	private final ObjectWriter objectWriter = new ObjectMapper().writerWithType(MODULE_DEFINITIONS_LIST);

	private final ObjectReader objectReader = new ObjectMapper().reader(MODULE_DEFINITIONS_LIST);

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
		return pagingUtility.getPagedData(pageable, findAll());
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
			Map<String, String> map = new HashMap<>();
			map.put(DEFINITION_KEY, entity.getDefinition());

			map.put(MODULE_DEFINITIONS_KEY, objectWriter.writeValueAsString(entity.getModuleDefinitions()));

			CuratorFramework client = zkConnection.getClient();
			String path = Paths.build(Paths.STREAMS, entity.getName());
			byte[] binary = ZooKeeperUtils.mapToBytes(map);

			BackgroundPathAndBytesable<?> op = client.checkExists().forPath(path) == null
					? client.create() : client.setData();

			op.forPath(path, binary);

			logger.trace("Saved stream {} with properties {}", path, map);

			StreamDefinitionRepositoryUtils.saveDependencies(moduleDependencyRepository, entity);
		}
		catch (Exception e) {
			// NodeExistsException indicates that we tried to create the
			// path just after another thread/jvm successfully created it 
			ZooKeeperUtils.wrapAndThrowIgnoring(e, NodeExistsException.class);
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
			Map<String, String> map = ZooKeeperUtils.bytesToMap(bytes);
			StreamDefinition streamDefinition = new StreamDefinition(id, map.get(DEFINITION_KEY));
			if (map.get(MODULE_DEFINITIONS_KEY) != null) {
				List<ModuleDefinition> moduleDefinitions = objectReader.readValue(map.get(MODULE_DEFINITIONS_KEY));
				streamDefinition.setModuleDefinitions(moduleDefinitions);
			}
			return streamDefinition;
		}
		catch (Exception e) {
			//NoNodeException - the definition does not exist
			ZooKeeperUtils.wrapAndThrowIgnoring(e, NoNodeException.class);
		}
		return null;
	}

	@Override
	public boolean exists(String id) {
		try {
			return (null != zkConnection.getClient().checkExists().forPath(Paths.build(Paths.STREAMS, id)));
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	@Override
	public List<StreamDefinition> findAll() {
		try {
			return this.findAll(zkConnection.getClient().getChildren().forPath(Paths.STREAMS));
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
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
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	@Override
	public void delete(String id) {
		logger.trace("Deleting stream {}", id);
		String path = Paths.build(Paths.STREAMS, id);
		try {
			zkConnection.getClient().delete().deletingChildrenIfNeeded().forPath(path);
		}
		catch (Exception e) {
			//NoNodeException - nothing to delete
			ZooKeeperUtils.wrapAndThrowIgnoring(e, NoNodeException.class);
		}
	}

	@Override
	public void delete(StreamDefinition entity) {
		StreamDefinitionRepositoryUtils.deleteDependencies(moduleDependencyRepository, entity);
		this.delete(entity.getName());
	}

	@Override
	public void delete(Iterable<? extends StreamDefinition> entities) {
		for (StreamDefinition streamDefinition : entities) {
			this.delete(streamDefinition);
		}
	}

	@Override
	public void deleteAll() {
		try {
			delete(findAll());
		}
		catch (Exception e) {
			//NoNodeException - nothing to delete
			ZooKeeperUtils.wrapAndThrowIgnoring(e, NoNodeException.class);
		}
	}

	@Override
	public Iterable<StreamDefinition> findAllInRange(String from, boolean fromInclusive, String to, boolean toInclusive) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
