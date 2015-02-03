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

package org.springframework.xd.analytics.metrics.common;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.retry.RetryOperations;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.xd.analytics.metrics.redis.RedisAggregateCounterRepository;
import org.springframework.xd.analytics.metrics.redis.RedisCounterRepository;
import org.springframework.xd.analytics.metrics.redis.RedisFieldValueCounterRepository;
import org.springframework.xd.analytics.metrics.redis.RedisGaugeRepository;
import org.springframework.xd.analytics.metrics.redis.RedisRichGaugeRepository;

import java.util.Collections;

/**
 * Provides Redis backed repositories, to be tested one by one in Redis variant of tests.
 *
 * @author Mark Pollack
 * @author Luke Taylor
 * @author Gary Russell
 *
 */
@Configuration
public class RedisRepositoriesConfig {

	@Bean
	public RedisFieldValueCounterRepository redisFieldValueCounterRepository() {
		return new RedisFieldValueCounterRepository(redisConnectionFactory(), retryOperations());
	}

	@Bean
	public RedisRichGaugeRepository redisRichGaugeRepository() {
		return new RedisRichGaugeRepository(redisConnectionFactory(), retryOperations());
	}

	@Bean
	public RedisGaugeRepository redisGaugeRepository() {
		return new RedisGaugeRepository(redisConnectionFactory(), retryOperations());
	}

	@Bean
	@Qualifier("simple")
	public RedisCounterRepository redisCounterRepository() {
		return new RedisCounterRepository(redisConnectionFactory(), retryOperations());
	}

	@Bean
	public RedisAggregateCounterRepository redisAggregateCounterRepository() {
		return new RedisAggregateCounterRepository(redisConnectionFactory(), retryOperations());
	}

	@Bean
	public StringRedisTemplate stringRedisTemplate() {
		return new StringRedisTemplate(redisConnectionFactory());
	}

	@Bean
	public RedisTemplate<String, Long> longRedisTemplate() {
		RedisTemplate<String, Long> result = new RedisTemplate<String, Long>();
		result.setConnectionFactory(redisConnectionFactory());
		return result;
	}

	@Bean
	public RetryOperations retryOperations() {
		RetryTemplate retryTemplate = new RetryTemplate();
		retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3, Collections.<Class<? extends Throwable>, Boolean>singletonMap(RedisConnectionFailureException.class, true)));
		ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
		backOffPolicy.setInitialInterval(1000L);
		backOffPolicy.setMaxInterval(1000L);
		backOffPolicy.setMultiplier(2);
		retryTemplate.setBackOffPolicy(backOffPolicy);
		return retryTemplate;
	}

	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		try {
			JedisConnectionFactory cf = new JedisConnectionFactory();
			cf.setHostName("localhost");
			cf.setPort(6379);
			cf.afterPropertiesSet();
			return cf;
		}
		catch (RedisConnectionFailureException e) {
			RedisConnectionFactory mockCF = mock(RedisConnectionFactory.class);
			when(mockCF.getConnection()).thenReturn(mock(RedisConnection.class));
			return mockCF;
		}
	}

}
