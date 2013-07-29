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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.xd.analytics.metrics.redis.RedisAggregateCounterRepository;
import org.springframework.xd.analytics.metrics.redis.RedisCounterRepository;
import org.springframework.xd.analytics.metrics.redis.RedisFieldValueCounterRepository;
import org.springframework.xd.analytics.metrics.redis.RedisGaugeRepository;
import org.springframework.xd.analytics.metrics.redis.RedisRichGaugeRepository;

import static org.mockito.Mockito.*;

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
		return new RedisFieldValueCounterRepository(redisConnectionFactory());
	}

	@Bean
	public RedisRichGaugeRepository redisRichGaugeRepository() {
		return new RedisRichGaugeRepository(redisConnectionFactory());
	}

	@Bean
	public RedisGaugeRepository redisGaugeRepository() {
		return new RedisGaugeRepository(redisConnectionFactory());
	}

	@Bean
	public RedisCounterRepository redisCounterRepository() {
		return new RedisCounterRepository(redisConnectionFactory());
	}

	@Bean
	public RedisAggregateCounterRepository redisAggregateCounterRepository() {
		return new RedisAggregateCounterRepository(redisConnectionFactory());
	}

	@Bean
	public StringRedisTemplate stringRedisTemplate() {
		return new StringRedisTemplate(redisConnectionFactory());
	}

	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		try {
			LettuceConnectionFactory cf = new LettuceConnectionFactory();
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
