/*
 * Copyright 2011-2013 the original author or authors.
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
import org.springframework.util.StringUtils;
import org.springframework.xd.analytics.metrics.MetricsException;
import org.springframework.xd.analytics.metrics.core.MetricUtils;
import org.springframework.xd.analytics.metrics.core.RichGauge;

/**
 * @author Luke Taylor
 */
public final class RedisRichGaugeRepository extends AbstractRedisMetricRepository <RichGauge, String> {
	private static final String ZERO = serialize(new RichGauge("zero"));

	public RedisRichGaugeRepository(RedisConnectionFactory connectionFactory) {
		super(connectionFactory, "richgauges.");
	}

	@Override
	RichGauge create(String name, String value) {
		String[] parts = StringUtils.delimitedListToStringArray(value, " ");

		return new RichGauge(
				name,
				Double.valueOf(parts[0]),
				Double.valueOf(parts[1]),
				Double.valueOf(parts[2]),
				Double.valueOf(parts[3]),
				Double.valueOf(parts[4]),
				Long.valueOf(parts[5])
		);
	}

	@Override
	public <S extends RichGauge> S save(S gauge) {
		String key = getMetricKey(gauge.getName());
		valueOperations.set(key, serialize(gauge));
		return gauge;
	}

	@Override
	String defaultValue() {
		return ZERO;
	}

	public void setValue(String name, double value) {
		String key = getMetricKey(name);
		RichGauge g = findOne(name);
		if (g == null) {
			throw new MetricsException("Gauge " + name + " not found");
		}
		MetricUtils.setRichGaugeValue(g, value);
		valueOperations.set(key, serialize(g));
	}

	private static String serialize(RichGauge g) {
		StringBuilder sb = new StringBuilder();
		sb.append(Double.toString(g.getValue())).append(" ");
		sb.append(Double.toString(g.getAlpha())).append(" ");
		sb.append(Double.toString(g.getAverage())).append(" ");
		sb.append(Double.toString(g.getMax())).append(" ");
		sb.append(Double.toString(g.getMin())).append(" ");
		sb.append(Long.toString(g.getCount()));
		return sb.toString();
	}

	public void reset(String name) {
		valueOperations.set(getMetricKey(name), ZERO);
	}
}
