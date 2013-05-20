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

package org.springframework.xd.dirt.stream;

import java.util.List;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.redis.outbound.RedisQueueOutboundChannelAdapter;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;

/**
 * @author Mark Fisher
 */
public class RedisStreamDeployer implements StreamDeployer {

	private final StreamParser streamParser = new DefaultStreamParser();

	private final RedisQueueOutboundChannelAdapter adapter;

	public RedisStreamDeployer(RedisConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		this.adapter = new RedisQueueOutboundChannelAdapter("queue.deployer", connectionFactory);
		this.adapter.setExtractPayload(false);
		this.adapter.afterPropertiesSet();
	}

	@Override
	public void deployStream(String name, String config) {
		List<ModuleDeploymentRequest> requests = this.streamParser.parse(name, config);
		for (ModuleDeploymentRequest request : requests) {
			Message<?> message = MessageBuilder.withPayload(request.toString()).build();
			this.adapter.handleMessage(message);
		}
	}

}
