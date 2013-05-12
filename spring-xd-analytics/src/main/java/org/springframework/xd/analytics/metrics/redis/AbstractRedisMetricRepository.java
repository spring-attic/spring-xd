package org.springframework.xd.analytics.metrics.redis;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.Assert;

/**
 * Common base functionality for Redis implementations.
 *
 * @author Luke Taylor
 */
abstract class AbstractRedisMetricRepository {
	private volatile int defaultExpiryTimeInMinutes = -1;

	protected final String metricPrefix;
	protected final ValueOperations<String, Long> valueOperations;
	protected final RedisOperations<String, Long> redisOperations;

	AbstractRedisMetricRepository(RedisConnectionFactory connectionFactory, String metricPrefix) {
		Assert.notNull(connectionFactory);
		Assert.hasText(metricPrefix, "metric prefix cannot be empty");
		this.metricPrefix = metricPrefix;
		this.redisOperations = RedisUtils.createStringLongRedisTemplate(connectionFactory);
		this.valueOperations = redisOperations.opsForValue();
	}

	@Override
	public void deleteAll() {
		Set<String> keys = redisOperations.keys(metricPrefix + "*");
		if (keys.size() > 0) {
			redisOperations.delete(keys);
		}
	}

	/**
	 *
	 * @param metricKey the full key of the metric, including any prefix.
	 * @param numberOfMinutes the number of minutes the expiry time should be set to.
	 */
	public void updateExpiryTimeInMinutes(String metricKey, int numberOfMinutes) {
		this.redisOperations.expire(metricKey, numberOfMinutes, TimeUnit.MINUTES);
	}

	public int getDefaultExpiryTimeInMinutes() {
		return defaultExpiryTimeInMinutes;
	}

	public void setDefaultExpiryTimeInMinutes(int defaultExpiryTimeInMinutes) {
		this.defaultExpiryTimeInMinutes = defaultExpiryTimeInMinutes;
	}

}
