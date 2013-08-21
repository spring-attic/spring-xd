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

import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.Message;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Jennifer Hickey
 */
public class RedisQueueInboundChannelAdapter extends MessageProducerSupport {

	private final String queueName;

	private volatile boolean extractPayload = true;

	private final RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();

	private volatile TaskScheduler taskScheduler;

	private volatile ScheduledFuture<?> listenerTask;

	private RedisSerializer<?> serializer;

	private boolean enableDefaultSerializer = true;

	public RedisQueueInboundChannelAdapter(String queueName, RedisConnectionFactory connectionFactory) {
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

	public void setSerializer(RedisSerializer<?> serializer) {
		this.serializer = serializer;
	}

	public void setEnableDefaultSerializer(boolean enableDefaultSerializer) {
		this.enableDefaultSerializer = enableDefaultSerializer;
	}

	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	@Override
	protected void onInit() {
		super.onInit();
		initializeRedisTemplate();
		this.taskScheduler = this.getTaskScheduler();
		if (this.taskScheduler == null) {
			ThreadPoolTaskScheduler tpts = new ThreadPoolTaskScheduler();
			tpts.afterPropertiesSet();
			this.taskScheduler = tpts;
		}
	}

	@Override
	protected void doStart() {
		super.doStart();
		this.listenerTask = this.taskScheduler.schedule(new ListenerTask(), new Date());
	}

	@Override
	protected void doStop() {
		super.doStop();
		if (this.listenerTask != null) {
			this.listenerTask.cancel(true);
		}
	}

	private void initializeRedisTemplate() {
		if (this.serializer == null) {
			if (enableDefaultSerializer) {
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
		if (extractPayload) {
			this.serializer = new StringRedisSerializer();
		}
		else {
			this.serializer = new MessageRedisSerializer();
		}
	}

	private class ListenerTask implements Runnable {

		@Override
		public void run() {
			try {
				while (isRunning()) {
					Object next = redisTemplate.boundListOps(queueName).rightPop(5, TimeUnit.SECONDS);
					if (next != null) {
						try {
							Message<?> message = null;
							if (extractPayload) {
								message = MessageBuilder.withPayload(next).build();
							}
							else {
								Assert.isInstanceOf(Message.class, next);
								message = (Message<?>) next;
							}
							sendMessage(message);
						}
						catch (Exception e) {
							logger.error("Error sending message", e);
						}
					}

				}
			}
			catch (RedisSystemException e) {
				if (isRunning()) {
					logger.error("Error polling Redis queue", e);
				}
			}
		}
	}
}
