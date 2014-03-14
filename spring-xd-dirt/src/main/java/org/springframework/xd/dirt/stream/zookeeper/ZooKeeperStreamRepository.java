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
import org.apache.zookeeper.data.Stat;

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
public class ZooKeeperStreamRepository implements StreamRepository {

	private final ZooKeeperConnection zkConnection;

	private final MapBytesUtility mapBytesUtility = new MapBytesUtility();

	@Autowired
	public ZooKeeperStreamRepository(ZooKeeperConnection zkConnection) {
		this.zkConnection = zkConnection;
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
		StreamsPath path = new StreamsPath().setStreamName(id);
		try {
			Stat stat = zkConnection.getClient().checkExists().forPath(path.build());
			if (stat != null) {
				byte[] data = zkConnection.getClient().getData().forPath(path.build());
				Map<String, String> map = mapBytesUtility.toMap(data);
				Stream stream = new Stream(new StreamDefinition(id,
						map.get("definition"), Boolean.parseBoolean(map.get("deploy"))));
				if (stream.getDefinition().isDeploy()) {
					stream.setStartedAt(new Date(stat.getCtime()));
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
		int count = 0;
		List<Stream> all = findAll();
		for (Stream stream : all) {
			if (stream.getDefinition().isDeploy()) {
				count++;
			}
		}
		return count;
	}

	@Override
	public void delete(String id) {
		// stream instances are "deleted" when a StreamListener undeploys a stream
	}

	@Override
	public void delete(Stream entity) {
		// stream instances are "deleted" when a StreamListener undeploys a stream
	}

	@Override
	public void delete(Iterable<? extends Stream> entities) {
		// stream instances are "deleted" when a StreamListener undeploys a stream
	}

	@Override
	public void deleteAll() {
		// stream instances are "deleted" when a StreamListener undeploys a stream
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
