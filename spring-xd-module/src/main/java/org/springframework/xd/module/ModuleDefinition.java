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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import org.springframework.util.Assert;

/**
 * An instance of ModuleDefinition reflects the fact that a given module (identified by its type and name) is
 * 'available', <i>i.e.</i> that it can be used in a job or stream definition.
 *
 * @author Gary Russell
 * @author Eric Bottard
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public abstract class ModuleDefinition implements Comparable<ModuleDefinition> {

	private String name;

	private ModuleType type;

	protected ModuleDefinition() {
		// For (subclass) JSON deserialization only
	}

	protected ModuleDefinition(String name, ModuleType type) {
		Assert.hasLength(name, "name cannot be blank");
		Assert.notNull(type, "type cannot be null");
		this.name = name;
		this.type = type;
	}

	public static ModuleDefinition simple(String name, ModuleType type, String location) {
		return new SimpleModuleDefinition(name, type, location);
	}

	public static ModuleDefinition composed(String name, ModuleType type, String dslDefinition, List<ModuleDefinition> children) {
		return new CompositeModuleModuleDefinition(name, type, dslDefinition, children);
	}

	public static ModuleDefinition dummy(String name, ModuleType type) {
		return new SimpleModuleDefinition(name, type, "file:/tmp/dummy/location");
	}

	/**
	 * Determine if this a composed module
	 *
	 * @return true if this is a composed module, false otherwise.
	 */
	@JsonIgnore
	public abstract boolean isComposed();

	public String getName() {
		return name;
	}

	public ModuleType getType() {
		return type;
	}

	/**
	 * Compares the module definitions using the name of the {@link ModuleDefinition}
	 */
	@Override
	public int compareTo(ModuleDefinition other) {
		return this.getName().compareTo(other.getName());
	}

}
