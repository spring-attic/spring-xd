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
package org.springframework.xd.analytics.metrics.repository;

import java.util.List;

import org.springframework.data.repository.Repository;
import org.springframework.xd.analytics.metrics.core.Gauge;

/**
 * A repository to save, delete and find Gauge instances.  Uses the Spring Data Repository 
 * marker interface and conventions for method names and behavior.
 * 
 * The name is the id and should be unique.
 *
 * Note: Will shift to inherit from CrudRepository
 * 
 * @author Mark Pollack
 *
 */
public interface GaugeRepository extends Repository<Gauge, String> {

	//TODO inherit from CrudRepository to share a common interface 
	
	/**
	 * Saves a given entity. Use the returned instance for further operations as the save operation might have changed 
	 * the entity instance completely.
	 * @param gauge
	 * @return the saved entity
	 */
	Gauge save(Gauge gauge);
	
	/**
	 * Deletes the gauge with the given name
	 * @param name must not be null
	 * @throws IllegalArgumentException in case the given name is null
	 */
	void delete(String name);
	
	/**
	 * Deletes a given Gauge
	 * @param gauge
	 * @throws IllegalArgumentException in case the given Gauge is null
	 */
	void delete(Gauge gauge);
	
	/**
	 * Removes a gauge by name
	 * @param name must not null
	 * @return the Gauge with the given name or null if none found
	 * @throws IllegalArgumentException if name is null
	 */
	Gauge findOne(String name);
	
	//TODO add exists
	
	/**
	 * Returns all gauge instances
	 * @return all gauges
	 */
	List<Gauge> findAll();
	
}
