/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.integration.redis;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.amqp.utils.test.TestUtils;
import org.springframework.expression.Expression;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.redis.inbound.RedisQueueMessageDrivenEndpoint;
import org.springframework.xd.dirt.integration.bus.Binding;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.integration.bus.PartitionCapableBusTests;
import org.springframework.xd.dirt.integration.bus.RedisTestMessageBus;
import org.springframework.xd.test.redis.RedisTestSupport;

/**
 * @author Gary Russell
 */
public class RedisMessageBusTests extends PartitionCapableBusTests {

	@Rule
	public RedisTestSupport redisAvailableRule = new RedisTestSupport();

	@Override
	protected MessageBus getMessageBus() {
		if (testMessageBus == null) {
			testMessageBus = new RedisTestMessageBus(redisAvailableRule.getResource(), getCodec());
		}
		return testMessageBus;
	}

	@Override
	public void testSendAndReceivePubSub() throws Exception {

		TimeUnit.SECONDS.sleep(2); //TODO remove timing issue

		super.testSendAndReceivePubSub();
	}

	@Test
	public void testConsumerProperties() throws Exception {
		MessageBus bus = getMessageBus();
		Properties properties = new Properties();
		bus.bindConsumer("props.0", new DirectChannel(), properties);
		@SuppressWarnings("unchecked")
		List<Binding> bindings = TestUtils.getPropertyValue(bus, "messageBus.bindings", List.class);
		assertEquals(1, bindings.size());
		AbstractEndpoint endpoint = bindings.get(0).getEndpoint();
		assertThat(endpoint, instanceOf(RedisQueueMessageDrivenEndpoint.class));
		bus.unbindConsumers("props.0");
		assertEquals(0, bindings.size());

		properties.put("concurrency", "2");
		bus.bindConsumer("props.0", new DirectChannel(), properties);
		assertEquals(1, bindings.size());
		endpoint = bindings.get(0).getEndpoint();
		assertThat(endpoint.getClass().getName(), containsString("CompositeRedisQueueMessageDrivenEndpoint"));
		assertEquals(2, TestUtils.getPropertyValue(endpoint, "consumers", Collection.class).size());
		bus.unbindConsumers("props.0");
		assertEquals(0, bindings.size());
	}

	@Override
	protected String getEndpointRouting(AbstractEndpoint endpoint) {
		return TestUtils.getPropertyValue(endpoint, "handler.delegate.queueNameExpression", Expression.class).getExpressionString();
	}

	@Override
	protected String getPubSubEndpointRouting(AbstractEndpoint endpoint) {
		return TestUtils.getPropertyValue(endpoint, "handler.delegate.topicExpression", Expression.class).getExpressionString();
	}

}
