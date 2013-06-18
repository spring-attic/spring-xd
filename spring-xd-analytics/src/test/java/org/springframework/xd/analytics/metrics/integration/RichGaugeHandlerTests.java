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

import java.io.IOException;
import java.util.Collections;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.transformer.MessageTransformationException;
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
		assertEquals(18.0, gauge.getAverage(), 0.001);
		//Included here because the message handler constructor creates the gauge. Don't want to 
		//delete it in @After.
		repo.delete("test");
	}

	@Test
	public void testhandlerWithExpression() {

		@SuppressWarnings("resource")
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();

		MapPropertySource propertiesSource = new MapPropertySource("test", Collections.singletonMap("valueExpression",
				(Object) "payload.get('price').asDouble()"));

		applicationContext.getEnvironment().getPropertySources().addLast(propertiesSource);

		applicationContext
				.setConfigLocation("/org/springframework/xd/analytics/metrics/integration/RichGaugeHandlerTests-context.xml");

		applicationContext.refresh();
		input = applicationContext.getBean("input", MessageChannel.class);
		repo = applicationContext.getBean(RedisRichGaugeRepository.class);

		String json = "{\"symbol\":\"VMW\", \"price\":73}";
		StringToJsonNodeTransformer txf = new StringToJsonNodeTransformer();
		JsonNode node = txf.transform(json);
		assertEquals(73.0, node.get("price").asDouble(), 0.001);

		input.send(new GenericMessage<Object>(node));

		RichGauge gauge = repo.findOne("test");
		assertNotNull(gauge);
		assertEquals(73.0, gauge.getValue(), 0.001);
		//assertEquals(1, gauge.getCount());

		//Included here because the message handler constructor creates the gauge. Don't want to 
		//delete it in @After.
		repo.delete("test");
	}

	static class StringToJsonNodeTransformer {
		private ObjectMapper mapper = new ObjectMapper();

		public JsonNode transform(String json) {
			try {
				JsonParser parser = mapper.getJsonFactory().createJsonParser(json);
				return parser.readValueAsTree();
			} catch (JsonParseException e) {
				throw new MessageTransformationException("unable to parse input: " + e.getMessage(), e);
			} catch (IOException e) {
				throw new MessageTransformationException("unable to create json parser: " + e.getMessage(), e);
			}
		}
	}

}
