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

package org.springframework.xd.dirt.server.admin.deployment;

import java.util.HashMap;
import java.util.Map;

import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.RuntimeModuleDeploymentProperties;


/**
 * Default implementation of {@link ModuleDeploymentPropertiesProvider}
 * for type {@link RuntimeModuleDeploymentProperties}.
 *
 * @author Ilayaperumal Gopinathan
 */
public class RuntimeModuleDeploymentPropertiesProvider implements
		ModuleDeploymentPropertiesProvider<RuntimeModuleDeploymentProperties> {

	/**
	 * Map to keep track of how many instances of a module this provider
	 * has generated properties for. This is used to generate a unique
	 * id for each module deployment per container for stream partitioning.
	 */
	private final Map<ModuleDescriptor.Key, Integer> mapModuleCount = new HashMap<ModuleDescriptor.Key, Integer>();

	/**
	 * The {@link ModuleDeploymentProperties} provider for a {@link ModuleDescriptor}
	 */
	protected final ModuleDeploymentPropertiesProvider<ModuleDeploymentProperties> deploymentPropertiesProvider;

	/**
	 * Construct a {@code DefaultRuntimeModuleDeploymentPropertiesProvider}.
	 *
	 * @param propertiesProvider the module deployment properties provider
	 */
	public RuntimeModuleDeploymentPropertiesProvider(
			ModuleDeploymentPropertiesProvider<ModuleDeploymentProperties> propertiesProvider) {
		this.deploymentPropertiesProvider = propertiesProvider;
	}

	/**
	 * Return the runtime deployment properties for the given module descriptor.
	 * Currently, this implementation assigns the module sequence for the given descriptor and add it to
	 * the runtime deployment properties.
	 */
	@Override
	public RuntimeModuleDeploymentProperties propertiesForDescriptor(ModuleDescriptor moduleDescriptor) {
		RuntimeModuleDeploymentProperties properties = new RuntimeModuleDeploymentProperties();

		properties.putAll(deploymentPropertiesProvider.propertiesForDescriptor(moduleDescriptor));

		ModuleDescriptor.Key moduleKey = moduleDescriptor.createKey();
		Integer index = mapModuleCount.get(moduleKey);
		if (index == null) {
			index = 0;
		}
		mapModuleCount.put(moduleKey, index + 1);
		// sequence number only applies if count > 0
		properties.setSequence((properties.getCount() == 0) ? 0 : mapModuleCount.get(moduleKey));
		return properties;
	}
}
