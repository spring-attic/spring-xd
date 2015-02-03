/*
 * Copyright 2014 the original author or authors.
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
import java.util.Set;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.retry.RetryOperations;
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

	private final String metricPrefix;

	private final ValueOperations<String, V> valueOperations;


	public ValueOperations<String, V> getValueOperations() {
		return valueOperations;
	}


	public RedisOperations<String, V> getRedisOperations() {
		return redisOperations;
	}

	private final RedisOperations<String, V> redisOperations;

	@SuppressWarnings("unchecked")
	AbstractRedisMetricRepository(RedisConnectionFactory connectionFactory, String metricPrefix, Class<V> valueClass) {
		this(connectionFactory, metricPrefix, valueClass, null);
	}

	@SuppressWarnings("unchecked")
	AbstractRedisMetricRepository(RedisConnectionFactory connectionFactory, String metricPrefix, Class<V> valueClass,
								  RetryOperations retryOperations) {
		Assert.notNull(connectionFactory);
		Assert.hasText(metricPrefix, "metric prefix cannot be empty");
		this.metricPrefix = metricPrefix;
		this.redisOperations = RedisUtils.createRedisRetryTemplate(connectionFactory, valueClass, retryOperations);
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
	 * @return the value carried by the given metric
	 */
	abstract V value(M metric);

	/**
	 * Provides the key for a named metric. By default this appends the name to the metricPrefix value.
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
		valueOperations.set(metricKey, value(metric));
		return metric;
	}

	@Override
	public <S extends M> Iterable<S> save(Iterable<S> metrics) {
		List<S> results = new ArrayList<S>();
		for (S m : metrics) {
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
		for (M metric : metrics) {
			delete(metric);
		}
	}

	@Override
	public M findOne(String name) {
		Assert.notNull(name, "The name of the metric must not be null");
		String gaugeKey = getMetricKey(name);
		if (redisOperations.hasKey(gaugeKey)) {
			V value = this.valueOperations.get(gaugeKey);
			return create(name, value);
		}
		else {
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
		// TODO asking for keys is not recommended. See http://redis.io/commands/keys
		// Need to keep track of created instances explicitly.
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
		List<M> results = new ArrayList<M>();

		for (String k : keys) {
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
