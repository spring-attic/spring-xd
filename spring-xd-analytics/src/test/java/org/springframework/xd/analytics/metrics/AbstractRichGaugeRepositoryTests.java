/*
 * Copyright 2011-2013 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.springframework.xd.analytics.metrics.core.RichGauge;
import org.springframework.xd.analytics.metrics.core.RichGaugeRepository;

/**
 * @author Luke Taylor
 */
public abstract class AbstractRichGaugeRepositoryTests {

	protected abstract RichGaugeRepository createService();

	@Test
	public void getOrCreateReturnsCorrectInstance() throws Exception {
		RichGaugeRepository gs = createService();
		gs.recordValue("test", 9.99, -1D);
		gs.recordValue("test", 0.01, -1D);

		RichGauge g = gs.findOne("test");
		assertEquals("test", g.getName());
		assertEquals(0.01, g.getValue(), 1E-6);
		assertEquals(5.0, g.getAverage(), 1E-6);
		assertEquals(9.99, g.getMax(), 1E-6);
		assertEquals(0.01, g.getMin(), 1E-6);
	}

	@Test
	public void resetExistingGaugeCausesReset() throws Exception {
		RichGaugeRepository gs = createService();
		gs.recordValue("test", 9.99, -1D);
		gs.reset("test");
		RichGauge g = gs.findOne("test");
		assertEquals(0.0, g.getValue(), 1E-6);
	}

	@Test
	public void testExponentialMovingAverage() throws Exception {
		RichGaugeRepository gs = createService();
		gs.recordValue("test", 71.0, 0.1D);
		RichGauge g = gs.findOne("test");
		assertEquals(71.0, g.getAverage(), 1E-6);
		gs.recordValue("test", 70.0, 0.1D);
		g = gs.findOne("test");
		assertEquals(71.0, g.getAverage(), 1E-6);
		gs.recordValue("test", 69.0, 0.1D);
		g = gs.findOne("test");
		assertEquals(70.9, g.getAverage(), 1E-6);
		gs.recordValue("test", 68.0, 0.1D);
		g = gs.findOne("test");
		assertEquals(70.71, g.getAverage(), 1E-6);
	}
}
