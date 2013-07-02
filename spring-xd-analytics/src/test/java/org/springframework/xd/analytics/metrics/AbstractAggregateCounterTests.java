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
import org.joda.time.DateTimeField;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.chrono.ISOChronology;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.xd.analytics.metrics.core.AggregateCount;
import org.springframework.xd.analytics.metrics.core.AggregateCounterService;

/**
 * @author Luke Taylor
 */
public abstract class AbstractAggregateCounterTests {
	protected final String counterName = "test";

	@Autowired
	protected AggregateCounterService counterService;

	private final DateTimeField MINUTE_RESOLUTION = ISOChronology.getInstanceUTC().minuteOfHour();
	private final DateTimeField HOUR_RESOLUTION = ISOChronology.getInstanceUTC().hourOfDay();

	@Test
	public void test66MinuteCount() throws Exception {
		final DateTime start = new DateTime(2013, 6, 28, 23, 0, 0, 0);
		final DateTime end   = start.plusMinutes(66);
		DateTime now = start;
		int val = 1;
		while (now.isBefore(end)) {
			counterService.increment(counterName, val++, now);
			now = now.plus(Duration.standardMinutes(1));
		}
		int[] counts = counterService.getCounts(counterName, new Interval(start, end), MINUTE_RESOLUTION).counts;
		assertEquals(66, counts.length);
		assertEquals(1, counts[0]);
		assertEquals(65, counts[64]);
		assertEquals(66, counts[65]);
	}

	@Test
	public void testTwoDaysDataSimulation() throws Exception {
		final DateTime start = new DateTime(2013, 6, 28, 23, 27, 0, 0);
		final DateTime end   = start.plusDays(2);
		DateTime now = start;

		int total = 0;
		while (now.isBefore(end)) {
			int minute = now.getMinuteOfHour();
			counterService.increment(counterName, minute, now);
			now = now.plusMinutes(1);
			total += minute;
		}

		// Check the total
		assertEquals(total, counterService.getTotalCounts(counterName));

		// Query the entire period
		Interval queryInterval = new Interval(start, end);
		AggregateCount aggregateCount = counterService.getCounts(counterName, queryInterval, MINUTE_RESOLUTION);
		assertEquals(counterName, aggregateCount.name);
		assertEquals(queryInterval, aggregateCount.interval);
		int[] counts = aggregateCount.counts;
		assertEquals(2*24*60, counts.length);
		assertEquals(27, counts[0]);
		assertEquals(28,counts[1]);
		assertEquals(59, counts[32]);
		for (int i = 33; i < counts.length; i++) {
			int expect = (i - 33) % 60;
			assertEquals("Count at index " + i + " should be " + expect, expect, counts[i]);
		}

		// Query a 24 hour period in minutes
		now = start.plusHours(5).withMinuteOfHour(0);
		queryInterval = new Interval(now, now.plusHours(24));

		aggregateCount = counterService.getCounts(counterName, queryInterval, MINUTE_RESOLUTION);
		counts = aggregateCount.counts;
		assertEquals(24*60, counts.length);
		assertEquals(0, counts[0]); // on an hour boundary
		for (int i = 1; i < counts.length; i++) {
			int expect = i % 60;
			assertEquals("Count at index " + i + " should be " + expect, expect, counts[i]);
		}

		// Query the entire period in hours
		queryInterval = new Interval(start, end);
		aggregateCount = counterService.getCounts(counterName, queryInterval, HOUR_RESOLUTION);
		counts = aggregateCount.counts;
		assertEquals(48, counts.length);
		// The first hour starts before the first counts are added
		assertEquals(1419, counts[0]); // sum [27..59]
		for (int i = 1; i < counts.length; i++)
			assertEquals(1770, counts[i]); // sum [0..59]
	}
}
