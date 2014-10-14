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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.xd.analytics.metrics.core.AggregateCount;
import org.springframework.xd.analytics.metrics.core.AggregateCountResolution;
import org.springframework.xd.analytics.metrics.core.AggregateCounterRepository;
import org.springframework.xd.analytics.metrics.core.Counter;
import org.springframework.xd.store.AbstractInMemoryRepository;

/**
 * In-memory aggregate counter with minute resolution.
 * <p/>
 * Note that the data is permanently accumulated, so will grow steadily in size until the host process is restarted.
 *
 * @author Luke Taylor
 * @author Eric Bottard
 */
@Qualifier("aggregate")
public class InMemoryAggregateCounterRepository extends AbstractInMemoryRepository<Counter, String> implements
		AggregateCounterRepository {

	private Map<String, InMemoryAggregateCounter> aggregates = new HashMap<String, InMemoryAggregateCounter>();

	@Override
	public long increment(String name) {
		return increment(name, 1L);
	}

	@Override
	public long increment(String name, long amount) {
		return increment(name, amount, DateTime.now());
	}

	@Override
	public long decrement(String name) {
		throw new UnsupportedOperationException("Can't decrement an AggregateCounter");
	}

	@Override
	public void reset(String name) {
		delete(name);
	}

	@Override
	public long increment(String name, long amount, DateTime dateTime) {
		InMemoryAggregateCounter counter = getOrCreate(name);
		return counter.increment(amount, dateTime);
	}

	@Override
	public AggregateCount getCounts(String name, int nCounts, AggregateCountResolution resolution) {
		return getOrCreate(name).getCounts(nCounts, new DateTime(), resolution);
	}

	@Override
	public AggregateCount getCounts(String name, Interval interval, AggregateCountResolution resolution) {
		return getOrCreate(name).getCounts(interval, resolution);
	}

	@Override
	public AggregateCount getCounts(String name, int nCounts, DateTime end, AggregateCountResolution resolution) {
		return getOrCreate(name).getCounts(nCounts, end, resolution);
	}

	private synchronized InMemoryAggregateCounter getOrCreate(String name) {
		InMemoryAggregateCounter c = aggregates.get(name);
		if (c == null) {
			c = new InMemoryAggregateCounter(name);
			aggregates.put(name, c);
		}
		return c;
	}

	@Override
	public <S extends Counter> S save(S entity) {
		aggregates.remove(entity.getName());
		increment(entity.getName(), (int) entity.getValue(), DateTime.now());
		return entity;
	}

	@Override
	protected String keyFor(Counter entity) {
		return entity.getName();
	}

	@Override
	public Counter findOne(String id) {
		InMemoryAggregateCounter aggregate = getOrCreate(id);
		return aggregate;
	}

	@Override
	public Iterable<Counter> findAll() {
		return new ArrayList<Counter>(aggregates.values());
	}

	@Override
	public long count() {
		return aggregates.size();
	}

	@Override
	public void delete(String id) {
		aggregates.remove(id);
	}

	@Override
	public void delete(Counter entity) {
		delete(entity.getName());
	}

	@Override
	public void deleteAll() {
		aggregates.clear();
	}

}
