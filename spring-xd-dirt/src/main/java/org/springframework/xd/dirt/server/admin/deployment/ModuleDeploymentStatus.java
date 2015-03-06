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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.core.ModuleDeploymentsPath;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.module.ModuleDescriptor;

/**
 * Status of a module deployment for a container. In addition
 * to the {@code get...} methods, this class provides a {@link #toMap}
 * method which returns a map that contains the state and error
 * description which can be written to ZooKeeper. The module deployment
 * status path can be obtained via {@link #buildPath()}.
 *
 * @author Patrick Peralta
 */
public class ModuleDeploymentStatus {

	/**
	 * Key used in status map to indicate the module deployment status.
	 * The status map is written by the container deploying the module.
	 */
	private static final String STATUS_KEY = "status";

	/**
	 * Key used in status map to provide an error description.
	 * The status map is written by the container deploying the module.
	 * This value will be written if there is an error deploying the module.
	 */
	private static final String ERROR_DESCRIPTION_KEY = "errorDescription";

	/**
	 * Module deployment states.
	 */
	public enum State {
		/**
		 * Module was successfully deployed.
		 */
		deployed,

		/**
		 * An error occurred during module deployment (usually on the container side).
		 */
		failed
	}

	/**
	 * Target container for module deployment request.
	 */
	private final String container;

	/**
	 * Sequence number of the module in the module deployment request.
	 */
	private final int moduleSequence;

	/**
	 * Module descriptor key for the module being deployed.
	 */
	private final ModuleDescriptor.Key key;

	/**
	 * Status of module deployment.
	 */
	private final State state;

	/**
	 * Error description; will be null unless there was an error
	 * deploying the module.
	 */
	private final String errorDescription;

	/**
	 * Construct a {@code ModuleDeploymentStatus}.
	 *
	 * @param container         target container name
	 * @param moduleSequence    module sequence number
	 * @param key               module descriptor key
	 * @param state             deployment state
	 * @param errorDescription  error description (may be null)
	 */
	public ModuleDeploymentStatus(String container, int moduleSequence, ModuleDescriptor.Key key,
			State state, String errorDescription) {
		this.container = container;
		this.moduleSequence = moduleSequence;
		this.key = key;
		this.state = state;
		this.errorDescription = errorDescription;
	}

	/**
	 * Construct a {@code ModuleDeploymentStatus}.
	 *
	 * @param container  target container name
	 * @param moduleSequence    module sequence number
	 * @param key        module descriptor key
	 * @param map        map containing status and (possibly) an error description
	 */
	public ModuleDeploymentStatus(String container, int moduleSequence, ModuleDescriptor.Key key,
			Map<String, String> map) {
		this.container = container;
		this.moduleSequence = moduleSequence;
		this.key = key;
		Assert.state(map.containsKey(STATUS_KEY),
				String.format("missing key '%s' from map; contents: %s", STATUS_KEY, map));
		this.state = State.valueOf(map.get(STATUS_KEY));
		this.errorDescription = map.get(ERROR_DESCRIPTION_KEY);
	}

	/**
	 * @return target container name
	 *
	 * @see #container
	 */
	public String getContainer() {
		return container;
	}

	/**
	 * @return module sequence number
	 *
	 * @see #moduleSequence
	 */
	public int getModuleSequence() {
		return moduleSequence;
	}

	/**
	 * @return module sequence number as string
	 *
	 * @see #moduleSequence
	 */
	public String getModuleSequenceAsString() {
		return String.valueOf(moduleSequence);
	}

	/**
	 * @return module descriptor key
	 *
	 * @see #key
	 */
	public ModuleDescriptor.Key getKey() {
		return key;
	}

	/**
	 * @return module deployment status
	 *
	 * @see #state
	 */
	public State getState() {
		return state;
	}

	/**
	 * @return error description; may be null
	 *
	 * @see #errorDescription
	 */
	public String getErrorDescription() {
		return errorDescription;
	}

	/**
	 * Return a map containing the state and error description
	 * (if any) for this status. This map may be serialized
	 * into ZooKeeper to write out the module deployment status.
	 *
	 * @return map containing the module deployment status
	 * @see #buildPath
	 */
	public Map<String, String> toMap() {
		Map<String, String> map = new HashMap<String, String>();
		map.put(STATUS_KEY, state.toString());
		if (StringUtils.hasText(errorDescription)) {
			map.put(ERROR_DESCRIPTION_KEY, errorDescription);
		}
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Return the ZooKeeper path that this module deployment
	 * status should be written to
	 *
	 * @return path for this status
	 * @see #toMap
	 */
	public String buildPath() {
		return Paths.build(new ModuleDeploymentsPath()
				.setContainer(container)
				.setDeploymentUnitName(key.getGroup())
				.setModuleType(key.getType().toString())
				.setModuleLabel(key.getLabel())
				.setModuleSequence(String.valueOf(moduleSequence)).build(), Paths.STATUS);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return "ModuleDeploymentStatus{" +
				"container='" + container + '\'' +
				", moduleSequence=" + moduleSequence +
				", key=" + key +
				", state=" + state +
				", errorDescription='" + errorDescription + '\'' +
				'}';
	}

}
