/*
 * Copyright 2011-2013 the original author or authors.
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

import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.DateTimeField;
import org.joda.time.Interval;
import org.springframework.xd.analytics.metrics.core.AggregateCount;
import org.springframework.xd.analytics.metrics.core.AggregateCounter;
import org.springframework.xd.analytics.metrics.core.AggregateCounterRepository;
import org.springframework.xd.store.AbstractInMemoryRepository;

/**
 * In-memory aggregate counter with minute resolution.
 * 
 * Note that the data is permanently accumulated, so will grow steadily in size until the
 * host process is restarted.
 * 
 * @author Luke Taylor
 */
public class InMemoryAggregateCounterRepository extends AbstractInMemoryRepository<AggregateCounter, String> implements
		AggregateCounterRepository {

	@Override
	public long increment(String name) {
		return increment(name, 1, DateTime.now());
	}

	@Override
	public long increment(String name, int amount, DateTime dateTime) {
		InMemoryAggregateCounter counter = getOrCreate(name);
		return counter.increment(amount, dateTime);
	}

	@Override
	public int getTotal(String name) {
		return getOrCreate(name).getTotal();
	}

	@Override
	public AggregateCount getCounts(String name, Interval interval, DateTimeField resolution) {
		return getOrCreate(name).getCounts(interval, resolution);
	}

	private synchronized InMemoryAggregateCounter getOrCreate(String name) {
		AggregateCounter c = findOne(name);
		if (c == null) {
			c = new InMemoryAggregateCounter(name);
			save(c);
		}
		else if (!(c instanceof InMemoryAggregateCounter)) {
			c = new InMemoryAggregateCounter(c);
		}
		return (InMemoryAggregateCounter) c;
	}

	@Override
	public Set<String> getAll() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Auto-generated method stub");
	}

	@Override
	protected String keyFor(AggregateCounter entity) {
		return entity.getName();
	}
}
