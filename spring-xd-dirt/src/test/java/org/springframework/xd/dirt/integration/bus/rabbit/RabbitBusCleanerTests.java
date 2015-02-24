/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.dirt.integration.bus.rabbit;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.xd.dirt.integration.bus.MessageBusSupport;
import org.springframework.xd.dirt.plugins.AbstractStreamPlugin;
import org.springframework.xd.test.rabbit.RabbitAdminTestSupport;
import org.springframework.xd.test.rabbit.RabbitTestSupport;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;

/**
 * @author Gary Russell
 * @since 1.2
 */
public class RabbitBusCleanerTests {

	@Rule
	public RabbitAdminTestSupport adminTest = new RabbitAdminTestSupport();

	@Rule
	public RabbitTestSupport test = new RabbitTestSupport();

	@Test
	public void testClean() {
		final RabbitBusCleaner cleaner = new RabbitBusCleaner();
		final RestTemplate template = RabbitBusCleaner.buildRestTemplate("http://localhost:15672", "guest", "guest");
		final String uuid = UUID.randomUUID().toString();
		for (int i = 0; i < 5; i++) {
			String queueName = MessageBusSupport.constructPipeName("xdbus.",
					AbstractStreamPlugin.constructPipeName(uuid, i));
			URI uri = UriComponentsBuilder.fromUriString("http://localhost:15672/api/queues")
					.pathSegment("{vhost}", "{queue}")
					.buildAndExpand("/", queueName)
					.encode().toUri();
			template.put(uri, new Queue(false, true));
			uri = UriComponentsBuilder.fromUriString("http://localhost:15672/api/queues")
					.pathSegment("{vhost}", "{queue}")
					.buildAndExpand("/", MessageBusSupport.constructDLQName(queueName)).encode().toUri();
			template.put(uri, new Queue(false, true));
		}
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory("localhost");
		new RabbitTemplate(connectionFactory).execute(new ChannelCallback<Void>() {

			@Override
			public Void doInRabbit(Channel channel) throws Exception {
				String queueName = MessageBusSupport.constructPipeName("xdbus.",
						AbstractStreamPlugin.constructPipeName(uuid, 4));
				String consumerTag = channel.basicConsume(queueName, new DefaultConsumer(channel));
				try {
					waitForConsumerStateNot(queueName, 0);
					cleaner.clean(uuid);
					fail("Expected exception");
				}
				catch (RabbitAdminException e) {
					assertEquals("Queue " + queueName + " is in use", e.getMessage());
				}
				channel.basicCancel(consumerTag);
				waitForConsumerStateNot(queueName, 1);
				return null;
			}

			private void waitForConsumerStateNot(String queueName, int state) throws InterruptedException {
				int n = 0;
				URI uri = UriComponentsBuilder.fromUriString("http://localhost:15672/api/queues").pathSegment(
						"{vhost}", "{queue}")
						.buildAndExpand("/", queueName).encode().toUri();
				while (n++ < 100) {
					@SuppressWarnings("unchecked")
					Map<String, Object> queueInfo = template.getForObject(uri, Map.class);
					if (!queueInfo.get("consumers").equals(Integer.valueOf(state))) {
						break;
					}
					Thread.sleep(100);
				}
				assertTrue("Consumer state remained at " + state + " after 10 seconds", n < 100);
			}

		});
		connectionFactory.destroy();
		List<String> cleaned = cleaner.clean(uuid);
		assertEquals(10, cleaned.size());
		for (int i = 0; i < 5; i++) {
			assertEquals("xdbus." + uuid + "." + i, cleaned.get(i * 2));
			assertEquals("xdbus." + uuid + "." + i + ".dlq", cleaned.get(i * 2 + 1));
		}
	}

	public static class Queue {

		private boolean autoDelete;

		private boolean durable;

		public Queue(boolean autoDelete, boolean durable) {
			this.autoDelete = autoDelete;
			this.durable = durable;
		}


		@JsonProperty("auto_delete")
		protected boolean isAutoDelete() {
			return autoDelete;
		}


		protected void setAutoDelete(boolean autoDelete) {
			this.autoDelete = autoDelete;
		}


		protected boolean isDurable() {
			return durable;
		}


		protected void setDurable(boolean durable) {
			this.durable = durable;
		}

	}

}
