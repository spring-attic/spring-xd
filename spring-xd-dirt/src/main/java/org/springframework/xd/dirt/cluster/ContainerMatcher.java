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

package org.springframework.xd.dirt.cluster;

import java.util.Collection;

import org.springframework.xd.dirt.core.ModuleDeploymentProperties;
import org.springframework.xd.dirt.module.ModuleDescriptor;

/**
 * Strategy interface for matching a {@link org.springframework.xd.dirt.module.ModuleDescriptor}
 * to one of the candidate container nodes.
 *
 * @author Mark Fisher
 */
public interface ContainerMatcher {

	/**
	 * Matches the provided module against one of the candidate containers.
	 *
	 * @param moduleDescriptor      the module to match against
	 * @param deploymentProperties  deployment properties for the module; this provides
	 *                              hints such as the number of containers and other
	 *                              matching criteria
	 * @param containerRepository   the container repository that provides the ability
	 *                              to look up containers
	 *
	 * @return a collection of matched containers; collection is empty if no suitable containers are found
	 */
	Collection<Container> match(ModuleDescriptor moduleDescriptor,
			ModuleDeploymentProperties deploymentProperties, ContainerRepository containerRepository);

}
