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

package org.springframework.xd.analytics.metrics.redis;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.util.Map;
import java.util.Set;

import org.joda.time.*;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.xd.analytics.metrics.common.ServicesConfig;
import org.springframework.xd.analytics.metrics.core.AggregateCount;
import org.springframework.xd.analytics.metrics.core.AggregateCounter;

@ContextConfiguration(classes=ServicesConfig.class, loader=AnnotationConfigContextLoader.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisAggregateCounterTests {
	private final String counterName = "test";

	@Autowired
	private RedisAggregateCounterService counterService;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	@Before
	@After
	public void beforeAndAfter() {
		Set<String> keys = stringRedisTemplate.opsForSet().members(counterService.getKeyForAllCounterNames());
		if (keys.size() > 0) {
			stringRedisTemplate.delete(keys);
		}
		stringRedisTemplate.delete(counterService.getKeyForAllCounterNames());
		stringRedisTemplate.delete(counterService.getKeyForAllRootCounterNames());
	}

	@Test
	public void testTwoDaysDataSimulation() throws Exception {
		final DateTime start = new DateTime(2013, 6, 28, 23, 27, 0, 0);
		final DateTime end   = start.plusDays(2);
		DateTime now   = start;

		int total = 0;
		while (now.isBefore(end)) {
			int minute = now.getMinuteOfHour();
			counterService.increment(counterName, minute, now);
			now = now.plusMinutes(1);
			total += minute;
		}

		// Check the total
		assertEquals(total, counterService.getTotalCounts(counterName));

		DateTimeField resolution = ISOChronology.getInstanceUTC().minuteOfHour();

		// Query the entire period
		Interval queryInterval = new Interval(start, end);
		AggregateCount aggregateCount = counterService.getCounts(counterName, queryInterval, resolution);
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

		// Query a 24 hour period
		now = start.plusHours(5).withMinuteOfHour(0);
		queryInterval = new Interval(now, now.plusHours(24));

		aggregateCount = counterService.getCounts(counterName, queryInterval, resolution);
		counts = aggregateCount.counts;
		assertEquals(24*60, counts.length);
		assertEquals(0, counts[0]); // on an hour boundary
		for (int i = 1; i < counts.length; i++) {
			int expect = i % 60;
			assertEquals("Count at index " + i + " should be " + expect, expect, counts[i]);
		}
	}

}
