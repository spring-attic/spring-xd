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

import static org.springframework.xd.analytics.metrics.core.MetricUtils.*;

import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.core.RichGauge;
import org.springframework.xd.analytics.metrics.core.RichGaugeRepository;

/**
 * Memory backed implementation of GaugeRepository that uses a ConcurrentMap
 *
 * @author Luke Taylor
 */
public class InMemoryRichGaugeRepository extends InMemoryMetricRepository<RichGauge>
		implements RichGaugeRepository {

	@Override
	public void recordValue(String name, double value, double alpha) {
		RichGauge gauge = getOrCreate(name);
		setRichGaugeValue(gauge, value, alpha);
	}

	@Override
	public void reset(String name) {
		RichGauge gauge = getOrCreate(name);
		setRichGaugeValue(gauge, 0, -1D);
	}

	@Override
	protected RichGauge getOrCreate(String name) {
		Assert.notNull(name, "Gauge name can not be null");
		RichGauge gauge = findOne(name);
		if (gauge == null) {
			gauge = new RichGauge(name);
			save(gauge);
		}
		return gauge;
	}

	@Override
	protected RichGauge create(String name) {
		return new RichGauge(name);
	}
}
