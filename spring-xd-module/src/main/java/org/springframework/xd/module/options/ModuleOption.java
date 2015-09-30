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

package org.springframework.xd.module.options;


/**
 * Provides information about an option that a user can set to customize the behavior of a module.
 * 
 * @author Eric Bottard
 */
public class ModuleOption {

	private final String name;

	private final String description;

	private Object defaultValue;

	private boolean hidden = false;

	private String type;


	public ModuleOption(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public ModuleOption withType(Class<?> type) {
		if (type != null) {
			this.type = type.getCanonicalName();
		}
		return this;
	}

	public ModuleOption withType(String type) {
		this.type = type;
		return this;
	}

	public ModuleOption withDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
		return this;
	}

	public ModuleOption hidden(boolean hidden) {
		this.hidden = hidden;
		return this;
	}


	public boolean isHidden() {
		return hidden;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getType() {
		return type;
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ModuleOption that = (ModuleOption) o;

		if (hidden != that.hidden) return false;
		if (defaultValue != null ? !defaultValue.equals(that.defaultValue) : that.defaultValue != null) return false;
		if (description != null ? !description.equals(that.description) : that.description != null) return false;
		if (!name.equals(that.name)) return false;
		if (type != null ? !type.equals(that.type) : that.type != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + (description != null ? description.hashCode() : 0);
		result = 31 * result + (defaultValue != null ? defaultValue.hashCode() : 0);
		result = 31 * result + (hidden ? 1 : 0);
		result = 31 * result + (type != null ? type.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "ModuleOption [name=" + name + ", type=" + type + ", defaultValue=" + defaultValue + ", description="
				+ description + "]";
	}


}
