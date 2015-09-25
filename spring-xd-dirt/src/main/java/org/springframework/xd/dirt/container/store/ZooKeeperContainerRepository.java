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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.utils.ThreadUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.cluster.NoSuchContainerException;
import org.springframework.xd.dirt.module.store.ModuleMetadata;
import org.springframework.xd.dirt.module.store.ZooKeeperModuleMetadataRepository;
import org.springframework.xd.dirt.rest.PasswordUtils;
import org.springframework.xd.dirt.util.PagingUtility;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;

/**
 * ZooKeeper backed repository for runtime info about Containers. This
 * implementation uses {@link PathChildrenCache} to cache container
 * info from ZooKeeper. The cache is used for all <em>reads</em>, whereas
 * all <em>writes</em> are written directly to ZooKeeper. This means
 * that a read that immediately follows a write <em>may</em> return
 * {@code null} if the cache has not been updated yet.
 *
 * @author Mark Fisher
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 * @author Patrick Peralta
 */
public class ZooKeeperContainerRepository implements ContainerRepository, ApplicationListener<ApplicationEvent> {

	/**
	 * Logger.
	 */
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	/**
	 * ZooKeeper connection.
	 */
	private final ZooKeeperConnection zkConnection;

	private final ZooKeeperModuleMetadataRepository zkModuleMetadataRepository;

	/**
	 * Paging support for this repository.
	 */
	private final PagingUtility<Container> pagingUtility = new PagingUtility<Container>();

	/**
	 * Paging support for detailed containers.
	 */
	private final PagingUtility<DetailedContainer> detailedContainersUtil = new PagingUtility<DetailedContainer>();

	/**
	 * Atomic reference to the {@link PathChildrenCache} for containers
	 * under the {@link Paths#CONTAINERS} node. This reference should
	 * <em>not</em> be used directly; instead use {@link #ensureCache}
	 * to ensure the cache is initialized.
	 *
	 * @see #ensureCache
	 */
	private final AtomicReference<PathChildrenCache> cacheRef = new AtomicReference<PathChildrenCache>();

	/**
	 * Construct a {@code ZooKeeperContainerRepository}.
	 *
	 * @param zkConnection the ZooKeeper connection
	 * @param moduleMetadataRepository the module metadata repository
	 */
	@Autowired
	public ZooKeeperContainerRepository(ZooKeeperConnection zkConnection,
			ZooKeeperModuleMetadataRepository moduleMetadataRepository) {
		this.zkConnection = zkConnection;
		this.zkModuleMetadataRepository = moduleMetadataRepository;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContextStoppedEvent || event instanceof ContextClosedEvent) {
			closeCache();
		}
	}

	/**
	 * Close the {@link PathChildrenCache container cache} and null out
	 * the {@link #cacheRef atmoic reference}.
	 */
	private void closeCache() {
		PathChildrenCache cache = cacheRef.get();
		if (cache != null) {
			try {
				cache.close();
			}
			catch (Exception e) {
				// ignore exception on close
			}
			finally {
				cacheRef.compareAndSet(cache, null);
			}
		}
	}

	/**
	 * Return a {@link PathChildrenCache} for containers, creating and
	 * initializing a new instance if necessary.
	 *
	 * @return a {@code PathChildrenCache} for containers
	 * @throws java.lang.IllegalStateException if the cache could not be initialized
	 *        (likely due to a ZooKeeper connection error)
	 */
	private PathChildrenCache ensureCache() {
		if (cacheRef.get() == null) {
			synchronized (cacheRef) {
				if (cacheRef.get() == null) {
					CuratorFramework client = zkConnection.getClient();
					PathChildrenCache cache = new PathChildrenCache(client, Paths.CONTAINERS,
							true, ThreadUtils.newThreadFactory("ContainerCache"));
					cache.getListenable().addListener(new PathChildrenCacheListener() {

						@Override
						public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) {
							// shut down the cache if ZooKeeper connection goes away
							ZooKeeperUtils.logCacheEvent(logger, event);
							if (event.getType() == PathChildrenCacheEvent.Type.CONNECTION_SUSPENDED ||
									event.getType() == PathChildrenCacheEvent.Type.CONNECTION_LOST) {
								closeCache();
							}
						}
					});
					try {
						Paths.ensurePath(client, Paths.CONTAINERS);
						cacheRef.set(cache);
						cache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
					}
					catch (Exception e) {
						try {
							cache.close();
						}
						catch (Exception ce) {
							// ignore exception on close
						}
						finally {
							cacheRef.compareAndSet(cache, null);
						}
						throw ZooKeeperUtils.wrapThrowable(e);
					}
				}
			}
		}

		PathChildrenCache cache = cacheRef.get();
		Assert.state(cache != null, "Container cache not initialized " +
				"(likely as a result of a ZooKeeper connection error)");
		return cache;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterable<Container> findAll(Sort sort) {
		// todo: add support for sort
		return findAll();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Page<Container> findAll(Pageable pageable) {
		return pagingUtility.getPagedData(pageable, findAll());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <S extends Container> S save(S entity) {
		CuratorFramework client = zkConnection.getClient();
		String path = Paths.build(Paths.CONTAINERS, entity.getName());

		try {
			client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path,
					ZooKeeperUtils.mapToBytes(entity.getAttributes()));
			return entity;
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void update(Container entity) {
		CuratorFramework client = zkConnection.getClient();
		String path = Paths.build(Paths.CONTAINERS, entity.getName());

		try {
			Stat stat = client.checkExists().forPath(path);
			if (stat == null) {
				throw new NoSuchContainerException("Could not find container with id " + entity.getName());
			}
			client.setData().forPath(path, ZooKeeperUtils.mapToBytes(entity.getAttributes()));
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e, e.getMessage());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <S extends Container> Iterable<S> save(Iterable<S> entities) {
		List<S> results = new ArrayList<S>();
		for (S entity : entities) {
			results.add(save(entity));
		}
		return results;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Container findOne(String id) {
		Container container = null;
		String containerPath = path(id);
		ChildData childData = ensureCache().getCurrentData(containerPath);
		byte[] data = null;

		if (childData != null) {
			data = childData.getData();
		}
		if (data != null) {
			container = new Container(id, ZooKeeperUtils.bytesToMap(data));
		}

		return container;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean exists(String id) {
		return ensureCache().getCurrentData(path(id)) != null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Container> findAll() {
		List<Container> results = new ArrayList<Container>();
		List<ChildData> children = new ArrayList<ChildData>(ensureCache().getCurrentData());

		// sort containers by node creation date (cluster join time)
		Collections.sort(children, new Comparator<ChildData>() {

			@Override
			public int compare(ChildData c1, ChildData c2) {
				long t1 = c1.getStat().getCtime();
				long t2 = c2.getStat().getCtime();
				return (t1 < t2) ? -1 : ((t1 == t2) ? 0 : 1);
			}
		});

		for (ChildData childData : children) {
			results.add(findOne(Paths.stripPath(childData.getPath())));
		}

		return results;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterable<Container> findAll(Iterable<String> ids) {
		List<Container> results = new ArrayList<Container>();
		for (String id : ids) {
			Container entity = findOne(id);
			if (entity != null) {
				results.add(entity);
			}
		}
		return results;
	}

	/**
	 * Find all the {@link DetailedContainer}s in the XD cluster.
	 *
	 * @param pageable the pagination info
	 * @param maskSensitiveProperties Shall sensitive data such as passwords be masked?
	 * @return the paged list of {@link DetailedContainer}s
	 */
	@Override
	public Page<DetailedContainer> findAllRuntimeContainers(Pageable pageable, boolean maskSensitiveProperties) {
		List<DetailedContainer> results = new ArrayList<DetailedContainer>();
		List<Container> containers = this.findAll();

		for (Container container : containers) {
			DetailedContainer runtimeContainer = new DetailedContainer(container);
			final List<ModuleMetadata> deployedModules = zkModuleMetadataRepository.findAllByContainerId(
					container.getName());

			if (maskSensitiveProperties) {
				for (ModuleMetadata moduleMetadata : deployedModules) {
					PasswordUtils.maskPropertiesIfNecessary(moduleMetadata.getModuleOptions());
				}
			}

			runtimeContainer.setDeployedModules(deployedModules);
			runtimeContainer.setDeploymentSize(deployedModules.size());
			results.add(runtimeContainer);
		}
		return detailedContainersUtil.getPagedData(pageable, results);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long count() {
		return ensureCache().getCurrentData().size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void delete(String id) {
		// Container metadata is "deleted" when a Container departs
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void delete(Container entity) {
		// Container metadata is "deleted" when a Container departs
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void delete(Iterable<? extends Container> entities) {
		// Container metadata is "deleted" when a Container departs
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deleteAll() {
		// Container metadata is "deleted" when a Container departs
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Iterable<Container> findAllInRange(String from, boolean fromInclusive, String to,
			boolean toInclusive) {
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	/**
	 * Return the path for a container.
	 *
	 * @param id container id
	 * @return path for the container
	 * @see Paths#build
	 */
	private String path(String id) {
		return Paths.build(Paths.CONTAINERS, id);
	}

}
