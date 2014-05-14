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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.core.StreamDeploymentsPath;
import org.springframework.xd.dirt.stream.Stream;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.StreamRepository;
import org.springframework.xd.dirt.util.MapBytesUtility;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.ModuleType;

/**
 * Stream instance repository. It should only return values for Streams that are deployed.
 *
 * @author Mark Fisher
 */
// todo: the StreamRepository abstraction can be removed once we are fully zk-enabled since we do not need to
// support multiple impls at that point
public class ZooKeeperStreamRepository implements StreamRepository, InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(ZooKeeperStreamRepository.class);

	private final ZooKeeperConnection zkConnection;

	private final MapBytesUtility mapBytesUtility = new MapBytesUtility();

	private final RepositoryConnectionListener connectionListener = new RepositoryConnectionListener();

	@Autowired
	public ZooKeeperStreamRepository(ZooKeeperConnection zkConnection) {
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
	public Iterable<Stream> findAll(Sort sort) {
		// todo: implement sort support
		return findAll();
	}

	@Override
	public Page<Stream> findAll(Pageable pageable) {
		List<Stream> all = findAll();
		if (CollectionUtils.isEmpty(all)) {
			return new PageImpl<Stream>(all);
		}
		Collections.sort(all);

		int offSet = pageable.getOffset();
		int size = pageable.getPageSize();

		List<Stream> page = new ArrayList<Stream>();
		for (int i = offSet; i < Math.min(all.size(), offSet + size); i++) {
			page.add(all.get(i));
		}

		return new PageImpl<Stream>(page, pageable, all.size());
	}

	@Override
	public <S extends Stream> S save(S entity) {
		// stream instances are "saved" when a StreamDeploymentListener deploys a stream
		return entity;
	}

	@Override
	public <S extends Stream> Iterable<S> save(Iterable<S> entities) {
		// stream instances are "saved" when a StreamDeploymentListener deploys a stream
		return entities;
	}

	@Override
	public Stream findOne(String id) {
		CuratorFramework client = zkConnection.getClient();
		String path = Paths.build(Paths.STREAMS, id);
		try {
			Stat definitionStat = client.checkExists().forPath(path);
			if (definitionStat != null) {
				byte[] data = client.getData().forPath(path);
				Map<String, String> map = mapBytesUtility.toMap(data);
				Stream stream = new Stream(new StreamDefinition(id, map.get("definition")));

				Stat deployStat = client.checkExists().forPath(Paths.build(Paths.STREAM_DEPLOYMENTS, id));
				if (deployStat != null) {
					stream.setStartedAt(new Date(deployStat.getCtime()));
					return stream;
				}
			}
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
		return null;
	}

	@Override
	public boolean exists(String id) {
		return null != findOne(id);
	}

	@Override
	public List<Stream> findAll() {
		try {
			return findAll(zkConnection.getClient().getChildren().forPath(Paths.STREAM_DEPLOYMENTS));
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	@Override
	public List<Stream> findAll(Iterable<String> ids) {
		List<Stream> results = new ArrayList<Stream>();
		try {
			for (String stream : ids) {
				Stream s = findOne(stream);
				if (s != null) {
					results.add(s);
				}
			}
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
		return results;
	}

	@Override
	public long count() {
		try {
			Stat stat = zkConnection.getClient().checkExists().forPath(Paths.STREAM_DEPLOYMENTS);
			return stat != null ? stat.getNumChildren() : 0;
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	@Override
	public void delete(String id) {
		logger.info("Undeploying stream {}", id);

		CuratorFramework client = zkConnection.getClient();
		Deque<String> paths = new ArrayDeque<String>();

		// If this stream contains processor modules, place them into
		// a tree keyed by the ZK transaction id. The ZK transaction
		// id maintains total ordering of all changes. This allows
		// the undeployment of processor modules in the reverse order in
		// which they were deployed.
		Map<Long, String> txMap = new TreeMap<Long, String>();
		try {
			List<String> processorDeployments = client.getChildren().forPath(new StreamDeploymentsPath()
					.setStreamName(id).setModuleType(ModuleType.processor.toString()).build());
			for (String processorName : processorDeployments) {
				String path = new StreamDeploymentsPath()
						.setStreamName(id)
						.setModuleType(ModuleType.processor.toString())
						.setModuleLabel(processorName).build();
				Stat stat = client.checkExists().forPath(path);
				Assert.notNull(stat);
				txMap.put(stat.getCzxid(), path);
			}
		}
		catch (Exception e) {
			//NoNodeException - nothing to delete
			ZooKeeperUtils.wrapAndThrowIgnoring(e, KeeperException.NoNodeException.class);
		}

		paths.add(new StreamDeploymentsPath().setStreamName(id).setModuleType(ModuleType.sink.toString()).build());
		paths.add(new StreamDeploymentsPath().setStreamName(id).setModuleType(ModuleType.processor.toString()).build());
		for (String processorDeployment : txMap.values()) {
			paths.add(processorDeployment);
		}
		paths.add(new StreamDeploymentsPath().setStreamName(id).setModuleType(ModuleType.source.toString()).build());

		for (Iterator<String> iterator = paths.descendingIterator(); iterator.hasNext();) {
			try {
				String path = iterator.next();
				logger.trace("removing path {}", path);
				client.delete().deletingChildrenIfNeeded().forPath(path);
			}
			catch (Exception e) {
				ZooKeeperUtils.wrapAndThrowIgnoring(e, KeeperException.NoNodeException.class);
			}
		}

		String streamDeploymentPath = Paths.build(Paths.STREAM_DEPLOYMENTS, id);
		try {
			client.delete().forPath(streamDeploymentPath);
		}
		catch (KeeperException.NoNodeException e) {
			// ignore
		}
		catch (KeeperException.NotEmptyException e) {
			List<String> children = new ArrayList<String>();
			try {
				children.addAll(client.getChildren().forPath(streamDeploymentPath));
			}
			catch (Exception ex) {
				children.add("Could not load list of children due to " + ex);
			}
			throw new IllegalStateException(String.format(
					"The following children were not deleted from %s: %s", streamDeploymentPath, children), e);
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	@Override
	public void delete(Stream entity) {
		Assert.notNull(entity, "stream must not be null");
		delete(entity.getDefinition().getName());
	}

	@Override
	public void delete(Iterable<? extends Stream> entities) {
		for (Stream stream : entities) {
			delete(stream);
		}
	}

	@Override
	public void deleteAll() {
		try {
			List<String> children = zkConnection.getClient().getChildren().forPath(Paths.STREAM_DEPLOYMENTS);
			for (String child : children) {
				delete(child);
			}
		}
		catch (Exception e) {
			//NoNodeException - nothing to delete
			ZooKeeperUtils.wrapAndThrowIgnoring(e, KeeperException.NoNodeException.class);
		}
	}

	@Override
	public Iterable<Stream> findAllInRange(String from, boolean fromInclusive, String to, boolean toInclusive) {
		List<Stream> all = findAll();
		if (CollectionUtils.isEmpty(all)) {
			return Collections.emptyList();
		}
		Collections.sort(all);

		List<Stream> results = new ArrayList<Stream>();
		for (Stream stream : all) {
			if (stream.getDefinition().getName().compareTo(to) > 1) {
				break;
			}
			if (stream.getDefinition().getName().compareTo(from) < 0) {
				continue;
			}
			results.add(stream);
		}
		return results;
	}

}
