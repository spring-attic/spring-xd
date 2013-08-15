/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.xd.analytics.metrics.core.FieldValueCounter;
import org.springframework.xd.dirt.rest.AbstractControllerIntegrationTest;
import org.springframework.xd.dirt.rest.Dependencies;
import org.springframework.xd.dirt.rest.RestConfiguration;
import org.springframework.xd.dirt.rest.metrics.FieldValueCountersController;

import scala.actors.threadpool.Arrays;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests proper behavior of {@link FieldValueCountersController}.
 * 
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, Dependencies.class })
public class FieldValueCountersControllerIntegrationTests extends AbstractControllerIntegrationTest {

	@Test
	public void testInexistantCounterRetrieval() throws Exception {
		mockMvc.perform(get("/metrics/field-value-counters/notthere")).andExpect(status().isNotFound());
	}

	@Test
	public void testExistingCounterRetrieval() throws Exception {
		Map<String, Double> values = new HashMap<String, Double>();
		values.put("VMW", 13.0);
		values.put("GOOG", 23.0);
		when(fieldValueCounterRepository.findOne("iamthere")).thenReturn(new FieldValueCounter("iamthere", values));

		mockMvc.perform(get("/metrics/field-value-counters/iamthere"))//
		.andExpect(status().isOk())//
		.andExpect(jsonPath("$.name").value("iamthere"))//
		.andExpect(jsonPath("$.counts['VMW']").value(13.0));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testCounterListing() throws Exception {
		FieldValueCounter[] counters = new FieldValueCounter[10];
		for (int i = 0; i < counters.length; i++) {
			counters[i] = new FieldValueCounter("c" + i);
		}
		when(fieldValueCounterRepository.findAll()).thenReturn(Arrays.asList(counters));

		mockMvc.perform(get("/metrics/field-value-counters")).andExpect(status().isOk())//
		.andExpect(jsonPath("$.content", Matchers.hasSize(10)));
	}
	
	@Test
	public void testDeleteFieldValueCounter() throws Exception {
		when(fieldValueCounterRepository.exists("deleteme")).thenReturn(true);
		mockMvc.perform(delete("/metrics/field-value-counters/{name}", "deleteme")).andExpect(status().isOk());
		verify(fieldValueCounterRepository).delete("deleteme");
	}
	
	@Test
	public void testDeleteUnknownFieldValueCounter() throws Exception {
		when(fieldValueCounterRepository.exists("deleteme")).thenReturn(false);
		mockMvc.perform(delete("/metrics/field-value-counters/{name}", "deleteme")).andExpect(status().isNotFound());
	}
}
