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

import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.core.Counter;
import org.springframework.xd.analytics.metrics.repository.redis.RedisCounterRepository;
import org.springframework.xd.analytics.metrics.service.CounterService;

/**
 * A Redis backed implementation.
 * 
 * @author Mark Pollack
 * 
 */
public class RedisCounterService implements CounterService {

	private RedisCounterRepository counterRepository;
	private final Object monitor = new Object();

	public RedisCounterService(RedisCounterRepository counterRepository) {
		Assert.notNull(counterRepository, "Counter Repository can not be null");
		this.counterRepository = counterRepository;
	}

	@Override
	public Counter getOrCreate(String name) {
		Assert.notNull(name, "Counter name can not be null");
		synchronized (this.monitor) { 
			Counter counter = counterRepository.findOne(name);
			if (counter == null) {
				counter = new Counter(name);
				this.counterRepository.save(counter);
			}
			return counter;
		}
	}

	@Override
	public void increment(String name) {
		counterRepository.increment(name);

	}

	@Override
	public void decrement(String name) {
		counterRepository.decrement(name);

	}

	@Override
	public void reset(String name) {
		counterRepository.reset(name);
	}

}
