/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.stream.memory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.xd.dirt.module.ModuleDependencyRepository;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.stream.StreamDefinitionRepository;
import org.springframework.xd.dirt.stream.StreamDefinitionRepositoryUtils;
import org.springframework.xd.store.AbstractInMemoryRepository;

/**
 * An in memory store of {@link StreamDefinition}s.
 * 
 * @author Eric Bottard
 * 
 */
public class InMemoryStreamDefinitionRepository extends AbstractInMemoryRepository<StreamDefinition, String> implements
		StreamDefinitionRepository {

	private final ModuleDependencyRepository dependencyRepository;

	@Autowired
	public InMemoryStreamDefinitionRepository(ModuleDependencyRepository dependencyRepository) {
		this.dependencyRepository = dependencyRepository;
	}

	@SuppressWarnings("unchecked")
	@Override
	public StreamDefinition save(StreamDefinition entity) {
		StreamDefinition sd = super.save(entity);
		StreamDefinitionRepositoryUtils.saveDependencies(dependencyRepository, sd);
		return sd;
	}

	@Override
	public void delete(StreamDefinition entity) {
		StreamDefinitionRepositoryUtils.deleteDependencies(dependencyRepository, entity);
		super.delete(entity);
	};

	@Override
	public void delete(String id) {
		StreamDefinition def = this.findOne(id);
		if (def != null) {
			this.delete(def);
		}
	};

	@Override
	protected String keyFor(StreamDefinition entity) {
		return entity.getName();
	}

}
