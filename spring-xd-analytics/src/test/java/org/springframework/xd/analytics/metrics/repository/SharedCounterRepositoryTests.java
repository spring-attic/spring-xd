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
package org.springframework.xd.analytics.metrics.repository;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.xd.analytics.metrics.core.Counter;


public class SharedCounterRepositoryTests {


	public void testCrud(CounterRepository repo) {
		String myCounterName = "myCounter";
		String yourCounterName = "yourCounter";
		
		// Create and save a Counter  named 'myCounter'
		Counter c1 = new Counter(myCounterName);		
		Counter myCounter = repo.save(c1);
		assertThat(myCounter.getName(), is(notNullValue()));
		// Create and save a Counter named 'yourCounter'
		Counter c2 = new Counter(yourCounterName);
		Counter yourCounter = repo.save(c2);
		assertThat(yourCounter.getName(), is(notNullValue()));
		
		// Retrieve by name and compare for equality to previously saved instance.
		Counter result = repo.findOne(myCounterName);
		assertThat(result, equalTo(myCounter));
		
		//
		result = repo.findOne(yourCounter.getName());
		assertThat(result, equalTo(yourCounter));
		
		
		List<Counter> counters = repo.findAll();
		assertThat(counters.size(), equalTo(2));
		
		repo.delete(myCounter);
		assertThat(repo.findOne(myCounterName), is(nullValue()));
		
		repo.delete(yourCounter.getName());
		assertThat(repo.findOne(yourCounterName), is(nullValue()));
		
		counters = repo.findAll();
		assertThat(counters.size(), equalTo(0));	
	}
}
