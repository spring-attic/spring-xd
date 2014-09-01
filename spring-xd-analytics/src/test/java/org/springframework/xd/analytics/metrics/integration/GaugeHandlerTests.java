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

package org.springframework.xd.analytics.metrics.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;

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
import org.springframework.xd.analytics.metrics.core.Gauge;
import org.springframework.xd.analytics.metrics.core.GaugeRepository;
import org.springframework.xd.analytics.metrics.redis.RedisGaugeRepository;
import org.springframework.xd.test.redis.RedisTestSupport;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author David Turanski
 * @author Luke Taylor
 * @author Gary Russell
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = GaugeHandlerTestsConfig.class)
public class GaugeHandlerTests {

	@Rule
	public RedisTestSupport redisAvailableRule = new RedisTestSupport();

	@Autowired
	RedisGaugeRepository repo;

	@Test
	public void testConvertToDouble() {
		GaugeRepository gaugeRepo = mock(GaugeRepository.class);
		GaugeHandler handler = new GaugeHandler(gaugeRepo, "'test'");
		int i = 4;
		long val = handler.convertToLong(i);
		assertEquals(4, val, 0.001);
		double d = 4.0;
		val = handler.convertToLong(d);
		assertEquals(4, val, 0.001);
		float f = 4.0f;
		val = handler.convertToLong(f);
		assertEquals(4, val, 0.001);
		long l = 4L;
		val = handler.convertToLong(l);
		assertEquals(4, val, 0.001);
		String s = "4";
		val = handler.convertToLong(s);
		assertEquals(4, val);
	}

	@Autowired
	MessageChannel input;

	@Test
	public void testhandler() {
		input.send(new GenericMessage<Long>(10L));
		input.send(new GenericMessage<Long>(20L));
		input.send(new GenericMessage<Long>(24L));

		Gauge gauge = repo.findOne("test");
		assertNotNull(gauge);
		assertEquals(24, gauge.getValue());
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

		applicationContext.register(GaugeHandlerTestsConfig.class);

		applicationContext.refresh();
		input = applicationContext.getBean("input", MessageChannel.class);
		repo = applicationContext.getBean(RedisGaugeRepository.class);

		String json = "{\"symbol\":\"VMW\", \"price\":73}";
		StringToJsonNodeTransformer txf = new StringToJsonNodeTransformer();
		JsonNode node = txf.transform(json);
		assertEquals(73, node.get("price").asLong());

		input.send(new GenericMessage<Object>(node));

		Gauge gauge = repo.findOne("test");
		assertNotNull(gauge);
		assertEquals(73, gauge.getValue());
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
@ImportResource("org/springframework/xd/analytics/metrics/integration/GaugeHandlerTests-context.xml")
class GaugeHandlerTestsConfig {

	@Bean
	public RedisConnectionFactory connectionFactory() {
		try {
			JedisConnectionFactory cf = new JedisConnectionFactory();
			cf.setHostName("localhost");
			cf.setPort(6379);
			cf.afterPropertiesSet();
			return cf;
		}
		catch (RedisConnectionFailureException e) {
			RedisConnectionFactory mockCF = mock(RedisConnectionFactory.class);
			when(mockCF.getConnection()).thenReturn(mock(RedisConnection.class));
			return mockCF;
		}
	}
}
