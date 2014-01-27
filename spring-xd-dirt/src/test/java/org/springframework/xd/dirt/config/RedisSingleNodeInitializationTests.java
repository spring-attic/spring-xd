/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.config;

import static org.junit.Assert.assertTrue;

import org.junit.Rule;

import org.springframework.context.ApplicationContext;
import org.springframework.integration.redis.inbound.RedisInboundChannelAdapter;
import org.springframework.integration.redis.inbound.RedisQueueMessageDrivenEndpoint;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.integration.x.redis.RedisMessageBus;
import org.springframework.messaging.MessageChannel;
import org.springframework.xd.test.redis.RedisTestSupport;

/**
 * @author David Turanski
 */
public class RedisSingleNodeInitializationTests extends AbstractSingleNodeInitializationTests {

	@Rule
	public RedisTestSupport redisAvailableRule = new RedisTestSupport();

	@Override
	protected String getTransport() {
		return "redis";
	}

	@Override
	protected Class<? extends MessageBus> getExpectedMessageBusType() {
		return RedisMessageBus.class;
	}

	@Override
	protected MessageChannel getControlChannel() {
		RedisQueueMessageDrivenEndpoint rqmde = context.getBean("redisInboundAdapter",
				RedisQueueMessageDrivenEndpoint.class);
		assertTrue(rqmde.isRunning());
		assertTrue(rqmde.isListening());
		RedisInboundChannelAdapter rica = context.getBean(RedisInboundChannelAdapter.class);
		assertTrue(rica.isRunning());
		return TestUtils.getPropertyValue(rica, "outputChannel", MessageChannel.class);
	}

	@Override
	protected void cleanup(ApplicationContext context) {
	}

}
