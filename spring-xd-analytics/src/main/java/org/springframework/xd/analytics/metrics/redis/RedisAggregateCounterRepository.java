/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.xd.analytics.metrics.redis;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Months;
import org.joda.time.MutableDateTime;
import org.joda.time.ReadableDateTime;
import org.joda.time.Years;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.retry.RetryOperations;
import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.core.AggregateCount;
import org.springframework.xd.analytics.metrics.core.AggregateCountResolution;
import org.springframework.xd.analytics.metrics.core.AggregateCounterRepository;
import org.springframework.xd.analytics.metrics.core.MetricUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis implementation of {@link AggregateCounterRepository}. Subclasses and intercepts calls to
 * {@link RedisCounterRepository} to also track counts in various redis hashes.
 *
 * @author Eric Bottard
 * @author Luke Taylor
 */
@Qualifier("aggregate")
public class RedisAggregateCounterRepository extends RedisCounterRepository implements AggregateCounterRepository {

	protected HashOperations<String, String, Long> hashOperations;

	protected SetOperations<String, String> setOperations;

	public RedisAggregateCounterRepository(RedisConnectionFactory redisConnectionFactory, RetryOperations retryOperations) {
		super("aggregatecounters", redisConnectionFactory, retryOperations);
		RedisRetryTemplate<String, String> redisTemplate = new RedisRetryTemplate<String, String>();
		redisTemplate.setConnectionFactory(redisConnectionFactory);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new StringRedisSerializer());
		redisTemplate.setHashKeySerializer(new StringRedisSerializer());
		redisTemplate.setHashValueSerializer(new GenericToStringSerializer<Long>(Long.class));
		redisTemplate.setRetryOperations(retryOperations);
		redisTemplate.afterPropertiesSet();
		hashOperations = redisTemplate.opsForHash();
		setOperations = redisTemplate.opsForSet();
	}

	@Override
	public long increment(String name) {
		return increment(name, 1, new DateTime());
	}

	@Override
	public long increment(String name, long amount) {
		return increment(name, amount, new DateTime());
	}

	@Override
	public long increment(String name, long amount, DateTime dateTime) {
		final AggregateKeyGenerator akg = new AggregateKeyGenerator(getPrefix(), name, dateTime);

		String bookkeepingKey = bookkeepingKeyFor(name);

		doIncrementHash(akg.getYearsKey(), akg.getYear(), amount, bookkeepingKey);
		doIncrementHash(akg.getYearKey(), akg.getMonth(), amount, bookkeepingKey);
		doIncrementHash(akg.getMonthKey(), akg.getDay(), amount, bookkeepingKey);
		doIncrementHash(akg.getDayKey(), akg.getHour(), amount, bookkeepingKey);
		doIncrementHash(akg.getHourKey(), akg.getMinute(), amount, bookkeepingKey);

		return super.increment(name, amount);
	}

	/**
	 * Return the key under which are stored the names of the other keys used for the given counter.
	 */
	private String bookkeepingKeyFor(String counterName) {
		return "metric_meta.aggregatecounters." + counterName;
	}

	/**
	 * Internally increments the given hash key, keeping track of created hash for a given counter, so they can be
	 * cleaned up when needed.
	 */
	private void doIncrementHash(String key, String hashKey, long amount, String bookkeepingKey) {
		long newValue = hashOperations.increment(key, hashKey, amount);
		// TODO: the following test does not necessarily mean that the hash
		// is new, just that the key inside that hash is new. So we end up
		// calling add more than needed
		if (newValue == amount) {
			setOperations.add(bookkeepingKey, key);
		}
	}

	@Override
	public AggregateCount getCounts(String name, int nCounts, AggregateCountResolution resolution) {
		return getCounts(name, nCounts, new DateTime(), resolution);
	}

	@Override
	public AggregateCount getCounts(String name, int nCounts, DateTime endDate, AggregateCountResolution resolution) {
		Assert.notNull(endDate, "endDate cannot be null");

		return getCounts(name, new Interval(resolution.minus(endDate, nCounts - 1), endDate), resolution);
	}

	/**
	 * For each query, we need to convert the interval into two variations. One is the start and end points rounded to
	 * the resolution (used to calculate the number of entries to be returned from the query). The second is the start
	 * and end buckets we have to retrieve which may contain entries for the interval. For example, when querying
	 * at day resolution, the number of entries is the number of Joda time days between the start (rounded down to a
	 * day boundary) and the end plus one day (also rounded down). However, we need load the data from the buckets
	 * from the month the start day occurs in to the month end day occurs in. These are then concatenated, using the
	 * start day as the start index into the first array, and writing the total number of entries in sequence from that
	 * point into the combined result counts array.
	 */
	@Override
	public AggregateCount getCounts(String name, Interval interval, AggregateCountResolution resolution) {

		DateTime end = interval.getEnd();
		Chronology c = interval.getChronology();

		long[] counts;

		if (resolution == AggregateCountResolution.minute) {
			// Iterate through each hour in the interval and load the minutes for it
			MutableDateTime dt = new MutableDateTime(interval.getStart());
			dt.setRounding(c.hourOfDay());
			Duration step = Duration.standardHours(1);
			List<long[]> hours = new ArrayList<long[]>();
			while (dt.isBefore(end) || dt.isEqual(end)) {
				hours.add(getMinCountsForHour(name, dt));
				dt.add(step);
			}
			counts = MetricUtils.concatArrays(hours, interval.getStart().getMinuteOfHour(),
					interval.toPeriod().toStandardMinutes().getMinutes() + 1);

		}
		else if (resolution == AggregateCountResolution.hour) {
			DateTime cursor = new DateTime(c.dayOfMonth().roundFloor(interval.getStart().getMillis()));
			List<long[]> days = new ArrayList<long[]>();
			Duration step = Duration.standardHours(24);
			while (cursor.isBefore(end)) {
				days.add(getHourCountsForDay(name, cursor));
				cursor = cursor.plus(step);
			}

			counts = MetricUtils.concatArrays(days, interval.getStart().getHourOfDay(),
					interval.toPeriod().toStandardHours().getHours() + 1);

		}
		else if (resolution == AggregateCountResolution.day) {
			DateTime startDay = new DateTime(c.dayOfYear().roundFloor(interval.getStart().getMillis()));
			DateTime endDay = new DateTime(c.dayOfYear().roundFloor(end.plusDays(1).getMillis()));
			int nDays = Days.daysBetween(startDay, endDay).getDays();
			DateTime cursor = new DateTime(c.monthOfYear().roundFloor(interval.getStart().getMillis()));
			List<long[]> months = new ArrayList<long[]>();
			DateTime endMonth = new DateTime(c.monthOfYear().roundCeiling(interval.getEnd().plusMonths(1).getMillis()));
			while (cursor.isBefore(endMonth)) {
				months.add(getDayCountsForMonth(name, cursor));
				cursor = cursor.plusMonths(1);
			}

			counts = MetricUtils.concatArrays(months, interval.getStart().getDayOfMonth() - 1, nDays);
		}
		else if (resolution == AggregateCountResolution.month) {
			DateTime startMonth = new DateTime(c.monthOfYear().roundFloor(interval.getStartMillis()));
			DateTime endMonth = new DateTime(c.monthOfYear().roundFloor(end.plusMonths(1).getMillis()));
			int nMonths = Months.monthsBetween(startMonth, endMonth).getMonths();
			DateTime cursor = new DateTime(c.year().roundFloor(interval.getStartMillis()));
			List<long[]> years = new ArrayList<long[]>();
			DateTime endYear = new DateTime(c.year().roundCeiling(interval.getEnd().plusYears(1).getMillis()));
			while (cursor.isBefore(endYear)) {
				years.add(getMonthCountsForYear(name, cursor));
				cursor = cursor.plusYears(1);
			}

			counts = MetricUtils.concatArrays(years, interval.getStart().getMonthOfYear() - 1, nMonths);
		}
		else if (resolution == AggregateCountResolution.year) {
			DateTime startYear = new DateTime(interval.getStart().getYear(), 1, 1, 0, 0);
			DateTime endYear = new DateTime(end.getYear() + 1, 1, 1, 0, 0);
			int nYears = Years.yearsBetween(startYear, endYear).getYears();
			Map<String, Long> yearCounts = getYearCounts(name);
			counts = new long[nYears];

			for (int i = 0; i < nYears; i++) {
				int year = startYear.plusYears(i).getYear();
				Long count = yearCounts.get(Integer.toString(year));
				if (count == null) {
					count = 0L;
				}
				counts[i] = count;
			}
		}
		else {
			throw new IllegalStateException("Shouldn't happen. Unhandled resolution: " + resolution);
		}
		return new AggregateCount(name, interval, counts, resolution);
	}

	private Map<String, Long> getYearCounts(String name) {
		AggregateKeyGenerator akg = new AggregateKeyGenerator(getPrefix(), name, new DateTime());
		return getEntries(akg.getYearsKey());
	}

	private long[] getMonthCountsForYear(String name, DateTime year) {
		AggregateKeyGenerator akg = new AggregateKeyGenerator(getPrefix(), name, year);
		return convertToArray(getEntries(akg.getYearKey()), year.monthOfYear().getMaximumValue(), true); // Months in this year
	}

	private long[] getDayCountsForMonth(String name, DateTime month) {
		AggregateKeyGenerator akg = new AggregateKeyGenerator(getPrefix(), name, month.withTimeAtStartOfDay());
		return convertToArray(getEntries(akg.getMonthKey()), month.dayOfMonth().getMaximumValue(), true); // Days in this month
	}

	private long[] getHourCountsForDay(String name, DateTime day) {
		AggregateKeyGenerator akg = new AggregateKeyGenerator(getPrefix(), name, day.withTimeAtStartOfDay());
		return convertToArray(getEntries(akg.getDayKey()), 24, false);
	}

	private long[] getMinCountsForHour(String name, ReadableDateTime dateTime) {
		return getMinCountsForHour(name, dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(),
				dateTime.getHourOfDay());
	}

	private long[] getMinCountsForHour(String name, int year, int month, int day, int hour) {
		DateTime dt = new DateTime().withYear(year).withMonthOfYear(month).withDayOfMonth(day).withHourOfDay(hour);
		AggregateKeyGenerator akg = new AggregateKeyGenerator(getPrefix(), name, dt);
		return convertToArray(getEntries(akg.getHourKey()), 60, false);
	}

	private Map<String, Long> getEntries(String key) {
		return hashOperations.entries(key);
	}

	/**
	 * Will convert a (possibly sparse) map whose keys are String versions of numbers between 0 and size, to an array.
	 */
	private long[] convertToArray(Map<String, Long> map, int size, boolean unitOffset) {
		long[] values = new long[size];
		// Some joda fields (e.g. days of month are unit offset)
		int arrayOffset = unitOffset ? -1 : 0;
		for (Map.Entry<String, Long> cursor : map.entrySet()) {
			int offset = Integer.parseInt(cursor.getKey()) + arrayOffset;
			values[offset] = cursor.getValue();
		}
		return values;
	}

	@Override
	public void delete(String id) {
		String metricMetaKey = bookkeepingKeyFor(id);
		super.delete(id);
		Set<String> otherKeys = setOperations.members(metricMetaKey);
		// Add metric-meta SET's key
		otherKeys.add(metricMetaKey);
		redisOperations.delete(otherKeys);
	}
}
