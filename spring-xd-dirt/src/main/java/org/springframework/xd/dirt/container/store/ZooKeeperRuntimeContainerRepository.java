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
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.cluster.RuntimeContainer;
import org.springframework.xd.dirt.module.store.ModuleMetadata;
import org.springframework.xd.dirt.module.store.ZooKeeperModuleMetadataRepository;
import org.springframework.xd.dirt.util.PagingUtility;

/**
 * ZooKeeper backed {@link RuntimeContainerRepository}.
 *
 * @author Ilayaperumal Gopinathan
 */
public class ZooKeeperRuntimeContainerRepository implements RuntimeContainerRepository {

	private final ZooKeeperContainerRepository zkContainerRepository;

	private final ZooKeeperModuleMetadataRepository zkModuleMetadataRepository;

	private final PagingUtility<RuntimeContainer> pagingUtility = new PagingUtility<RuntimeContainer>();

	/**
	 * Construct ZooKeeper backed {@link RuntimeContainerRepository}.
	 *
	 * @param containerRepository the container repository
	 * @param moduleMetadataRepository the module metadata repository
	 */
	@Autowired
	public ZooKeeperRuntimeContainerRepository(ZooKeeperContainerRepository containerRepository,
			ZooKeeperModuleMetadataRepository moduleMetadataRepository) {
		this.zkContainerRepository = containerRepository;
		this.zkModuleMetadataRepository = moduleMetadataRepository;
	}

	/**
	 * Find all the {@RuntimeContainer}s in the XD cluster.
	 *
	 * @param pageable the pagination info
	 * @return the paged list of {@RuntimeContainer}s
	 */
	@Override
	public Page<RuntimeContainer> findAllRuntimeContainers(Pageable pageable) {
		List<RuntimeContainer> results = new ArrayList<RuntimeContainer>();
		List<Container> containers = zkContainerRepository.findAll();

		for (Container container : containers) {
			RuntimeContainer runtimeContainer = new RuntimeContainer(container);
			List<ModuleMetadata> deployedModules = zkModuleMetadataRepository.findAllByContainerId(container.getName());
			runtimeContainer.setDeployedModules(deployedModules);
			runtimeContainer.setDeploymentSize(deployedModules.size());
			results.add(runtimeContainer);
		}
		return pagingUtility.getPagedData(pageable, results);
	}
}
