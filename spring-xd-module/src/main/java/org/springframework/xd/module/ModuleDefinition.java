/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.module;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Defines a module.
 *
 * @author Gary Russell
 * @author Eric Bottard
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class ModuleDefinition implements Comparable<ModuleDefinition> {

	private volatile String name;

	private volatile ModuleType type;

	private final Resource resource;

	private volatile String definition;

	/**
	 * If a composed module, the list of modules
	 */
	private List<ModuleDefinition> composedModuleDefinitions = new ArrayList<ModuleDefinition>();

	@SuppressWarnings("unused")
	private ModuleDefinition() {
		// no arg constructor for Jackson serialization
		// JSON serialization ignores the resource, so set it here to a value.
		resource = new DescriptiveResource("Dummy resource");
	}

	public ModuleDefinition(String name, ModuleType moduleType) {
		this(name, moduleType, new DescriptiveResource("Dummy resource"));
	}

	public ModuleDefinition(String name, ModuleType type, Resource resource) {
		this(name, type, resource, null);
	}

	public ModuleDefinition(String name, ModuleType type, Resource resource, URL[] classpath) {
		Assert.hasLength(name, "name cannot be blank");
		Assert.notNull(type, "type cannot be null");
		Assert.notNull(resource, "resource cannot be null");
		this.resource = resource;
		this.name = name;
		this.type = type;
	}

	/**
	 * Determine if this a composed module
	 *
	 * @return true if this is a composed module, false otherwise.
	 */
	public boolean isComposed() {
		return !CollectionUtils.isEmpty(this.composedModuleDefinitions);
	}

	/**
	 * Set the list of composed modules if this is a composite module, can not be null
	 *
	 * @param composedModuleDefinitions list of composed modules
	 */
	public void setComposedModuleDefinitions(List<ModuleDefinition> composedModuleDefinitions) {
		Assert.notNull(composedModuleDefinitions, "composedModuleDefinitions cannot be null");
		this.composedModuleDefinitions = composedModuleDefinitions;
	}

	public List<ModuleDefinition> getComposedModuleDefinitions() {
		return composedModuleDefinitions;
	}

	public String getName() {
		return name;
	}

	public ModuleType getType() {
		return type;
	}

	public Resource getResource() {
		return resource;
	}

	public String getDefinition() {
		return definition;
	}

	public void setDefinition(String definition) {
		this.definition = definition;
	}

	/**
	 * Compares the module definitions using the name of the {@link ModuleDefinition}
	 */
	@Override
	public int compareTo(ModuleDefinition other) {
		return this.getName().compareTo(other.getName());
	}

}
