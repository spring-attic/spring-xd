/*
 * Copyright 2013-2015 the original author or authors.
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.xd.analytics.metrics.core.RichGauge;
import org.springframework.xd.dirt.rest.AbstractControllerIntegrationTest;
import org.springframework.xd.dirt.rest.Dependencies;
import org.springframework.xd.dirt.rest.RestConfiguration;

/**
 * @author Luke Taylor
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, Dependencies.class })
public class RichGaugeControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Test
	public void gaugeRetrievalSucceedsWithCorrectValues() throws Exception {

		when(richGaugeRepository.findOne("mygauge")).thenReturn(
				new RichGauge("mygauge", 57.0, -1.0, 56.0, 57.0, 55.0, 2));

		mockMvc.perform(get("/metrics/rich-gauges/mygauge").accept(MediaType.APPLICATION_JSON)).andExpect(
				status().isOk()).andExpect(jsonPath("$.name").value("mygauge")).andExpect(
						jsonPath("$.value").value(57.0)).andExpect(jsonPath("$.average").value(56.0)).andExpect(
								jsonPath("$.max").value(57.0)).andExpect(jsonPath("$.min").value(55.0));
	}

	@Test
	public void testDeleteRichGauge() throws Exception {
		when(richGaugeRepository.exists("deleteme")).thenReturn(true);
		mockMvc.perform(delete("/metrics/rich-gauges/{name}", "deleteme")).andExpect(status().isOk());
		verify(richGaugeRepository).delete("deleteme");
	}

	@Test
	public void testDeleteUnknownGauge() throws Exception {
		when(richGaugeRepository.exists("deleteme")).thenReturn(false);
		mockMvc.perform(delete("/metrics/rich-gauges/{name}", "deleteme")).andExpect(status().isNotFound());
	}

	@Test
	public void testRichGaugesListing() throws Exception {
		RichGauge[] counters = new RichGauge[10];
		for (int i = 0; i < counters.length; i++) {
			counters[i] = new RichGauge("c" + i, 15, 0.5, 16, 20, 10, 100);
		}
		when(richGaugeRepository.findAll()).thenReturn(Arrays.asList(counters));

		ResultActions resActions = mockMvc.perform(get("/metrics/rich-gauges")).andExpect(status().isOk())//
		.andExpect(jsonPath("$.content", Matchers.hasSize(10)));

		for (int i = 0; i < 10; i++) {
			resActions.andExpect(jsonPath("$.content[" + i + "].name").value("c" + i));
			resActions.andExpect(jsonPath("$.content[" + i + "].value").doesNotExist());
			resActions.andExpect(jsonPath("$.content[" + i + "].alpha").doesNotExist());
			resActions.andExpect(jsonPath("$.content[" + i + "].average").doesNotExist());
			resActions.andExpect(jsonPath("$.content[" + i + "].max").doesNotExist());
			resActions.andExpect(jsonPath("$.content[" + i + "].min").doesNotExist());
			resActions.andExpect(jsonPath("$.content[" + i + "].count").doesNotExist());
		}
	}

	@Test
	public void testDetailedRichGaugesListing() throws Exception {
		RichGauge[] counters = new RichGauge[10];
		for (int i = 0; i < counters.length; i++) {
			counters[i] = new RichGauge("c" + i, 15, 0.5, 16, 20, 10, 100);
		}
		when(richGaugeRepository.findAll()).thenReturn(Arrays.asList(counters));

		ResultActions resActions = mockMvc.perform(get("/metrics/rich-gauges?detailed=true")).andExpect(status().isOk())//
		.andExpect(jsonPath("$.content", Matchers.hasSize(10)));

		for (int i = 0; i < 10; i++) {
			resActions.andExpect(jsonPath("$.content[" + i + "].name").value("c" + i));
			resActions.andExpect(jsonPath("$.content[" + i + "].value").value(15.0));
			resActions.andExpect(jsonPath("$.content[" + i + "].alpha").value(0.5));
			resActions.andExpect(jsonPath("$.content[" + i + "].average").value(16.0));
			resActions.andExpect(jsonPath("$.content[" + i + "].max").value(20.0));
			resActions.andExpect(jsonPath("$.content[" + i + "].min").value(10.0));
			resActions.andExpect(jsonPath("$.content[" + i + "].count").value(100));
		}
	}

}
