/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.analytics.metrics.core;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import org.junit.Test;

public class GaugeTests {

	@Test
	public void nameOnly() {
		Gauge g = new Gauge("myGauge");
		assertThat(g.getName(), equalTo("myGauge"));
	}

	@Test
	public void nameWithValue() {
		Gauge g = new Gauge("myGauge", 314);
		assertThat(g.getName(), equalTo("myGauge"));
		assertThat(g.getValue(), equalTo(314L));
	}

	@Test
	public void equalsContract() {
		EqualsVerifier.forClass(Gauge.class).suppress(Warning.NULL_FIELDS).verify();
	}

	@Test
	public void equalsAndHashcodeWorkForSetStorage() throws Exception {
		Gauge g = new Gauge("myGauge", 8);
		HashSet<Gauge> set = new HashSet<Gauge>();
		set.add(g);
		g.set(99);
		assertTrue(set.contains(g));
	}
}
