
package org.springframework.xd.analytics.metrics.core;

import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.DateTimeField;
import org.joda.time.Interval;

/**
 * A repository to interact with {@link AggregateCounter}s.
 * 
 * @author Luke Taylor
 */
public interface AggregateCounterRepository {

	/**
	 * Increments the named counter using the current time.
	 */
	void increment(String name);

	/**
	 * Increments the named counter by a specific amount for the given instant.
	 */
	void increment(String name, int amount, DateTime dateTime);

	/**
	 * Removes all the data for the named counter.
	 */
	void deleteCounter(String name);

	/**
	 * Returns the total number of counts for the entire lifespan of the named counter.
	 */
	int getTotalCounts(String name);

	/**
	 * Query function to allow the counts for a specific interval to be retrieved.
	 * 
	 * @param name the counter to query
	 * @param interval the time interval to return data for. Includes start, excludes end.
	 * @param resolution the resolution at which the data should be returned (minutes or
	 *        hours)
	 * @return an object containing an indexed array of the aggregate counts for the given
	 *         query.
	 */
	AggregateCount getCounts(String name, Interval interval, DateTimeField resolution);

	/**
	 * Returns the names of all available aggregate counters.
	 */
	Set<String> getAll();
}
