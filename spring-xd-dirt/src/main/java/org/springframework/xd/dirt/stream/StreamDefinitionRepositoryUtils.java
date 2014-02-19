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

package org.springframework.xd.dirt.stream;

import java.util.List;

import org.springframework.util.Assert;
import org.springframework.xd.dirt.module.ModuleDependencyRepository;
import org.springframework.xd.module.ModuleDefinition;


/**
 * Encapsulate shared functionality between implementations of StreamDefinitionRepository that can't be done using
 * inheritance
 * 
 * @author Mark Pollack
 */
public abstract class StreamDefinitionRepositoryUtils {

	/**
	 * Save the dependencies of each module to its containing stream.
	 * 
	 * @param moduleDependencyRepository finds and stores module dependencies
	 * @param streamDefinition the stream definition.
	 */
	public static void saveDependencies(ModuleDependencyRepository moduleDependencyRepository,
			StreamDefinition streamDefinition) {
		validate(moduleDependencyRepository, streamDefinition);
		List<ModuleDefinition> moduleDefinitions = streamDefinition.getModuleDefinitions();

		for (ModuleDefinition moduleDefinition : moduleDefinitions) {
			moduleDependencyRepository.store(moduleDefinition.getName(), moduleDefinition.getType(),
					"stream:" + streamDefinition.getName());
		}
	}

	/**
	 * Delete the dependencies of each module to its containing stream.
	 * 
	 * @param moduleDependencyRepository deletes module dependencies
	 * @param streamDefinition the stream definition.
	 */
	public static void deleteDependencies(ModuleDependencyRepository moduleDependencyRepository,
			StreamDefinition streamDefinition) {
		validate(moduleDependencyRepository, streamDefinition);
		List<ModuleDefinition> moduleDefinitions = streamDefinition.getModuleDefinitions();
		for (ModuleDefinition moduleDefinition : moduleDefinitions) {
			moduleDependencyRepository.delete(moduleDefinition.getName(), moduleDefinition.getType(),
					"stream:" + streamDefinition.getName());
		}
	}


	private static void validate(ModuleDependencyRepository moduleDependencyRepository,
			StreamDefinition streamDefinition) {
		Assert.notNull(moduleDependencyRepository, "No ModuleDependencyRepository specified");
		Assert.notNull(streamDefinition, "No StreamDefinition specified");
	}

}
