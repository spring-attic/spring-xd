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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Collections;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.xd.analytics.metrics.core.RichGauge;
import org.springframework.xd.analytics.metrics.core.RichGaugeRepository;
import org.springframework.xd.analytics.metrics.redis.RedisRichGaugeRepository;
import org.springframework.xd.test.redis.RedisTestSupport;

/**
 * @author David Turanski
 * @author Gary Russell
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RichGaugeHandlerTestsConfig.class)
public class RichGaugeHandlerTests {

	@Rule
	public RedisTestSupport redisAvailableRule = new RedisTestSupport();

	@Autowired
	RedisRichGaugeRepository repo;

	@Autowired
	MessageChannel input;

	@Test
	public void testParseDouble() {
		RichGaugeRepository richGaugeRepository = mock(RichGaugeRepository.class);
		RichGaugeHandler handler = new RichGaugeHandler(richGaugeRepository, "'test'", -1);
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
		// Included here because the message handler constructor creates the gauge. Don't want to
		// delete it in @After.
		repo.delete("test");
	}

	@Test
	public void testhandlerWithExpression() {

		@SuppressWarnings("resource")
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

		MapPropertySource propertiesSource = new MapPropertySource("test", Collections.singletonMap("valueExpression",
				(Object) "payload.get('price').asDouble()"));

		applicationContext.getEnvironment().getPropertySources().addLast(propertiesSource);

		applicationContext.register(RichGaugeHandlerTestsConfig.class);

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
		// assertEquals(1, gauge.getCount());

		// Included here because the message handler constructor creates the gauge. Don't want to
		// delete it in @After.
		repo.delete("test");
	}

	static class StringToJsonNodeTransformer {

		private ObjectMapper mapper = new ObjectMapper();

		public JsonNode transform(String json) {
			try {
				return mapper.readTree(json);
			}
			catch (JsonParseException e) {
				throw new MessageTransformationException("unable to parse input: " + e.getMessage(), e);
			}
			catch (IOException e) {
				throw new MessageTransformationException("unable to create json parser: " + e.getMessage(), e);
			}
		}
	}

}


@Configuration
@ImportResource("org/springframework/xd/analytics/metrics/integration/RichGaugeHandlerTests-context.xml")
class RichGaugeHandlerTestsConfig {

	@Bean
	public RedisConnectionFactory connectionFactory() {
		try {
			JedisConnectionFactory cf = new JedisConnectionFactory();
			cf.setHostName("localhost");
			cf.setPort(6379);
			cf.afterPropertiesSet();
			cf.getConnection().close();
			return cf;
		} // The following is to have setup properly finishing
		// The actual test(s) won't be executed thx to RedisAvailableRule
		catch (RedisConnectionFailureException e) {
			RedisConnectionFactory mockCF = mock(RedisConnectionFactory.class);
			when(mockCF.getConnection()).thenReturn(mock(RedisConnection.class));
			return mockCF;
		}
	}
}
