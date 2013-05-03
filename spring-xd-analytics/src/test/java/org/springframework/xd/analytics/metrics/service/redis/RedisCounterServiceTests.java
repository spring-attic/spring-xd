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
package org.springframework.xd.analytics.metrics.service.redis;

import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.xd.analytics.metrics.repository.CounterRepository;
import org.springframework.xd.analytics.metrics.repository.redis.RedisCounterRepository;
import org.springframework.xd.analytics.metrics.service.AbstractCounterServiceTests;
import org.springframework.xd.analytics.metrics.service.CounterService;


public class RedisCounterServiceTests extends AbstractCounterServiceTests {

	private RedisCounterRepository counterRepository;
	
	public CounterService getCounterServiceImplementation() {
		return new RedisCounterService(counterRepository);
	}

	@Override
	public CounterRepository getCounterRepository() {
		JedisConnectionFactory cf = new JedisConnectionFactory();
		cf.afterPropertiesSet();
		counterRepository = new RedisCounterRepository(cf);
		return counterRepository;
	}

}
