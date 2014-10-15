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

package org.springframework.xd.dirt.stream;

import static org.springframework.xd.dirt.stream.ParsingContext.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.module.DependencyException;
import org.springframework.xd.dirt.module.ModuleAlreadyExistsException;
import org.springframework.xd.dirt.module.ModuleDefinitionRepository;
import org.springframework.xd.dirt.module.NoSuchModuleException;
import org.springframework.xd.module.CompositeModuleModuleDefinition;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;


/**
 * Provides save, delete and dependency operations for managing a composite module definition.
 *
 * @author Mark Pollack
 */
public class CompositeModuleDefinitionService {

	private final ModuleDefinitionRepository moduleDefinitionRepository;

	private final XDStreamParser streamParser;

	@Autowired
	public CompositeModuleDefinitionService(ModuleDefinitionRepository moduleDefinitionRepository,
			XDStreamParser streamParser) {
		this.moduleDefinitionRepository = moduleDefinitionRepository;
		this.streamParser = streamParser;
	}

	public ModuleDefinitionRepository getModuleDefinitionRepository() {
		return moduleDefinitionRepository;
	}

	public ModuleDefinition save(String name, String definition) {
		List<ModuleDescriptor> modules = this.streamParser.parse(name, definition, module);

		ModuleType type = this.determineType(modules);
		if (moduleDefinitionRepository.findByNameAndType(name, type) != null) {
			throw new ModuleAlreadyExistsException(name, type);
		}

		// Create ModuleDefinition instance from list of ModuleDescriptors
		List<ModuleDefinition> composedModuleDefinitions = createComposedModuleDefinitions(modules);
		ModuleDefinition moduleDefinition = new CompositeModuleModuleDefinition(name, type, definition, composedModuleDefinitions);

		this.moduleDefinitionRepository.save(moduleDefinition);
		return moduleDefinition;
	}

	public void delete(String name, ModuleType type) {
		ModuleDefinition definition = moduleDefinitionRepository.findByNameAndType(name, type);
		if (definition == null) {
			throw new NoSuchModuleException(name, type);
		}
		if (definition.getDefinition() == null) {
			throw new IllegalStateException(String.format("Cannot delete non-composed module %s:%s", type, name));
		}
		Set<String> dependents = this.moduleDefinitionRepository.findDependentModules(name, type);
		if (!dependents.isEmpty()) {
			throw new DependencyException("Cannot delete module %2$s:%1$s because it is used by %3$s", name, type,
					dependents);
		}

		this.moduleDefinitionRepository.delete(definition);
	}

	private List<ModuleDefinition> createComposedModuleDefinitions(
			List<ModuleDescriptor> moduleDescriptors) {

		List<ModuleDefinition> moduleDefinitions = new ArrayList<ModuleDefinition>(moduleDescriptors.size());
		for (ModuleDescriptor moduleDescriptor : moduleDescriptors) {
			moduleDefinitions.add(moduleDefinitionRepository.findByNameAndType(moduleDescriptor.getModuleName(),
					moduleDescriptor.getType()));
		}
		return moduleDefinitions;
	}

	private ModuleType determineType(List<ModuleDescriptor> modules) {
		Assert.isTrue(modules != null && modules.size() > 0, "at least one module required");
		if (modules.size() == 1) {
			return modules.get(0).getType();
		}
		Collections.sort(modules);
		ModuleType firstType = modules.get(0).getType();
		ModuleType lastType = modules.get(modules.size() - 1).getType();
		boolean hasInput = firstType != ModuleType.source;
		boolean hasOutput = lastType != ModuleType.sink;
		if (hasInput && hasOutput) {
			return ModuleType.processor;
		}
		if (hasInput) {
			return ModuleType.sink;
		}
		if (hasOutput) {
			return ModuleType.source;
		}
		throw new IllegalArgumentException("invalid module composition; must expose input and/or output channel");
	}


}
