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
		RichGauge original = new RichGauge("test");
		original.set(9.99);
		original.set(0.01);
		gs.save(original);

		RichGauge g = gs.findOne("test");
		assertEquals("test", g.getName());
		assertEquals(0.01, g.getValue(), 1E-6);
		assertEquals(5.0, g.getAverage(), 1E-6);
		assertEquals(9.99, g.getMax(), 1E-6);
		assertEquals(0.01, g.getMin(), 1E-6);
	}

}
