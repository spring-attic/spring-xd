/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.module;

import org.springframework.xd.module.ModuleType;


/**
 * Thrown when trying to create a new module with the given name and type, but one already exists.
 *
 * @author Eric Bottard
 */
@SuppressWarnings("serial")
public class ModuleAlreadyExistsException extends ModuleException {

	private final String name;

	private final ModuleType type;

	/**
	 * Construct a new exception with the given parameters. The message template will be interpolated using
	 * {@link String#format(String, Object...)} with the module name and type being the first and second replacements.
	 */
	public ModuleAlreadyExistsException(String messageTemplate, String name, ModuleType type) {
		super(String.format(messageTemplate, name, type));
		this.name = name;
		this.type = type;
	}

	public ModuleAlreadyExistsException(String name, ModuleType type) {
		this("There is already a module named '%s' with type '%s'", name, type);
	}

	public String getName() {
		return name;
	}

	public ModuleType getType() {
		return type;
	}

}
