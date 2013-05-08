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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.core.Counter;
import org.springframework.xd.analytics.metrics.core.CounterRepository;

/**
 * Memory backed implementation of CounterRepository that uses a ConcurrentMap
 *
 * @author Mark Pollack
 *
 */
public class InMemoryCounterRepository implements CounterRepository {

	//Map of id to counters.
	private final ConcurrentMap<String, Counter> map = new ConcurrentHashMap<String, Counter>();

	@Override
	public Counter save(Counter counter) {
		map.put(counter.getName(), counter);
		return counter;
	}

	@Override
	public void delete(String name) {
		Assert.notNull(name, "The name of the counter must not be null");
		map.remove(name);
	}

	@Override
	public void delete(Counter counter) {
		Assert.notNull(counter, "The counter must not be null");
		map.remove(counter.getName());
	}

	@Override
	public Counter findOne(String name) {
		Assert.notNull(name, "The name of the counter must not be null");
		return map.get(name);
	}

	@Override
	public List<Counter> findAll() {
		return new ArrayList<Counter>(map.values());
	}

}
