
package org.springframework.xd.analytics.metrics.redis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeField;
import org.joda.time.Duration;
import org.joda.time.DurationField;
import org.joda.time.Interval;
import org.joda.time.MutableDateTime;
import org.joda.time.ReadableDateTime;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.core.AggregateCount;
import org.springframework.xd.analytics.metrics.core.AggregateCounter;
import org.springframework.xd.analytics.metrics.core.AggregateCounterRepository;
import org.springframework.xd.analytics.metrics.core.MetricUtils;

/**
 * Redis implementation for {@link AggregateCounterRepository}. Uses several Redis hashes
 * under specific keys to store values.
 * 
 * @see AggregateKeyGenerator
 * 
 * @author Eric Bottard
 * @author Mark Pollack
 * @author Luke Taylor
 */
public class RedisAggregateCounterRepository implements AggregateCounterRepository {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private final StringRedisTemplate redisTemplate;

	public RedisAggregateCounterRepository(RedisConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory);
		this.redisTemplate = new StringRedisTemplate(connectionFactory);
		this.redisTemplate.afterPropertiesSet();
	}

	@Override
	public long increment(String name) {
		return increment(name, 1, new DateTime());
	}

	@Override
	@SuppressWarnings("unchecked")
	public long increment(final String name, final int amount, DateTime dateTime) {
		final AggregateKeyGenerator akg = new AggregateKeyGenerator(name, dateTime);
		final String metaKey = getKeyForCounterKeys(name);

		RedisCallback<Long> incrementAction = new RedisCallback<Long>() {

			RedisSerializer keySerializer = redisTemplate.getKeySerializer();

			RedisSerializer valueSerializer = redisTemplate.getValueSerializer();

			RedisSerializer hks = redisTemplate.getHashKeySerializer();

			@Override
			public Long doInRedis(RedisConnection connection) throws DataAccessException {
				final long value = amount;

				byte[] totalKey = keySerializer.serialize(akg.getTotalKey());
				long returnVal = connection.incrBy(totalKey, value);

				byte[] meta = keySerializer.serialize(metaKey);
				if (returnVal == value) {
					connection.sAdd(meta, totalKey);
					connection.sAdd(keySerializer.serialize(getKeyForAllRootCounterNames()),
							valueSerializer.serialize(name));
				}

				doIncrement(connection, meta, akg.getYearsKey(), akg.getYear(), amount);
				doIncrement(connection, meta, akg.getYearKey(), akg.getMonth(), amount);
				doIncrement(connection, meta, akg.getMonthKey(), akg.getDay(), amount);
				doIncrement(connection, meta, akg.getDayKey(), akg.getHour(), amount);
				doIncrement(connection, meta, akg.getHourKey(), akg.getMinute(), amount);

				return returnVal;
			}

			/**
			 * Increment value inside a specific hash, making sure the bookkeeping meta
			 * set is up to date.
			 * 
			 * @param meta the key used for overall tracking of aggregate counters
			 * @param key the name of the hash to act on
			 * @param hkey the key inside the hash to increment
			 * @param amount the quantity to increment by
			 */
			private void doIncrement(RedisConnection connection, byte[] meta, String key, String hkey, long amount) {
				byte[] keyBytes = keySerializer.serialize(key);
				byte[] hKeyBytes = hks.serialize(hkey);
				long returnVal = connection.hIncrBy(keyBytes, hKeyBytes, amount);

				if (returnVal == amount) {
					// Add new counters to bookkeeping set of keys
					// TODO: This currently involves unnecessary operations since the
					// return value
					// is that of the individual hash key and does not indicate that the
					// hash is new
					connection.sAdd(meta, valueSerializer.serialize(key));
				}
			}
		};

		return redisTemplate.execute(incrementAction);
	}

	@Override
	public int getTotal(String name) {
		AggregateKeyGenerator akg = new AggregateKeyGenerator(name);
		logger.trace("TotalCounts - TotalKey = " + akg.getTotalKey());
		Object val = redisTemplate.opsForValue().get(akg.getTotalKey());
		if (val != null) {
			try {
				logger.trace("TotalCounts - Total Value Type = " + val.getClass());
				logger.trace("TotalCounts - Total Value = " + val);
				return Integer.parseInt(val.toString());
			}
			catch (NumberFormatException e) {
				logger.error("Could not parse total count value returned from redis key [" + akg.getTotalKey() + "]", e);
				return -1;
			}
		}
		else {
			logger.trace("TotalCounts - val is null");
			return -1;
		}
	}

	@Override
	public void delete(String name) {
		String keySet = getKeyForCounterKeys(name);
		SetOperations<String, String> setOps = redisTemplate.opsForSet();
		Set<String> keys = setOps.members(keySet);
		redisTemplate.delete(keys);
		setOps.remove(getKeyForAllRootCounterNames(), name);
		AggregateKeyGenerator akg = new AggregateKeyGenerator(name);
		redisTemplate.delete(akg.getTotalKey());
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
			counts = MetricUtils.concatArrays(hours, interval.getStart().getMinuteOfHour(),
					interval.toPeriod().toStandardMinutes().getMinutes(), 60);

		}
		else if (resolutionDuration.getUnitMillis() == DateTimeConstants.MILLIS_PER_HOUR) {
			DateTime cursor = new DateTime(c.dayOfMonth().roundFloor(interval.getStart().getMillis()));
			List<int[]> days = new ArrayList<int[]>();
			Duration step = Duration.standardHours(24);
			while (cursor.isBefore(end)) {
				days.add(getHourCountsForDay(name, cursor));
				cursor = cursor.plus(step);
			}

			counts = MetricUtils.concatArrays(days, interval.getStart().getHourOfDay(),
					interval.toPeriod().toStandardHours().getHours(), 24);

		}
		else {
			throw new IllegalArgumentException("Only minute or hour resolution is currently supported");
		}
		return new AggregateCount(name, interval, counts, resolution);
	}

	private int[] getHourCountsForDay(String name, DateTime day) {
		AggregateKeyGenerator akg = new AggregateKeyGenerator(name, day.toDateMidnight());
		return convertToArray(getEntries(akg.getDayKey()), 24);
	}

	private int[] getMinCountsForHour(String name, ReadableDateTime dateTime) {
		return getMinCountsForHour(name, dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(),
				dateTime.getHourOfDay());
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
		return redisTemplate.opsForSet().members(getKeyForAllRootCounterNames());
	}

	private Map<Object, Object> getEntries(String key) {
		// TODO cleanup with more type specific template data types
		return redisTemplate.opsForHash().entries(key);
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

	@Override
	public <S extends AggregateCounter> S save(S entity) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public <S extends AggregateCounter> Iterable<S> save(Iterable<S> entities) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public AggregateCounter findOne(String id) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public boolean exists(String id) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Iterable<AggregateCounter> findAll() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public Iterable<AggregateCounter> findAll(Iterable<String> ids) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public long count() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public void delete(AggregateCounter entity) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public void delete(Iterable<? extends AggregateCounter> entities) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	public void deleteAll() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

}
