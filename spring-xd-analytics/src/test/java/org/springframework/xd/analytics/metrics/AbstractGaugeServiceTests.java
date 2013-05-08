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
package org.springframework.xd.analytics.metrics;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.springframework.xd.analytics.metrics.core.Gauge;
import org.springframework.xd.analytics.metrics.core.GaugeService;
import org.springframework.xd.analytics.metrics.core.GaugeRepository;

public class AbstractGaugeServiceTests {


	public void simpleTest(GaugeService cs, GaugeRepository repo) {
		Gauge gauge = cs.getOrCreate("simpleGauge");

		String gaugeName = gauge.getName();
		assertThat(gaugeName, equalTo("simpleGauge"));

		cs.setValue(gaugeName, 1);
		Gauge g = repo.findOne(gaugeName);
		assertThat(g.getValue(), equalTo(1L));

		cs.setValue(gaugeName, 20);
		assertThat(repo.findOne(gaugeName).getValue(), equalTo(20L));

		cs.reset(gaugeName);
		assertThat(repo.findOne(gaugeName).getValue(), equalTo(0L));

		Gauge gauge2 = cs.getOrCreate("simpleGauge");
		assertThat(gauge, equalTo(gauge2));

	}

}
