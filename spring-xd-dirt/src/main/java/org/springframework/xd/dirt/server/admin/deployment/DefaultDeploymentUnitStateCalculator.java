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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.core.DeploymentUnit;
import org.springframework.xd.dirt.core.DeploymentUnitStatus;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;

/**
 * Default {@link org.springframework.xd.dirt.server.admin.deployment.DeploymentUnitStateCalculator}
 * implementation for streams and jobs. This implementation uses the following
 * criteria:
 * <ul>
 *     <li>If all expected modules are deployed, the state is considered
 *         {@link org.springframework.xd.dirt.core.DeploymentUnitStatus.State#deployed deployed}.</li>
 *     <li>If one or more of the modules do not have the number of expected
 *         instances but there is at least one instance of each module,
 *         the state is considered
 *         {@link org.springframework.xd.dirt.core.DeploymentUnitStatus.State#incomplete incomplete}.</li>
 *     <li>If one or more of the modules do not have any instances deployed,
 *         the state is considered
 *         {@link org.springframework.xd.dirt.core.DeploymentUnitStatus.State#failed failed}.</li>
 * </ul>
 *
 * @author Patrick Peralta
 */
public class DefaultDeploymentUnitStateCalculator implements DeploymentUnitStateCalculator {

	/**
	 * Logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(DefaultDeploymentUnitStateCalculator.class);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DeploymentUnitStatus calculate(DeploymentUnit deploymentUnit,
			ModuleDeploymentPropertiesProvider<ModuleDeploymentProperties> provider,
			Collection<ModuleDeploymentStatus> deploymentStatuses) {

		Map<ModuleDescriptor.Key, Count> moduleCount = new HashMap<ModuleDescriptor.Key, Count>();
		for (ModuleDescriptor descriptor : deploymentUnit.getModuleDescriptors()) {
			moduleCount.put(descriptor.createKey(),
					new Count(provider.propertiesForDescriptor(descriptor).getCount()));
		}
		StringBuilder builder = new StringBuilder();

		logger.debug("Evaluating state for {}", deploymentUnit.getName());
		logger.trace("moduleCountMap: {}", moduleCount);

		for (ModuleDeploymentStatus deploymentStatus : deploymentStatuses) {
			logger.trace("\t{}", deploymentStatus);
			if (deploymentStatus.getState() == ModuleDeploymentStatus.State.deployed) {
				ModuleDescriptor.Key key = deploymentStatus.getKey();
				Count count = moduleCount.get(key);
				if (count != null && (count.expected == 0 || ++count.actual == count.expected)) {
					moduleCount.remove(key);
				}
			}
			else if (StringUtils.hasText(deploymentStatus.getErrorDescription())) {
				if (builder.length() > 0) {
					builder.append("; ");
				}
				builder.append(deploymentStatus.getErrorDescription());
			}
		}

		logger.trace("moduleCountMap after evaluation: {}", moduleCount);

		if (moduleCount.isEmpty()) {
			return new DeploymentUnitStatus(DeploymentUnitStatus.State.deployed, builder.toString());
		}

		// since there are some modules that did not meet the expected count,
		// iterate the map and determine if any modules failed to deploy at all;
		// if each module type has at least one deployment the status is considered
		// incomplete
		boolean failed = false;
		for (Count count : moduleCount.values()) {
			if (count.actual == 0) {
				failed = true;
				break;
			}
		}

		DeploymentUnitStatus.State state = (failed)
				? DeploymentUnitStatus.State.failed
				: DeploymentUnitStatus.State.incomplete;

		return new DeploymentUnitStatus(state, builder.toString());
	}


	/**
	 * Structure to track the number of expected and actual module deployments.
	 */
	private static class Count {

		/**
		 * The number of expected module instances.
		 */
		final int expected;

		/**
		 * The number of actual module instances.
		 */
		int actual;

		/**
		 * Construct a {@code Count} with the expected module count.
		 *
		 * @param expected expected number of modules
		 */
		private Count(int expected) {
			this.expected = expected;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String toString() {
			return "Count{" +
					"expected=" + expected +
					", actual=" + actual +
					'}';
		}

	}

}
