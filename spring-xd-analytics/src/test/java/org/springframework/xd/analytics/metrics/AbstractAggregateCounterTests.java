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

	private final DateTimeField MINUTE_RESOLUTION = ISOChronology.getInstanceUTC().minuteOfHour();

	private final DateTimeField HOUR_RESOLUTION = ISOChronology.getInstanceUTC().hourOfDay();

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
		long[] counts = aggregateCounterRepository.getCounts(counterName, new Interval(start, end), MINUTE_RESOLUTION).counts;
		// the counts.length should be 66 + 1 to include the 66th minute's count
		assertEquals(67, counts.length);
		assertEquals(1, counts[0]);
		assertEquals(66, counts[65]);
		assertEquals(67, counts[66]);
	}

	@Test
	public void testTwoDaysDataSimulation() throws Exception {
		final DateTime start = new DateTime(2013, 6, 28, 23, 27, 0, 0);
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
				MINUTE_RESOLUTION);
		assertEquals(counterName, aggregateCount.name);
		assertEquals(queryInterval, aggregateCount.interval);
		long[] counts = aggregateCount.counts;
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

		aggregateCount = aggregateCounterRepository.getCounts(counterName, queryInterval, MINUTE_RESOLUTION);
		counts = aggregateCount.counts;
		// Add 'now.plusHours(24)' minute time to the count
		assertEquals((24 * 60) + 1, counts.length);
		assertEquals(0, counts[0]); // on an hour boundary
		for (int i = 1; i < counts.length; i++) {
			int expect = i % 60;
			assertEquals("Count at index " + i + " should be " + expect, expect, counts[i]);
		}

		// Query the entire period in hours
		queryInterval = new Interval(start, end);
		aggregateCount = aggregateCounterRepository.getCounts(counterName, queryInterval, HOUR_RESOLUTION);
		counts = aggregateCount.counts;
		// counts.length should include end time's hour; hence 48+1
		assertEquals(49, counts.length);
		// The first hour starts before the first counts are added
		assertEquals(1419, counts[0]); // sum [27..59]
		for (int i = 1; i < (counts.length - 1); i++) {
			assertEquals(1770, counts[i]); // sum [0..59]
		}
		// The last hour ends at 27th minute
		assertEquals(378, counts[counts.length - 1]); // sum [0..27]
	}
}
