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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.core.Gauge;
import org.springframework.xd.analytics.metrics.core.GaugeRepository;

/**
 * Redis backed implementation that uses Redis keys to store and update the value.
 * The naming strategy for keys in Redis is "gauges."  This means a Gauge named simpleGauge appears
 * under the name "gauges.simpleGauge" in Redis.
 *
 * There is a default expiry of 60 minutes for the gauges stored in redis.  This can be changed
 * using a setter method
 *
 * @author Mark Pollack
 *
 */
public class RedisGaugeRepository extends AbstractRedisMetricRepository implements GaugeRepository {

	public RedisGaugeRepository(RedisConnectionFactory connectionFactory) {
		this(connectionFactory, "gauges.");
	}

	public RedisGaugeRepository(RedisConnectionFactory connectionFactory, String gaugePrefix) {
		super(connectionFactory, gaugePrefix);
	}

	@Override
	public Gauge save(Gauge gauge) {
		//Apply prefix for persistence purposes
		String gaugeKey = getGaugeKey(gauge);
		if (this.valueOperations.get(gaugeKey) == null) {
			this.valueOperations.set(gaugeKey, 0L);
		}
		return gauge;
	}

	@Override
	public void delete(String name) {
		Assert.notNull(name, "The name of the gauge must not be null");
		//Apply prefix for persistence purposes
		this.redisOperations.delete(getGaugeKey(name));
	}

	@Override
	public void delete(Gauge gauge) {
		Assert.notNull(gauge, "The gauge must not be null");
		//Apply prefix for persistence purposes
		this.redisOperations.delete(getGaugeKey(gauge));
	}

	@Override
	public Gauge findOne(String name) {
		Assert.notNull(name, "The name of the gauge must not be null");
		String gaugeKey = getGaugeKey(name);
		if (redisOperations.hasKey(gaugeKey)) {
			Long value = this.valueOperations.get(gaugeKey);
			Gauge g = new Gauge(name, value);
			return g;
		} else {
			return null;
		}
	}

	@Override
	public List<Gauge> findAll() {
		List<Gauge> gauges = new ArrayList<Gauge>();
		//TODO asking for keys is not recommended.  See http://redis.io/commands/keys
		//     Need to keep track of created gauges explicitly.
		Set<String> keys = this.redisOperations.keys(this.metricPrefix + "*");
		for (String key : keys) {
			if (!key.matches(metricPrefix + ".+?_\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}:\\d{2}")) {
				Long value = this.valueOperations.get(key);
				String name = key.substring(metricPrefix.length());
				Gauge g = new Gauge(name, value);
				gauges.add(g);
			}
		}
		return gauges;

	}

	public void setValue(String name, long value) {
		valueOperations.set(getGaugeKey(name), value);
	}

	public void reset(String name) {
		valueOperations.set(getGaugeKey(name), 0L);
	}

	public String getGaugeKey(Gauge gauge) {
		return metricPrefix + gauge.getName();
	}

	public String getGaugeKey(String name) {
		return metricPrefix + name;
	}

}
