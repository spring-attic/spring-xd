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
import org.springframework.redis.x.NoOpRedisSerializer;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class RedisQueueOutboundChannelAdapter extends AbstractMessageHandler {

	private final String queueName;

	private volatile boolean extractPayload = true;

	private final RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();

	private final ObjectMapper objectMapper = new ObjectMapper();

	private volatile boolean sendBytes;

	public RedisQueueOutboundChannelAdapter(String queueName, RedisConnectionFactory connectionFactory) {
		this(queueName, connectionFactory, new StringRedisSerializer());
	}

	public RedisQueueOutboundChannelAdapter(String queueName, RedisConnectionFactory connectionFactory,
			RedisSerializer<?> valueSerializer) {
		Assert.hasText(queueName, "queueName is required");
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		this.queueName = queueName;
		this.redisTemplate.setConnectionFactory(connectionFactory);
		StringRedisSerializer stringSerializer = new StringRedisSerializer();
		this.redisTemplate.setKeySerializer(stringSerializer);
		this.redisTemplate.setValueSerializer(valueSerializer);
		this.redisTemplate.setHashKeySerializer(stringSerializer);
		this.redisTemplate.setHashValueSerializer(stringSerializer);
		this.redisTemplate.afterPropertiesSet();
		this.sendBytes = valueSerializer instanceof NoOpRedisSerializer;
	}

	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Object payload = message.getPayload();
		if (this.sendBytes && this.extractPayload) {
			Assert.isInstanceOf(byte[].class, payload);
			this.redisTemplate.boundListOps(this.queueName).leftPush(payload);
		}
		else {
			String s = (this.extractPayload) ? payloadAsString(payload) : this.objectMapper.writeValueAsString(message);
			if (logger.isDebugEnabled()) {
				logger.debug("sending to redis queue '" + this.queueName + "': " + s);
			}
			this.redisTemplate.boundListOps(this.queueName).leftPush(s);
		}
	}

	private String payloadAsString(Object payload) throws Exception {
		if (payload instanceof byte[]) {
			return new String((byte[]) payload, "UTF-8");
		}
		else if (payload instanceof char[]) {
			return new String((char[]) payload);
		}
		else {
			return payload.toString();
		}
	}

}
