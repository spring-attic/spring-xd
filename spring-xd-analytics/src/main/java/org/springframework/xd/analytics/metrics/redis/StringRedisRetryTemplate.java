/*
 * Copyright 2015 the original author or authors.
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

import org.springframework.data.redis.connection.DefaultStringRedisConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.retry.RetryOperations;

/**
 * String-focused extension of RedisRetryTemplate.
 *
 * @author Mark Pollack
 */
public class StringRedisRetryTemplate extends RedisRetryTemplate<String, String> {


	/**
	 * Constructs a new <code>StringRedisTemplate</code> instance with a set of retry operations.
	 *
	 * {@link #afterPropertiesSet()} still need to be called if creating outside a spring managed context
	 *
	 * @param connectionFactory connection factory for creating new connections
	 */
	public StringRedisRetryTemplate(RedisConnectionFactory connectionFactory, RetryOperations retryOperations) {
		RedisSerializer<String> stringSerializer = new StringRedisSerializer();
		setKeySerializer(stringSerializer);
		setValueSerializer(stringSerializer);
		setHashKeySerializer(stringSerializer);
		setHashValueSerializer(stringSerializer);
		setRetryOperations(retryOperations);
		setConnectionFactory(connectionFactory);
		afterPropertiesSet();
	}

	protected RedisConnection preProcessConnection(RedisConnection connection, boolean existingConnection) {
		return new DefaultStringRedisConnection(connection);
	}
}
