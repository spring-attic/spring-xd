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

package test;

import org.junit.Test;

import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.x.redis.RedisQueueOutboundChannelAdapter;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;

/**
 * @author Mark Fisher
 */
public class ModuleDeploymentTest {

	// for now copy 'test.xml' to /tmp/dirt/modules/generic
	// then run redis-server and ContainerLauncher before this test

	@Test
	public void test() throws Exception {
		JedisConnectionFactory connectionFactory = new JedisConnectionFactory();
		connectionFactory.afterPropertiesSet();
		RedisQueueOutboundChannelAdapter adapter = new RedisQueueOutboundChannelAdapter("queue.deployer", connectionFactory);
		adapter.setExtractPayload(false);
		adapter.afterPropertiesSet();
		ModuleDeploymentRequest request = new ModuleDeploymentRequest();
		request.setGroup("");
		request.setType("generic");
		request.setModule("test");
		request.setIndex(0);
		Message<?> message = MessageBuilder.withPayload(request.toString()).build();
		adapter.handleMessage(message);
	}

}
