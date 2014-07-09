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

import org.apache.commons.collections.CollectionUtils;
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
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.JobDefinitionRepository;
import org.springframework.xd.dirt.util.PagingUtility;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;

/**
 * A Zookeeper backed repository for persisting {@link JobDefinition}s.
 * 
 * @author Mark Fisher
 * @author David Turanski
 */
// todo: the JobDefinitionRepository abstraction can be removed once we are fully zk-enabled since we do not need to
// support multiple impls at that point
public class ZooKeeperJobDefinitionRepository implements JobDefinitionRepository, InitializingBean {

	private final Logger logger = LoggerFactory.getLogger(ZooKeeperJobDefinitionRepository.class);

	private final ZooKeeperConnection zkConnection;

	private final PagingUtility<JobDefinition> pagingUtility = new PagingUtility<JobDefinition>();

	private final RepositoryConnectionListener connectionListener = new RepositoryConnectionListener();

	@Autowired
	public ZooKeeperJobDefinitionRepository(ZooKeeperConnection zkConnection) {
		this.zkConnection = zkConnection;
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
	public Iterable<JobDefinition> findAll(Sort sort) {
		// todo: this ignores the Sort
		List<JobDefinition> all = findAll();
		Collections.sort(all);
		return all;
	}

	@Override
	public Page<JobDefinition> findAll(Pageable pageable) {
		return pagingUtility.getPagedData(pageable, findAll());
	}

	@Override
	public <S extends JobDefinition> Iterable<S> save(Iterable<S> entities) {
		List<S> results = new ArrayList<S>();
		for (S entity : entities) {
			results.add(this.save(entity));
		}
		return results;
	}

	@Override
	public <S extends JobDefinition> S save(S entity) {
		try {
			Map<String, String> map = new HashMap<String, String>();
			map.put("definition", entity.getDefinition());

			CuratorFramework client = zkConnection.getClient();
			String path = Paths.build(Paths.JOBS, entity.getName());
			byte[] binary = ZooKeeperUtils.mapToBytes(map);

			BackgroundPathAndBytesable<?> op = client.checkExists().forPath(path) == null
					? client.create() : client.setData();

			op.forPath(path, binary);

			logger.info("Saved job {} with properties {}", path, map);
		}

		catch (Exception e) {
			//NodeExistsException indicates that we tried to create the
			// path just after another thread/jvm successfully created it
			ZooKeeperUtils.wrapAndThrowIgnoring(e, NodeExistsException.class);
		}
		return entity;
	}

	@Override
	public JobDefinition findOne(String id) {
		try {
			byte[] bytes = zkConnection.getClient().getData().forPath(Paths.build(Paths.JOBS, id));
			if (bytes == null) {
				return null;
			}
			Map<String, String> map = ZooKeeperUtils.bytesToMap(bytes);
			return new JobDefinition(id, map.get("definition"));
		}
		catch (Exception e) {
			ZooKeeperUtils.wrapAndThrowIgnoring(e, NoNodeException.class);
		}
		return null;
	}

	@Override
	public boolean exists(String id) {
		try {
			return (null != zkConnection.getClient().checkExists().forPath(Paths.build(Paths.JOBS, id)));
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	@Override
	public List<JobDefinition> findAll() {
		try {
			return this.findAll(zkConnection.getClient().getChildren().forPath(Paths.JOBS));
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	@Override
	public List<JobDefinition> findAll(Iterable<String> ids) {
		List<JobDefinition> results = new ArrayList<JobDefinition>();
		for (String id : ids) {
			results.add(this.findOne(id));
		}
		return results;
	}

	@Override
	public long count() {
		try {
			Stat stat = zkConnection.getClient().checkExists().forPath(Paths.JOBS);
			return stat == null ? 0 : stat.getNumChildren();
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	@Override
	public void delete(String id) {
		try {
			zkConnection.getClient().delete().deletingChildrenIfNeeded().forPath(Paths.build(Paths.JOBS, id));
		}
		catch (Exception e) {
			//NoNodeException - nothing to delete
			ZooKeeperUtils.wrapAndThrowIgnoring(e, NoNodeException.class);
		}
	}

	@Override
	public void delete(JobDefinition entity) {
		this.delete(entity.getName());
	}

	@Override
	public void delete(Iterable<? extends JobDefinition> entities) {
		for (JobDefinition JobDefinition : entities) {
			this.delete(JobDefinition);
		}
	}

	@Override
	public void deleteAll() {
		try {
			delete(findAll());
		}
		catch (Exception e) {
			//NoNodeException - no top level node, ignore
			ZooKeeperUtils.wrapAndThrowIgnoring(e, NoNodeException.class);
		}
	}

	@Override
	public Iterable<JobDefinition> findAllInRange(String from, boolean fromInclusive, String to, boolean toInclusive) {
		List<JobDefinition> all = findAll();
		if (CollectionUtils.isEmpty(all)) {
			return Collections.emptyList();
		}
		Collections.sort(all);

		List<JobDefinition> results = new ArrayList<JobDefinition>();
		for (JobDefinition definition : all) {
			if (definition.getName().compareTo(to) > 1) {
				break;
			}
			if (definition.getName().compareTo(from) < 0) {
				continue;
			}
			results.add(definition);
		}
		return results;
	}

}
