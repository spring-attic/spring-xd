/*
 * Copyright 2013 the original author or authors.
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

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.xd.store.AbstractInMemoryRepository;


/**
 * InMemory extension for the runtime module info repository with the modules mapped under their containers.
 * 
 * @author Ilayaperumal Gopinathan
 */
public class InMemoryRuntimeContainerModuleInfoRepository extends
		AbstractInMemoryRepository<RuntimeModuleInfoEntity, String> implements
		RuntimeContainerModuleInfoRepository {

	@Override
	protected String keyFor(RuntimeModuleInfoEntity entity) {
		// In case in-memory there is a single container with id:0; hence key is group:index
		return entity.getGroup() + ":" + entity.getIndex();
	}

	@Override
	public Page<RuntimeModuleInfoEntity> findAllByContainerId(Pageable pageable, String containerId) {
		return slice((List<RuntimeModuleInfoEntity>) findAll(), pageable);
	}

}
