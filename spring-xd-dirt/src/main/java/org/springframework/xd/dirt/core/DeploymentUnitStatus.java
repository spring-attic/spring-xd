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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Status for a {@link org.springframework.xd.dirt.core.DeploymentUnit}.
 * For deployment units that consist of multiple modules, this state
 * is calculated based on the aggregate of all modules for the deployment
 * unit.
 * <p/>
 * The deployment status tracked by this class can be converted to a
 * {@link Map} which can be used to serialize this data into ZooKeeper.
 *
 * @see org.springframework.xd.dirt.server.admin.deployment.DeploymentUnitStateCalculator
 *
 * @author Patrick Peralta
 */
public class DeploymentUnitStatus {

	/**
	 * Key used in map to indicate the state.
	 *
	 * @see #toMap
	 */
	private static final String STATE_KEY = "state";

	/**
	 * Key used in map to indicate the error description.
	 *
	 * @see #toMap
	 */
	private static final String ERROR_DESCRIPTION = "errorDescription";

	/**
	 * State for this deployment unit.
	 */
	private final State state;

	/**
	 * Error description for this deployment unit; may be null.
	 */
	private final String errorDescription;

	/**
	 * Deployment unit states.
	 */
	public enum State {

		/**
		 * The deployment unit is not deployed.
		 */
		undeployed,

		/**
		 * One or more of the deployment unit modules are being deployed.
		 */
		deploying,

		/**
		 * All expected modules for the deployment unit have been deployed.
		 */
		deployed,

		/**
		 * Some expected modules for the deployment unit have not been
		 * deployed; however the deployment unit is considered functional.
		 */
		incomplete,

		/**
		 * Some or all of the expected modules for the deployment unit
		 * have failed to deploy; the deployment unit is not considered functional.
		 */
		failed,

		/**
		 * The deployment unit is in the process of being undeployed.
		 */
		undeploying,

		/**
		 * The deployment status could not be calculated; this is typically
		 * due to an error loading the deployment unit.
		 */
		unknown
	}

	/**
	 * Construct a {@code DeploymentUnitStatus}.
	 *
	 * @param state the state for this deployment unit
	 */
	public DeploymentUnitStatus(State state) {
		this(state, null);
	}

	/**
	 * Construct a {@code DeploymentUnitStatus} with an error description.
	 *
	 * @param state             the state for this deployment unit
	 * @param errorDescription  error description for this state
	 */
	public DeploymentUnitStatus(State state, String errorDescription) {
		Assert.notNull(state);
		this.state = state;
		this.errorDescription = errorDescription;
	}

	/**
	 * Construct a {@code DeploymentUnitStatus} using the entries in
	 * the provided map.
	 *
	 * @param mapStatus map containing deployment unit status entries
	 * @see #STATE_KEY
	 * @see #ERROR_DESCRIPTION
	 */
	public DeploymentUnitStatus(Map<String, String> mapStatus) {
		String stateString = mapStatus.get(STATE_KEY);
		Assert.state(StringUtils.hasText(stateString));
		this.state = State.valueOf(stateString.toLowerCase());
		this.errorDescription = mapStatus.get(ERROR_DESCRIPTION);
	}

	/**
	 * Return the state of the deployment unit.
	 *
	 * @return deployment unit state
	 */
	public State getState() {
		return state;
	}

	/**
	 * Return the error description for the deployment unit; may be null.
	 *
	 * @return error description for the deployment unit; may be null
	 */
	public String getErrorDescription() {
		return errorDescription;
	}

	/**
	 * Return a map containing the deployment unit status information.
	 *
	 * @return map with deployment unit status information
	 * @see #STATE_KEY
	 * @see #ERROR_DESCRIPTION
	 */
	public Map<String, String> toMap() {
		Map<String, String> map = new HashMap<String, String>();
		map.put(STATE_KEY, state.toString());
		if (StringUtils.hasText(errorDescription)) {
			map.put(ERROR_DESCRIPTION, errorDescription);
		}
		return Collections.unmodifiableMap(map);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("DeploymentStatus{state=");
		builder.append(state);
		if (StringUtils.hasText(errorDescription)) {
			builder.append(",error(s)=").append(errorDescription);
		}
		builder.append("}");
		return builder.toString();
	}

}
