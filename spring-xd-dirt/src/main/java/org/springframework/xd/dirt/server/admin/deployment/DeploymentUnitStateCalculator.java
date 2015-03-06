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

import org.springframework.xd.dirt.core.DeploymentUnit;
import org.springframework.xd.dirt.core.DeploymentUnitStatus;
import org.springframework.xd.module.ModuleDeploymentProperties;

/**
 * Calculates the overall status for a deployment unit (stream or job).
 * The inputs used to calculate this state include:
 * <ul>
 *     <li>The stream/job itself, including all module information</li>
 *     <li>Deployment properties provider for the individual modules;
 *         this may include attributes such as module count</li>
 *     <li>Collection of deployment statuses for each individual module</li>
 * </ul>
 *
 * @author Patrick Peralta
 *
 * @see org.springframework.xd.dirt.core.DeploymentUnitStatus
 */
public interface DeploymentUnitStateCalculator {

	/**
	 * For a given deployment unit (stream or job) calculate the overall status.
	 *
	 * @param deploymentUnit      deployment unit (stream or job)
	 * @param provider            deployment properties provider
	 * @param deploymentStatuses  deployment statuses for each module in the
	 *                            deployment unit.
	 * @return the status for the stream/job
	 */
	DeploymentUnitStatus calculate(DeploymentUnit deploymentUnit,
			ModuleDeploymentPropertiesProvider<ModuleDeploymentProperties> provider,
			Collection<ModuleDeploymentStatus> deploymentStatuses);
}
