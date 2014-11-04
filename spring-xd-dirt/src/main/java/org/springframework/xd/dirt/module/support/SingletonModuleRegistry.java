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

package org.springframework.xd.dirt.module.support;

import java.io.IOException;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.module.ArchiveModuleRegistry;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDefinitions;
import org.springframework.xd.module.ModuleType;

/**
 * An implementation of {@link org.springframework.xd.dirt.module.ModuleRegistry} useful for standalone module projects
 * which implement a single module with configuration contained under a known top level directory (/config by default).
 * Any {@link org.springframework.xd.module.ModuleType} may be used to resolve module definitions as long as the
 * requested ModuleType matches the ModuleType configured for this instance. The configured root ('/' by default) must
 * be a directory and must contain 'config/[moduleName].properties
 *
 * @author David Turanski
 */
public class SingletonModuleRegistry extends ArchiveModuleRegistry {
	private String moduleName;

	private final ModuleType moduleType;

	/**
	 * Create an instance using the default root ('/').
	 *
	 * @param moduleType the expected module type
	 * @param moduleName the expected module name
	 */
	public SingletonModuleRegistry(ModuleType moduleType, String moduleName) {
		this(moduleType, moduleName, "/");

	}

	/**
	 * Create an instance using a custom root location.
	 *
	 * @param moduleType the expected module type
	 * @param moduleName the expected module name
	 * @param root must be a directory
	 */
	public SingletonModuleRegistry(ModuleType moduleType, String moduleName, String root) {
		super(root);
		Assert.notNull(moduleType, "moduleType cannot be null.");
		Assert.hasText(moduleName, "moduleName cannot be empty or null.");
		this.moduleType = moduleType;
		this.moduleName = moduleName;
	}

	/**
	 * Only return the root path if the search parameters are valid for this module. The requested module type and name
	 * must be equal to the configured values for this instance or a wild card. Also, the suffix must be an empty
	 * String
	 *
	 * @param moduleType the moduleType name
	 * @param moduleName the module name
	 * @param suffix the suffix
	 * @return the root resource
	 * @throws IOException
	 */
	@Override
	protected Resource[] getResources(String moduleType,
			String moduleName, String suffix) throws IOException {
		if ((this.moduleType.name().equals(moduleType) || "*".equals(moduleType)) &&
				(this.moduleName.equals(moduleName) || "*".equals(moduleName)) &&
				StringUtils.isEmpty(suffix)) {
			return this.resolver.getResources(this.root);
		}
		return new Resource[0];
	}

	/**
	 * Only add a {@link org.springframework.xd.module.ModuleDefinition} if the root contains
	 * config/[moduleName].properties .
	 *
	 * @param resource the requested resource (should be wired to the configured root in this case.
	 * @param holder the ModuleDefinition as a List.
	 * @throws IOException
	 */
	protected void fromResource(Resource resource, List<ModuleDefinition> holder) throws IOException {
		if (!resource.exists()) {
			return;
		}

		if (!resource.getFile().isDirectory()) {
			return;
		}

		Resource moduleConfigProperties = resolver.getResource(String.format("classpath:%sconfig/%s.properties",
				this.root,
				this.moduleName));

		Assert.isTrue(moduleConfigProperties.exists(), "required module properties file is missing: " +
				moduleConfigProperties.getURL());


		ModuleDefinition found = ModuleDefinitions.simple(this.moduleName, this.moduleType,
				moduleConfigProperties.getFilename());

		holder.add(found);
	}
}
