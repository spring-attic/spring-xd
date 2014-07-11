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

package org.springframework.xd.analytics.metrics.core;

import org.joda.time.DateTime;
import org.joda.time.Interval;

/**
 * A repository to interact with Aggregate Counters.
 * 
 * @author Luke Taylor
 */
public interface AggregateCounterRepository extends CounterRepository {

	/**
	 * Increments the named counter by a specific amount for the given instant.
	 */
	long increment(String name, long amount, DateTime dateTime);


	/**
	 * Query function which returns the last 'n' points for a particular resolution.
	 *
	 * @param name the counter to query
	 * @param nCounts the number of data points to return
	 * @param resolution the resolution at which the data should be returned (minute, hour, day, month)
	 * @return an object containing an indexed array of the aggregate counts for the given query.
	 */
	AggregateCount getCounts(String name, int nCounts, AggregateCountResolution resolution);

	/**
	 * Query function to allow the counts for a specific interval to be retrieved.
	 * 
	 *
	 * @param name the counter to query
	 * @param interval the time interval to return data for. Includes start and end.
	 * @param resolution the resolution at which the data should be returned (minute, hour, day, month)
	 * @return an object containing an indexed array of the aggregate counts for the given query.
	 */
	AggregateCount getCounts(String name, Interval interval, AggregateCountResolution resolution);


	/**
	 * Queries by requesting a number of points, ending on the given date (inclusive).
	 * The date may be null to use the current time, thus returning the last nCounts point
	 * at the given resolution.
	 *
	 * @param name the counter to query
	 * @param end the end of the query interval (inclusive). Cannot be null.
	 * @param nCounts the number of data points to return
	 * @param resolution the resolution at which the data should be returned (minute, hour, day, month)
	 * @return an object containing an indexed array of the counts .
	 */
	AggregateCount getCounts(String name, int nCounts, DateTime end, AggregateCountResolution resolution);

}
