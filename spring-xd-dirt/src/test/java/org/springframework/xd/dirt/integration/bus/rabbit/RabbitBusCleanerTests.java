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


import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.xd.dirt.integration.bus.MessageBusSupport;
import org.springframework.xd.dirt.plugins.AbstractJobPlugin;
import org.springframework.xd.dirt.plugins.AbstractStreamPlugin;
import org.springframework.xd.dirt.plugins.job.JobEventsListenerPlugin;
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
	public void testCleanStream() {
		final RabbitBusCleaner cleaner = new RabbitBusCleaner();
		final RestTemplate template = RabbitBusCleaner.buildRestTemplate("http://localhost:15672", "guest", "guest");
		final String uuid = UUID.randomUUID().toString();
		String firstQueue = null;
		for (int i = 0; i < 5; i++) {
			String queueName = MessageBusSupport.applyPrefix("xdbus.",
					AbstractStreamPlugin.constructPipeName(uuid, i));
			if (firstQueue == null) {
				firstQueue = queueName;
			}
			URI uri = UriComponentsBuilder.fromUriString("http://localhost:15672/api/queues")
					.pathSegment("{vhost}", "{queue}")
					.buildAndExpand("/", queueName)
					.encode().toUri();
			template.put(uri, new AmqpQueue(false, true));
			uri = UriComponentsBuilder.fromUriString("http://localhost:15672/api/queues")
					.pathSegment("{vhost}", "{queue}")
					.buildAndExpand("/", MessageBusSupport.constructDLQName(queueName)).encode().toUri();
			template.put(uri, new AmqpQueue(false, true));
		}
		CachingConnectionFactory connectionFactory = test.getResource();
		RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
		final FanoutExchange fanout = new FanoutExchange(
				MessageBusSupport.applyPrefix("xdbus.", MessageBusSupport.applyPubSub(
						AbstractStreamPlugin.constructTapPrefix(uuid) + ".foo.bar")));
		rabbitAdmin.declareExchange(fanout);
		rabbitAdmin.declareBinding(BindingBuilder.bind(new Queue(firstQueue)).to(fanout));
		new RabbitTemplate(connectionFactory).execute(new ChannelCallback<Void>() {

			@Override
			public Void doInRabbit(Channel channel) throws Exception {
				String queueName = MessageBusSupport.applyPrefix("xdbus.",
						AbstractStreamPlugin.constructPipeName(uuid, 4));
				String consumerTag = channel.basicConsume(queueName, new DefaultConsumer(channel));
				try {
					waitForConsumerStateNot(queueName, 0);
					cleaner.clean(uuid, false);
					fail("Expected exception");
				}
				catch (RabbitAdminException e) {
					assertEquals("Queue " + queueName + " is in use", e.getMessage());
				}
				channel.basicCancel(consumerTag);
				waitForConsumerStateNot(queueName, 1);
				try {
					cleaner.clean(uuid, false);
					fail("Expected exception");
				}
				catch (RabbitAdminException e) {
					assertThat(e.getMessage(), startsWith("Cannot delete exchange " +
							fanout.getName() + "; it has bindings:"));
				}
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
		rabbitAdmin.deleteExchange(fanout.getName()); // easier than deleting the binding
		rabbitAdmin.declareExchange(fanout);
		connectionFactory.destroy();
		Map<String, List<String>> cleanedMap = cleaner.clean(uuid, false);
		assertEquals(2, cleanedMap.size());
		List<String> cleanedQueues = cleanedMap.get("queues");
		assertEquals(10, cleanedQueues.size());
		for (int i = 0; i < 5; i++) {
			assertEquals("xdbus." + uuid + "." + i, cleanedQueues.get(i * 2));
			assertEquals("xdbus." + uuid + "." + i + ".dlq", cleanedQueues.get(i * 2 + 1));
		}
		List<String> cleanedExchanges = cleanedMap.get("exchanges");
		assertEquals(1, cleanedExchanges.size());
		assertEquals(fanout.getName(), cleanedExchanges.get(0));
	}

	@Test
	public void testCleanJob() {
		final RabbitBusCleaner cleaner = new RabbitBusCleaner();
		final String uuid = UUID.randomUUID().toString();
		Set<String> jobExchanges = new HashSet<>(JobEventsListenerPlugin.getEventListenerChannels(uuid).values());
		jobExchanges.add(JobEventsListenerPlugin.getEventListenerChannelName(uuid));
		CachingConnectionFactory connectionFactory = test.getResource();
		RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
		for (String exchange : jobExchanges) {
			FanoutExchange fanout = new FanoutExchange(
					MessageBusSupport.applyPrefix("xdbus.", MessageBusSupport.applyPubSub(exchange)));
			rabbitAdmin.declareExchange(fanout);
		}
		rabbitAdmin.declareQueue(new Queue(MessageBusSupport.applyPrefix("xdbus.",
				AbstractJobPlugin.getJobChannelName(uuid))));
		Map<String, List<String>> cleanedMap = cleaner.clean(uuid, true);
		assertEquals(2, cleanedMap.size());
		List<String> cleanedQueues = cleanedMap.get("queues");
		assertEquals(1, cleanedQueues.size());
		List<String> cleanedExchanges = cleanedMap.get("exchanges");
		assertEquals(6, cleanedExchanges.size());
	}

	public static class AmqpQueue {

		private boolean autoDelete;

		private boolean durable;

		public AmqpQueue(boolean autoDelete, boolean durable) {
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
