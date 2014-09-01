/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.analytics.metrics.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import org.junit.Test;

/**
 * @author Luke Taylor
 */
public class RichGaugeTests {

	private static double D = 1.0E-5;

	@Test
	public void valueMeanMinAndMaxAreCorrect() throws Exception {
		RichGauge g = new RichGauge("blah", 1.0);
		assertEquals(1.0, g.getValue(), D);
		assertEquals(1.0, g.getMax(), D);
		assertEquals(1.0, g.getMin(), D);
		assertEquals(1.0, g.getAverage(), D);

		g.set(1.5, -1D);
		assertEquals(1.25, g.getAverage(), D);
		g.set(0.5, -1D);

		assertEquals(3, g.getCount());
		assertEquals(0.5, g.getValue(), D);
		assertEquals(1.5, g.getMax(), D);
		assertEquals(0.5, g.getMin(), D);
		assertEquals(1.0, g.getAverage(), D);
	}

	@Test
	public void resetWorks() throws Exception {
		RichGauge g = new RichGauge("blah", 99.999);
		g.set(199.997, -1D);

		g.reset();
		assertEquals(0.0, g.getMax(), D);
		assertEquals(0.0, g.getMin(), D);
		assertEquals(0.0, g.getAverage(), D);
		assertEquals(0.0, g.getValue(), D);
		assertEquals(0, g.getCount());
	}

	// Data from http://www.itl.nist.gov/div898/handbook/pmc/section4/pmc431.htm
	@Test
	public void testExponentialMovingAverage() throws Exception {
		RichGauge g = new RichGauge("blah");
		g.set(71.0, 0.1D);
		assertEquals(71.0, g.getAverage(), D);
		g.set(70.0, 0.1D);
		assertEquals(71.0, g.getAverage(), D);
		g.set(69.0, 0.1D);
		assertEquals(70.9, g.getAverage(), D);
		g.set(68.0, 0.1D);
		assertEquals(70.71, g.getAverage(), D);

	}

	@Test
	public void equalsContract() {
		EqualsVerifier.forClass(RichGauge.class).suppress(Warning.NULL_FIELDS).verify();
	}

	@Test
	public void equalsAndHashcodeWorkForSetStorage() throws Exception {
		RichGauge g = new RichGauge("myGauge", 9.82);
		HashSet<RichGauge> set = new HashSet<RichGauge>();
		set.add(g);
		g.set(99.9, -1D);
		assertTrue(set.contains(g));
	}
}
