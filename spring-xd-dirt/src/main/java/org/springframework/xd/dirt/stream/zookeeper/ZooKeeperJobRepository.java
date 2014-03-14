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
import org.springframework.xd.dirt.core.JobsPath;
import org.springframework.xd.dirt.stream.Job;
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.JobRepository;
import org.springframework.xd.dirt.util.MapBytesUtility;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;

/**
 * Job instance repository. It should only return values for Jobs that are deployed.
 * 
 * @author Mark Fisher
 */
// todo: the JobRepository abstraction can be removed once we are fully zk-enabled since we do not need to
// support multiple impls at that point
public class ZooKeeperJobRepository implements JobRepository {

	private final ZooKeeperConnection zkConnection;

	private final MapBytesUtility mapBytesUtility = new MapBytesUtility();

	@Autowired
	public ZooKeeperJobRepository(ZooKeeperConnection zkConnection) {
		this.zkConnection = zkConnection;
	}

	@Override
	public Iterable<Job> findAll(Sort sort) {
		// todo: implement sort support
		return findAll();
	}

	@Override
	public Page<Job> findAll(Pageable pageable) {
		List<Job> all = findAll();
		if (CollectionUtils.isEmpty(all)) {
			return new PageImpl<Job>(all);
		}
		Collections.sort(all);

		int offSet = pageable.getOffset();
		int size = pageable.getPageSize();

		List<Job> page = new ArrayList<Job>();
		for (int i = offSet; i < Math.min(all.size(), offSet + size); i++) {
			page.add(all.get(i));
		}

		return new PageImpl<Job>(page, pageable, all.size());
	}

	@Override
	public <S extends Job> S save(S entity) {
		// job instances are "saved" when a JobListener deploys a job
		return entity;
	}

	@Override
	public <S extends Job> Iterable<S> save(Iterable<S> entities) {
		// job instances are "saved" when a JobListener deploys a job
		return entities;
	}

	@Override
	public Job findOne(String id) {
		JobsPath path = new JobsPath().setJobName(id);
		try {
			Stat stat = zkConnection.getClient().checkExists().forPath(path.build());
			if (stat != null) {
				byte[] data = zkConnection.getClient().getData().forPath(path.build());
				Map<String, String> map = mapBytesUtility.toMap(data);
				Job job = new Job(new JobDefinition(id,
						map.get("definition"),
						Boolean.parseBoolean(map.get("deploy"))));
				if (job.getDefinition().isDeploy()) {
					job.setStartedAt(new Date(stat.getCtime()));
					return job;
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
	public List<Job> findAll() {
		try {
			return findAll(zkConnection.getClient().getChildren().forPath(Paths.JOBS));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Job> findAll(Iterable<String> ids) {
		List<Job> results = new ArrayList<Job>();
		try {
			for (String jobName : ids) {
				Job job = findOne(jobName);
				if (job != null) {
					results.add(job);
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
		List<Job> all = findAll();
		for (Job job : all) {
			if (job.getDefinition().isDeploy()) {
				count++;
			}
		}
		return count;
	}

	@Override
	public void delete(String id) {
		// job instances are "deleted" when a JobListener undeploys a job
	}

	@Override
	public void delete(Job entity) {
		// job instances are "deleted" when a JobListener undeploys a job
	}

	@Override
	public void delete(Iterable<? extends Job> entities) {
		// job instances are "deleted" when a JobListener undeploys a job
	}

	@Override
	public void deleteAll() {
		// job instances are "deleted" when a JobListener undeploys a job
	}

	@Override
	public Iterable<Job> findAllInRange(String from, boolean fromInclusive, String to, boolean toInclusive) {
		List<Job> all = findAll();
		if (CollectionUtils.isEmpty(all)) {
			return Collections.emptyList();
		}
		Collections.sort(all);

		List<Job> results = new ArrayList<Job>();
		for (Job job : all) {
			if (job.getDefinition().getName().compareTo(to) > 1) {
				break;
			}
			if (job.getDefinition().getName().compareTo(from) < 0) {
				continue;
			}
			results.add(job);
		}
		return results;
	}

}
