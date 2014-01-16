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

package org.springframework.xd.dirt.plugins;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.integration.redis.outbound.RedisQueueOutboundChannelAdapter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.test.redis.RedisTestSupport;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class ModuleDeploymentTests {

	// run redis-server and RedisContainerLauncher (or StreamServer) before this test

	@Rule
	public RedisTestSupport redisAvailableRule = new RedisTestSupport();

	private static long now = System.currentTimeMillis();

	@Test
	public void testProcessor() throws Exception {
		LettuceConnectionFactory connectionFactory = redisAvailableRule.getResource();
		RedisQueueOutboundChannelAdapter adapter = new RedisQueueOutboundChannelAdapter("queue.deployer." + now,
				connectionFactory);
		adapter.setExtractPayload(false);
		adapter.afterPropertiesSet();
		ModuleDeploymentRequest request = new ModuleDeploymentRequest();
		request.setGroup("test");
		request.setType(ModuleType.processor);
		request.setModule("filter");
		request.setIndex(0);
		Message<?> message = MessageBuilder.withPayload(request.toString()).build();
		adapter.handleMessage(message);
	}

	@Test
	public void testSimpleStream() throws Exception {
		LettuceConnectionFactory connectionFactory = redisAvailableRule.getResource();
		RedisQueueOutboundChannelAdapter adapter = new RedisQueueOutboundChannelAdapter("queue.deployer." + now,
				connectionFactory);
		adapter.setExtractPayload(false);
		adapter.afterPropertiesSet();
		ModuleDeploymentRequest sinkRequest = new ModuleDeploymentRequest();
		sinkRequest.setGroup("teststream");
		sinkRequest.setType(ModuleType.sink);
		sinkRequest.setModule("log");
		sinkRequest.setIndex(1);
		Message<?> sinkMessage = MessageBuilder.withPayload(sinkRequest.toString()).build();
		adapter.handleMessage(sinkMessage);
		ModuleDeploymentRequest sourceRequest = new ModuleDeploymentRequest();
		sourceRequest.setGroup("teststream");
		sourceRequest.setType(ModuleType.source);
		sourceRequest.setModule("time");
		sourceRequest.setIndex(0);
		Message<?> sourceMessage = MessageBuilder.withPayload(sourceRequest.toString()).build();
		adapter.handleMessage(sourceMessage);
	}

}
