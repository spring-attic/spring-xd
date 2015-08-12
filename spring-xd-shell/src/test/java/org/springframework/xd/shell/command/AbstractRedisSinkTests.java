/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.shell.command;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.xd.shell.command.fixtures.HttpSource;
import org.springframework.xd.test.redis.RedisTestSupport;


/**
 * Integration tests for Redis sink.
 *
 * @author Ilayaperumal Gopinathan
 */
public class AbstractRedisSinkTests extends AbstractStreamIntegrationTest {

	@Rule
	public RedisTestSupport redisAvailableRule = new RedisTestSupport();

	private RedisTemplate<String, Object> redisTemplate;

	private RedisMessageListenerContainer container;


	@Before
	public void setup() {
		createTemplate();
	}

	@Test
	public void testRedisTopicExpressionSink() throws InterruptedException {
		String topicName = "testTopicExpression";
		CountDownLatch latch = new CountDownLatch(1);
		setupMessageListener(latch, topicName);
		final HttpSource httpSource = newHttpSource();
		stream().create(generateStreamName(), "%s | redis --topicExpression='''%s'''", httpSource,
				topicName);
		Thread.sleep(1000);
		final String stringToPost = "test";
		httpSource.ensureReady().postData(stringToPost);
		latch.await(3, TimeUnit.SECONDS);
		assertEquals(0, latch.getCount());
		container.stop();
	}

	@Test
	public void testRedisTopicSink() throws InterruptedException {
		String topicName = "testTopic";
		CountDownLatch latch = new CountDownLatch(1);
		setupMessageListener(latch, topicName);
		final HttpSource httpSource = newHttpSource();
		stream().create(generateStreamName(), "%s | redis --topic=%s", httpSource, topicName);
		Thread.sleep(1000);
		final String stringToPost = "test";
		httpSource.ensureReady().postData(stringToPost);
		latch.await(3, TimeUnit.SECONDS);
		assertEquals(0, latch.getCount());
		container.stop();
	}

	@Test
	public void testRedisQueueExpressionSink() throws InterruptedException {
		final String listName = "testListExpression";
		final String stringToPost = "test";
		final HttpSource httpSource = newHttpSource();
		stream().create(generateStreamName(), "%s | redis --queueExpression='''%s'''", httpSource, listName);
		Thread.sleep(1000);
		httpSource.ensureReady().postData(stringToPost);
		byte[] leftPop = (byte[]) createTemplate().boundListOps(listName).leftPop(5, TimeUnit.SECONDS);
		Assert.assertEquals(stringToPost, new String(leftPop));
	}

	@Test
	public void testRedisQueueSink() throws InterruptedException {
		final String listName = "testList";
		final String stringToPost = "test";
		final HttpSource httpSource = newHttpSource();
		stream().create(generateStreamName(), "%s | redis --queue=%s", httpSource, listName);
		Thread.sleep(1000);
		httpSource.ensureReady().postData(stringToPost);
		byte[] leftPop = (byte[]) createTemplate().boundListOps(listName).leftPop(5, TimeUnit.SECONDS);
		Assert.assertEquals(stringToPost, new String(leftPop));
	}

	@Test
	public void testRedisStoreExpressionSink() throws InterruptedException {
		final String setName = "testSetExpression";
		final String stringToPost = "test";
		final HttpSource httpSource = newHttpSource();
		stream().create(generateStreamName(), "%s | redis --keyExpression='''%s''' --collectionType='SET'", httpSource,
				setName);
		Thread.sleep(1000);
		httpSource.ensureReady().postData(stringToPost);
		byte[] popped = (byte[]) createTemplate().boundSetOps(setName).pop();
		Assert.assertEquals(stringToPost, new String(popped));
	}

	@Test
	public void testRedisStoreSink() throws InterruptedException {
		final String setName = "testSet";
		final String stringToPost = "test";
		final HttpSource httpSource = newHttpSource();
		stream().create(generateStreamName(), "%s | redis --key=%s --collectionType='SET'", httpSource,
				setName);
		Thread.sleep(1000);
		httpSource.ensureReady().postData(stringToPost);
		byte[] popped = (byte[]) createTemplate().boundSetOps(setName).pop();
		Assert.assertEquals(stringToPost, new String(popped));
	}

	private RedisTemplate<String, Object> createTemplate() {
		if (this.redisTemplate != null) {
			return this.redisTemplate;
		}
		RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();
		template.setConnectionFactory(this.redisAvailableRule.getResource());
		template.setKeySerializer(new StringRedisSerializer());
		template.setEnableDefaultSerializer(false);
		template.afterPropertiesSet();
		this.redisTemplate = template;
		return template;
	}

	private void setupMessageListener(CountDownLatch latch, String topic) throws InterruptedException {
		MessageListenerAdapter listener = new MessageListenerAdapter();
		listener.setDelegate(new Listener(latch));
		listener.afterPropertiesSet();

		this.container = new RedisMessageListenerContainer();
		container.setConnectionFactory(this.redisAvailableRule.getResource());
		container.afterPropertiesSet();
		container.addMessageListener(listener, Collections.<Topic> singletonList(new ChannelTopic(topic)));
		container.start();
		Thread.sleep(1000);
	}

	private static class Listener {

		private final CountDownLatch latch;

		private Listener(CountDownLatch latch) {
			this.latch = latch;
		}

		@SuppressWarnings("unused")
		public void handleMessage(Object s) {
			this.latch.countDown();
		}
	}
}
