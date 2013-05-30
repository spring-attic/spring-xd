/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.xd.analytics.metrics.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.analytics.metrics.core.RichGauge;
import org.springframework.xd.analytics.metrics.core.RichGaugeService;
import org.springframework.xd.analytics.metrics.redis.RedisRichGaugeRepository;

/**
 * @author David Turanski
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class RichGaugeHandlerTests {
	@Autowired
	RedisRichGaugeRepository repo;

	@Test
	public void testParseDouble() {
		RichGaugeService richGaugeService = mock(RichGaugeService.class);
		RichGaugeHandler handler = new RichGaugeHandler(richGaugeService, "test");
		int i = 4;
		double val = handler.convertToDouble(i);
		assertEquals(4.0, val, 0.001);
		double d = 4;
		val = handler.convertToDouble(d);
		assertEquals(4.0, val, 0.001);
		float f = 4;
		val = handler.convertToDouble(f);
		assertEquals(4.0, val, 0.001);
		long l = 4L;
		val = handler.convertToDouble(l);
		assertEquals(4.0, val, 0.001);
		String s = "4.0";
		val = handler.convertToDouble(s);
		assertEquals(4.0, val, 0.001);
	}

	@Autowired
	MessageChannel input;

	@Test
	public void testhandler() {
		input.send(new GenericMessage<Double>(10.0));
		input.send(new GenericMessage<Double>(20.0));
		input.send(new GenericMessage<Double>(24.0));

		RichGauge gauge = repo.findOne("test");
		assertNotNull(gauge);
		assertEquals(3, gauge.getCount());
		assertEquals(24.0, gauge.getMax(), 0.001);
		assertEquals(10.0, gauge.getMin(), 0.001);
		assertEquals(18.0, gauge.getMean(), 0.001);
		//Included here because the message handler constructor creates the gauge. Don't want to 
		//delete it in @After.
		repo.delete("test");
	}
}
