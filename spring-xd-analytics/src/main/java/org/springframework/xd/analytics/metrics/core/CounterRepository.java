/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.xd.analytics.metrics.core;

import java.util.List;

import org.springframework.data.repository.Repository;

/**
 * A repository to save, delete and find Counter instances.  Uses the Spring Data Repository
 * marker interface and conventions for method names and behavior.
 *
 * The name is the id and should be unique.
 *
 * Note: Will shift to inherit from CrudRepository
 *
 * @author Mark Pollack
 *
 */
public interface CounterRepository extends Repository<Counter, String> {

	//TODO inherit from CrudRepository to share a common interface to aid in testing

	/**
	 * Saves a given entity. Use the returned instance for further operations as the save operation might have changed
	 * the entity instance completely.
	 * @param counter
	 * @return the saved entity
	 */
	Counter save(Counter counter);

	/**
	 * Deletes the counter with the given name
	 * @param name must not be null
	 * @throws IllegalArgumentException in case the given name is null
	 */
	void delete(String name);

	/**
	 * Deletes a given Counter
	 * @param counter
	 * @throws IllegalArgumentException in case the given Counter is null
	 */
	void delete(Counter counter);

	/**
	 * Removes a counter by name
	 * @param name must not null
	 * @return the Counter with the given name or null if none found
	 * @throws IllegalArgumentException if name is null
	 */
	Counter findOne(String name);

	//TODO add exists

	/**
	 * Returns all counter instances
	 * @return all counters
	 */
	List<Counter> findAll();

}
