/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.test.fixtures;

import java.io.IOException;
import java.net.Socket;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.util.Assert;


/**
 * A test fixture that allows testing of the rabbit source module.
 *
 * @author Glenn Renfro
 */
public class RabbitSource extends AbstractModuleFixture {

	public static final String DEFAULT_QUEUE = "rabbit_source";

	public static final String DEFAULT_EXCHANGE = "RabbitSource";

	private String queue;

	private CachingConnectionFactory connectionFactory;

	/**
	 * Initialize and instance of the RabbitSource
	 *
	 * @param connectionFactory The MQ Connection Factory to be used by the Rabbit Source.
	 * @param queue The queue to be monitored by the source.
	 */
	public RabbitSource(CachingConnectionFactory connectionFactory, String queue) {
		Assert.hasText(queue, "queue must not be empty nor null");
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		this.queue = queue;
		this.connectionFactory = connectionFactory;
	}

	/**
	 * Renders the DSL for this fixture.
	 */
	@Override
	protected String toDSL() {
		return String.format("rabbit --host=%s  --queues=%s ", connectionFactory.getHost(), queue);
	}

	/**
	 * Creates an instance of the Rabbit Source using defaults
	 *
	 * @param connectionFactory the MQ Connection Factory for this source.
	 * @return A fully qualified instance of Rabbit Source
	 */
	public static RabbitSource withDefaults(CachingConnectionFactory connectionFactory) {
		Assert.notNull(connectionFactory, "the connection factory must not be null");
		return new RabbitSource(connectionFactory, DEFAULT_QUEUE);
	}

	/**
	 * Verifies that the port to the broker is available. If not throws an IllegalStateException. The timeout is set for
	 * 2 seconds.
	 *
	 * @return a reference to the Rabbit source.
	 */
	public RabbitSource ensureReady() {
		return ensureReady(2000);
	}

	/**
	 * Verifies that the port to the broker is available. If not throws an IllegalStateException.
	 *
	 * @param timeout The max time to try to get the connection to the broker.
	 * @return a reference to the rabbit source.
	 */
	public RabbitSource ensureReady(int timeout) {
		long giveUpAt = System.currentTimeMillis() + timeout;
		while (System.currentTimeMillis() < giveUpAt) {
			try {
				new Socket(connectionFactory.getHost(), connectionFactory.getPort());
				return this;
			}
			catch (IOException e) {
				try {
					Thread.sleep(100);
				}
				catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
				}
			}
		}
		throw new IllegalStateException(String.format(
				"Source [%s] does not seem to be listening after waiting for %dms", this, timeout));
	}

	/**
	 * Creates an instance of the queue on the rabbit broker. If already present then no action is taken.
	 */
	public void createQueue() {
		RabbitAdmin admin = new RabbitAdmin(connectionFactory);
		Queue sourceQueue = new Queue(queue);
		admin.declareQueue(sourceQueue);
		TopicExchange exchange = new TopicExchange(DEFAULT_EXCHANGE);
		admin.declareExchange(exchange);
		admin.declareBinding(
				BindingBuilder.bind(sourceQueue).to(exchange).with("foo." + queue));
	}

	/**
	 * Creates an instance of the queue on the rabbit broker. If already present then no action is taken.
	 */
	public void destroyQueue() {
		RabbitAdmin admin = new RabbitAdmin(connectionFactory);
		admin.deleteQueue(queue);
	}

	/**
	 * Sends data to the RabbitMQ Broker.
	 *
	 * @param data String to be transmitted to the Broker.
	 */
	public void sendData(String data) {
		Assert.hasText(data, "data must not be null nor empty");
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.convertAndSend(DEFAULT_EXCHANGE, "foo." + queue, data);
	}

	/**
	 * Sets the queue for this fixture.
	 *
	 * @param queue the name of the queue to be associated with this source.
	 * @return The current instance of RabbitSource
	 */
	public RabbitSource queue(String queue) {
		Assert.hasText(queue, "queue should not be empty nor null");
		this.queue = queue;
		return this;
	}

}
