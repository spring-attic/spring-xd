package org.springframework.xd.analytics.metrics.core;

import java.util.List;

import org.springframework.data.repository.Repository;

/**
 * A repository to save, delete and find Metric instances.  Uses the Spring Data Repository
 * marker interface and conventions for method names and behavior.
 *
 * The name is the id and should be unique.
 *
 * Note: Will shift to inherit from CrudRepository
 *
 * @author Mark Pollack
 * @author Luke Taylor
 */
public interface MetricRepository<M extends Metric> extends Repository<M, String> {
	/**
	 * Saves a given metric. Use the returned instance for further operations as the save operation might have changed
	 * the entity instance completely.
	 * @param metric
	 * @return the saved entity
	 */
	M save(M metric);

	/**
	 * Deletes the counter with the given name
	 * @param name must not be null
	 * @throws IllegalArgumentException in case the given name is null
	 */
	void delete(String name);

	/**
	 * Deletes a given Metric
	 * @param metric
	 * @throws IllegalArgumentException in case the given Metric is null
	 */
	void delete(M metric);

	/**
	 * Removes a counter by name
	 * @param name must not null
	 * @return the Counter with the given name or null if none found
	 * @throws IllegalArgumentException if name is null
	 */
	M findOne(String name);

	//TODO add exists

	/**
	 * Returns all instances of the parametrized metric.
	 * @return all instances of this metric type.
	 */
	List<M> findAll();

	/**
	 * Deletes all the metrics managed by this repository.
	 */
	void deleteAll();
}
