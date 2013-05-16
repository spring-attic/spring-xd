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

import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.MetricsException;
import org.springframework.xd.analytics.metrics.core.FieldValueCounter;
import org.springframework.xd.analytics.metrics.core.FieldValueCounterRepository;
import org.springframework.xd.analytics.metrics.core.FieldValueCounterService;

/**
 * An in memory based implementation of the FieldValueCounterService
 * 
 * @author Mark Pollack
 */
public class InMemoryFieldValueCounterService implements
		FieldValueCounterService {

	private FieldValueCounterRepository fieldValueRepository;
	private final Object monitor = new Object();
	
	public InMemoryFieldValueCounterService(InMemoryFieldValueCounterRepository fieldValueRepository) {
		Assert.notNull(fieldValueRepository, "FieldValueCounterRepository can not be null");
		this.fieldValueRepository = fieldValueRepository;
	}
	
	
	@Override
	public FieldValueCounter getOrCreate(String name) {
		Assert.notNull(name, "FieldValueCounter name can not be null");
		synchronized (this.monitor){
			FieldValueCounter counter = fieldValueRepository.findOne(name);
			if (counter == null) {
				counter = new FieldValueCounter(name);
				this.fieldValueRepository.save(counter);
			}
			return counter;
		}
	}

	@Override
	public void increment(String name, String fieldName) {
		synchronized (monitor) {
			modifyFieldValue(name, fieldName, 1);
		}
	}

	@Override
	public void decrement(String name, String fieldName) {
		synchronized (monitor) {
			modifyFieldValue(name, fieldName, -1);
		}
	}

	@Override
	public void reset(String name, String fieldName) {
		FieldValueCounter counter = findCounter(name);
		Map<String, Double> data = counter.getFieldValueCount();
		if (data.containsKey(fieldName)) {
			data.put(fieldName, 0D);
		}
	}
	
	private FieldValueCounter findCounter(String name) {
		FieldValueCounter counter = this.fieldValueRepository.findOne(name);
		if (counter == null) {
			throw new MetricsException("FieldValueCounter " + name + " not found");
		}
		return counter;
	}
	
	private void modifyFieldValue(String name, String fieldName, double delta) {
		FieldValueCounter counter = findCounter(name);
		Map<String, Double> data = counter.getFieldValueCount();
		double count = data.containsKey(fieldName) ? data.get(fieldName) : 0;
		data.put(fieldName, count + delta);			
		this.fieldValueRepository.save(counter);
	}

}
