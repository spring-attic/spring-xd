/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.xd.shell.converter;

import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.Completion;
import org.springframework.shell.core.Converter;
import org.springframework.shell.core.MethodTarget;
import org.springframework.shell.core.SimpleParser;
import org.springframework.shell.support.logging.HandlerUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.xd.rest.client.SpringXDOperations;
import org.springframework.xd.rest.domain.DeployableResource;
import org.springframework.xd.rest.domain.NamedResource;
import org.springframework.xd.shell.XDShell;


/**
 * A no-op converter from String to String, but that helps with completion in names of already existing entities.
 * 
 * @author Eric Bottard
 * @author Glenn Renfro
 */
@Component
public class ExistingXDEntityConverter implements Converter<String> {

	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleParser.class);

	@Autowired
	private XDShell xdShell;

	/**
	 * Option Context of arguments that want to benefit from this converter should have this suffix present, with the
	 * appended part representing the kind of entity they're interested in (<i>e.g.</i> {@code existing-stream}).
	 */
	private static final String EXISTING_PREFIX = "existing-";

	@Override
	public boolean supports(Class<?> type, String optionContext) {
		return String.class.isAssignableFrom(type)
				&& (optionContext != null && optionContext.contains(EXISTING_PREFIX));
	}

	@Override
	public String convertFromText(String value, Class<?> targetType, String optionContext) {
		return value;
	}

	@Override
	public boolean getAllPossibleValues(List<Completion> completions, Class<?> targetType, String existingData,
			String optionContext, MethodTarget target) {
		String kind = determineKind(optionContext);
		SpringXDOperations springXDOperations = xdShell.getSpringXDOperations();
		DeployedCriteria criteria = DeployedCriteria.parse(optionContext);

		boolean kindSupported = true;
		try {
			if ("stream".equals(kind)) {
				populate(completions, springXDOperations.streamOperations().list(), criteria, "Streams");
			}
			else if ("job".equals(kind)) {
				populate(completions, springXDOperations.jobOperations().list(), criteria, "Jobs");
			}
			else if ("counter".equals(kind)) {
				populate(completions, springXDOperations.counterOperations().list(), criteria, "Counters");
			}
			else if ("fvc".equals(kind)) {
				populate(completions, springXDOperations.fvcOperations().list(), criteria, "Field Value Counters");
			}
			else if ("gauge".equals(kind)) {
				populate(completions, springXDOperations.gaugeOperations().list(), criteria, "Gauges");
			}
			else if ("rich-gauge".equals(kind)) {
				populate(completions, springXDOperations.richGaugeOperations().list(), criteria, "Rich Gauges");
			}
			else if ("aggregate-counter".equals(kind)) {
				populate(completions, springXDOperations.aggrCounterOperations().list(), criteria, "Aggregate Counters");
			}
			else {
				kindSupported = false;
			}
		}
		catch (Exception e) {
			LOGGER.warn(String.format("Completion unavailable (%s)", e.getMessage()));
			return false;
		}
		Assert.isTrue(kindSupported, "Unsupported kind: " + kind);
		return true;
	}

	private void populate(List<Completion> completions, Iterable<? extends NamedResource> resources,
			DeployedCriteria criteria, String kind) {
		for (NamedResource named : resources) {
			if (criteria.matches(named)) {
				completions.add(new Completion(named.getName(), named.getName(), criteria.heading(named, kind), 0));
			}
		}
	}

	/**
	 * Additional criteria that may be passed in the option context to allow restriction on deployed, undeployed or all
	 * entities.
	 * 
	 * @author Eric Bottard
	 */
	private static enum DeployedCriteria {
		/**
		 * Whether to only display deployed instances. Fails when info is not available.
		 */
		deployed {

			@Override
			boolean matches(NamedResource resource) {
				DeployableResource deployable = (DeployableResource) resource;
				return deployed.toString().equals(deployable.getStatus());
			}

			@Override
			String heading(NamedResource resource, String kind) {
				// No need to split, there is only one group
				return null;
			}
		},

		/**
		 * Whether to only display undeployed instances. Fails when info is not available.
		 */
		undeployed {

			@Override
			boolean matches(NamedResource resource) {
				DeployableResource deployable = (DeployableResource) resource;
				return undeployed.toString().equals(deployable.getStatus());
			}

			@Override
			String heading(NamedResource resource, String kind) {
				// No need to split, there is only one group
				return null;
			}
		},

		/**
		 * Display all instances. Groups instances in two categories when info is available.
		 */
		all {

			@Override
			boolean matches(NamedResource resource) {
				return true;
			}

			@Override
			String heading(NamedResource resource, String kind) {
				if (resource instanceof DeployableResource) {
					DeployableResource deployable = (DeployableResource) resource;
					if (undeployed.toString().equals(deployable.getStatus())) {
						return String.format("Undeployed %s", kind);
					}
					return String.format("Deployed %s", kind);
				}
				return null;
			}
		};

		/**
		 * Whether the filter matches the given resource.
		 */
		abstract boolean matches(NamedResource resource);

		/**
		 * A grouping header depending on the status of the resource, or {@code null} if no information can be derived /
		 * makes sense.
		 */
		abstract String heading(NamedResource resource, String kind);

		private static DeployedCriteria parse(String optionContext) {
			List<String> list = Arrays.asList(optionContext.split(" "));
			boolean hasDeployed = list.contains("deployed");
			boolean hasUndeployed = list.contains("undeployed");
			if (hasDeployed && !hasUndeployed) {
				return deployed;
			}
			else if (hasUndeployed && !hasDeployed) {
				return undeployed;
			}
			else if (hasDeployed && hasUndeployed) {
				throw new IllegalArgumentException("deployed and undeployed are mutually exclusive");
			}
			else {
				return all;
			}

		}
	}

	private String determineKind(String optionContext) {
		String[] options = optionContext.split(" ");
		for (String option : options) {
			if (option.startsWith(EXISTING_PREFIX)) {
				return option.substring(EXISTING_PREFIX.length());
			}
		}
		throw new IllegalStateException("Could not determine kind: " + optionContext);
	}

}
