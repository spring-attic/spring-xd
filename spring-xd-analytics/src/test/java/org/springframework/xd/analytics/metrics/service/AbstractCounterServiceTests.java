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
package org.springframework.xd.analytics.metrics.service;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.xd.analytics.metrics.core.Counter;
import org.springframework.xd.analytics.metrics.repository.CounterRepository;

public abstract class AbstractCounterServiceTests {
	
	private volatile JedisConnectionFactory cf = new JedisConnectionFactory();
	private volatile StringRedisTemplate stringRedisTemplate;	
	
	@After
	@Before
	public void beforeAndAfter() {
		cf = new JedisConnectionFactory();
		cf.afterPropertiesSet();
		stringRedisTemplate = new StringRedisTemplate(cf);
		stringRedisTemplate.afterPropertiesSet();
		Set<String> keys = stringRedisTemplate.keys("counts." + "*");
		if (keys.size() > 0) {
			stringRedisTemplate.delete(keys);
		}
		
		CounterRepository repo = getCounterRepository();
		//TODO delete to support wildcards
		repo.delete("simpleCounter");
		repo.delete("counts.simpleCounter");
	}


	
	public StringRedisTemplate getRedisTemplate() {
		return this.stringRedisTemplate;
	}
	
	public JedisConnectionFactory getConnectionFactory() {
		return this.cf;
		
	}

	@Test
	public void simpleTest() {
		CounterService cs = getCounterServiceImplementation();
		CounterRepository repo =  getCounterRepository();
		Counter counter = cs.getOrCreate("simpleCounter");
		
		String counterName = counter.getName();
		assertThat(counterName, equalTo("simpleCounter"));			
		
		cs.increment(counterName);
		Counter c = repo.findOne(counterName);
		assertThat(c.getCount(), equalTo(1L));
		
		cs.increment(counterName);
		assertThat(repo.findOne(counterName).getCount(), equalTo(2L));
		
		cs.decrement(counterName);
		assertThat(repo.findOne(counterName).getCount(), equalTo(1L));
		
		cs.reset(counterName);
		assertThat(repo.findOne(counterName).getCount(), equalTo(0L));
		
		Counter counter2 = cs.getOrCreate("simpleCounter");
		assertThat(counter, equalTo(counter2));
		
	}

	public abstract CounterService getCounterServiceImplementation();

	public abstract CounterRepository getCounterRepository();
}