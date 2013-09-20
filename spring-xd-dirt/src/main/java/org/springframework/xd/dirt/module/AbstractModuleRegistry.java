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
	public ModuleDefinition findDefinition(String name, String type) {
		Resource resource = this.locateApplicationContext(name, type);
		if (resource == null) {
			return null;
		}
		URL[] classpath = maybeLocateClasspath(resource, name, type);
		ModuleDefinition module = new ModuleDefinition(name, type, resource, classpath);
		// TODO: add properties from a property registry
		return module;
	}

	/**
	 * Return an array of jar files locations or {@code null} if the module is a plain xml file. Default implementation
	 * returns {@code null}.
	 */
	protected URL[] maybeLocateClasspath(Resource resource, String name, String type) {
		return null;
	}

	@Override
	public List<ModuleDefinition> findDefinitions(String name) {
		ArrayList<ModuleDefinition> definitions = new ArrayList<ModuleDefinition>();
		for (ModuleType type : ModuleType.values()) {
			ModuleDefinition definition = findDefinition(name, type.getTypeName());
			if (definition != null) {
				definitions.add(definition);
			}
		}
		return definitions;
	}

	/**
	 * Return a resource pointing to an {@link ApplicationContext} XML definition file.
	 */
	protected abstract Resource locateApplicationContext(String name, String type);
}
