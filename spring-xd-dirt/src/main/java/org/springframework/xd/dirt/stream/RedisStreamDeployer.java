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

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.x.redis.RedisQueueOutboundChannelAdapter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.module.ModuleDeploymentRequest;

/**
 * @author Mark Fisher
 */
public class RedisStreamDeployer implements StreamDeployer {

	private final RedisQueueOutboundChannelAdapter adapter;

	public RedisStreamDeployer(RedisConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		this.adapter = new RedisQueueOutboundChannelAdapter("queue.deployer", connectionFactory);
		this.adapter.setExtractPayload(false);
		this.adapter.afterPropertiesSet();
	}

	@Override
	public void deployStream(String name, String config) {
		// TODO: replace with a StreamParser that returns Module instances and supports parameters
		String[] modules = StringUtils.tokenizeToStringArray(config, "|");
		Assert.isTrue(modules.length > 1, "at least 2 modules required");
		for (int i = modules.length - 1; i >= 0; i--) {
			String type = (i == 0) ? "source" : (i == modules.length - 1) ? "sink" : "processor";
			ModuleDeploymentRequest request = new ModuleDeploymentRequest();
			request.setGroup(name);
			request.setType(type);
			request.setModule(modules[i]);
			request.setIndex(i);
			Message<?> message = MessageBuilder.withPayload(request.toString()).build();
			this.adapter.handleMessage(message);
		}
	}

}
