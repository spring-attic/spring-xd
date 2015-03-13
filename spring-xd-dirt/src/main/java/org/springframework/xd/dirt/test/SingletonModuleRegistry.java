/*
 * Copyright 2014 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.test;

import java.util.Collections;
import java.util.List;

import org.springframework.xd.dirt.module.ModuleRegistry;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDefinitions;
import org.springframework.xd.module.ModuleType;

/**
 * An implementation of {@link org.springframework.xd.dirt.module.ModuleRegistry} suitable to lookup a module that is at
 * the root of the classpath. In a typical source-form project (such as Maven), the module definition files will reside
 * in {@code src/main/resource/} (as in {@code src/main/resources/config/foo.xml}) and as such will end up at the "root"
 * of the classpath at runtime, with no way for a "normal" registry to infer the module type (and possibly name).
 *
 * <p>This implementation is thus typically useful when writing tests for a module in the project that is defining that
 * very same project.
 * </p>
 *
 * @author David Turanski
 * @author Eric Bottard
 */
public class SingletonModuleRegistry implements ModuleRegistry {
	private final String moduleName;

	private final ModuleType moduleType;

	private final String location;

	public SingletonModuleRegistry(ModuleType moduleType, String moduleName) {
		this(moduleType, moduleName, "classpath:");
	}
	public SingletonModuleRegistry(ModuleType moduleType, String moduleName, String location) {
		this.moduleName = moduleName;
		this.moduleType = moduleType;
		this.location = location;
	}

	@Override
	public ModuleDefinition findDefinition(String name, ModuleType moduleType) {
		if (moduleName.equals(name) && this.moduleType == moduleType) {
			return ModuleDefinitions.simple(moduleName, moduleType, location);
		}
		return null;
	}

	@Override
	public List<ModuleDefinition> findDefinitions(String name) {
		if (moduleName.equals(name)) {
			return Collections.singletonList((ModuleDefinition)ModuleDefinitions.simple(moduleName, moduleType, location));
		}
		else {
			return Collections.emptyList();
		}
	}

	@Override
	public List<ModuleDefinition> findDefinitions(ModuleType type) {
		if (type == moduleType) {
			return Collections.singletonList((ModuleDefinition)ModuleDefinitions.simple(moduleName, moduleType, location));
		}
		else {
			return Collections.emptyList();
		}
	}

	@Override
	public List<ModuleDefinition> findDefinitions() {
		return Collections.singletonList((ModuleDefinition)ModuleDefinitions.simple(moduleName, moduleType, location));
	}
}
