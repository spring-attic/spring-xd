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

import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryOperations;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.StringUtils;
import org.springframework.xd.analytics.metrics.core.MetricUtils;
import org.springframework.xd.analytics.metrics.core.RichGauge;
import org.springframework.xd.analytics.metrics.core.RichGaugeRepository;

import java.util.Collections;
import java.util.List;

/**
 * Repository for rich-gauges backed by Redis.
 *
 * @author Luke Taylor
 * @author Eric Bottard
 */
public class RedisRichGaugeRepository extends
		AbstractRedisMetricRepository<RichGauge, String> implements RichGaugeRepository {

	private static final String ZERO = serialize(new RichGauge("ZERO"));

	private RetryTemplate retryTemplate;

	public RedisRichGaugeRepository(RedisConnectionFactory connectionFactory, RetryOperations retryOperations) {
		super(connectionFactory, "richgauges.", String.class, retryOperations);

		// In case of concurrent access on same key, retry a bit before giving up eventually
		retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3, Collections.<Class<? extends Throwable>, Boolean>singletonMap(OptimisticLockingFailureException.class, true)));
		ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
		backOffPolicy.setInitialInterval(50L);
		backOffPolicy.setMaxInterval(1000L);
		backOffPolicy.setMultiplier(2);
		retryTemplate.setBackOffPolicy(backOffPolicy);

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

	@Override
	RichGauge create(String name, String value) {
		String[] parts = StringUtils.delimitedListToStringArray(value, " ");

		return new RichGauge(name, Double.valueOf(parts[0]), Double.valueOf(parts[1]),
				Double.valueOf(parts[2]), Double.valueOf(parts[3]),
				Double.valueOf(parts[4]), Long.valueOf(parts[5]));
	}

	@Override
	String defaultValue() {
		return ZERO;
	}

	@Override
	String value(RichGauge metric) {
		return serialize(metric);
	}

	@Override
	public void recordValue(final String name, final double value, final double alpha) {
		final String key = getMetricKey(name);

		retryTemplate.execute(new RetryCallback<Void, RuntimeException>() {

			@Override
			public Void doWithRetry(RetryContext context) {
				return getRedisOperations().execute(new SessionCallback<Void>() {
					@Override
					@SuppressWarnings("unchecked")
					public <K, V> Void execute(RedisOperations<K, V> operations) throws DataAccessException {
						operations.watch((K) key);
						RichGauge g = findOne(name);
						if (g == null) {
							g = new RichGauge(name);
						}

						operations.multi();
						MetricUtils.setRichGaugeValue(g, value, alpha);
						operations.opsForValue().set((K) key, (V) serialize(g));

						List<Object> result = operations.exec();
						if (result == null) {
							throw new OptimisticLockingFailureException(String.format("Failed to set value of rich-gauge '%s' to %f", name, value));
						}
						return null;
					}
				});
			}
		});
	}

	public void setRetryTemplate(RetryTemplate retryTemplate) {
		this.retryTemplate = retryTemplate;
	}

	@Override
	public void reset(String name) {
		getValueOperations().set(getMetricKey(name), ZERO);
	}
}
