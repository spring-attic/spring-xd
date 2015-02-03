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

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.retry.RetryOperations;

/**
 * Utility to create a RedisRetryTemplate instance property configured for use with XD analytical operations
 *
 * @author Luke Taylor
 */
class RedisUtils {

	/**
	 * Create a RedisRetryTemplate for a specific value class and serializer options.
	 * @param connectionFactory the connection factory to use
	 * @param valueClass the class to use for the {@link org.springframework.data.redis.serializer.RedisSerializer}
	 *                   for values.
	 * @param retryOperations
	 * @param <K> the Redis key type against which the template works (usually a String)
	 * @param <V> the Redis value type against which the template works
	 * @return a newly instantiated RedisRetryTemplate
	 */
	static <K, V> RedisRetryTemplate<K, V> createRedisRetryTemplate(RedisConnectionFactory connectionFactory, Class<V> valueClass,
			RetryOperations retryOperations) {
		RedisRetryTemplate<K, V> redisTemplate = new RedisRetryTemplate<K, V>();
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new GenericToStringSerializer<V>(valueClass));

		// avoids proxy
		redisTemplate.setExposeConnection(true);

		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.setRetryOperations(retryOperations);

		redisTemplate.afterPropertiesSet();
		return redisTemplate;
	}
}
