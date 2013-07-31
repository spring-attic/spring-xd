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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.xd.analytics.metrics.core.Counter;
import org.springframework.xd.analytics.metrics.core.CounterRepository;
import org.springframework.xd.store.AbstractInMemoryRepository;

/**
 * Memory backed implementation of Counter repository that uses a ConcurrentMap.
 * 
 * @author Mark Pollack
 * @author Eric Bottard
 * 
 */
@Qualifier("simple")
public class InMemoryCounterRepository extends AbstractInMemoryRepository<Counter, String> implements CounterRepository {

	@Override
	public long increment(String name) {
		return increment(name, 1L);
	}

	@Override
	public long increment(String name, long amount) {
		Counter c = getOrCreate(name);
		return c.increment(amount);
	}

	@Override
	public synchronized long decrement(String name) {
		Counter c = getOrCreate(name);
		return c.decrement(1L);
	}

	@Override
	public synchronized void reset(String name) {
		save(new Counter(name));
	}

	private synchronized Counter getOrCreate(String name) {
		Counter result = findOne(name);
		if (result == null) {
			result = new Counter(name);
			result = save(result);
		}
		return result;
	}

	@Override
	protected String keyFor(Counter entity) {
		return entity.getName();
	}

}
