package org.springframework.xd.analytics.metrics.redis;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.core.AggregateCount;
import org.springframework.xd.analytics.metrics.core.AggregateCounterService;
import org.springframework.xd.analytics.metrics.core.MetricUtils;

/**
 * @author Mark Pollack
 * @author Luke Taylor
 */
public class RedisAggregateCounterService implements AggregateCounterService {
	protected final Log logger = LogFactory.getLog(this.getClass());
	private final StringRedisTemplate stringRedisTemplate;

	public RedisAggregateCounterService(RedisConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory);
		this.stringRedisTemplate = new StringRedisTemplate(connectionFactory);
		this.stringRedisTemplate.afterPropertiesSet();
	}

	@Override
	public void increment(String name) {
		increment(name, 1, new DateTime());
	}

	@Override
	public void increment(String name, int amount, DateTime dateTime) {
		AggregateKeyGenerator akg = new AggregateKeyGenerator(name, dateTime);
		
		//Keep track of all the hashes created in a set, enables easy deletion of hashes.
		SetOperations<String, String> setOps = stringRedisTemplate.opsForSet();
		String metaKey = getKeyForCounterKeys(name);

		//Increment total count.
		Long returnVal = stringRedisTemplate.opsForValue().increment(akg.getTotalKey(), amount);
		if (returnVal == amount) {
			// Add new counters to bookkeeping set of keys
			setOps.add(metaKey, akg.getTotalKey());
			setOps.add(getKeyForAllRootCounterNames(), name);
		}
		HashOperations<String, String, String> hashOps = stringRedisTemplate.opsForHash();
		returnVal = hashOps.increment(akg.getYearsKey(), akg.getYear(), amount);
		if (returnVal == amount) {
			setOps.add(metaKey, akg.getYearsKey());
		}
		returnVal = hashOps.increment(akg.getYearKey(), akg.getMonth(), amount);
		if (returnVal == amount) {
			setOps.add(metaKey, akg.getYearKey());
		}
		returnVal = hashOps.increment(akg.getMonthKey(), akg.getDay(), amount);
		if (returnVal == amount) {
			setOps.add(metaKey, akg.getMonthKey());
		}
		returnVal = hashOps.increment(akg.getDayKey(), akg.getHour(), amount);
		if (returnVal == amount) {
			setOps.add(metaKey, akg.getDayKey());
		}
		returnVal = hashOps.increment(akg.getHourKey(), akg.getMinute(), amount);
		if (returnVal == amount) {
			setOps.add(metaKey, akg.getHourKey());
		}
	}

	@Override
	public int getTotalCounts(String name) {
		AggregateKeyGenerator akg = new AggregateKeyGenerator(name);
		logger.trace("TotalCounts - TotalKey = " + akg.getTotalKey());
		Object val = stringRedisTemplate.opsForValue().get(akg.getTotalKey());
		if (val != null) {
			try {
				logger.trace("TotalCounts - Total Value Type = " + val.getClass());
				logger.trace("TotalCounts - Total Value = " + val);
				return Integer.parseInt(val.toString());
			} catch (NumberFormatException e) {
				logger.error("Could not parse total count value returned from redis key [" + akg.getTotalKey() + "]", e);
				return -1;
			}
		} else {
			logger.trace("TotalCounts - val is null");
			return -1;
		}
	}

	@Override
	public void deleteCounter(String name) {
		String keySet = getKeyForCounterKeys(name);
		SetOperations<String, String> setOps = stringRedisTemplate.opsForSet();
		Set<String> keys = setOps.members(keySet);
		stringRedisTemplate.delete(keys);
		setOps.remove(getKeyForAllRootCounterNames(), name);
		AggregateKeyGenerator akg = new AggregateKeyGenerator(name);
		stringRedisTemplate.delete(akg.getTotalKey());
	}

	/**
	 * Return the counts at the specified resolution over a given time interval.
	 */
	@Override
	public AggregateCount getCounts(String name, Interval interval, DateTimeField resolution) {
		MutableDateTime dt = new MutableDateTime(interval.getStart());

		DateTime end = interval.getEnd();
		Chronology c = interval.getChronology();
		DurationField resolutionDuration = resolution.getDurationField();

		int[] counts;

		if (resolutionDuration.getUnitMillis() == DateTimeConstants.MILLIS_PER_MINUTE) {
			// Iterate through each hour in the interval and load the minutes for it
			dt.setRounding(c.hourOfDay());
			Duration step = Duration.standardHours(1);
			List<int[]> hours = new ArrayList<int[]>();
			while (dt.isBefore(end)) {
				hours.add(getMinCountsForHour(name, dt));
				dt.add(step);
			}
			counts = MetricUtils.concatArrays(hours,
					interval.getStart().getMinuteOfHour(),
					interval.toPeriod().toStandardMinutes().getMinutes(),
					60);

		} else if (resolutionDuration.getUnitMillis() == DateTimeConstants.MILLIS_PER_HOUR) {
			DateTime cursor = new DateTime(c.dayOfMonth().roundFloor(interval.getStart().getMillis()));
			List<int[]> days = new ArrayList<int[]>();
			Duration step = Duration.standardHours(24);
			while (cursor.isBefore(end)) {
				days.add(getHourCountsForDay(name, cursor));
				cursor = cursor.plus(step);
			}

			counts = MetricUtils.concatArrays(days,
					interval.getStart().getHourOfDay(),
					interval.toPeriod().toStandardHours().getHours(),
					24);

		} else {
			throw new IllegalArgumentException("Only minute or hour resolution is currently supported");
		}
		return new AggregateCount(name, interval, counts, resolution);
	}

	private int[] getHourCountsForDay(String name, DateTime day) {
		AggregateKeyGenerator akg = new AggregateKeyGenerator(name, day.toDateMidnight());
		return convertToArray(getEntries(akg.getDayKey()), 24);
	}

	private int[] getMinCountsForHour(String name, ReadableDateTime dateTime) {
		return getMinCountsForHour(name, dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(), dateTime.getHourOfDay());
	}

	private int[] getMinCountsForHour(String name, int year, int month, int day, int hour) {
		DateTime dt = new DateTime().withYear(year).withMonthOfYear(month).withDayOfMonth(day).withHourOfDay(hour);
		AggregateKeyGenerator akg = new AggregateKeyGenerator(name, dt);
		return convertToArray(getEntries(akg.getHourKey()), 60);
	}

	/**
	 * The key containing the set under which all the keys for a counter are stored.
	 */
	String getKeyForCounterKeys(String counterName) {
		return "metric_meta.aggregatecounters." + counterName;
	}

	/**
	 * Meta-key under which the names of all counters are stored.
	 */
	String getKeyForAllRootCounterNames() {
		return "metric_meta.aggregatecounters.allRootCounterNames";
	}

	@Override
	public Set<String> getAll() {
		return stringRedisTemplate.opsForSet().members(getKeyForAllRootCounterNames());
	}

	private Map<Object, Object> getEntries(String key) {
		//TODO cleanup with more type specific template data types
		return stringRedisTemplate.opsForHash().entries(key);
	}

	private int[] convertToArray(Map<Object, Object> map, int size) {
		int[] minutes = new int[size];
		for (Map.Entry<Object, Object> cursor : map.entrySet()) {
			Integer minute = Integer.valueOf(cursor.getKey().toString());
			Integer count = Integer.valueOf(cursor.getValue().toString());
			minutes[minute] = count;
		}
		return minutes;
	}

}
