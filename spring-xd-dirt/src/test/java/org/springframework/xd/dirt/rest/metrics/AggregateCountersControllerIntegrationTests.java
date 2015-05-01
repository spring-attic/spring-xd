/*
 * Copyright 2015 the original author or authors.
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.xd.analytics.metrics.core.AggregateCount;
import org.springframework.xd.analytics.metrics.core.AggregateCountResolution;
import org.springframework.xd.analytics.metrics.core.Counter;
import org.springframework.xd.dirt.rest.AbstractControllerIntegrationTest;
import org.springframework.xd.dirt.rest.Dependencies;
import org.springframework.xd.dirt.rest.RestConfiguration;

/**
 * @author Alex Boyko
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = { RestConfiguration.class, Dependencies.class })
public class AggregateCountersControllerIntegrationTests extends AbstractControllerIntegrationTest {

	private void setupAggCounts(int number) {
		Counter[] counters = new Counter[10];
		AggregateCount[] aggCounters = new AggregateCount[10];
		DateTime to = new DateTime();
		DateTime from = to.minusMinutes(59);
		Interval interval = new Interval(from, to);
		AggregateCountResolution resolution = AggregateCountResolution.minute;
		long[] counts = new long[60];
		for (int i = 0; i < 60; i++) {
			counts[i] = i;
		}
		for (int i = 0; i < counters.length; i++) {
			counters[i] = new Counter("c" + i, i);
			aggCounters[i] = new AggregateCount("c" + i, interval, counts, resolution);
			when(aggregateCounterRepository.getCounts(org.mockito.Matchers.eq(aggCounters[i].getName()),
					org.mockito.Matchers.any(Interval.class), org.mockito.Matchers.eq(resolution))).thenReturn(
							aggCounters[i]);
		}
		when(aggregateCounterRepository.findAll()).thenReturn(Arrays.asList(counters));
	}

	@Test
	public void testAggregateCountersListing() throws Exception {
		setupAggCounts(10);

		ResultActions resActions = mockMvc.perform(get("/metrics/aggregate-counters")).andExpect(status().isOk())//
		.andExpect(jsonPath("$.content", Matchers.hasSize(10)));

		for (int i = 0; i < 10; i++) {
			resActions.andExpect(jsonPath("$.content[" + i + "].name").value("c" + i));
			resActions.andExpect(jsonPath("$.content[" + i + "].counts").doesNotExist());
		}
	}

	@Test
	public void testDetailedAggregateCountersListing() throws Exception {
		setupAggCounts(10);

		ResultActions resActions = mockMvc.perform(get("/metrics/aggregate-counters?detailed=true&resolution=minute")).andExpect(
				status().isOk())//
				.andExpect(jsonPath("$.content", Matchers.hasSize(10)));

		for (int i = 0; i < 10; i++) {
			resActions.andExpect(jsonPath("$.content[" + i + "].name").value("c" + i));
			resActions.andExpect(jsonPath("$.content[" + i + "].counts").exists());
		}
	}

}
