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

import org.junit.Test;
import org.springframework.xd.analytics.metrics.repository.memory.InMemoryGaugeRepository;
import org.springframework.xd.analytics.metrics.service.AbstractGaugeServiceTests;
import org.springframework.xd.analytics.metrics.service.GaugeService;



public class InMemoryGaugeServiceTests extends AbstractGaugeServiceTests {

	private InMemoryGaugeRepository counterRepository  = new InMemoryGaugeRepository();
	
	public GaugeService getCounterServiceImplementation() {
		return new InMemoryGaugeService(counterRepository);
	}
	
	@Test
	public void testService() {
		super.simpleTest(getCounterServiceImplementation(), counterRepository);
	}

}
