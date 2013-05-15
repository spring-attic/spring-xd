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

import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.core.FieldValueCounter;
import org.springframework.xd.analytics.metrics.core.FieldValueCounterService;

/**
 * 
 * @author Mark Pollack
 *
 */
public class RedisFieldValueCounterService implements FieldValueCounterService {

	private RedisFieldValueCounterRepository fieldValueCounterRepository;
	
	public RedisFieldValueCounterService(RedisFieldValueCounterRepository fieldValueCounterRepository) {
		Assert.notNull(fieldValueCounterRepository, "Counter Repository can not be null");
		this.fieldValueCounterRepository = fieldValueCounterRepository;
	}
	
	@Override
	public FieldValueCounter getOrCreate(String name) {
		Assert.notNull(name, "FieldValueCounter name can not be null");
		FieldValueCounter counter = fieldValueCounterRepository.findOne(name);
		if (counter == null) {
			counter = new FieldValueCounter(name);
			this.fieldValueCounterRepository.save(counter);
		}
		return counter;
	}

	@Override
	public void increment(String name, String fieldName) {
		fieldValueCounterRepository.increment(name, fieldName);
	}

	@Override
	public void decrement(String name, String fieldName) {
		fieldValueCounterRepository.decrement(name, fieldName);
	}

	@Override
	public void reset(String name, String fieldName) {
		fieldValueCounterRepository.reset(name, fieldName);
	}

}
