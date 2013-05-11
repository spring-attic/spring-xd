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
import org.springframework.xd.analytics.metrics.core.Metric;
import org.springframework.xd.analytics.metrics.core.MetricRepository;

/**
 * Memory backed implementation of MetricRepository that uses a ConcurrentMap
 *
 * @author Luke Taylor
 *
 */
class InMemoryMetricRepository<M extends Metric> implements MetricRepository<M> {

	//Map of id to counters.
	private final ConcurrentMap<String, M> map = new ConcurrentHashMap<String, M>();

	@Override
	public M save(M counter) {
		map.put(counter.getName(), counter);
		return counter;
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
	public M findOne(String name) {
		Assert.notNull(name, "The name of the metric must not be null");
		return map.get(name);
	}

	@Override
	public List<M> findAll() {
		return new ArrayList<M>(map.values());
	}

}
