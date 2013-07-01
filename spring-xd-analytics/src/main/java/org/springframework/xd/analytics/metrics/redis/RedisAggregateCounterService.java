package org.springframework.xd.analytics.metrics.redis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
import org.springframework.xd.analytics.metrics.core.DayCounts;

/**
 * @author Mark Pollack
 * @author Luke Taylor
 */
public class RedisAggregateCounterService implements AggregateCounterService {
	protected final Log logger = LogFactory.getLog(this.getClass());
	private final StringRedisTemplate stringRedisTemplate;
	private final String metricPrefix;

	public RedisAggregateCounterService(RedisConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory);
		this.stringRedisTemplate = new StringRedisTemplate(connectionFactory);
		this.stringRedisTemplate.afterPropertiesSet();
		this.metricPrefix = "aggregatecounters.";
	}

	@Override
	public void increment(String name) {
		increment(name, 1, new DateTime());
	}

	@Override
	public void increment(String name, int amount, DateTime dateTime) {
		String key = getMetricKey(name);
		AggregateKeyGenerator akg = new AggregateKeyGenerator(key, dateTime);
		
		//Keep track of all the hashes created in a set, enables easy deletion of hashes.
		SetOperations<String, String> setOps = stringRedisTemplate.opsForSet();
		String metaKey = getKeyForAllCounterNames();

		//Increment total count.
		Long returnVal = stringRedisTemplate.opsForValue().increment(akg.getTotalKey(), amount);
		if (returnVal == amount) {
			// Add new counters to bookkeeping set of keys
			setOps.add(metaKey, akg.getTotalKey());
			setOps.add(getKeyForAllRootCounterNames(), key);
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
		AggregateKeyGenerator akg = new AggregateKeyGenerator(getMetricKey(name));
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

	/**
	 * Get the counts for the current day, at a specific resolution.
	 */
	@Override
	public DayCounts getDayCounts(String name, DateTimeField resolution) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Return the counts at the specified resolution over a given time interval.
	 */
	@Override
	public AggregateCount getCounts(String name, Interval interval, DateTimeField resolution) {
		MutableDateTime dt = new MutableDateTime(interval.getStart());
		dt.setRounding(resolution);

		DateTime end = interval.getEnd();
		Chronology c = interval.getChronology();
		DurationField resolutionDuration = resolution.getDurationField();

		if (resolutionDuration.getUnitMillis() == 60000) {
			// Iterate through each hour in the interval and load the minutes for it
			dt.setRounding(c.hourOfDay());
			Duration step = Duration.standardHours(1);
			List<int[]> hours = new ArrayList<int[]>();
			while (dt.isBefore(end)) {
				logger.debug("Getting data for " + AggregateKeyGenerator.dateTimeFormatter.print(dt));
				hours.add(getMinCountsForHour(name, dt));
				dt.add(step);
			}
			int[] counts = new int[interval.toPeriod().toStandardMinutes().getMinutes()];

			int startMinute = interval.getStart().getMinuteOfHour();

			for (int i=0; i < hours.size(); i++) {
				int[] hour = hours.get(i);
				for (int j=0; j < 60; j++) {
					int minute = i*60 + j - startMinute; // minute in the complete interval

					if (minute >= counts.length) {
						break;
					}

					if (minute >= 0) {
						counts[minute] = hour[j];
					}
				}
			}
			return new AggregateCount(name, interval, counts, resolution);
		} else {
			throw new IllegalArgumentException("Only minute resolution is currently supported");
		}
	}

	private Map<Integer, Integer> getYearlyCounts(String name) {
		String counterName = getMetricKey(name);
		AggregateKeyGenerator akg = new AggregateKeyGenerator(counterName);
		return convertMap(getEntries(akg.getYearsKey()));
	}

	private Map<Integer, Integer> getMonthlyCountsForYear(String name, Integer year) {
		String counterName = getMetricKey(name);
		DateTime dt = new DateTime().withYear(year);
		AggregateKeyGenerator akg = new AggregateKeyGenerator(counterName, dt);
		return convertMap(getEntries(akg.getYearKey()));
	}

	private Map<Integer, Integer> getDayCountsForMonth(String name, int year, int month) {
		String counterName = getMetricKey(name);
		DateTime dt = new DateTime().withYear(year).withMonthOfYear(month);
		AggregateKeyGenerator akg = new AggregateKeyGenerator(counterName, dt);
		return convertMap(getEntries(akg.getMonthKey()));
	}

	private Map<Integer, Integer> getHourCountsForDay(String name, int year, int month, int day) {
		String counterName = getMetricKey(name);
		DateTime dt = new DateTime().withYear(year).withMonthOfYear(month).withDayOfMonth(day);
		AggregateKeyGenerator akg = new AggregateKeyGenerator(counterName, dt);
		return convertMap(getEntries(akg.getDayKey()));
	}

	int[] getMinCountsForHour(String name, ReadableDateTime dateTime) {
		return getMinCountsForHour(name, dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(), dateTime.getHourOfDay());
	}

	int[] getMinCountsForHour(String name, int year, int month, int day, int hour) {
		String counterName = getMetricKey(name);
		DateTime dt = new DateTime().withYear(year).withMonthOfYear(month).withDayOfMonth(day).withHourOfDay(hour);
		AggregateKeyGenerator akg = new AggregateKeyGenerator(counterName, dt);
		return convertToMinuteArray(getEntries(akg.getHourKey()));
	}


	String getKeyForAllCounterNames() {
		return "meta.counters.allCounterNames";
	}

	String getKeyForAllRootCounterNames() {
		return "meta.counters.allRootCounterNames";
	}

//	Set<String> getAllRootCounterNames() {
//		return stringRedisTemplate.opsForSet().members(getKeyForAllRootCounterNames());
//	}

	protected String getMetricKey(String metricName) {
		return metricPrefix + metricName;
	}

	private Map<Object, Object> getEntries(String key) {
		//TODO cleanup with more type specific template data types
		return stringRedisTemplate.opsForHash().entries(key);
	}

	private int[] convertToMinuteArray(Map<Object, Object> map) {
		int[] minutes = new int[60];
		for (Map.Entry<Object, Object> cursor : map.entrySet()) {
			Integer minute = Integer.valueOf(cursor.getKey().toString());
			Integer count = Integer.valueOf(cursor.getValue().toString());
			minutes[minute] = count;
		}
		return minutes;
	}

	/**
	 *
	 * @param map returned data from redis
	 *
	 * @return parsed data, stripped key prefixes.
	 */
	private Map<Integer, Integer> convertMap(Map<Object, Object> map) {
		Map<Integer, Integer> countMap = new TreeMap<Integer,Integer>();
		for (Map.Entry<Object, Object> cursor : map.entrySet()) {
			String key = cursor.getKey().toString();
			Integer value = Integer.valueOf(cursor.getValue().toString());
			countMap.put(Integer.valueOf(key), value);
		}
		return countMap;
	}

}
