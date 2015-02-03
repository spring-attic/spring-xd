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

import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.retry.RetryOperations;
import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.core.FieldValueCounter;
import org.springframework.xd.analytics.metrics.core.FieldValueCounterRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RedisFieldValueCounterRepository implements FieldValueCounterRepository {

	private final String metricPrefix;

	private final StringRedisRetryTemplate redisTemplate;

	private static final String MARKER = "_marker_";

	public RedisFieldValueCounterRepository(RedisConnectionFactory connectionFactory, RetryOperations retryOperations) {
		this(connectionFactory, "fieldvaluecounters.", retryOperations);
	}

	public RedisFieldValueCounterRepository(RedisConnectionFactory connectionFactory, String metricPrefix,
											RetryOperations retryOperations) {
		Assert.notNull(connectionFactory);
		Assert.hasText(metricPrefix, "metric prefix cannot be empty");
		this.metricPrefix = metricPrefix;
		redisTemplate = new StringRedisRetryTemplate(connectionFactory, retryOperations);
		// avoids proxy
		redisTemplate.setExposeConnection(true);
		redisTemplate.afterPropertiesSet();
	}

	/*
	 * Note: Handler implementations typically use increment() variants to save state. The save() contract
	 * is to store the counter as a whole. Simplest approach is erase/rewrite.
	 */
	@Override
	public <S extends FieldValueCounter> S save(S fieldValueCounter) {
		delete(fieldValueCounter.getName());
		increment(fieldValueCounter.getName(), MARKER, 0);
		for (Map.Entry<String, Double> entry : fieldValueCounter.getFieldValueCount().entrySet()) {
			increment(fieldValueCounter.getName(), entry.getKey(), entry.getValue());
		}
		return fieldValueCounter;
	}



	@Override
	public <S extends FieldValueCounter> Iterable<S> save(Iterable<S> metrics) {
		List<S> results = new ArrayList<S>();
		for (S m : metrics) {
			results.add(save(m));
		}
		return results;
	}

	@Override
	public void delete(String name) {
		Assert.notNull(name, "The name of the FieldValueCounter must not be null");
		this.redisTemplate.delete(getMetricKey(name));
	}

	@Override
	public void delete(FieldValueCounter fieldValueCounter) {
		Assert.notNull(fieldValueCounter, "The FieldValueCounter must not be null");
		this.redisTemplate.delete(getMetricKey(fieldValueCounter.getName()));

	}

	@Override
	public void delete(Iterable<? extends FieldValueCounter> fvcs) {
		for (FieldValueCounter fvc : fvcs) {
			delete(fvc);
		}
	}

	@Override
	public FieldValueCounter findOne(String name) {
		Assert.notNull(name, "The name of the FieldValueCounter must not be null");
		String metricKey = getMetricKey(name);
		if (redisTemplate.hasKey(metricKey)) {
			Map<String, Double> values = getZSetData(metricKey);
			FieldValueCounter c = new FieldValueCounter(name, values);
			return c;
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
	public List<FieldValueCounter> findAll() {
		List<FieldValueCounter> counters = new ArrayList<FieldValueCounter>();
		// TODO asking for keys is not recommended. See
		// http://redis.io/commands/keys
		Set<String> keys = this.redisTemplate.keys(this.metricPrefix + "*");
		for (String key : keys) {
			// TODO remove this naming convention for minute aggregates
			if (!key.matches(this.metricPrefix
					+ ".+?_\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}:\\d{2}")) {
				if (this.redisTemplate.type(key) == DataType.ZSET) {
					Map<String, Double> values = getZSetData(key);
					String name = key.substring(metricPrefix.length());
					FieldValueCounter c = new FieldValueCounter(name, values);
					counters.add(c);
				}
			}
		}
		return counters;
	}

	@Override
	public Iterable<FieldValueCounter> findAll(Iterable<String> keys) {
		List<FieldValueCounter> results = new ArrayList<FieldValueCounter>();

		for (String k : keys) {
			FieldValueCounter value = findOne(k);
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

	@Override
	public void deleteAll() {
		Set<String> keys = redisTemplate.keys(metricPrefix + "*");
		if (keys.size() > 0) {
			redisTemplate.delete(keys);
		}
	}

	@Override
	public void increment(String counterName, String fieldName) {
		redisTemplate.boundZSetOps(getMetricKey(counterName)).incrementScore(fieldName, 1.0);
	}

	public void increment(String counterName, String fieldName, double score) {
		redisTemplate.boundZSetOps(getMetricKey(counterName)).incrementScore(fieldName, score);
	}

	@Override
	public void decrement(String counterName, String fieldName) {
		redisTemplate.boundZSetOps(getMetricKey(counterName)).incrementScore(fieldName, -1.0);
	}


	public void decrement(String counterName, String fieldName, double score) {
		redisTemplate.boundZSetOps(getMetricKey(counterName)).incrementScore(fieldName, -score);
	}

	@Override
	public void reset(String counterName, String fieldName) {
		redisTemplate.boundZSetOps(getMetricKey(counterName)).remove(fieldName);
	}


	/**
	 * Provides the key for a named metric. By default this appends the name to the metricPrefix value.
	 * 
	 * @param metricName the name of the metric
	 * @return the redis key under which the metric is stored
	 */
	protected String getMetricKey(String metricName) {
		return metricPrefix + metricName;
	}

	protected Map<String, Double> getZSetData(String counterKey) {
		// TODO directly serialize into a Map vs Set of TypedTuples to avoid extra copy
		Set<TypedTuple<String>> rangeWithScore = this.redisTemplate
				.boundZSetOps(counterKey).rangeWithScores(0, -1);
		Map<String, Double> values = new HashMap<String, Double>(
				rangeWithScore.size());
		for (Iterator<TypedTuple<String>> iterator = rangeWithScore.iterator(); iterator
				.hasNext();) {
			TypedTuple<String> typedTuple = iterator.next();
			if (!typedTuple.getValue().equals(MARKER)) {
				values.put(typedTuple.getValue(), typedTuple.getScore());
			}
		}
		return values;
	}

}
