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

import static org.springframework.xd.analytics.metrics.core.MetricUtils.setGaugeValue;

import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.core.Gauge;
import org.springframework.xd.analytics.metrics.core.GaugeRepository;

/**
 * Memory backed Gauge repository that uses a ConcurrentMap
 * 
 * @author Mark Pollack
 * 
 */
public final class InMemoryGaugeRepository extends InMemoryMetricRepository<Gauge> implements
		GaugeRepository {

	@Override
	protected Gauge create(String name) {
		return new Gauge(name);
	}

	@Override
	protected Gauge getOrCreate(String name) {
		Assert.notNull(name, "Gauge name can not be null");
		Gauge gauge = findOne(name);
		if (gauge == null) {
			gauge = new Gauge(name);
			save(gauge);
		}
		return gauge;
	}

	@Override
	public void recordValue(String name, long value) {
		Gauge gauge = getOrCreate(name);
		setGaugeValue(gauge, value);
	}

	@Override
	public void reset(String name) {
		Gauge gauge = getOrCreate(name);
		setGaugeValue(gauge, 0);
	}

}
