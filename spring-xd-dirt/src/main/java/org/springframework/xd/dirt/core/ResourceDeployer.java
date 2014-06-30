/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.dirt.core;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.xd.dirt.stream.StreamDefinition;

/**
 * Interface for XD Resource Services.
 *
 * @param <R> the kind of resource to deploy (<i>e.g.</i> {@link StreamDefinition})
 *
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 */
public interface ResourceDeployer<R extends BaseDefinition> {

	/**
	 * Deploy a resource (job or stream).
	 *
	 * @param name name of the resource
	 * @param properties deployment properties to use (may not be null)
	 */
	void deploy(String name, Map<String, String> properties);

	/**
	 * @return Iterable all definitions
	 */
	Iterable<R> findAll();

	/**
	 * Return a slice of all definitions.
	 */
	Page<R> findAll(Pageable pageable);

	R save(R resource);

	/**
	 * Retrieves a single Definition or null if none is found.
	 *
	 * @param name of the definition to find. Must not be null.
	 */
	R findOne(String name);

	/**
	 * Delete the Definition using the provided name. The definition may also be {@link #undeploy(String) undeployed} as
	 * part of that process.
	 *
	 * @param name the name of the definition to delete
	 */
	void delete(String name);

	void undeploy(String name);

	/**
	 * Delete all the definitions
	 */
	void deleteAll();

	/**
	 * Undeploy all the deployed resources.
	 */
	void undeployAll();
}
