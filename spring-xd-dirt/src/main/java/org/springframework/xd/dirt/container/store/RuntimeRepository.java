/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.container.store;

import java.util.List;

import org.springframework.xd.dirt.cluster.ContainerRuntime;

/**
 * Repository for persisting {@link ContainerRuntime} entities for admins.
 *
 * @author Janne Valkealahti
 */
public interface RuntimeRepository {

	/**
	 * Update the container runtime attributes.
	 *
	 * @param runtime the container runtime entity
	 */
	void update(ContainerRuntime runtime);

	/**
	 * Save the container tuntime attributes.
	 *
	 * @param runtime the container runtime entity
	 * @return the container runtime
	 */
	ContainerRuntime save(ContainerRuntime runtime);

	/**
	 * Returns all container runtime entities.
	 *
	 * @return all container runtime entities
	 */
	List<ContainerRuntime> findAll();

	/**
	 * Returns whether a container runtime with the given id exists.
	 *
	 * @param id container runtime id
	 * @return true if a container runtime with the given id exists, {@literal false} otherwise
	 */
	boolean exists(String id);

	/**
	 * Retrieves a container runtime by its id.
	 *
	 * @param id container runtime id
	 * @return a container runtime
	 */
	ContainerRuntime findOne(String id);

}
