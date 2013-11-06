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

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.xd.module.ModuleType;


/**
 * Used to compute and track dependencies between modules and other elements of the system.
 * 
 * @author Eric Bottard
 */
public class ModuleDependencyTracker {

	private final ModuleDependencyRepository dependencyRepository;

	@Autowired
	public ModuleDependencyTracker(ModuleDependencyRepository dependencyRepository) {
		this.dependencyRepository = dependencyRepository;
	}

	public void record(ModuleDeploymentRequest source, String target) {
		dependencyRepository.store(source.getModule(), source.getType(), target);
		if (source instanceof CompositeModuleDeploymentRequest) {
			CompositeModuleDeploymentRequest composed = (CompositeModuleDeploymentRequest) source;
			for (ModuleDeploymentRequest child : composed.getChildren()) {
				record(child, target);
			}
		}
	}


	/**
	 * Return the set of things that depend on the given module.
	 * 
	 * @return a set of Strings of the form {@code type:name}, never {@code null}
	 */
	public Set<String> findDependents(String name, ModuleType type) {
		return dependencyRepository.findDependents(name, type);
	}

	/**
	 * @param request
	 * @param dependencyKey
	 */
	public void remove(ModuleDeploymentRequest source, String target) {
		dependencyRepository.delete(source.getModule(), source.getType(), target);
		if (source instanceof CompositeModuleDeploymentRequest) {
			CompositeModuleDeploymentRequest composed = (CompositeModuleDeploymentRequest) source;
			for (ModuleDeploymentRequest child : composed.getChildren()) {
				remove(child, target);
			}
		}
	}
}
