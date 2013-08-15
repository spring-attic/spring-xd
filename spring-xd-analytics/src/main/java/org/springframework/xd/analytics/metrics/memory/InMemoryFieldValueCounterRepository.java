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

package org.springframework.xd.analytics.metrics.memory;

import java.util.Map;

import org.springframework.xd.analytics.metrics.core.FieldValueCounter;
import org.springframework.xd.analytics.metrics.core.FieldValueCounterRepository;

/**
 * Memory backed implementation of FieldValueCounterRepository that uses a ConcurrentMap
 * 
 * @author Mark Pollack
 * 
 */
public class InMemoryFieldValueCounterRepository extends InMemoryMetricRepository<FieldValueCounter> implements
		FieldValueCounterRepository {

	@Override
	protected FieldValueCounter create(String name) {
		return new FieldValueCounter(name);
	}

	@Override
	public synchronized void increment(String name, String fieldName) {
		modifyFieldValue(name, fieldName, 1);
	}

	@Override
	public synchronized void decrement(String name, String fieldName) {
		modifyFieldValue(name, fieldName, -1);
	}

	@Override
	public void reset(String name, String fieldName) {
		FieldValueCounter counter = getOrCreate(name);
		Map<String, Double> data = counter.getFieldValueCount();
		if (data.containsKey(fieldName)) {
			data.put(fieldName, 0D);
		}
	}

	private void modifyFieldValue(String name, String fieldName, double delta) {
		FieldValueCounter counter = getOrCreate(name);
		Map<String, Double> data = counter.getFieldValueCount();
		double count = data.containsKey(fieldName) ? data.get(fieldName) : 0;
		data.put(fieldName, count + delta);
		save(counter);
	}

}
