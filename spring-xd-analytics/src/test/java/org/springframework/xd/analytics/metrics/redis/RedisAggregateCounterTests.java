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

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.Interval;
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

	private static DateTimeFormatter formatter = AggregateKeyGenerator.dateTimeFormatter;

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
	public void play() {
		// February 28, 2013 at 23:27
		counterService.increment(counterName, 1, formatter.parseDateTime("201302282327"));
		DateTime firstDateTime = new DateTime(2013, 2, 28, 23, 27, 0, 0);

		assertThat(counterService.getTotalCounts(counterName), equalTo(1));

		Map<Integer, Integer> yearlyCounts = counterService.getYearlyCounts(counterName);
		assertThat(yearlyCounts.size(), equalTo(1));
		assertThat(yearlyCounts, hasEntry(2013, 1));

		Map<Integer, Integer> monthlyCounterForYear = counterService.getMonthlyCountsForYear(counterName, 2013);
		assertThat(monthlyCounterForYear.size(), equalTo(1));
		assertThat(monthlyCounterForYear, hasEntry(2, 1));

		Map<Integer, Integer> dayCountsForMonth = counterService.getDayCountsForMonth(counterName, 2013, 2);
		assertThat(dayCountsForMonth.size(), equalTo(1));
		assertThat(dayCountsForMonth, hasEntry(28, 1));

		Map<Integer, Integer> hourCountsForDay = counterService.getHourCountsForDay(counterName, 2013, 2, 28);
		assertThat(hourCountsForDay.size(), equalTo(1));
		assertThat(hourCountsForDay, hasEntry(23, 1));

		int[] minCountsForHour = counterService.getMinCountsForHour(counterName, 2013, 2, 28, 23);
		assertEquals(1, minCountsForHour[27]);

		AggregateCount ac = counterService.getCounts(counterName, new Interval(firstDateTime, firstDateTime.withMinuteOfHour(59)), ISOChronology.getInstance().minuteOfDay());

		// For this interval, 27th minute is at offset 0
		assertEquals(1, ac.counts[0]);

		// February 28, 2013 at 22:56
		counterService.increment(counterName, 1, formatter.parseDateTime("201302282256"));
		DateTime secondDateTime = new DateTime(2013, 2, 28, 22, 56, 0, 0);

/*
		DateTimeFormatter originalFormatter = DateTimeFormat.forPattern("yyyy.MM.dd-HH:mm");
		String firstDateString = firstDateTime.toString(originalFormatter);
		String secondDateString = secondDateTime.toString(originalFormatter);

		// February 28, 2013 at 23:28 - one minute higher than other event, use as upper bound for search over past hour.
		DateTime thirdDateTime = new DateTime(2013, 2, 28, 23, 28, 0, 0);
		Map<String, String> originalRepresentation = counterService.getMinForLastHourInOriginalFormat(counterName, thirdDateTime);

		//{2013.02.28-22:56=1, 2013.02.28-23:27=1}
		assertTrue(originalRepresentation.containsKey(firstDateString));
		assertTrue(originalRepresentation.containsKey(secondDateString));
		assertEquals(originalRepresentation.get(firstDateString), "1");
		assertEquals(originalRepresentation.get(secondDateString), "1");
  */

		//TODO test counter list.

		//TODO range queries
	}
}
