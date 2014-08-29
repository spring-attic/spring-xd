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

package org.springframework.xd.analytics.metrics.integration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.Properties;

import org.joda.time.DateTime;
import org.junit.Test;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.xd.analytics.metrics.core.AggregateCount;
import org.springframework.xd.analytics.metrics.core.AggregateCountResolution;
import org.springframework.xd.analytics.metrics.core.AggregateCounterRepository;
import org.springframework.xd.analytics.metrics.memory.InMemoryAggregateCounterRepository;


/**
 * Tests the aggregate-counter module.
 *
 * @author Eric Bottard
 */
public class AggregateCounterTests {

	private MessageChannel input() {
		return (MessageChannel) applicationContext.getBean("input");
	}

	private AggregateCounterRepository repository() {
		return applicationContext.getBean(AggregateCounterRepository.class);
	}


	private ApplicationContext applicationContext;

	@Test
	public void testCountNow() {
		applicationContext = new AnnotationConfigApplicationContext(
				NullTimefieldAggregateCounterTestsConfig.class);

		input().send(new GenericMessage<Object>(""));

		AggregateCount counts = repository().getCounts("foo", 5, AggregateCountResolution.hour);
		assertThat(counts.getCounts(), equalTo(new long[] { 0, 0, 0, 0, 1 }));
	}

	@Test
	public void testCountWithTimestampInMessageAndCustomFormat() {
		applicationContext = new AnnotationConfigApplicationContext(
				WithTimefieldAggregateCounterTestsConfig.class);

		input().send(new GenericMessage<Object>(Collections.singletonMap("ts", "14/10/1978")));

		DateTime from = new DateTime(1980, 1, 1, 0, 0);

		AggregateCount counts = repository().getCounts("foo", 5, from, AggregateCountResolution.year);
		assertThat(counts.getCounts(), equalTo(new long[] { 0, 0, 1, 0, 0 }));
	}

	@Test
	public void testCountWithCustomIncrement() {
		applicationContext = new AnnotationConfigApplicationContext(
				CustomIncrementAggregateCounterTestsConfig.class);

		input().send(new GenericMessage<Object>("43"));

		AggregateCount counts = repository().getCounts("foo", 5, AggregateCountResolution.hour);
		assertThat(counts.getCounts(), equalTo(new long[] { 0, 0, 0, 0, 43 }));
	}

	@Configuration
	@ImportResource("file:../modules/sink/aggregate-counter/config/aggregate-counter.xml")
	public static class NullTimefieldAggregateCounterTestsConfig {

		@Bean
		public PropertyPlaceholderConfigurer ppc() {
			PropertyPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertyPlaceholderConfigurer();
			Properties props = new Properties();
			props.put("timeField", "null");
			props.put("dateFormat", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			props.put("computedNameExpression", "'foo'");
			props.put("incrementExpression", "1");
			propertyPlaceholderConfigurer.setProperties(props);
			return propertyPlaceholderConfigurer;
		}


		@Bean
		public AggregateCounterRepository aggregateCounterRepository() {
			return new InMemoryAggregateCounterRepository();
		}
	}

	@Configuration
	@ImportResource("file:../modules/sink/aggregate-counter/config/aggregate-counter.xml")
	public static class CustomIncrementAggregateCounterTestsConfig {

		@Bean
		public PropertyPlaceholderConfigurer ppc() {
			PropertyPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertyPlaceholderConfigurer();
			Properties props = new Properties();
			props.put("timeField", "null");
			props.put("dateFormat", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
			props.put("computedNameExpression", "'foo'");
			props.put("incrementExpression", "payload");
			propertyPlaceholderConfigurer.setProperties(props);
			return propertyPlaceholderConfigurer;
		}


		@Bean
		public AggregateCounterRepository aggregateCounterRepository() {
			return new InMemoryAggregateCounterRepository();
		}
	}

	@Configuration
	@ImportResource("file:../modules/sink/aggregate-counter/config/aggregate-counter.xml")
	public static class WithTimefieldAggregateCounterTestsConfig {

		@Bean
		public PropertyPlaceholderConfigurer ppc() {
			PropertyPlaceholderConfigurer propertyPlaceholderConfigurer = new PropertyPlaceholderConfigurer();
			Properties props = new Properties();
			props.put("timeField", "payload.ts");
			props.put("dateFormat", "dd/MM/yyyy");
			props.put("computedNameExpression", "'foo'");
			props.put("incrementExpression", "1");
			propertyPlaceholderConfigurer.setProperties(props);
			return propertyPlaceholderConfigurer;
		}


		@Bean
		public AggregateCounterRepository aggregateCounterRepository() {
			return new InMemoryAggregateCounterRepository();
		}
	}

}
