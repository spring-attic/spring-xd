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

package org.springframework.xd.dirt.core;

import java.util.List;
import java.util.Map;

import org.springframework.xd.module.ModuleDescriptor;

/**
 * {@code DeploymentUnit} is a collection of modules that are intended to be
 * deployed as a single unit.
 *
 * @author Patrick Peralta
 */
public interface DeploymentUnit {

	/**
	 * The name for this deployment unit, typically used as a unique identifier
	 * for this deployment unit type.
	 *
	 * @return deployment unit name
	 */
	String getName();

	/**
	 * List of modules that comprise this deployment unit.
	 *
	 * @return list of modules
	 */
	List<ModuleDescriptor> getModuleDescriptors();

	/**
	 * Properties used to indicate deployment information for this deployment unit.
	 *
	 * @return deployment properties
	 */
	Map<String, String> getDeploymentProperties();
}
