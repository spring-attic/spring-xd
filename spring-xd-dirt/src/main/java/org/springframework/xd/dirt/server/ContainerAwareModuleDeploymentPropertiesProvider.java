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

package org.springframework.xd.dirt.server;

import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;

/**
 * Callback interface to obtain {@link org.springframework.xd.module.ModuleDeploymentProperties}
 * for a {@link org.springframework.xd.module.ModuleDescriptor} within the context of deployment
 * to the provided {@link org.springframework.xd.dirt.cluster.Container}.
 *
 * @author Patrick Peralta
 */
public interface ContainerAwareModuleDeploymentPropertiesProvider
		extends ModuleDeploymentPropertiesProvider {

	/**
	 * Return the deployment properties for the module descriptor
	 * that are specific for deployment to the provided {@link org.springframework.xd.dirt.cluster.Container}.
	 *
	 * @param descriptor module descriptor for module to be deployed
	 * @param container  target container for deployment
	 * @return deployment properties for module
	 */
	ModuleDeploymentProperties propertiesForDescriptor(ModuleDescriptor descriptor,
			Container container);
}
