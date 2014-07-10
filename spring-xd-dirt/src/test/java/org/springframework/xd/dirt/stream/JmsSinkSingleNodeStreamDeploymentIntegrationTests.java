/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.stream;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import org.springframework.xd.dirt.integration.bus.RedisTestMessageBus;
import org.springframework.xd.dirt.test.sink.NamedChannelSink;
import org.springframework.xd.dirt.test.sink.SingleNodeNamedChannelSinkFactory;
import org.springframework.xd.dirt.test.source.NamedChannelSource;
import org.springframework.xd.dirt.test.source.SingleNodeNamedChannelSourceFactory;
import org.springframework.xd.test.redis.RedisTestSupport;

/**
 * @author liujiong
 */
public class JmsSinkSingleNodeStreamDeploymentIntegrationTests extends
		AbstractDistributedTransportSingleNodeStreamDeploymentIntegrationTests {


	@ClassRule
	public static RedisTestSupport redisAvailableRule = new RedisTestSupport();

	@BeforeClass
	public static void setUp() {
		setUp("redis");
	}

	@ClassRule
	public static ExternalResource initializeRedisTestMessageBus = new ExternalResource() {

		@Override
		protected void before() {
			if (testMessageBus == null || !(testMessageBus instanceof RedisTestMessageBus)) {
				testMessageBus = new RedisTestMessageBus(redisAvailableRule.getResource());
			}
		}
	};


	@Test
	public void fileSourceStreamReceivesJmsSinkStreamOutput() throws Exception {
		StreamDefinition mqtt1 = new StreamDefinition("jmstest", "file | jms");
		integrationSupport.createAndDeployStream(mqtt1);

		NamedChannelSource source = new SingleNodeNamedChannelSourceFactory(integrationSupport.messageBus()).createNamedChannelSource("queue:jmstest1.0");
		NamedChannelSink sink = new SingleNodeNamedChannelSinkFactory(integrationSupport.messageBus()).createNamedChannelSink("queue:jmstest1.0");

		Thread.sleep(1000);
		source.sendPayload("hello");
		Object result = sink.receivePayload(1000);

		assertEquals("hello", result);
		source.unbind();
		sink.unbind();
	}
}
