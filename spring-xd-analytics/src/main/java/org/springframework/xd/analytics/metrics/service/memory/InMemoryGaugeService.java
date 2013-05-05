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
package org.springframework.xd.analytics.metrics.service.memory;

import java.lang.reflect.Field;

import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.xd.analytics.metrics.core.Gauge;
import org.springframework.xd.analytics.metrics.repository.GaugeRepository;
import org.springframework.xd.analytics.metrics.repository.memory.InMemoryGaugeRepository;
import org.springframework.xd.analytics.metrics.service.GaugeService;

/**
 * An in memory based implementation.  Gauge value is manipulated using reflection to avoid exposing a setter
 * on the Gauge class.
 * 
 * @author Mark Pollack
 *
 */
public class InMemoryGaugeService implements GaugeService {

	private GaugeRepository gaugeRepository;
	private final Object monitor = new Object();
	
	public InMemoryGaugeService(InMemoryGaugeRepository gaugeRepository) {
		Assert.notNull(gaugeRepository, "Gauge Repository can not be null");
		this.gaugeRepository = gaugeRepository;
	}
	
	
	@Override
	public Gauge getOrCreate(String name) {
		Assert.notNull(name, "Gauge name can not be null");
		synchronized (this.monitor) {
			Gauge gauge = gaugeRepository.findOne(name);
			if (gauge == null) {
				gauge = new Gauge(name);
				this.gaugeRepository.save(gauge);
			}
			return gauge;
		}
	}

	@Override
	public void setValue(String name, long value) {
		synchronized (monitor) {
			Gauge gauge = gaugeRepository.findOne(name);
			if (gauge != null) {
				Field findField = ReflectionUtils.findField(Gauge.class, "value");
				ReflectionUtils.makeAccessible(findField);				
				ReflectionUtils.setField(findField, gauge, value);
			}
		}
	}

	@Override
	public void reset(String name) {
		synchronized (monitor) {
			Gauge gauge = gaugeRepository.findOne(name);
			if (gauge != null) {
				Field findField = ReflectionUtils.findField(Gauge.class,	"value");
				ReflectionUtils.makeAccessible(findField);				
				ReflectionUtils.setField(findField, gauge, 0);
			}
		}
	}

}
