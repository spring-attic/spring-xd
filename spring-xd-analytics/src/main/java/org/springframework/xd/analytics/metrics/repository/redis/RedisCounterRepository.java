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
package org.springframework.xd.analytics.metrics.repository.redis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.analytics.metrics.core.Counter;
import org.springframework.xd.analytics.metrics.repository.CounterRepository;

/**
 * Redis backed implementation that uses Redis keys to store and update the value.
 * The naming strategy for keys in Redis is "counts."  This means a counter named simpleCounter appears
 * under the name "counts.simpleCounter" in Redis.
 * 
 * There is a default expiry of 60 minutes for the counters stored in redis.  This can be changed 
 * using a setter method
 * 
 * @author Mark Pollack
 *
 */
public class RedisCounterRepository implements CounterRepository {

	private static final String DEFAULT_COUNTER_PREFIX = "counts.";	
	private volatile String counterPrefix = DEFAULT_COUNTER_PREFIX;
	
	private static final int DEFAULT_EXPIRY_TIME_IN_MINUTES = 60;
	private volatile int defaultExpiryTimeInMinutes = DEFAULT_EXPIRY_TIME_IN_MINUTES;
	private final ValueOperations<String, Long> valueOperations;
	private final RedisOperations<String, Long> redisOperations;
	
	public RedisCounterRepository(RedisConnectionFactory connectionFactory) {
		this(connectionFactory, null);	
	}
	
	public RedisCounterRepository(RedisConnectionFactory connectionFactory, String counterPrefix) {
		Assert.notNull(connectionFactory);
		RedisTemplate<String, Long> redisTemplate = new RedisTemplate<String, Long>();
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new GenericToStringSerializer<Long>(Long.class));

		//avoids proxy
		redisTemplate.setExposeConnection(true);
		
		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.afterPropertiesSet();
		this.redisOperations = redisTemplate;
		this.valueOperations = redisOperations.opsForValue();
		
		if (StringUtils.hasText(counterPrefix)) {
			this.counterPrefix = counterPrefix;
		}
		
	}
	
	@Override
	public Counter save(Counter counter) {
		//Apply prefix for persistence purposes
		String counterKey = getCounterKey(counter);
		if (this.valueOperations.get(counterKey) == null) {
			this.valueOperations.set(counterKey, 0L);
		}
		return counter;
	}

	@Override
	public void delete(String name) {
		Assert.notNull(name, "The name of the counter must not be null");
		//Apply prefix for persistence purposes
		this.redisOperations.delete(getCounterKey(name));
	}

	@Override
	public void delete(Counter counter) {
		Assert.notNull(counter, "The counter must not be null");
		//Apply prefix for persistence purposes
		this.redisOperations.delete(getCounterKey(counter));
	}


	@Override
	public Counter findOne(String name) {
		Assert.notNull(name, "The name of the counter must not be null");
		String counterKey = getCounterKey(name);
		if (redisOperations.hasKey(counterKey)) {
			Long value = this.valueOperations.get(counterKey);
			Counter c = new Counter(name, value); 
			return c;
		} else {
			return null;
		}
	}
	
	@Override
	public List<Counter> findAll() {
		List<Counter> counters = new ArrayList<Counter>();
		//TODO asking for keys is not recommended.  See http://redis.io/commands/keys
		//     Need to keep track of created counters explicitly.
		Set<String> keys = this.redisOperations.keys(this.counterPrefix + "*");
		for (String key : keys) {
			if (!key.matches(counterPrefix + ".+?_\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}:\\d{2}")) {
				Long value = this.valueOperations.get(key);
				String name = key.substring(counterPrefix.length());
				Counter c = new Counter(name, value); 
				counters.add(c);
			}
		}
		return counters;
		
	}

	
	public void increment(String name) {
		valueOperations.increment(getCounterKey(name), 1);
	}
	
	public void decrement(String name) {
		valueOperations.increment(getCounterKey(name), -1);
	}
	
	public void reset(String name) {
		valueOperations.set(getCounterKey(name), 0L);
	}
	
	public void updateExpirtyTimeInMinutes(String counterName, int numberOfMinutes) {
		this.redisOperations.expire(getCounterKey(counterName), defaultExpiryTimeInMinutes, TimeUnit.MINUTES);
	}
	
	
	public int getDefaultExpiryTimeInMinutes() {
		return defaultExpiryTimeInMinutes;
	}

	public void setDefaultExpiryTimeInMinutes(int defaultExpiryTimeInMinutes) {
		this.defaultExpiryTimeInMinutes = defaultExpiryTimeInMinutes;
	}
	

	public String getCounterKey(Counter counter) {
		return counterPrefix + counter.getName();
	}
	
	public String getCounterKey(String name) {
		return counterPrefix + name;
	}

}
