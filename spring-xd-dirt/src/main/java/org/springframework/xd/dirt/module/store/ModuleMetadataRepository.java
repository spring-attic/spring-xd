/*
 * Copyright 2013-2014 the original author or authors.
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.xd.store.DomainRepository;

/**
 * Repository for persisting {@link ModuleMetadata}
 *
 * @author Ilayaperumal Gopinathan
 */
public interface ModuleMetadataRepository extends DomainRepository<ModuleMetadata, ModuleMetadata.Id> {

	/**
	 * Find paged {@link ModuleMetadata} for all the modules deployed
	 * into the given container.
	 *
	 * @param pageable the pageable metadata
	 * @param containerId the container Id
	 * @return Paged the {@link ModuleMetadata}
	 */
	Page<ModuleMetadata> findAllByContainerId(Pageable pageable, String containerId);

	/**
	 * Find all the modules that are deployed into the given container.
	 *
	 * @param containerId the containerId
	 * @return {@link ModuleMetadata} of the modules deployed into this container.
	 */
	Iterable<ModuleMetadata> findAllByContainerId(String containerId);

	/**
	 * Find paged {@link ModuleMetadata} for the modules of given moduleId.
	 *
	 * @param pageable the pageable metadata
	 * @param moduleId the module Id
	 * @return Paged the {@link ModuleMetadata}
	 */
	Page<ModuleMetadata> findAllByModuleId(Pageable pageable, String moduleId);

	/**
	 * Find {@link ModuleMetadata} for the module that has the given module id and
	 * deployed into given container.
	 *
	 * @param containerId the container Id
	 * @param moduleId the moduleId
	 * @return the corresponding {@link ModuleMetadata}
	 */
	ModuleMetadata findOne(String containerId, String moduleId);

}
