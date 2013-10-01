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

import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;

/**
 * Simple {@link ModuleRegistry} that loads modules from the testmodules location
 * 
 * @author Jennifer Hickey
 */
public class ClasspathTestModuleRegistry extends AbstractModuleRegistry {

	private static final String MODULE_DIR = "src/test/resources/testmodules/";

	@Override
	protected Resource locateApplicationContext(String name, String type) {
		ClassPathResource classPathResource = new ClassPathResource("testmodules/" + type + "/" + name + ".xml");
		if (classPathResource.exists()) {
			return classPathResource;
		}
		return null;
	}

	@Override
	public List<ModuleDefinition> findDefinitions(ModuleType type) {
		ClassPathResource classPathResource = new ClassPathResource(MODULE_DIR);
		FileModuleRegistry registry = new FileModuleRegistry(classPathResource.getPath());

		return registry.findDefinitions(type);
	}


	@Override
	public List<ModuleDefinition> findDefinitions() {
		ClassPathResource classPathResource = new ClassPathResource(MODULE_DIR);
		FileModuleRegistry registry = new FileModuleRegistry(classPathResource.getPath());

		return registry.findDefinitions();
	}

}
