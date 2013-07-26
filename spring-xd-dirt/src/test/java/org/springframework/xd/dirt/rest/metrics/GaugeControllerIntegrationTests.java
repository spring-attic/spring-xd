/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.xd.dirt.rest.metrics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.xd.analytics.metrics.core.GaugeRepository;
import org.springframework.xd.dirt.rest.AbstractControllerIntegrationTest;
import org.springframework.xd.dirt.rest.MockedDependencies;
import org.springframework.xd.dirt.rest.RestConfiguration;

/**
 * @author Luke Taylor
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, MockedDependencies.class, MetricsControllerIntegrationTestsConfig.class })
public class GaugeControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Autowired
	private GaugeRepository gaugeRepository;

	@Test
	public void gaugeRetrievalSucceeds() throws Exception {
		gaugeRepository.setValue("mygauge", 55);

		mockMvc.perform(get("/metrics/gauges/mygauge").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("mygauge"))
				.andExpect(jsonPath("$.value").value(55));
	}
}
