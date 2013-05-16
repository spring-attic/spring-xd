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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.xd.analytics.metrics.core.FieldValueCounter;
import org.springframework.xd.analytics.metrics.core.FieldValueCounterRepository;
public abstract class SharedFieldValueCounterRepositoryTests {

	@Autowired
	protected FieldValueCounterRepository fvRepository;
	
	@Test(expected = IllegalArgumentException.class)
	public void testDeleteNullString() {
		fvRepository.delete((String)null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDeleteNullGauge() {
		fvRepository.delete((FieldValueCounter)null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFindOneNullCounter() {
		fvRepository.findOne(null);
	}
	
	@Test
	public void testCrud() {
		String myFVCounter = "myFVCounter";
		String yourFVCounter = "yourFVCounter";
		FieldValueCounterRepository repo = fvRepository;
		
		Map<String, Double> fieldValueCount = new HashMap<String, Double>();
		fieldValueCount.put("elephant", 1.0);
		fieldValueCount.put("pig", 1.0);

		FieldValueCounter c1 = new FieldValueCounter(myFVCounter, fieldValueCount);		
		FieldValueCounter myCounter = repo.save(c1);
		assertThat(myCounter.getName(), is(myFVCounter));
		
		// Create and save a Counter named 'yourCounter'
		fieldValueCount = new HashMap<String, Double>();
		fieldValueCount.put("turtle", 5.0);
		fieldValueCount.put("fish", 6.0);
		fieldValueCount.put("bird", 7.0);
		FieldValueCounter c2 = new FieldValueCounter(yourFVCounter, fieldValueCount);
		FieldValueCounter yourCounter = repo.save(c2);
		assertThat(yourCounter.getName(), is(yourFVCounter));
		
		// Retrieve by name 
		FieldValueCounter result = repo.findOne(myFVCounter);
		assertThat(result, is(notNullValue()));
		assertThat(result.getName(), equalTo(myCounter.getName()));		
		assertThat(result.getFieldValueCount().size(), equalTo(2));
		fieldValueCount = result.getFieldValueCount();
		assertThat(fieldValueCount.get("elephant"), equalTo(1.0));
		assertThat(fieldValueCount.get("pig"), equalTo(1.0));
		
		result = repo.findOne(yourCounter.getName());
		assertThat(result.getName(), equalTo(yourCounter.getName()));
		assertThat(result.getFieldValueCount().size(), equalTo(3));
		fieldValueCount = result.getFieldValueCount();
		assertThat(fieldValueCount.get("turtle"), equalTo(5.0));
		assertThat(fieldValueCount.get("fish"), equalTo(6.0));
		assertThat(fieldValueCount.get("bird"), equalTo(7.0));
		
		
		List<FieldValueCounter> counters = repo.findAll();
		assertThat(counters.size(), equalTo(2));
		
		repo.delete(myCounter);
		assertThat(repo.findOne(myFVCounter), is(nullValue()));
		
		repo.delete(yourCounter.getName());
		assertThat(repo.findOne(yourFVCounter), is(nullValue()));
		
		counters = repo.findAll();
		assertThat(counters.size(), equalTo(0));	
	}

	
}
