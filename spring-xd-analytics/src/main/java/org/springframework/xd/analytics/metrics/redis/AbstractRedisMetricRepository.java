package org.springframework.xd.analytics.metrics.redis;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.core.Metric;
import org.springframework.xd.analytics.metrics.core.MetricRepository;

/**
 * Common base functionality for Redis implementations.
 *
 * Only handles single values (not lists, maps etc).
 *
 * @author Luke Taylor
 */
abstract class AbstractRedisMetricRepository<M extends Metric, V> implements MetricRepository<M> {
	protected final String metricPrefix;
	protected final ValueOperations<String, V> valueOperations;
	protected final RedisOperations<String, V> redisOperations;

	@SuppressWarnings("unchecked")
	AbstractRedisMetricRepository(RedisConnectionFactory connectionFactory, String metricPrefix) {
		Assert.notNull(connectionFactory);
		Assert.hasText(metricPrefix, "metric prefix cannot be empty");
		this.metricPrefix = metricPrefix;
		ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
		Class<?> valueClass = (Class<?>) parameterizedType.getActualTypeArguments()[1];
		this.redisOperations = RedisUtils.createRedisTemplate(connectionFactory, valueClass);
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
	 * Template method to create a single instance of the metric.
	 *
	 * @param name the metric name
	 * @param value the initial value.
	 * @return the metric instance
	 */
	abstract M create(String name, V value);

	/**
	 * @return The default value for a metric (usually zero).
	 */
	abstract V defaultValue();

	/**
	 * Provides the key for a named metric.
	 * By default this appends the name to the metricPrefix value.
	 *
	 * @param metricName the name of the metric
	 * @return the redis key under which the metric is stored
	 */
	protected String getMetricKey(String metricName) {
		return metricPrefix + metricName;
	}

	@Override
	public <S extends M> S save(S metric) {
		String metricKey = getMetricKey(metric.getName());
		if (this.valueOperations.get(metricKey) == null) {
			this.valueOperations.set(metricKey, defaultValue());
		}
		return metric;
	}

	@Override
	public <S extends M> Iterable<S> save(Iterable<S> metrics) {
		List<S> results = new ArrayList<S>();
		for (S m: metrics) {
			results.add(save(m));
		}
		return results;
	}

	@Override
	public void delete(String name) {
		Assert.notNull(name, "The name of the metric must not be null");
		this.redisOperations.delete(getMetricKey(name));
	}

	@Override
	public void delete(M metric) {
		Assert.notNull(metric, "The metric must not be null");
		this.redisOperations.delete(getMetricKey(metric.getName()));
	}

	@Override
	public void delete(Iterable<? extends M> metrics) {
		for (M metric: metrics) {
			delete(metric);
		}
	}

	@Override
	public M findOne(String name) {
		Assert.notNull(name, "The name of the gauge must not be null");
		String gaugeKey = getMetricKey(name);
		if (redisOperations.hasKey(gaugeKey)) {
			V value = this.valueOperations.get(gaugeKey);
			return create(name, value);
		} else {
			return null;
		}
	}

	@Override
	public boolean exists(String s) {
		return findOne(s) != null;
	}

	@Override
	public List<M> findAll() {
		List<M> gauges = new ArrayList<M>();
		//TODO asking for keys is not recommended.  See http://redis.io/commands/keys
		//     Need to keep track of created instances explicitly.
		Set<String> keys = this.redisOperations.keys(this.metricPrefix + "*");
		for (String key : keys) {
			if (!key.matches(metricPrefix + ".+?_\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}:\\d{2}")) {
				V value = this.valueOperations.get(key);
				String name = key.substring(metricPrefix.length());
				M m = create(name, value);
				gauges.add(m);
			}
		}
		return gauges;

	}

	@Override
	public Iterable<M> findAll(Iterable<String> keys) {
		List<M> results = new ArrayList<M> ();

		for (String k: keys) {
			M value = findOne(k);
			if (value != null) {
				results.add(value);
			}
		}
		return results;
	}

	@Override
	public long count() {
		return findAll().size();
	}
}
