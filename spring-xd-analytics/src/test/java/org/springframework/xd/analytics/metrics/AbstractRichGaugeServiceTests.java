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
import org.springframework.xd.analytics.metrics.core.RichGaugeService;

/**
 * @author Luke Taylor
 */
public abstract class AbstractRichGaugeServiceTests {

	protected abstract RichGaugeService createService();

	@Test
	public void getOrCreateReturnsCorrectInstance() throws Exception {
		RichGaugeService gs = createService();
		gs.getOrCreate("test");
		gs.setValue("test", 9.99);

		RichGauge g = gs.getOrCreate("test");
		assertEquals("test", g.getName());
		assertEquals(9.99, g.getValue(), 1E-6);
	}

	@Test(expected = MetricsException.class)
	public void setValueOnMissingGaugeRaisesException() {
		RichGaugeService gs = createService();
		gs.setValue("test", 9.99);
	}

	@Test
	public void resetExistingGaugeCausesReset() throws Exception {
		RichGaugeService gs = createService();
		gs.getOrCreate("test");
		gs.setValue("test", 9.99);
		gs.reset("test");
		RichGauge g = gs.getOrCreate("test");
		assertEquals(0.0, g.getValue(), 1E-6);
	}
}

