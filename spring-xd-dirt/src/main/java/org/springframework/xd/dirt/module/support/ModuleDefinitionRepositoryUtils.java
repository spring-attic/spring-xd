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

package org.springframework.xd.dirt.module.support;

import java.util.List;

import org.springframework.xd.dirt.module.ModuleDependencyRepository;
import org.springframework.xd.module.ModuleDefinition;


/**
 * Encapsulate shared functionality between implementations of ModuleDefinitionRepository that can't be done using
 * inheritance
 * 
 * @author Mark Pollack
 */
public abstract class ModuleDefinitionRepositoryUtils {

	public static void saveDependencies(ModuleDependencyRepository moduleDependencyRepository, ModuleDefinition source,
			String target) {
		moduleDependencyRepository.store(source.getName(), source.getType(), target);
		if (source.isComposed()) {
			List<ModuleDefinition> composedModuleDefinitions = source.getComposedModuleDefinitions();
			for (ModuleDefinition child : composedModuleDefinitions) {
				saveDependencies(moduleDependencyRepository, child, target);
			}
		}
	}

	public static void deleteDependencies(ModuleDependencyRepository moduleDependencyRepository,
			ModuleDefinition source,
			String target) {
		moduleDependencyRepository.delete(source.getName(), source.getType(), target);
		if (source.isComposed()) {
			List<ModuleDefinition> composedModuleDefinitions = source.getComposedModuleDefinitions();
			for (ModuleDefinition child : composedModuleDefinitions) {
				deleteDependencies(moduleDependencyRepository, child, target);
			}
		}
	}
}
