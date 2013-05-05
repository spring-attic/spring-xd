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
package org.springframework.xd.analytics.metrics.service.redis;

import org.springframework.util.Assert;
import org.springframework.xd.analytics.metrics.core.Gauge;
import org.springframework.xd.analytics.metrics.repository.redis.RedisGaugeRepository;
import org.springframework.xd.analytics.metrics.service.GaugeService;

/**
 * A Redis backed implementation.
 * 
 * @author Mark Pollack
 * 
 */
public class RedisGaugeService implements GaugeService {

	private RedisGaugeRepository gaugeRepository;
	private final Object monitor = new Object();

	public RedisGaugeService(RedisGaugeRepository gaugeRepository) {
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
	public void reset(String name) {
		gaugeRepository.reset(name);
	}

	@Override
	public void setValue(String name, long value) {
		gaugeRepository.setValue(name, value);
	}

}
