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
package org.springframework.xd.analytics.metrics;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.xd.analytics.metrics.core.Counter;
import org.springframework.xd.analytics.metrics.core.CounterRepository;


public abstract class SharedCounterRepositoryTests {

	@Autowired
	protected CounterRepository counterRepository;

	@Test(expected = IllegalArgumentException.class)
	public void testDeleteNullString() {
		counterRepository.delete((String)null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDeleteNullCounter() {
		counterRepository.delete((Counter)null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFindOneNullCounter() {
		counterRepository.findOne(null);
	}

	@Test
	public void testCrud() {
		CounterRepository repo = counterRepository;
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
