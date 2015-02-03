/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.retry.RetryOperations;
import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.core.Counter;
import org.springframework.xd.analytics.metrics.core.CounterRepository;
import org.springframework.xd.store.AbstractRedisRepository;

/**
 *
 * @author Eric Bottard
 */
@Qualifier("simple")
public class RedisCounterRepository extends AbstractRedisRepository<Counter, String> implements CounterRepository {

	protected ValueOperations<String, Long> longOperations;

	public RedisCounterRepository(RedisConnectionFactory redisConnectionFactory, RetryOperations retryOperations) {
		this("counters", redisConnectionFactory, retryOperations);
	}

	public RedisCounterRepository(String repoPrefix, RedisConnectionFactory redisConnectionFactory,
			RetryOperations retryOperations) {
		super(repoPrefix, new StringRedisRetryTemplate(redisConnectionFactory, retryOperations));
		RedisTemplate<String, Long> longRedisTemplate = RedisUtils.createRedisRetryTemplate(redisConnectionFactory,
				Long.class, retryOperations);
		this.longOperations = longRedisTemplate.opsForValue();
	}

	@Override
	protected Counter deserialize(String redisKey, String v) {
		return new Counter(redisKey, Long.valueOf(v));
	}

	@Override
	protected String serialize(Counter entity) {
		return String.valueOf(entity.getValue());
	}

	@Override
	protected String keyFor(Counter entity) {
		Assert.notNull(entity);
		return entity.getName();
	}

	@Override
	protected String serializeId(String id) {
		return id;
	}

	@Override
	protected String deserializeId(String string) {
		return string;
	}

	@Override
	public long increment(String name) {
		return increment(name, 1L);
	}

	@Override
	public long increment(String name, long amount) {
		String redisKeyFromId = redisKeyFromId(name);
		trackMembership(redisKeyFromId);
		return longOperations.increment(redisKeyFromId, amount);
	}

	@Override
	public long decrement(String name) {
		String redisKeyFromId = redisKeyFromId(name);
		trackMembership(redisKeyFromId);
		return longOperations.increment(redisKeyFromId, -1);
	}

	@Override
	public void reset(String name) {
		String redisKeyFromId = redisKeyFromId(name);
		trackMembership(redisKeyFromId);
		longOperations.set(redisKeyFromId, 0L);
	}

}
