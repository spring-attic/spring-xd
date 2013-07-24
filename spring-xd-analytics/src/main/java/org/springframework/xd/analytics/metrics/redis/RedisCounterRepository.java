/*
 * Copyright 2013 the original author or authors.
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

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.xd.analytics.metrics.core.Counter;
import org.springframework.xd.analytics.metrics.core.CounterRepository;

/**
 * Redis backed implementation that uses Redis keys to store and update the value. The
 * naming strategy for keys in Redis is "counters." This means a counter named
 * simpleCounter appears under the name "counters.simpleCounter" in Redis.
 * 
 * There is a default expiry of 60 minutes for the counters stored in redis. This can be
 * changed using a setter method
 * 
 * @author Mark Pollack
 * 
 */
public final class RedisCounterRepository extends
		AbstractRedisMetricRepository<Counter, Long> implements CounterRepository {

	public RedisCounterRepository(RedisConnectionFactory connectionFactory) {
		this(connectionFactory, "counters.");
	}

	public RedisCounterRepository(RedisConnectionFactory connectionFactory,
			String metricPrefix) {
		super(connectionFactory, metricPrefix);
	}

	@Override
	Counter create(String name, Long value) {
		return new Counter(name, value);
	}

	@Override
	Long defaultValue() {
		return 0L;
	}

	@Override
	Long value(Counter metric) {
		return metric.getValue();
	}

	@Override
	public long increment(String name) {
		return valueOperations.increment(getMetricKey(name), 1);
	}

	@Override
	public long decrement(String name) {
		return valueOperations.increment(getMetricKey(name), -1);
	}

	@Override
	public void reset(String name) {
		valueOperations.set(getMetricKey(name), 0L);
	}

}
