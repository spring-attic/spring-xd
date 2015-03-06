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

import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;

/**
 * Callback interface to obtain {@link org.springframework.xd.module.ModuleDeploymentProperties}
 * for a {@link org.springframework.xd.module.ModuleDescriptor}.
 *
 * @author Patrick Peralta
 */
public interface ModuleDeploymentPropertiesProvider<P extends ModuleDeploymentProperties> {

	/**
	 * Return the deployment properties of type {@link ModuleDeploymentProperties} for the module descriptor.
	 *
	 * @param descriptor module descriptor for module to be deployed
	 * @return deployment properties for module
	 */
	P propertiesForDescriptor(ModuleDescriptor descriptor);
}
