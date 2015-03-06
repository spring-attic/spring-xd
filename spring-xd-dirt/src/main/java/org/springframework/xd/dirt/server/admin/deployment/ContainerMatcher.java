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

package org.springframework.xd.dirt.server.admin.deployment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.cluster.Container;
import org.springframework.xd.dirt.cluster.ContainerFilter;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;

/**
 * Implementation of a Container matching strategy that returns a collection of containers to deploy a
 * {@link ModuleDescriptor} to. This implementation examines the deployment properties for a stream to determine
 * the preferences for each individual module. The deployment properties can (optionally) specify two preferences:
 * <em>criteria</em> and <em>count</em>.
 * <p/>
 * The criteria indicates that a module should only be deployed to a container for which the criteria evaluates to
 * {@code true}. The criteria value should be a valid SpEL expression. If a criteria value is not provided, any
 * container can deploy the module.
 * <p/>
 * If a count for a module is not specified, by default one instance of that module will be deployed to one container. A
 * count of 0 indicates that all containers for which the criteria evaluates to {@code true} should deploy the module.
 * If no criteria expression is specified, all containers will deploy the module.
 * <p/>
 * In cases where all containers are not deploying a module, an attempt at container round robin distribution for module
 * deployments will be made (but not guaranteed).
 *
 * @author Patrick Peralta
 * @author Mark Fisher
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 */
public class ContainerMatcher {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ContainerMatcher.class);

	/**
	 * Current index for iterating over containers.
	 */
	private int index;

	/**
	 * Parser for criteria expressions.
	 */
	private final SpelExpressionParser expressionParser = new SpelExpressionParser();

	/**
	 * Evaluation context for criteria expressions.
	 */
	private final StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

	/**
	 * Collection of {@link ContainerFilter}s to apply to the candidate Containers.
	 */
	private final Collection<ContainerFilter> containerFilters;


	/**
	 * Creates a container matcher instance and prepares the SpEL evaluation context to support Map properties directly.
	 */
	public ContainerMatcher() {
		this(new ContainerFilter[0]);
	}

	/**
	 * Creates a container matcher instance with the provided {@link ContainerFilter}s
	 * and prepares the SpEL evaluation context to support Map properties directly.
	 */
	public ContainerMatcher(ContainerFilter... containerFilters) {
		this.containerFilters = (containerFilters != null)
				? Collections.unmodifiableList(Arrays.asList(containerFilters))
				: Collections.<ContainerFilter>emptyList();
		evaluationContext.addPropertyAccessor(new MapAccessor());
	}


	/**
	 * Matches the provided module against one of the candidate containers.
	 *
	 * @param moduleDescriptor      the module to match against
	 * @param deploymentProperties  deployment properties for the module; this provides
	 *                              hints such as the number of containers and other
	 *                              matching criteria
	 * @param containers            iterable list of containers to match against
	 *
	 * @return a collection of matched containers; collection is empty if no suitable containers are found
	 */
	public Collection<Container> match(ModuleDescriptor moduleDescriptor,
			ModuleDeploymentProperties deploymentProperties, Iterable<Container> containers) {
		Assert.notNull(moduleDescriptor, "'moduleDescriptor' cannot be null.");
		Assert.notNull(deploymentProperties, "'deploymentProperties' cannot be null.");
		Assert.notNull(containers, "'containers' cannot be null.");
		logger.debug("Matching containers for module {}", moduleDescriptor);
		String criteria = deploymentProperties.getCriteria();
		List<Container> candidates = findAllContainersMatchingCriteria(containers, criteria);
		Collection<Container> filteredContainers = applyFilters(moduleDescriptor, candidates);
		List<Container> results = new ArrayList<Container>(filteredContainers);
		if (results.isEmpty() && StringUtils.hasText(criteria)) {
			logger.warn("No currently available containers match deployment criteria '{}' for module '{}'.", criteria,
					moduleDescriptor.getModuleName());
		}
		return distributeForRequestedCount(results, deploymentProperties.getCount());
	}

	/**
	 * Apply all the available {@link ContainerFilter}s.
	 *
	 * @param moduleDescriptor the module descriptor for the module
	 * @param candidates the collection of available containers that match the selection criteria
	 * @return the container candidates after applying the {@link ContainerFilter}s
	 */
	private Collection<Container> applyFilters(ModuleDescriptor moduleDescriptor, Collection<Container> candidates) {
		for (ContainerFilter containerFilter : containerFilters) {
			candidates = containerFilter.filterContainers(moduleDescriptor, candidates);
		}
		return candidates;
	}

	/**
	 * Select a subset of containers to satisfy the requested number of module instances using a round robin
	 * distribution for successive calls. A count of 0 means all members that matched the criteria expression. count >=
	 * candidates means each of the candidates should host a module.
	 *
	 * @param candidates the list of available containers that match the selection criteria
	 * @param count the requested number of module instances to deploy
	 * @return a subset of candidates <= count
	 */
	private Collection<Container> distributeForRequestedCount(List<Container> candidates, int count) {
		int candidateCount = candidates.size();
		if (candidateCount == 0) {
			return candidates;
		}

		if (count <= 0 || count >= candidateCount) {

			return candidates;
		}
		else if (count == 1) {
			return Collections.singleton(candidates.get(getAndRotateIndex(candidateCount)));
		}
		else {
			// create a new list with the specific number of targeted containers;
			List<Container> targets = new ArrayList<Container>();
			while (targets.size() < count) {
				targets.add(candidates.get(getAndRotateIndex(candidateCount)));

			}
			return targets;
		}
	}

	/**
	 * Test all containers in the containerRepository against selection criteria
	 *
	 * @param containers the containers of Iterator type to match against
	 * @param criteria an optional SpEL expression evaluated against container attribute values
	 *
	 * @return the list of containers matching the criteria
	 */
	private List<Container> findAllContainersMatchingCriteria(Iterable<Container> containers, String criteria) {
		if (StringUtils.hasText(criteria)) {
			logger.debug("Matching containers for criteria '{}'", criteria);
		}

		List<Container> candidates = new ArrayList<Container>();

		for (Container container : containers) {
			logger.trace("Evaluating container {}", container);
			if (StringUtils.isEmpty(criteria) || isCandidate(container, criteria)) {
				logger.trace("\tAdded container {}", container);
				candidates.add(container);
			}
		}
		return candidates;
	}

	/**
	 * Evaluate the criteria expression against the attributes of the provided container to see if it is a candidate for
	 * module deployment.
	 *
	 * @param container the container instance whose attributes should be considered
	 * @param criteria the criteria expression to evaluate against the container attributes
	 * @return whether the container is a candidate
	 */
	private boolean isCandidate(Container container, String criteria) {
		try {
			return expressionParser.parseExpression(criteria).getValue(evaluationContext, container.getAttributes(),
					Boolean.class);
		}
		catch (SpelEvaluationException e) {
			if (e.getMessageCode().equals(SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE)) {
				logger.debug("candidate does not contain an attribute referenced in the criteria {}", criteria);
			}
			return false;
		}
		catch (EvaluationException e) {
			logger.debug("candidate not a match due to evaluation exception", e);
			return false;
		}
	}

	/**
	 * Rotate the cached index over the number of available containers.
	 *
	 * @param availableContainerCount the number of available containers
	 * @return the current count before rotating
	 */
	private synchronized int getAndRotateIndex(int availableContainerCount) {
		if (availableContainerCount <= 0) {
			return 0;
		}
		int i = index % availableContainerCount;
		index = i + 1;
		return i;
	}

}
