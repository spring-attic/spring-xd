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

package org.springframework.xd.dirt.module;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;

/**
 * Provides common behavior for {@link ModuleRegistry} and knows how to handle custom classpath population for storages
 * that support it.
 * 
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Gary Russell
 */
public abstract class AbstractModuleRegistry implements ModuleRegistry {

	@Override
	public ModuleDefinition findDefinition(String name, ModuleType moduleType) {
		Resource resource = this.locateApplicationContext(name, moduleType);
		if (resource == null) {
			return null;
		}
		URL[] classpath = maybeLocateClasspath(resource, name, moduleType);
		ModuleDefinition module = new ModuleDefinition(name, moduleType, resource, classpath);
		// TODO: add properties from a property registry
		return module;
	}

	@Override
	public List<ModuleDefinition> findDefinitions(String name) {
		ArrayList<ModuleDefinition> definitions = new ArrayList<ModuleDefinition>();
		for (ModuleType type : ModuleType.values()) {
			ModuleDefinition definition = findDefinition(name, type);
			if (definition != null) {
				definitions.add(definition);
			}
		}
		return definitions;
	}

	@Override
	public List<ModuleDefinition> findDefinitions(ModuleType type) {
		ArrayList<ModuleDefinition> results = new ArrayList<ModuleDefinition>();
		for (Resource resource : locateApplicationContexts(type)) {
			String name = inferModuleName(resource);
			results.add(new ModuleDefinition(name, type, resource, maybeLocateClasspath(resource, name,
					type)));
		}
		return results;
	}

	/**
	 * Recover the name of the module, given its resource location.
	 */
	protected abstract String inferModuleName(Resource resource);

	@Override
	public List<ModuleDefinition> findDefinitions() {
		ArrayList<ModuleDefinition> results = new ArrayList<ModuleDefinition>();
		for (ModuleType type : ModuleType.values()) {
			results.addAll(findDefinitions(type));
		}
		return results;
	}

	/**
	 * Return a resource pointing to an {@link ApplicationContext} XML definition file.
	 */
	protected abstract Resource locateApplicationContext(String name, ModuleType type);

	/**
	 * Return a list of resources pointing to {@link ApplicationContext} XML definition files.
	 */
	protected abstract List<Resource> locateApplicationContexts(ModuleType type);

	/**
	 * Return an array of jar files locations or {@code null} if the module is a plain xml file. Default implementation
	 * returns {@code null} system.
	 */
	protected URL[] maybeLocateClasspath(Resource resource, String name, ModuleType moduleType) {
		return null;
	}

}
