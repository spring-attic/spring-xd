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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.xd.analytics.metrics.core.Gauge;
import org.springframework.xd.analytics.metrics.core.GaugeRepository;


public abstract class SharedGaugeRepositoryTests {

	@Autowired
	protected GaugeRepository gaugeRepository;

	@Test(expected = IllegalArgumentException.class)
	public void testDeleteNullString() {
		gaugeRepository.delete((String) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDeleteNullGauge() {
		gaugeRepository.delete((Gauge) null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFindOneNullCounter() {
		gaugeRepository.findOne(null);
	}

	@Test
	public void setValueAndResetWorkAsExpected() {
		String gaugeName = "simpleGauge";
		gaugeRepository.recordValue(gaugeName, 1);
		Gauge g = gaugeRepository.findOne(gaugeName);
		assertThat(g.getValue(), equalTo(1L));

		gaugeRepository.recordValue(gaugeName, 20);
		assertThat(gaugeRepository.findOne(gaugeName).getValue(), equalTo(20L));

		gaugeRepository.reset(gaugeName);
		assertThat(gaugeRepository.findOne(gaugeName).getValue(), equalTo(0L));
	}

	@Test
	public void testCrud() {
		GaugeRepository repo = gaugeRepository;
		String myGaugeName = "myGauge";
		String yourGaugeName = "yourGauge";

		// Create and save a Gauge named 'myGauge'
		Gauge g1 = new Gauge(myGaugeName);
		Gauge myGauge = repo.save(g1);
		assertThat(myGauge.getName(), is(notNullValue()));
		// Create and save a Gauge named 'yourGauge'
		Gauge g2 = new Gauge(yourGaugeName);
		Gauge yourGauge = repo.save(g2);
		assertThat(yourGauge.getName(), is(notNullValue()));
		assertTrue(repo.exists(yourGauge.getName()));

		// Retrieve by name and compare for equality to previously saved instance.
		Gauge result = repo.findOne(myGaugeName);
		assertThat(result, equalTo(myGauge));

		result = repo.findOne(yourGauge.getName());
		assertThat(result, equalTo(yourGauge));

		List<Gauge> gauges = (List<Gauge>) repo.findAll();
		assertThat(gauges.size(), equalTo(2));

		repo.delete(myGauge);
		assertThat(repo.findOne(myGaugeName), is(nullValue()));

		repo.deleteAll();
		assertThat(repo.findOne(yourGaugeName), is(nullValue()));
		assertEquals(0, repo.count());

		repo.save(Arrays.asList(g1, g2));
		assertThat(repo.count(), equalTo(2L));

		repo.delete(Arrays.asList(g1, g2));
		assertEquals(0, repo.count());
	}

}
