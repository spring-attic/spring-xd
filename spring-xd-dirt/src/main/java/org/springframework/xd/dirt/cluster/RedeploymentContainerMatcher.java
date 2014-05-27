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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import org.springframework.util.Assert;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;

/**
 * Wrapper for {@link org.springframework.xd.dirt.cluster.ContainerMatcher}
 * used for redeployment of modules when a container exits the cluster.
 * This matcher provides the following functionality:
 * <ol>
 *     <li>only one container is returned</li>
 *     <li>containers whose names are present in the provided
 *         {@code exclusions} collection are not returned;
 *         these containers are presumed to have already
 *         deployed the module</li>
 * </ol>
 *
 * @author Patrick Peralta
 */
public class RedeploymentContainerMatcher implements ContainerMatcher {

	/**
	 * Wrapped {@link org.springframework.xd.dirt.cluster.ContainerMatcher}.
	 */
	private final ContainerMatcher containerMatcher;

	/**
	 * Set of container names that should <b>not</b> be returned by {@link #match}.
	 */
	private final Set<String> exclusions;

	/**
	 * Predicate used to determine if a container should be returned by {@link #match}.
	 */
	private final MatchingPredicate matchingPredicate;

	/**
	 * Construct an {@code ExcludingContainerMatcher}.
	 *
	 * @param containerMatcher  container matcher to wrap
	 * @param exclusions        collection of container names to exclude
	 */
	public RedeploymentContainerMatcher(ContainerMatcher containerMatcher, Collection<String> exclusions) {
		Assert.notNull(containerMatcher);
		Assert.notNull(exclusions);
		this.containerMatcher = containerMatcher;
		this.exclusions = Collections.unmodifiableSet(new HashSet<String>(exclusions));
		this.matchingPredicate = new MatchingPredicate();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<Container> match(ModuleDescriptor moduleDescriptor,
			ModuleDeploymentProperties deploymentProperties, Iterable<Container> containers) {

		Collection<Container> matches = containerMatcher.match(moduleDescriptor,
				deploymentProperties, Iterables.filter(containers, matchingPredicate));

		return (matches.size() == 0)
				? matches
				: Collections.singleton(matches.iterator().next());
	}

	/**
	 * Predicate used to determine if a container should be returned by {@link #match}.
	 */
	private class MatchingPredicate implements Predicate<Container> {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean apply(Container input) {
			return input != null && (!exclusions.contains(input.getName()));
		}
	}

}
