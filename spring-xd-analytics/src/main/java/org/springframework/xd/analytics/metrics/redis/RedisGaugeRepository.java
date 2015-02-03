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
import org.springframework.retry.RetryOperations;
import org.springframework.xd.analytics.metrics.core.Gauge;
import org.springframework.xd.analytics.metrics.core.GaugeRepository;

/**
 * Redis backed implementation that uses Redis keys to store and update the value. The naming strategy for keys in Redis
 * is "gauges." This means a Gauge named simpleGauge appears under the name "gauges.simpleGauge" in Redis.
 *
 * @author Mark Pollack
 */
public class RedisGaugeRepository extends AbstractRedisMetricRepository<Gauge, Long>
		implements GaugeRepository {

	public RedisGaugeRepository(RedisConnectionFactory connectionFactory, RetryOperations retryOperations) {
		this(connectionFactory, "gauges.", retryOperations);
	}

	public RedisGaugeRepository(RedisConnectionFactory connectionFactory,
								String gaugePrefix, RetryOperations retryOperations) {
		super(connectionFactory, gaugePrefix, Long.class, retryOperations);
	}


	@Override
	Gauge create(String name, Long value) {
		return new Gauge(name, value);
	}

	@Override
	Long defaultValue() {
		return 0L;
	}

	@Override
	Long value(Gauge metric) {
		return metric.getValue();
	}

	@Override
	public void recordValue(String name, long value) {
		getValueOperations().set(getMetricKey(name), value);
	}

	@Override
	public void reset(String name) {
		getValueOperations().set(getMetricKey(name), 0L);
	}

}
