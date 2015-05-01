/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.xd.analytics.metrics.core.Counter;
import org.springframework.xd.dirt.rest.AbstractControllerIntegrationTest;
import org.springframework.xd.dirt.rest.Dependencies;
import org.springframework.xd.dirt.rest.RestConfiguration;


/**
 * Tests proper behavior of {@link CountersController}.
 *
 * @author Eric Bottard
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, Dependencies.class })
public class CountersControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Test
	public void testInexistantCounterRetrieval() throws Exception {
		mockMvc.perform(get("/metrics/counters/notthere")).andExpect(status().isNotFound());
	}

	@Test
	public void testExistingCounterRetrieval() throws Exception {
		when(counterRepository.findOne("iamthere")).thenReturn(new Counter("iamthere", 12L));

		mockMvc.perform(get("/metrics/counters/iamthere"))//
		.andExpect(status().isOk())//
		.andExpect(jsonPath("$.name").value("iamthere"))//
		.andExpect(jsonPath("$.value").value(12));
	}

	@Test
	public void testCounterListing() throws Exception {
		Counter[] counters = new Counter[10];
		for (int i = 0; i < counters.length; i++) {
			counters[i] = new Counter("c" + i, i);
		}
		when(counterRepository.findAll()).thenReturn(Arrays.asList(counters));

		ResultActions resActions = mockMvc.perform(get("/metrics/counters")).andExpect(status().isOk())//
		.andExpect(jsonPath("$.content", Matchers.hasSize(10)));

		for (int i = 0; i < 10; i++) {
			resActions.andExpect(jsonPath("$.content[" + i + "].name").value("c" + i));
			resActions.andExpect(jsonPath("$.content[" + i + "].value").doesNotExist());
		}
	}

	@Test
	public void testDetailedCounterListing() throws Exception {
		Counter[] counters = new Counter[10];
		for (int i = 0; i < counters.length; i++) {
			counters[i] = new Counter("c" + i, i);
		}
		when(counterRepository.findAll()).thenReturn(Arrays.asList(counters));

		ResultActions resActions = mockMvc.perform(get("/metrics/counters?detailed=true")).andExpect(status().isOk())//
		.andExpect(jsonPath("$.content", Matchers.hasSize(10)));

		for (int i = 0; i < 10; i++) {
			resActions.andExpect(jsonPath("$.content[" + i + "].name").value("c" + i));
			resActions.andExpect(jsonPath("$.content[" + i + "].value").value(i));
		}
	}

	@Test
	public void testInexistantCounterDeletion() throws Exception {
		mockMvc.perform(delete("/metrics/counters/{name}", "deleteme")).andExpect(status().isNotFound());
	}

	@Test
	public void testCounterDeletion() throws Exception {
		when(counterRepository.exists("deleteme")).thenReturn(true);

		mockMvc.perform(delete("/metrics/counters/{name}", "deleteme")).andExpect(status().isOk());
		verify(counterRepository).delete("deleteme");
	}
}
