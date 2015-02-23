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

package org.springframework.xd.shell.bus;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.xd.dirt.integration.bus.RabbitAdminException;
import org.springframework.xd.dirt.integration.bus.RabbitBusCleaner;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;

/**
 *
 * @author Gary Russell
 */
public class RabbitBusCleanerTests {

	@Test
	@Ignore
	// requires the rabbit admin plugin
	public void testClean() {
		final RabbitBusCleaner cleaner = new RabbitBusCleaner();
		RestTemplate template = RabbitBusCleaner.buildRestTemplate("http://localhost:15672", "guest", "guest");
		final String uuid = UUID.randomUUID().toString();
		for (int i = 0; i < 5; i++) {
			URI uri = UriComponentsBuilder.fromUriString("http://localhost:15672/api/queues").pathSegment("{vhost}",
					"{queue}")
					.buildAndExpand("/", "xdbus." + uuid + "." + i).encode().toUri();
			template.put(uri, new Queue(false, true));
			uri = UriComponentsBuilder.fromUriString("http://localhost:15672/api/queues").pathSegment("{vhost}",
					"{queue}")
					.buildAndExpand("/", "xdbus." + uuid + "." + i + ".dlq").encode().toUri();
			template.put(uri, new Queue(false, true));
		}
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory("localhost");
		new RabbitTemplate(connectionFactory).execute(new ChannelCallback<Void>() {

			@Override
			public Void doInRabbit(Channel channel) throws Exception {
				String consumerTag = channel.basicConsume("xdbus." + uuid + ".4", new DefaultConsumer(channel));
				try {
					Thread.sleep(5000); // wait for consumer to show up in rest API
					cleaner.clean(uuid);
					fail("Expected exception");
				}
				catch (RabbitAdminException e) {
					assertEquals("Queue xdbus." + uuid + ".4 is in use", e.getMessage());
				}
				channel.basicCancel(consumerTag);
				Thread.sleep(5000); // wait for consumer to go away in rest API
				return null;
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

	public class Queue {

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
