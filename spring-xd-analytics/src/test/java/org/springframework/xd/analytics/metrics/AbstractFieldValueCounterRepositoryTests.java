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

/**
 * Base tests for various implementations of {@link FieldValueCounterRepository}.
 * 
 * @author Eric Bottard
 * @author Mark Pollack
 */
public abstract class AbstractFieldValueCounterRepositoryTests {

	@Autowired
	protected FieldValueCounterRepository fieldValueCounterRepository;

	private final String tickersFieldValueCounterName = "tickersFieldValueCounter";

	private final String mentionsFieldValueCounterName = "mentionsFieldValueCounter";

	@Test
	public void testCrud() {
		FieldValueCounter counter = new FieldValueCounter(tickersFieldValueCounterName);

		// Test with initial save
		FieldValueCounter fvTickersCounter = fieldValueCounterRepository.save(counter);
		assertThat(fieldValueCounterRepository.findOne(fvTickersCounter.getName()), notNullValue());

		fieldValueCounterRepository.increment(tickersFieldValueCounterName, "VMW");
		Map<String, Double> counts = fieldValueCounterRepository.findOne(tickersFieldValueCounterName).getFieldValueCount();
		assertThat(counts.get("VMW"), equalTo(1.0));

		fieldValueCounterRepository.increment(tickersFieldValueCounterName, "VMW");
		counts = fieldValueCounterRepository.findOne(tickersFieldValueCounterName).getFieldValueCount();
		assertThat(counts.get("VMW"), equalTo(2.0));

		//Check that we can save back another value.
		fvTickersCounter.getFieldValueCount().put("VMW", 1.0D);
		fieldValueCounterRepository.save(counter);
		counts = fieldValueCounterRepository.findOne(tickersFieldValueCounterName).getFieldValueCount();
		assertThat(counts.get("VMW"), equalTo(1.0));
		//Increment again to prepare for decrement test below
		fieldValueCounterRepository.increment(tickersFieldValueCounterName, "VMW");

		fieldValueCounterRepository.increment(tickersFieldValueCounterName, "ORCL");
		counts = fieldValueCounterRepository.findOne(tickersFieldValueCounterName).getFieldValueCount();
		assertThat(counts.get("ORCL"), equalTo(1.0));

		// Test without prior save

		fieldValueCounterRepository.increment(mentionsFieldValueCounterName, "mama");
		counts = fieldValueCounterRepository.findOne(mentionsFieldValueCounterName).getFieldValueCount();
		assertThat(counts.get("mama"), equalTo(1.0));

		fieldValueCounterRepository.increment(mentionsFieldValueCounterName, "mama");
		counts = fieldValueCounterRepository.findOne(mentionsFieldValueCounterName).getFieldValueCount();
		assertThat(counts.get("mama"), equalTo(2.0));

		fieldValueCounterRepository.increment(mentionsFieldValueCounterName, "papa");
		counts = fieldValueCounterRepository.findOne(mentionsFieldValueCounterName).getFieldValueCount();
		assertThat(counts.get("papa"), equalTo(1.0));

		fieldValueCounterRepository.decrement(tickersFieldValueCounterName, "VMW");
		counts = fieldValueCounterRepository.findOne(tickersFieldValueCounterName).getFieldValueCount();
		assertThat(counts.get("VMW"), equalTo(1.0));

		fieldValueCounterRepository.decrement(mentionsFieldValueCounterName, "mama");
		counts = fieldValueCounterRepository.findOne(mentionsFieldValueCounterName).getFieldValueCount();
		assertThat(counts.get("mama"), equalTo(1.0));

	}

	@Test(expected = IllegalArgumentException.class)
	public void testDeleteNullString() {
		fieldValueCounterRepository.delete((String) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDeleteNullGauge() {
		fieldValueCounterRepository.delete((FieldValueCounter) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFindOneNullCounter() {
		fieldValueCounterRepository.findOne(null);
	}

	@Test
	public void testCrud2() {
		String myFVCounter = "myFVCounter";
		String yourFVCounter = "yourFVCounter";

		Map<String, Double> fieldValueCount = new HashMap<String, Double>();
		fieldValueCount.put("elephant", 1.0);
		fieldValueCount.put("pig", 1.0);

		FieldValueCounter c1 = new FieldValueCounter(myFVCounter, fieldValueCount);
		FieldValueCounter myCounter = fieldValueCounterRepository.save(c1);
		assertThat(myCounter.getName(), is(myFVCounter));

		// Create and save a Counter named 'yourCounter'
		fieldValueCount = new HashMap<String, Double>();
		fieldValueCount.put("turtle", 5.0);
		fieldValueCount.put("fish", 6.0);
		fieldValueCount.put("bird", 7.0);
		FieldValueCounter c2 = new FieldValueCounter(yourFVCounter, fieldValueCount);
		FieldValueCounter yourCounter = fieldValueCounterRepository.save(c2);
		assertThat(yourCounter.getName(), is(yourFVCounter));

		// Retrieve by name
		FieldValueCounter result = fieldValueCounterRepository.findOne(myFVCounter);
		assertThat(result, is(notNullValue()));
		assertThat(result.getName(), equalTo(myCounter.getName()));
		assertThat(result.getFieldValueCount().size(), equalTo(2));
		fieldValueCount = result.getFieldValueCount();
		assertThat(fieldValueCount.get("elephant"), equalTo(1.0));
		assertThat(fieldValueCount.get("pig"), equalTo(1.0));

		result = fieldValueCounterRepository.findOne(yourCounter.getName());
		assertThat(result.getName(), equalTo(yourCounter.getName()));
		assertThat(result.getFieldValueCount().size(), equalTo(3));
		fieldValueCount = result.getFieldValueCount();
		assertThat(fieldValueCount.get("turtle"), equalTo(5.0));
		assertThat(fieldValueCount.get("fish"), equalTo(6.0));
		assertThat(fieldValueCount.get("bird"), equalTo(7.0));

		List<FieldValueCounter> counters = (List<FieldValueCounter>) fieldValueCounterRepository.findAll();
		assertThat(counters.size(), equalTo(2));

		fieldValueCounterRepository.delete(myCounter);
		assertThat(fieldValueCounterRepository.findOne(myFVCounter), is(nullValue()));

		fieldValueCounterRepository.delete(yourCounter.getName());
		assertThat(fieldValueCounterRepository.findOne(yourFVCounter), is(nullValue()));

		counters = (List<FieldValueCounter>) fieldValueCounterRepository.findAll();
		assertThat(counters.size(), equalTo(0));
	}
}
