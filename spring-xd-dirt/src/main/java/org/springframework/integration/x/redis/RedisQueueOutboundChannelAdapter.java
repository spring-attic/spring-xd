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

package org.springframework.integration.x.redis;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.Message;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Jennifer Hickey
 */
public class RedisQueueOutboundChannelAdapter extends AbstractMessageHandler {

	private final String queueName;

	private volatile boolean extractPayload = true;

	private volatile boolean enableDefaultSerializer = true;

	private final RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();

	private RedisSerializer<?> serializer;

	public RedisQueueOutboundChannelAdapter(String queueName, RedisConnectionFactory connectionFactory) {
		Assert.hasText(queueName, "queueName is required");
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		this.queueName = queueName;
		this.redisTemplate.setConnectionFactory(connectionFactory);
		this.redisTemplate.setEnableDefaultSerializer(false);
		StringRedisSerializer stringSerializer = new StringRedisSerializer();
		this.redisTemplate.setKeySerializer(stringSerializer);
		this.redisTemplate.setHashKeySerializer(stringSerializer);
		this.redisTemplate.setHashValueSerializer(stringSerializer);
	}

	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	public void setEnableDefaultSerializer(boolean enableDefaultSerializer) {
		this.enableDefaultSerializer = enableDefaultSerializer;
	}

	public void setSerializer(RedisSerializer<?> serializer) {
		this.serializer = serializer;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		initializeRedisTemplate();
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Object objToQueue = message;
		if(this.extractPayload) {
			objToQueue = message.getPayload();
		}
		if (logger.isDebugEnabled()) {
			logger.debug("sending to redis queue '" + this.queueName + "': " + objToQueue);
		}
		this.redisTemplate.boundListOps(this.queueName).leftPush(objToQueue);
	}

	private void initializeRedisTemplate() {
		if(this.serializer == null) {
			if(enableDefaultSerializer) {
				initializeDefaultSerializer();
			}
			else {
				Assert.isTrue(extractPayload, "extractPayload must be true if no serializer is set");
			}
		}
		this.redisTemplate.setValueSerializer(this.serializer);
		this.redisTemplate.afterPropertiesSet();
	}

	private void initializeDefaultSerializer() {
		if(extractPayload) {
			this.serializer = new StringRedisSerializer();
		}
		else {
			this.serializer = new MessageRedisSerializer();
		}
	}
}
