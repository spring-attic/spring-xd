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

import org.springframework.util.Assert;


/**
 * Provides information about an option that a user can set to customize the behavior of a module.
 * 
 * @author Eric Bottard
 */
public class ModuleOption {

	private String name;

	private String description;

	private String spel;

	private Class<?> type = String.class;


	private ModuleOption(String name) {
		this.name = name;
	}

	public static ModuleOption named(String name) {
		return new ModuleOption(name);
	}

	public ModuleOption withHelp(String description) {
		this.description = description;
		return this;
	}

	public ModuleOption withDefaultValue(String value) {
		this.spel = "'" + value + "'";
		return this;
	}

	public ModuleOption withDefaultExpression(String expression) {
		this.spel = expression;
		return this;
	}

	public ModuleOption withType(Class<?> type) {
		this.type = type;
		return this;
	}

	public String getName() {
		return name;
	}


	public String getDescription() {
		return description;
	}


	public String getSpel() {
		return spel;
	}


	public Class<?> getType() {
		return type;
	}


	public void setDescription(String description) {
		this.description = description;
	}


	public void setDefaultValue(String value) {
		withDefaultValue(value);
	}

	public void setDefaultExpression(String expression) {
		withDefaultExpression(expression);
	}

	public void setType(Class<?> type) {
		Assert.notNull(type, "Option type cannot be null");
		this.type = type;
	}

	@Override
	public String toString() {
		return String.format("ModuleOption[%s]", name);
	}

}
