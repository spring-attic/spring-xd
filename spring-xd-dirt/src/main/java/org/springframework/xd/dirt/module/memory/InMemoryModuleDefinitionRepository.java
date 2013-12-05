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

package org.springframework.xd.dirt.module.memory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.ModuleDependencyRepository;
import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.dirt.module.support.ModuleDefinitionRepositoryUtils;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.store.AbstractInMemoryRepository;

/**
 * An in memory store of {@link ModuleDefinition}s.
 * 
 * @author Glenn Renfro
 * @author Mark Fisher
 */
public class InMemoryModuleDefinitionRepository extends AbstractInMemoryRepository<ModuleDefinition, String> implements
		ModuleDefinitionRepository {

	private final ModuleRegistry moduleRegistry;

	private final ModuleDependencyRepository moduleDependencyRepository;

	public InMemoryModuleDefinitionRepository(ModuleRegistry moduleRegistry,
			ModuleDependencyRepository moduleDependencyRepository) {
		Assert.notNull(moduleRegistry, "moduleRegistry must not be null");
		Assert.notNull(moduleDependencyRepository, "moduleDependencyRepository must not be null");
		this.moduleRegistry = moduleRegistry;
		this.moduleDependencyRepository = moduleDependencyRepository;
	}

	@Override
	protected String keyFor(ModuleDefinition entity) {
		return entity.getType() + ":" + entity.getName();
	}

	@Override
	public Page<ModuleDefinition> findAll(Pageable pageable) {
		List<ModuleDefinition> definitions = moduleRegistry.findDefinitions();
		Iterator<ModuleDefinition> composed = super.findAll().iterator();
		while (composed.hasNext()) {
			definitions.add(composed.next());
		}
		Assert.isNull(pageable.getSort(), "Arbitrary sorting is not implemented");
		return slice(definitions, pageable);
	}

	@Override
	public List<ModuleDefinition> findByName(String name) {
		Assert.hasText(name, "name is required");
		List<ModuleDefinition> definitions = moduleRegistry.findDefinitions(name);
		Iterator<ModuleDefinition> composed = super.findAll().iterator();
		while (composed.hasNext()) {
			ModuleDefinition next = composed.next();
			if (name.equals(next.getName())) {
				definitions.add(next);
			}
		}
		return definitions;
	}

	@Override
	public ModuleDefinition findByNameAndType(String name, ModuleType type) {
		Assert.notNull(type, "type is required");
		ModuleDefinition definition = moduleRegistry.findDefinition(name, type);
		if (definition == null) {
			definition = super.findOne(type + ":" + name);
		}
		return definition;
	}

	@Override
	public Page<ModuleDefinition> findByType(Pageable pageable, ModuleType type) {
		if (type == null) {
			return findAll(pageable);
		}
		List<org.springframework.xd.module.ModuleDefinition> definitions = moduleRegistry.findDefinitions(type);
		Iterator<ModuleDefinition> composed = super.findAll().iterator();
		while (composed.hasNext()) {
			ModuleDefinition next = composed.next();
			if (type.equals(next.getType())) {
				definitions.add(next);
			}
		}
		Assert.isNull(pageable.getSort(), "Arbitrary sorting is not implemented");
		return slice(definitions, pageable);
	}

	@Override
	public Set<String> findDependentModules(String name, ModuleType type) {
		Set<String> dependentModules = moduleDependencyRepository.find(name, type);
		if (dependentModules == null) {
			return new HashSet<String>();
		}
		return dependentModules;
	}

	// TODO- BEGIN SHARED IMPL WITH RedisModuleDefintionRepository

	@SuppressWarnings("unchecked")
	@Override
	public ModuleDefinition save(ModuleDefinition entity) {
		ModuleDefinition md = super.save(entity);
		for (ModuleDefinition child : md.getComposedModuleDefinitions()) {
			ModuleDefinitionRepositoryUtils.saveDependencies(moduleDependencyRepository, child, dependencyKey(entity));
		}
		return md;
	}

	@Override
	public void delete(ModuleDefinition entity) {
		List<ModuleDefinition> composedModuleDefinitions = entity.getComposedModuleDefinitions();
		for (ModuleDefinition composedModule : composedModuleDefinitions) {
			ModuleDefinitionRepositoryUtils.deleteDependencies(moduleDependencyRepository, composedModule,
					dependencyKey(entity));
		}
		super.delete(entity);
	}

	@Override
	public void delete(String id) {
		ModuleDefinition def = this.findOne(id);
		if (def != null) {
			this.delete(def);
		}
	}


	// TODO refactor to use keyFor
	private String dependencyKey(ModuleDefinition moduleDefinition) {
		return String.format("module:%s:%s", moduleDefinition.getType(), moduleDefinition.getName());
	}

	// TODO END SHARED IMPL WITH RedisModuleDefintionRepository

	/**
	 * Post-process the list to only return elements matching the page request.
	 */
	@Override
	protected Page<ModuleDefinition> slice(List<ModuleDefinition> list, Pageable pageable) {
		int to = Math.min(list.size(), pageable.getOffset() + pageable.getPageSize());
		List<ModuleDefinition> data = list.subList(pageable.getOffset(), to);
		return new PageImpl<ModuleDefinition>(data, pageable, list.size());
	}

}
