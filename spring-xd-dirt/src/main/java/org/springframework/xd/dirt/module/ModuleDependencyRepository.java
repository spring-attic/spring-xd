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

import java.util.Set;

import org.springframework.xd.module.ModuleType;


/**
 * Used to track usage of modules from streams and composed modules.
 * 
 * @author Eric Bottard
 */
public interface ModuleDependencyRepository {

	/**
	 * Store one atomic dependency from a module (composed or not) to some target (stream, or composed module).
	 */
	void store(String moduleName, ModuleType type, /* BaseDefinition? */String target);

	/**
	 * Return the set of things that depend on the given module.
	 * 
	 * @return a set of Strings of the form {@code type:name}, never {@code null}
	 */
	Set</* BaseDefinition? */String> find(String name, ModuleType type);

	/**
	 * Remove an atomic dependency from a module (composed or not) to some target (stream, or composed module).
	 */
	void delete(String module, ModuleType type, /* BaseDefinition? */String target);

}
