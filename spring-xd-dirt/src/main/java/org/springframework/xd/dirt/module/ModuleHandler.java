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

package org.springframework.xd.dirt.module;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.stream.ModuleDefinition;
import org.springframework.xd.module.ModuleType;


/**
 * 
 * @author Glenn Renfro
 */
public class ModuleHandler {

	ModuleRegistry moduleRegistry;

	public ModuleHandler(ModuleRegistry moduleRegistry) {
		this.moduleRegistry = moduleRegistry;
	}

	public Page<ModuleDefinition> findAll(Pageable pageable) {
		List<org.springframework.xd.module.ModuleDefinition> definitions = moduleRegistry.findDefinitions();
		Assert.isNull(pageable.getSort(), "Arbitrary sorting is not implemented");
		return slice(convertRegistryDefToModuleDefinition(definitions), pageable);
	}

	public Page<ModuleDefinition> findAll(Pageable pageable, String type) {
		if (type.equalsIgnoreCase("all")) {
			return findAll(pageable);
		}
		ModuleType moduleType = ModuleType.getModuleTypeByTypeName(type);
		if (moduleType == null) {
			return new PageImpl<ModuleDefinition>(new ArrayList<ModuleDefinition>(), pageable, 0);
		}
		List<org.springframework.xd.module.ModuleDefinition> definitions = moduleRegistry.findDefinitions(moduleType);
		Assert.isNull(pageable.getSort(), "Arbitrary sorting is not implemented");
		return slice(convertRegistryDefToModuleDefinition(definitions), pageable);
	}

	public List<ModuleDefinition> convertRegistryDefToModuleDefinition(
			List<org.springframework.xd.module.ModuleDefinition> definitions) {
		ArrayList<ModuleDefinition> result = new ArrayList<ModuleDefinition>();
		for (org.springframework.xd.module.ModuleDefinition registryDefinition : definitions) {
			result.add(new ModuleDefinition(registryDefinition.getName(),
					registryDefinition.getResource().getDescription(), registryDefinition.getType()));
		}
		return result;
	}

	/**
	 * Post-process the list to only return elements matching the page request.
	 */
	protected Page<ModuleDefinition> slice(List<ModuleDefinition> list, Pageable pageable) {
		int to = Math.min(list.size(), pageable.getOffset() + pageable.getPageSize());
		List<ModuleDefinition> data = list.subList(pageable.getOffset(), to);
		return new PageImpl<ModuleDefinition>(data, pageable, list.size());
	}
}
