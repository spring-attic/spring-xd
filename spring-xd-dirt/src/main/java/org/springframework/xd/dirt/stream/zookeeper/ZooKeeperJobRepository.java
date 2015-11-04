/*
 * Copyright 2014-2015 the original author or authors.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.xd.dirt.core.DeploymentUnitStatus;
import org.springframework.xd.dirt.stream.Job;
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.JobRepository;
import org.springframework.xd.dirt.util.PagingUtility;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;

/**
 * Job instance repository. It should only return values for Jobs that are deployed.
 *
 * @author Mark Fisher
 * @author David Turanski
 * @author Gunnar Hillert
 */
// todo: the JobRepository abstraction can be removed once we are fully zk-enabled since we do not need to
// support multiple impls at that point
public class ZooKeeperJobRepository implements JobRepository, InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(ZooKeeperJobRepository.class);

	private final ZooKeeperConnection zkConnection;

	private final PagingUtility<Job> pagingUtility = new PagingUtility<Job>();

	private final RepositoryConnectionListener connectionListener = new RepositoryConnectionListener();

	@Autowired
	public ZooKeeperJobRepository(ZooKeeperConnection zkConnection) {
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
	public Iterable<Job> findAll(Sort sort) {
		// todo: implement sort support
		return findAll();
	}

	@Override
	public Page<Job> findAll(Pageable pageable) {
		return pagingUtility.getPagedData(pageable, findAll());
	}

	@Override
	public <S extends Job> S save(S entity) {
		// job instances are "saved" when a JobDeploymentListener deploys a job
		return entity;
	}

	@Override
	public <S extends Job> Iterable<S> save(Iterable<S> entities) {
		// job instances are "saved" when a JobDeploymentListener deploys a job
		return entities;
	}

	@Override
	public Job findOne(String id) {
		CuratorFramework client = zkConnection.getClient();
		String path = Paths.build(Paths.JOBS, id);
		try {
			Stat definitionStat = client.checkExists().forPath(path);
			if (definitionStat != null) {
				byte[] data = zkConnection.getClient().getData().forPath(path);
				Map<String, String> map = ZooKeeperUtils.bytesToMap(data);
				Job job = new Job(new JobDefinition(id, map.get("definition")));

				Stat deployStat = client.checkExists().forPath(Paths.build(Paths.JOB_DEPLOYMENTS, id));
				if (deployStat != null) {
					job.setStartedAt(new Date(deployStat.getCtime()));
					job.setStatus(getDeploymentStatus(id));
					return job;
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
	public List<Job> findAll() {
		try {
			return findAll(zkConnection.getClient().getChildren().forPath(Paths.JOBS));
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
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
			throw ZooKeeperUtils.wrapThrowable(e);
		}
		return results;
	}

	@Override
	public long count() {
		try {
			Stat stat = zkConnection.getClient().checkExists().forPath(Paths.JOB_DEPLOYMENTS);
			return stat != null ? stat.getNumChildren() : 0;
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	@Override
	public void delete(String id) {
		CuratorFramework client = zkConnection.getClient();

		try {
			client.setData().forPath(
					Paths.build(Paths.JOB_DEPLOYMENTS, id, Paths.STATUS),
					ZooKeeperUtils.mapToBytes(new DeploymentUnitStatus(
							DeploymentUnitStatus.State.undeploying).toMap()));
		}
		catch (Exception e) {
			logger.warn("Exception while transitioning job '{}' state to {}", id,
					DeploymentUnitStatus.State.undeploying, e);
		}

		try {
			client.delete().deletingChildrenIfNeeded().forPath(Paths.build(Paths.JOB_DEPLOYMENTS, id));
		}
		catch (Exception e) {
			//NoNodeException - nothing to delete
			ZooKeeperUtils.wrapAndThrowIgnoring(e, KeeperException.NoNodeException.class);
		}
	}

	@Override
	public void delete(Job entity) {
		delete(entity.getDefinition().getName());
	}

	@Override
	public void delete(Iterable<? extends Job> entities) {
		for (Job job : entities) {
			delete(job);
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
		catch (Exception e) {
			//NoNodeException - nothing to delete
			ZooKeeperUtils.wrapAndThrowIgnoring(e, KeeperException.NoNodeException.class);
		}
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
			if (job.getDefinition().getName().compareTo(to) > 0) {
				break;
			}
			if (job.getDefinition().getName().compareTo(from) < 0) {
				continue;
			}
			results.add(job);
		}
		return results;
	}

	@Override
	public DeploymentUnitStatus getDeploymentStatus(String s) {
		String path = Paths.build(Paths.JOB_DEPLOYMENTS, s, Paths.STATUS);
		byte[] statusBytes = null;

		try {
			statusBytes = zkConnection.getClient().getData().forPath(path);
		}
		catch (Exception e) {
			// missing node means this job has not been deployed
			ZooKeeperUtils.wrapAndThrowIgnoring(e, KeeperException.NoNodeException.class);
		}

		return (statusBytes == null)
				? new DeploymentUnitStatus(DeploymentUnitStatus.State.undeployed)
				: new DeploymentUnitStatus(ZooKeeperUtils.bytesToMap(statusBytes));
	}

}
