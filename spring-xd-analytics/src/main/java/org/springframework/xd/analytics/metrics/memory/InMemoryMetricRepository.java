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
import org.springframework.xd.analytics.metrics.core.Metric;
import org.springframework.xd.analytics.metrics.core.MetricRepository;

/**
 * Memory backed implementation of MetricRepository that uses a ConcurrentMap
 * 
 * @author Luke Taylor
 * 
 */
public abstract class InMemoryMetricRepository<M extends Metric> implements
		MetricRepository<M> {

	private final ConcurrentMap<String, M> map = new ConcurrentHashMap<String, M>();

	@Override
	public <S extends M> S save(S metric) {
		map.put(metric.getName(), metric);
		return metric;
	}

	@Override
	public <S extends M> Iterable<S> save(Iterable<S> metrics) {
		List<S> results = new ArrayList<S>();
		for (S m : metrics) {
			results.add(save(m));
		}
		return results;
	}

	@Override
	public void delete(String name) {
		Assert.notNull(name, "The name of the metric must not be null");
		map.remove(name);
	}

	@Override
	public void delete(M metric) {
		Assert.notNull(metric, "The metric instance must not be null");
		map.remove(metric.getName());
	}

	@Override
	public void delete(Iterable<? extends M> metrics) {
		for (M metric : metrics) {
			delete(metric);
		}
	}

	@Override
	public M findOne(String name) {
		Assert.notNull(name, "The name of the metric must not be null");
		return map.get(name);
	}

	@Override
	public boolean exists(String s) {
		return findOne(s) != null;
	}

	@Override
	public List<M> findAll() {
		return new ArrayList<M>(map.values());
	}

	@Override
	public List<M> findAll(Iterable<String> keys) {
		List<M> results = new ArrayList<M>();

		for (String k : keys) {
			M value = findOne(k);
			if (value != null) {
				results.add(value);
			}
		}
		return results;
	}

	@Override
	public long count() {
		return map.size();
	}

	@Override
	public void deleteAll() {
		map.clear();
	}

	protected M getOrCreate(String name) {
		synchronized (map) {
			M result = findOne(name);
			if (result == null) {
				result = create(name);
				result = save(result);
			}
			return result;
		}
	}

	protected abstract M create(String name);
}
