/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.x.redis;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.message.GenericMessage;
import org.springframework.xd.test.redis.RedisAvailableRule;

import static org.junit.Assert.assertEquals;

/**
 * Integration test of {@link RedisQueueInboundChannelAdapter}
 *
 * @author Jennifer Hickey
 */
public class RedisQueueInboundChannelAdapterTests {

	private static final String QUEUE_NAME = "inboundadaptertest";

	private LettuceConnectionFactory connectionFactory;

	private final BlockingDeque<Object> messages = new LinkedBlockingDeque<Object>(99);

	private RedisQueueInboundChannelAdapter adapter;

	@Rule
	public RedisAvailableRule redisAvailableRule = new RedisAvailableRule();

	@Before
	public void setUp() {
		messages.clear();
		this.connectionFactory = new LettuceConnectionFactory();
		connectionFactory.afterPropertiesSet();
		DirectChannel outputChannel = new DirectChannel();
		outputChannel.subscribe(new TestMessageHandler());
		adapter = new RedisQueueInboundChannelAdapter(QUEUE_NAME, connectionFactory);
		adapter.setOutputChannel(outputChannel);
	}

	@After
	public void tearDown() {
		adapter.stop();
		connectionFactory.getConnection().del(QUEUE_NAME.getBytes());
	}

	@Test
	public void testDefaultPayloadSerializer() throws Exception {
		StringRedisTemplate template = new StringRedisTemplate(connectionFactory);
		template.afterPropertiesSet();

		adapter.afterPropertiesSet();
		adapter.start();

		template.boundListOps(QUEUE_NAME).rightPush("message1");
		@SuppressWarnings("unchecked")
		Message<String> message = (Message<String>) messages.poll(1, TimeUnit.SECONDS);
		assertEquals("message1", message.getPayload());
	}

	@Test
	public void testDefaultMsgSerializer() throws Exception {
		RedisTemplate<String, Message<String>> template = new RedisTemplate<String, Message<String>>();
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new MessageRedisSerializer());
		template.setConnectionFactory(connectionFactory);
		template.afterPropertiesSet();

		adapter.setExtractPayload(false);
		adapter.afterPropertiesSet();
		adapter.start();

		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put("header1", "foo");
		template.boundListOps(QUEUE_NAME).rightPush(new GenericMessage<String>("message2", headers));
		@SuppressWarnings("unchecked")
		Message<String> message = (Message<String>) messages.poll(1, TimeUnit.SECONDS);
		assertEquals("message2", message.getPayload());
		assertEquals("foo", message.getHeaders().get("header1"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testNoSerializer() throws Exception {
		RedisTemplate<String, byte[]> template = new RedisTemplate<String, byte[]>();
		template.setEnableDefaultSerializer(false);
		template.setKeySerializer(new StringRedisSerializer());
		template.setConnectionFactory(connectionFactory);
		template.afterPropertiesSet();

		adapter.setEnableDefaultSerializer(false);
		adapter.afterPropertiesSet();
		adapter.start();

		template.boundListOps(QUEUE_NAME).rightPush("message3".getBytes());
		Message<byte[]> message = (Message<byte[]>) messages.poll(1, TimeUnit.SECONDS);
		assertEquals("message3", new String(message.getPayload()));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testNoSerializerNoExtractPayload() throws Exception {
		RedisTemplate<String, byte[]> template = new RedisTemplate<String, byte[]>();
		template.setEnableDefaultSerializer(false);
		template.setKeySerializer(new StringRedisSerializer());
		template.setConnectionFactory(connectionFactory);
		template.afterPropertiesSet();

		adapter.setEnableDefaultSerializer(false);
		adapter.setExtractPayload(false);
		adapter.afterPropertiesSet();
		adapter.start();
	}

	@Test
	public void testCustomPayloadSerializer() throws Exception {
		RedisTemplate<String, Long> template = new RedisTemplate<String, Long>();
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new GenericToStringSerializer<Long>(Long.class));
		template.setConnectionFactory(connectionFactory);
		template.afterPropertiesSet();

		adapter.setSerializer(new GenericToStringSerializer<Long>(Long.class));
		adapter.afterPropertiesSet();
		adapter.start();

		template.boundListOps(QUEUE_NAME).rightPush(5l);
		@SuppressWarnings("unchecked")
		Message<Long> message = (Message<Long>) messages.poll(1, TimeUnit.SECONDS);
		assertEquals(5L,(long) message.getPayload());
	}

	@Test
	public void testCustomMessageSerializer() throws Exception {
		RedisTemplate<String, Message<?>> template = new RedisTemplate<String, Message<?>>();
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(new TestMessageSerializer());
		template.setConnectionFactory(connectionFactory);
		template.afterPropertiesSet();

		adapter.setSerializer(new TestMessageSerializer());
		adapter.setExtractPayload(false);
		adapter.afterPropertiesSet();
		adapter.start();

		template.boundListOps(QUEUE_NAME).rightPush(new GenericMessage<Long>(10l));
		@SuppressWarnings("unchecked")
		Message<Long> message = (Message<Long>) messages.poll(1, TimeUnit.SECONDS);
		assertEquals(10L, (long)message.getPayload());
	}

	private class TestMessageHandler implements MessageHandler {

		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			messages.add(message);
		}
	}

	private class TestMessageSerializer implements RedisSerializer<Message<?>> {

		@Override
		public byte[] serialize(Message<?> t) throws SerializationException {
			return "Foo".getBytes();
		}

		@Override
		public Message<?> deserialize(byte[] bytes) throws SerializationException {
			return new GenericMessage<Long>(10l);
		}
	}
}
