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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;

/**
 * @author Mark Fisher
 * @author Glenn Renfro
 */
public class FileModuleRegistry extends AbstractModuleRegistry {

	private final File directory;

	public FileModuleRegistry(String directory) {
		File f = new File(directory);
		Assert.isTrue(f.isDirectory(), "not a directory: " + f.getAbsolutePath());
		this.directory = f;
	}

	@Override
	protected Resource loadResource(String name, String type) {
		File file = new File(directory, type + File.separator + name + ".xml");
		return new FileSystemResource(file);
	}

	@Override
	public List<ModuleDefinition> findDefinitions(String name) {
		ArrayList<ModuleDefinition> definitions = new ArrayList<ModuleDefinition>();
		for (ModuleType type : ModuleType.values()) {
			Resource resource = loadResource(name, type.name());
			if (resource.exists()) {
				ModuleDefinition moduleDef = new ModuleDefinition(name, type.getTypeName(), resource);
				definitions.add(moduleDef);
			}
		}
		return definitions;
	}
}
