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

package org.springframework.xd.dirt.module.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections.MapUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PlaceholderConfigurerSupport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.core.DeploymentUnitStatus;
import org.springframework.xd.dirt.core.ModuleDeploymentsPath;
import org.springframework.xd.dirt.stream.JobRepository;
import org.springframework.xd.dirt.stream.StreamRepository;
import org.springframework.xd.dirt.util.PagingUtility;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.dirt.zookeeper.ZooKeeperUtils;
import org.springframework.xd.module.ModuleType;

/**
 * ZooKeeper backed repository for runtime info about deployed modules.
 *
 * @author Ilayaperumal Gopinathan
 * @author David Turanski
 * @author Gary Russell
 */
public class ZooKeeperModuleMetadataRepository implements ModuleMetadataRepository {

	private static final String XD_MODULE_PROPERTIES_PREFIX = "xd.";

	private final ZooKeeperConnection zkConnection;

	private final StreamRepository streamRepository;

	private final JobRepository jobRepository;

	private final PagingUtility<ModuleMetadata> pagingUtility = new PagingUtility<ModuleMetadata>();

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	public ZooKeeperModuleMetadataRepository(ZooKeeperConnection zkConnection,
			StreamRepository streamRepository,
			JobRepository jobRepository) {
		this.zkConnection = zkConnection;
		this.streamRepository = streamRepository;
		this.jobRepository = jobRepository;
	}

	@Override
	public Iterable<ModuleMetadata> findAll(Sort sort) {
		// todo: add support for sort
		return findAll();
	}

	@Override
	public Page<ModuleMetadata> findAll(Pageable pageable) {
		return pagingUtility.getPagedData(pageable, findAll());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ModuleMetadata findOne(String containerId, String moduleId) {
		return findOne(new ModuleMetadata.Id(containerId, moduleId));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Page<ModuleMetadata> findAllByContainerId(Pageable pageable, String containerId) {
		Assert.hasText(containerId, "containerId is required");
		return pagingUtility.getPagedData(pageable, findAllByContainerId(containerId));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Page<ModuleMetadata> findAllByModuleId(Pageable pageable, String moduleId) {
		Assert.hasText(moduleId, "moduleId is required");
		List<ModuleMetadata> results = new ArrayList<ModuleMetadata>();
		for (String containerId : getAvailableContainerIds()) {
			ModuleMetadata metadata = findOne(new ModuleMetadata.Id(containerId, moduleId));
			if (metadata != null) {
				results.add(metadata);
			}
		}
		return pagingUtility.getPagedData(pageable, results);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Find the module metadata for the modules that are deployed into the
	 * given container and module metadata id.
	 *
	 * @param id unique id for module deployment
	 *
	 * @return {@link ModuleMetadata} of the module.
	 */
	@Override
	public ModuleMetadata findOne(ModuleMetadata.Id id) {
		Assert.notNull(id, "id is required");

		String moduleDeploymentPath = moduleDeploymentPath(id);
		String metadataPath = Paths.build(moduleDeploymentPath, Paths.METADATA);
		ModuleMetadata metadata = null;

		try {
			byte[] data = zkConnection.getClient().getData().forPath(metadataPath);
			if (data != null) {
				Map<String, String> metadataMap = ZooKeeperUtils.bytesToMap(data);
				new ModuleDeploymentsPath(moduleDeploymentPath);
				ModuleType moduleType = id.getModuleType();

				DeploymentUnitStatus status = moduleType == ModuleType.job
						? jobRepository.getDeploymentStatus(id.getUnitName())
						: streamRepository.getDeploymentStatus(id.getUnitName());

				metadata = new ModuleMetadata(id,
						getResolvedModuleOptions(metadataMap),
						getDeploymentProperties(moduleDeploymentPath),
						status.getState());
			}
		}
		catch (Exception e) {
			// NoNodeException - this node does not exist, will return null
			ZooKeeperUtils.wrapAndThrowIgnoring(e, NoNodeException.class);
		}
		return metadata;
	}

	/**
	 * Get the deployment properties associated with this module metadata path.
	 *
	 * @param moduleDeploymentsPath the module deployment path
	 * @return deployment properties
	 */
	private Properties getDeploymentProperties(String moduleDeploymentsPath) {
		Map<String, String> deploymentProperties = new HashMap<String, String>();
		try {
			deploymentProperties = ZooKeeperUtils.bytesToMap(zkConnection.getClient().getData()
					.forPath(moduleDeploymentsPath));
		}
		catch (Exception e) {
			ZooKeeperUtils.wrapAndThrowIgnoring(e, NoNodeException.class);
		}
		return MapUtils.toProperties(deploymentProperties);
	}

	/**
	 * Resolve the module option value using the module metadata.
	 *
	 * @param metadataMap the values map from ZK module metadata
	 * @return the resolved option values
	 */
	private Properties getResolvedModuleOptions(Map<String, String> metadataMap) {
		Map<String, String> optionsMap = new HashMap<String, String>();
		for (Map.Entry<String, String> entry : metadataMap.entrySet()) {
			String propertyKey = entry.getKey();
			String propertyValue = entry.getValue();
			if (!propertyKey.startsWith(XD_MODULE_PROPERTIES_PREFIX)
					&& !StringUtils.isEmpty(propertyValue)) {
				if (propertyValue.startsWith(PlaceholderConfigurerSupport.DEFAULT_PLACEHOLDER_PREFIX) &&
						propertyValue.endsWith(PlaceholderConfigurerSupport.DEFAULT_PLACEHOLDER_SUFFIX)) {
					// For a property ${module.property}, we just extract "module.property" and
					// check if the metadataMap has a value for it.
					String placeholderKey = propertyValue.substring(2, propertyValue.length() - 1);
					if (metadataMap.get(placeholderKey) != null) {
						propertyValue = metadataMap.get(placeholderKey);
					}
				}
				optionsMap.put(propertyKey, propertyValue);
			}
		}
		return MapUtils.toProperties(optionsMap);
	}

	@Override
	public boolean exists(ModuleMetadata.Id id) {
		try {
			return zkConnection.getClient().checkExists()
					.forPath(moduleDeploymentPath(id)) != null;
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	@Override
	public List<ModuleMetadata> findAll() {
		List<ModuleMetadata> results = new ArrayList<ModuleMetadata>();
		try {
			for (String containerId : getAvailableContainerIds()) {
				List<ModuleMetadata.Id> modules = getDeployedModules(containerId);
				for (ModuleMetadata.Id moduleId : modules) {
					ModuleMetadata metadata = findOne(moduleId);
					if (metadata != null) {
						results.add(metadata);
					}
				}
			}
			return results;
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	/**
	 * Get all the available containers' ids.
	 *
	 * @return the list of all available containers' ids.
	 */
	private List<String> getAvailableContainerIds() {
		try {
			return zkConnection.getClient().getChildren().forPath(Paths.CONTAINERS);
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
	}

	/**
	 * Find all the modules that are deployed into this container.
	 *
	 * @param containerId the containerId
	 * @return {@link ModuleMetadata} of the modules deployed into this container.
	 */
	public List<ModuleMetadata> findAllByContainerId(String containerId) {
		Assert.hasText(containerId, "containerId is required");

		List<ModuleMetadata.Id> deployedModules = getDeployedModules(containerId);
		logger.debug("deployedModules: {}", deployedModules);

		List<ModuleMetadata> results = new ArrayList<ModuleMetadata>(deployedModules.size());
		for (ModuleMetadata.Id moduleId : deployedModules) {
			ModuleMetadata metadata = findOne(moduleId);
			logger.debug("found metadata: {}", metadata);
			if (metadata != null) {
				results.add(metadata);
			}
		}
		return results;
	}

	/**
	 * Get all the deployed modules by the given containerId.
	 *
	 * @param containerId the containerId to filter
	 * @return the list of moduleIds of the deployed modules.
	 */
	private List<ModuleMetadata.Id> getDeployedModules(String containerId) {
		List<ModuleMetadata.Id> ids = new ArrayList<ModuleMetadata.Id>();
		try {
			CuratorFramework client = zkConnection.getClient();
			if (client.checkExists().forPath(Paths.build(Paths.CONTAINERS, containerId)) != null) {
				List<String> qualifiedIds = client.getChildren().forPath(containerAllocationPath(containerId));
				for (String qualifiedId : qualifiedIds) {
					ids.add(new ModuleMetadata.Id(containerId, qualifiedId));
				}
			}
		}
		catch (Exception e) {
			ZooKeeperUtils.wrapAndThrowIgnoring(e, NoNodeException.class);
		}
		return ids;
	}

	@Override
	public Iterable<ModuleMetadata> findAll(Iterable<ModuleMetadata.Id> ids) {
		List<ModuleMetadata> results = new ArrayList<ModuleMetadata>();
		for (ModuleMetadata.Id id : ids) {
			ModuleMetadata entity = findOne(id);
			if (entity != null) {
				results.add(entity);
			}
		}
		return results;
	}

	@Override
	public Iterable<ModuleMetadata> findAllInRange(ModuleMetadata.Id from,
			boolean fromInclusive, ModuleMetadata.Id to, boolean toInclusive) {
		throw new UnsupportedOperationException("Not supported.");
	}

	@Override
	public long count() {
		long count = 0;
		try {
			List<String> containerIds = zkConnection.getClient().getChildren().forPath(Paths.MODULE_DEPLOYMENTS);
			for (String containerId : containerIds) {
				Stat stat = zkConnection.getClient().checkExists().forPath(containerAllocationPath(containerId));
				if (stat != null) {
					count+= stat.getNumChildren();
				}
			}
		}
		catch (Exception e) {
			throw ZooKeeperUtils.wrapThrowable(e);
		}
		return count;
	}


	/**
	 * Return the module allocation path for the given container id.
	 *
	 * @param id container id
	 * @return path for module allocations for the container
	 */
	private String containerAllocationPath(String id) {
		return Paths.build(Paths.MODULE_DEPLOYMENTS, Paths.ALLOCATED, id);
	}

	/**
	 * Return the module deployment path for the module indicated
	 * by the provided {@link org.springframework.xd.dirt.module.store.ModuleMetadata.Id}.
	 *
	 * @param id id for module instance
	 * @return path for module allocation
	 */
	private String moduleDeploymentPath(ModuleMetadata.Id id) {
		return Paths.build(Paths.MODULE_DEPLOYMENTS, Paths.ALLOCATED, id.getContainerId(),
				id.getFullyQualifiedId());
	}

	@Override
	public <S extends ModuleMetadata> S save(S entity) {
		throw new UnsupportedOperationException();
	}

	@Override
	public <S extends ModuleMetadata> Iterable<S> save(Iterable<S> entities) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(ModuleMetadata.Id id) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(ModuleMetadata entity) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void delete(Iterable<? extends ModuleMetadata> entities) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void deleteAll() {
		throw new UnsupportedOperationException();
	}

}
