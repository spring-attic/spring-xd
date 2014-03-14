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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.xd.dirt.core.ModuleDescriptor;

/**
 * Implementation of {@link ContainerMatcher} that returns a collection of containers to deploy a
 * {@link ModuleDescriptor} to. This implementation examines the deployment manifest for a stream to determine the
 * preferences for each individual module. The deployment manifest can (optionally) specify two preferences:
 * <em>group</em> and <em>count</em>.
 * <p/>
 * A group indicates that a module should only be deployed to a container that belongs to a group. If a group preference
 * is not indicated, any container can deploy the module.
 * <p/>
 * If a count for a module is not specified, by default one instance of that module will be deployed to one container. A
 * count of 0 indicates that all containers in its specified group will deploy it. If no group is specified, all
 * containers will deploy the module.
 * <p/>
 * In cases where all containers are not deploying a module, an attempt at container round robin distribution for module
 * deployments will be made (but not guaranteed).
 * 
 * @author Patrick Peralta
 * @author Mark Fisher
 */
public class DefaultContainerMatcher implements ContainerMatcher {

	/**
	 * Logger.
	 */
	private static final Logger LOG = LoggerFactory.getLogger(DefaultContainerMatcher.class);

	/**
	 * Current index for iterating over containers.
	 */
	private int index = 0;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<Container> match(ModuleDescriptor moduleDescriptor, ContainerRepository containerRepository) {
		LOG.debug("Matching containers for module {}", moduleDescriptor);

		String group = moduleDescriptor.getGroup();
		List<Container> candidates = new ArrayList<Container>();

		for (Iterator<Container> iterator = containerRepository.getContainerIterator(); iterator.hasNext();) {
			Container container = iterator.next();
			LOG.trace("Evaluating container {}", container);
			if (group == null || container.getGroups().contains(group)) {
				LOG.trace("\tAdded container {}", container);
				candidates.add(container);
			}
		}

		if (candidates.isEmpty()) {
			// there are no containers available
			return candidates;
		}

		int count = moduleDescriptor.getCount();
		int candidateCount = candidates.size();
		if (count <= 0 || count >= candidateCount) {
			// count of 0 means all members of the group
			// (if no group specified it means all containers);
			// count >= candidates means each of the
			// containers should host a module
			return candidates;
		}
		else if (count == 1) {
			if (index + 1 > candidateCount) {
				index = 0;
			}
			return Collections.singleton(candidates.get(index++));
		}
		else {
			// create a new list with the specific number
			// of targeted containers;
			List<Container> targets = new ArrayList<Container>();
			while (targets.size() < count) {
				if (index + 1 > candidateCount) {
					index = 0;
				}
				targets.add(candidates.get(index++));
			}
			return targets;
		}
	}

}
