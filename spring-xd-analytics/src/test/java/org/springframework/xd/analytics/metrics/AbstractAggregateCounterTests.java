/*
 * Copyright 2011-2013 the original author or authors.
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

package org.springframework.xd.analytics.metrics;

import static org.junit.Assert.assertEquals;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.xd.analytics.metrics.core.AggregateCount;
import org.springframework.xd.analytics.metrics.core.AggregateCountResolution;
import org.springframework.xd.analytics.metrics.core.AggregateCounterRepository;

/**
 * @author Luke Taylor
 * @author Gary Russell
 * @author Ilayaperumal Gopinathan
 */
public abstract class AbstractAggregateCounterTests {

	protected final String counterName = "test";

	@Autowired
	protected AggregateCounterRepository aggregateCounterRepository;

	@Test
	public void test66MinuteCount() throws Exception {
		final DateTime start = new DateTime(2013, 6, 28, 23, 0, 0, 0);
		final DateTime end = start.plusMinutes(66);
		DateTime now = start;
		int val = 1;
		while (now.isBefore(end)) {
			aggregateCounterRepository.increment(counterName, val++, now);
			now = now.plus(Duration.standardMinutes(1));
		}
		// Include 66th minute's value
		aggregateCounterRepository.increment(counterName, val++, now);
		long[] counts = aggregateCounterRepository.getCounts(counterName, new Interval(start, end), AggregateCountResolution.minute).getCounts();
		// the counts.length should be 66 + 1 to include the 66th minute's count
		assertEquals(67, counts.length);
		assertEquals(1, counts[0]);
		assertEquals(66, counts[65]);
		assertEquals(67, counts[66]);
	}

	@Test
	public void testDayCounts() throws Exception {
		final DateTime start = new DateTime(2013, 11, 28, 23, 0);
		final DateTime end   = start.plusDays(66);

		DateTime now = start;
		int val = 1;
		while (!now.isAfter(end.plusDays(1))) {
			aggregateCounterRepository.increment(counterName, val++, now);
			now = now.plus(Duration.standardDays(1));
		}
		long[] counts = aggregateCounterRepository.getCounts(counterName, new Interval(start, end), AggregateCountResolution.day).getCounts();
		assertEquals(67, counts.length);

		for (int i=0; i < counts.length; i++) {
			assertEquals("count at index " + i + " should be " + (i+1), i+1, counts[i]);
		}

		counts = aggregateCounterRepository.getCounts(counterName, new Interval(end.minusDays(30), end), AggregateCountResolution.day).getCounts();
		assertEquals(31, counts.length);

		counts = aggregateCounterRepository.getCounts(counterName, 60, end, AggregateCountResolution.day).getCounts();
		assertEquals(60, counts.length);
		assertEquals(8, counts[0]);
		assertEquals(67, counts[59]);
	}

	@Test
	public void testMonthCounts() throws Exception {
		final DateTime start = new DateTime(2013, 11, 20, 0, 0);
		final DateTime end   = start.plusMonths(5);
		int val = 1;

		for (DateTime now = start; !now.isAfter(end); now = now.plusMonths(1)) {
			aggregateCounterRepository.increment(counterName, val++, now);
		}
		long[] counts = aggregateCounterRepository.getCounts(counterName, new Interval(start,end), AggregateCountResolution.month).getCounts();
		assertEquals(6, counts.length);

		for (int i = 0; i < counts.length; i++) {
			assertEquals("count at index " + i + " should be " + (i + 1), i + 1, counts[i]);
		}

		// Check a query for twelve months past
		counts = aggregateCounterRepository.getCounts(counterName, new Interval(end.minusMonths(11), end), AggregateCountResolution.month).getCounts();
		assertEquals(12, counts.length);

		counts = aggregateCounterRepository.getCounts(counterName, 3, end, AggregateCountResolution.month).getCounts();
		assertEquals(3, counts.length);
		assertEquals(6, counts[2]);
	}

	@Test
	public void testYearCounts() throws Exception {
		final DateTime start = new DateTime(2013, 11, 20, 0, 0);
		final DateTime end   = start.plusYears(5);
		int val = 1;

		for (DateTime now = start; !now.isAfter(end); now = now.plusYears(1)) {
			aggregateCounterRepository.increment(counterName, val++, now);
		}
		long[] counts = aggregateCounterRepository.getCounts(counterName, new Interval(start,end), AggregateCountResolution.year).getCounts();
		assertEquals(6, counts.length);
		assertEquals(6, counts[5]);
		assertEquals(1, counts[0]);
	}

	@Test
	public void testTwoDaysDataSimulation() throws Exception {
		final DateTime start = new DateTime(2013, 12, 30, 23, 27);
		final DateTime end = start.plusDays(2);
		DateTime now = start;

		int total = 0;

		while (now.isBefore(end)) {
			int minute = now.getMinuteOfHour();
			aggregateCounterRepository.increment(counterName, minute, now);
			now = now.plusMinutes(1);
			total += minute;
		}
		// Include end time's metric
		int endMinute = end.getMinuteOfHour();
		aggregateCounterRepository.increment(counterName, endMinute, now);
		total += endMinute;

		// Check the total
		assertEquals(total, aggregateCounterRepository.findOne(counterName).getValue());

		// Query the entire period
		Interval queryInterval = new Interval(start, end);
		AggregateCount aggregateCount = aggregateCounterRepository.getCounts(counterName, queryInterval,
				AggregateCountResolution.minute);
		assertEquals(counterName, aggregateCount.getName());
		assertEquals(queryInterval, aggregateCount.getInterval());
		long[] counts = aggregateCount.getCounts();
		// counts.length should include the end time's minute
		assertEquals((2 * 24 * 60) + 1, counts.length);
		assertEquals(27, counts[0]);
		assertEquals(28, counts[1]);
		assertEquals(59, counts[32]);
		for (int i = 33; i < counts.length; i++) {
			int expect = (i - 33) % 60;
			assertEquals("Count at index " + i + " should be " + expect, expect, counts[i]);
		}

		// Query a 24 hour period in minutes
		now = start.plusHours(5).withMinuteOfHour(0);
		queryInterval = new Interval(now, now.plusHours(24));

		aggregateCount = aggregateCounterRepository.getCounts(counterName, queryInterval, AggregateCountResolution.minute);
		counts = aggregateCount.getCounts();
		// Add 'now.plusHours(24)' minute time to the count
		assertEquals((24 * 60) + 1, counts.length);
		assertEquals(0, counts[0]); // on an hour boundary
		for (int i = 1; i < counts.length; i++) {
			int expect = i % 60;
			assertEquals("Count at index " + i + " should be " + expect, expect, counts[i]);
		}

		// Query the entire period in hours
		queryInterval = new Interval(start, end);
		aggregateCount = aggregateCounterRepository.getCounts(counterName, queryInterval, AggregateCountResolution.hour);
		counts = aggregateCount.getCounts();
		assertEquals(49, counts.length);
		// The first hour starts before the first counts are added
		assertEquals(1419, counts[0]); // sum [27..59]
		for (int i = 1; i < (counts.length - 1); i++) {
			assertEquals(1770, counts[i]); // sum [0..59]
		}
		// The last hour ends at 27th minute
		assertEquals(378, counts[counts.length - 1]); // sum [0..27]

		// Query the entire period in days
		aggregateCount = aggregateCounterRepository.getCounts(counterName, queryInterval, AggregateCountResolution.day);
		counts = aggregateCount.getCounts();
		assertEquals(3, counts.length);

		// Query beyond the period where we have data
		DateTime newStart = start.withYear(2012);
		aggregateCount = aggregateCounterRepository.getCounts(counterName, queryInterval.withStart(newStart), AggregateCountResolution.day);
		counts = aggregateCount.getCounts();
		assertEquals(368, counts.length);

		aggregateCount = aggregateCounterRepository.getCounts(counterName, queryInterval.withStart(newStart), AggregateCountResolution.month);
	}
}
