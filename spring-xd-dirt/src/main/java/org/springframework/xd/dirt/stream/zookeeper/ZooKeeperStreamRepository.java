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
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.xd.dirt.core.StreamsPath;
import org.springframework.xd.dirt.stream.Stream;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.StreamRepository;
import org.springframework.xd.dirt.util.MapBytesUtility;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;

/**
 * Stream instance repository. It should only return values for Streams that are deployed.
 * 
 * @author Mark Fisher
 */
// todo: the StreamRepository abstraction can be removed once we are fully zk-enabled since we do not need to
// support multiple impls at that point
public class ZooKeeperStreamRepository implements StreamRepository, InitializingBean {

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
		// stream instances are "saved" when a StreamListener deploys a stream
		return entity;
	}

	@Override
	public <S extends Stream> Iterable<S> save(Iterable<S> entities) {
		// stream instances are "saved" when a StreamListener deploys a stream
		return entities;
	}

	@Override
	public Stream findOne(String id) {
		CuratorFramework client = zkConnection.getClient();
		StreamsPath path = new StreamsPath().setStreamName(id);
		try {
			Stat definitionStat = client.checkExists().forPath(path.build());
			if (definitionStat != null) {
				byte[] data = client.getData().forPath(path.build());
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
			throw new RuntimeException(e);
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
			return findAll(zkConnection.getClient().getChildren().forPath(Paths.STREAMS));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
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
			throw new RuntimeException(e);
		}
		return results;
	}

	@Override
	public long count() {
		try {
			return zkConnection.getClient().checkExists().forPath(Paths.STREAM_DEPLOYMENTS).getNumChildren();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void delete(String id) {
		try {
			zkConnection.getClient().delete().forPath(Paths.build(Paths.STREAM_DEPLOYMENTS, id));
		}
		catch (KeeperException.NoNodeException e) {
			// ignore
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void delete(Stream entity) {
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
			List<String> children = zkConnection.getClient().getChildren().forPath(Paths.JOB_DEPLOYMENTS);
			for (String child : children) {
				delete(child);
			}
		}
		catch (KeeperException.NoNodeException e) {
			// ignore
		}
		catch (Exception e) {
			throw new RuntimeException(e);
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
