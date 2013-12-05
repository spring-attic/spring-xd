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

package org.springframework.xd.dirt.core;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;
import org.springframework.xd.module.ModuleDefinition;

/**
 * Represents the base model for a data flow in the system. Every Spring XD definition is represented by at least a
 * <code>name</code (which also uniquely identifies the definition). Each definition also has a <code>definition</code>
 * property which contains a String value representing the DSL to create the respective definition.
 * 
 * @author Gunnar Hillert
 * @since 1.0
 */
public abstract class BaseDefinition implements Comparable<BaseDefinition> {

	/**
	 * A String representation of the desired data flow, expressed in the XD DSL.
	 */
	private String definition;

	/**
	 * A unique name given to that Spring XD definition.
	 */
	private String name;

	/**
	 * The list of modules that are derived from parsing the definition
	 */
	private List<ModuleDefinition> moduleDefinitions = new ArrayList<ModuleDefinition>();

	protected BaseDefinition() {
		// no-arg constructor for serialization
	}

	public BaseDefinition(String name, String definition) {
		Assert.hasText(name, "Name can not be empty");
		Assert.hasText(definition, "Definition can not be empty");
		this.name = name;
		this.definition = definition;
	}

	/**
	 * Returns a String representation of the desired data flow, expressed in the XD DSL.
	 */
	public String getDefinition() {
		return definition;
	}

	/**
	 * Returns a unique name given to that stream definition.
	 */
	public String getName() {
		return name;
	}

	public void setModuleDefinitions(List<ModuleDefinition> moduleDefinitions) {
		this.moduleDefinitions = moduleDefinitions;
	}

	public List<ModuleDefinition> getModuleDefinitions() {
		return this.moduleDefinitions;
	}

	@Override
	public String toString() {
		return "BaseDefinition [definition=" + definition + ", name=" + name
				+ "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((definition == null) ? 0 : definition.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BaseDefinition other = (BaseDefinition) obj;
		if (definition == null) {
			if (other.definition != null)
				return false;
		}
		else if (!definition.equals(other.definition))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		}
		else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public int compareTo(BaseDefinition otherBaseDefinition) {
		return this.name.compareTo(otherBaseDefinition.name);
	}

}
