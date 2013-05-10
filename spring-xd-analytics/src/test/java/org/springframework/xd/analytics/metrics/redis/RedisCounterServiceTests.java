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

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.xd.analytics.metrics.AbstractCounterServiceTests;
import org.springframework.xd.analytics.metrics.core.CounterRepository;
import org.springframework.xd.analytics.metrics.core.CounterService;


public class RedisCounterServiceTests extends AbstractCounterServiceTests {

	private RedisCounterRepository counterRepository;


	@After
	@Before
	public void beforeAndAfter() {

		StringRedisTemplate stringRedisTemplate = TestUtils.getStringRedisTemplate();
		Set<String> keys = stringRedisTemplate.keys("counts." + "*");
		if (keys.size() > 0) {
			stringRedisTemplate.delete(keys);
		}

		CounterRepository repo = getCounterRepository();
		//TODO delete to support wildcards
		repo.delete("simpleCounter");
		repo.delete("counts.simpleCounter");
	}

	@Test
	public void testService() {
		super.simpleTest(getCounterServiceImplementation(), getCounterRepository());
	}

	public CounterService getCounterServiceImplementation() {
		return new RedisCounterService(getCounterRepository());
	}


	public RedisCounterRepository getCounterRepository() {
		counterRepository = new RedisCounterRepository(TestUtils.getJedisConnectionFactory());
		return counterRepository;
	}

}
