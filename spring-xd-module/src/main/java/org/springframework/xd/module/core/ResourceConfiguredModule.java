/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.module.core;

import org.springframework.core.io.Resource;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.options.ModuleOptions;

/**
 * A {@link org.springframework.xd.module.core.SimpleModule} configured using a bean definition resource (XML or
 * Groovy)
 *
 * @author David Turanski
 */
public class ResourceConfiguredModule extends SimpleModule {

	public ResourceConfiguredModule(ModuleDescriptor descriptor, ModuleDeploymentProperties deploymentProperties) {
		super(descriptor, deploymentProperties);
	}

	public ResourceConfiguredModule(ModuleDescriptor descriptor, ModuleDeploymentProperties deploymentProperties,
			ClassLoader classLoader, ModuleOptions moduleOptions) {
		super(descriptor, deploymentProperties, classLoader, moduleOptions);
	}

	//todo: change to use interpret the resource as the root module path
	// when ModuleDefinition is refactored (XD-2199)
	@Override
	protected void configureModuleApplicationContext(ModuleDefinition moduleDefinition) {
		Resource resource = moduleDefinition.getResource();
		if (resource != null && resource.exists() && resource.isReadable() &&
				(resource.getFilename().endsWith(".xml") || resource.getFilename().endsWith(".groovy"))) {
			addSource(moduleDefinition.getResource());
		}
	}
}
