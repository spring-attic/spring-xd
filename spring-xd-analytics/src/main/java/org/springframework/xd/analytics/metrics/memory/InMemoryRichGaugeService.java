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
import org.springframework.xd.analytics.metrics.MetricsException;
import org.springframework.xd.analytics.metrics.core.RichGauge;
import org.springframework.xd.analytics.metrics.core.RichGaugeService;

/**
 * An in memory based implementation of RichGaugeService.
 *
 * @author Luke Taylor
 */
public class InMemoryRichGaugeService implements RichGaugeService {
	private InMemoryMetricRepository<RichGauge> gaugeRepository;
	private final Object monitor = new Object();

	public InMemoryRichGaugeService(InMemoryRichGaugeRepository gaugeRepository) {
		Assert.notNull(gaugeRepository, "Gauge Repository can not be null");
		this.gaugeRepository = gaugeRepository;
	}

	@Override
	public RichGauge getOrCreate(String name) {
		Assert.notNull(name, "Gauge name can not be null");
		synchronized (this.monitor) {
			RichGauge gauge = gaugeRepository.findOne(name);
			if (gauge == null) {
				gauge = new RichGauge(name);
				this.gaugeRepository.save(gauge);
			}
			return gauge;
		}
	}

	@Override
	public void setValue(String name, double value) {
		synchronized (monitor) {
			RichGauge gauge = findExistingGauge(name);
			gaugeRepository.save(setRichGaugeValue(gauge, value));
		}
	}

	@Override
	public void reset(String name) {
		synchronized (monitor) {
			RichGauge gauge = findExistingGauge(name);
			gaugeRepository.save(resetRichGauge(gauge));
		}
	}

	@Override
	public void setAlpha(String name, double value) {
		synchronized (this.monitor) {
			RichGauge gauge = findExistingGauge(name);
			this.gaugeRepository.save(setRichGaugeAlpha(gauge, value));
		}
	}

	private RichGauge findExistingGauge(String name) {
		RichGauge gauge = gaugeRepository.findOne(name);
		if (gauge == null) {
			throw new MetricsException("Gauge " + name + " not found");
		}
		return gauge;
	}
}
