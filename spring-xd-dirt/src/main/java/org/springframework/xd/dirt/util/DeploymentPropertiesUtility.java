/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.xd.dirt.util;

import java.util.Map;

import org.springframework.xd.dirt.core.Job;
import org.springframework.xd.dirt.core.Stream;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;


/**
 * Class that provides utility methods to handle module deployment properties.
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 * @author Eric Bottard
 */
public class DeploymentPropertiesUtility {


	/**
	 * Based on the deployment properties for a {@link Stream}/{@link Job}, create an instance of
	 * {@link org.springframework.xd.module.ModuleDeploymentProperties} for a specific module
	 * in the {@link Stream}/{@link Job}.
	 *
	 * @param deploymentProperties deployment properties for a stream/job
	 * @param descriptor descriptor for module in the stream for which to create the properties
	 * @return deployment properties for the module
	 */
	public static ModuleDeploymentProperties createModuleDeploymentProperties(
			Map<String, String> deploymentProperties, ModuleDescriptor descriptor) {
		ModuleDeploymentProperties moduleDeploymentProperties = new ModuleDeploymentProperties();
		// first add properties that should apply to all modules unless overridden
		String wildcardPrefix = "module.*.";
		for (Map.Entry<String, String> prop : deploymentProperties.entrySet()) {
			String key = prop.getKey();
			if (key.startsWith(wildcardPrefix)) {
				moduleDeploymentProperties.put(key.substring(wildcardPrefix.length()), prop.getValue());
			}
		}
		// now add properties that are designated for this module explicitly
		String modulePrefix = String.format("module.%s.", descriptor.getModuleLabel());
		for (Map.Entry<String, String> prop : deploymentProperties.entrySet()) {
			String key = prop.getKey();
			if (key.startsWith(modulePrefix)) {
				moduleDeploymentProperties.put(key.substring(modulePrefix.length()), prop.getValue());
			}
		}
		return moduleDeploymentProperties;
	}

}
