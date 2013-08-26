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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeField;
import org.joda.time.Duration;
import org.joda.time.DurationField;
import org.joda.time.Interval;
import org.joda.time.MutableDateTime;
import org.joda.time.ReadableDateTime;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.xd.analytics.metrics.core.AggregateCount;
import org.springframework.xd.analytics.metrics.core.AggregateCounterRepository;
import org.springframework.xd.analytics.metrics.core.MetricUtils;

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

	/**
	 * @param redisConnectionFactory
	 */
	public RedisAggregateCounterRepository(RedisConnectionFactory redisConnectionFactory) {
		super("aggregatecounters", redisConnectionFactory);
		RedisTemplate<String, String> redisTemplate = new RedisTemplate<String, String>();
		redisTemplate.setConnectionFactory(redisConnectionFactory);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new StringRedisSerializer());
		redisTemplate.setHashKeySerializer(new StringRedisSerializer());
		redisTemplate.setHashValueSerializer(new GenericToStringSerializer<Long>(Long.class));
		redisTemplate.afterPropertiesSet();
		hashOperations = redisTemplate.opsForHash();
		setOperations = redisTemplate.opsForSet();
	}

	@Override
	public long increment(String name) {
		return increment(name, 1, new DateTime());
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
	public AggregateCount getCounts(String name, Interval interval, DateTimeField resolution) {

		DateTime end = interval.getEnd();
		Chronology c = interval.getChronology();
		DurationField resolutionDuration = resolution.getDurationField();

		long[] counts;

		if (resolutionDuration.getUnitMillis() == DateTimeConstants.MILLIS_PER_MINUTE) {
			// Iterate through each hour in the interval and load the minutes for it
			MutableDateTime dt = new MutableDateTime(interval.getStart());
			dt.setRounding(c.hourOfDay());
			Duration step = Duration.standardHours(1);
			List<long[]> hours = new ArrayList<long[]>();
			while (dt.isBefore(end)) {
				hours.add(getMinCountsForHour(name, dt));
				dt.add(step);
			}
			counts = MetricUtils.concatArrays(hours, interval.getStart().getMinuteOfHour(),
					interval.toPeriod().toStandardMinutes().getMinutes() + 1, 60);

		}
		else if (resolutionDuration.getUnitMillis() == DateTimeConstants.MILLIS_PER_HOUR) {
			DateTime cursor = new DateTime(c.dayOfMonth().roundFloor(interval.getStart().getMillis()));
			List<long[]> days = new ArrayList<long[]>();
			Duration step = Duration.standardHours(24);
			while (cursor.isBefore(end)) {
				days.add(getHourCountsForDay(name, cursor));
				cursor = cursor.plus(step);
			}

			counts = MetricUtils.concatArrays(days, interval.getStart().getHourOfDay(),
					interval.toPeriod().toStandardHours().getHours() + 1, 24);

		}
		else {
			throw new IllegalArgumentException("Only minute or hour resolution is currently supported");
		}
		return new AggregateCount(name, interval, counts, resolution);
	}

	private long[] getHourCountsForDay(String name, DateTime day) {
		AggregateKeyGenerator akg = new AggregateKeyGenerator(getPrefix(), name, day.toDateMidnight());
		return convertToArray(getEntries(akg.getDayKey()), 24);
	}

	private long[] getMinCountsForHour(String name, ReadableDateTime dateTime) {
		return getMinCountsForHour(name, dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(),
				dateTime.getHourOfDay());
	}

	private long[] getMinCountsForHour(String name, int year, int month, int day, int hour) {
		DateTime dt = new DateTime().withYear(year).withMonthOfYear(month).withDayOfMonth(day).withHourOfDay(hour);
		AggregateKeyGenerator akg = new AggregateKeyGenerator(getPrefix(), name, dt);
		return convertToArray(getEntries(akg.getHourKey()), 60);
	}

	private Map<String, Long> getEntries(String key) {
		return hashOperations.entries(key);
	}

	/**
	 * Will convert a (possibly sparse) map whose keys are String versions of numbers between 0 and size, to an array.
	 */
	private long[] convertToArray(Map<String, Long> map, int size) {
		long[] values = new long[size];
		for (Map.Entry<String, Long> cursor : map.entrySet()) {
			int offset = Integer.parseInt(cursor.getKey());
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
