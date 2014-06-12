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

import org.springframework.util.Assert;


/**
 * A test fixture that allows testing of the rabbit sink module.
 *
 * @author Glenn Renfro
 */
public class RabbitSink extends AbstractModuleFixture<RabbitSink> {

	public static final String DEFAULT_EXCHANGE = "RabbitSource";

	public static final String DEFAULT_HOST = "localhost";

	public static final String DEFAULT_ROUTING_KEY = "rabbitfixture.rabbit_sink";

	public static final int DEFAULT_PORT = 5672;

	private String exchange;

	private String host;

	private String routingKey;

	private int port;


	/**
	 * Initialize and instance of the RabbitSink
	 *
	 * @param exchange The exchange the sink associate
	 * @param host The host address where the broker is deployed
	 * @param routingKey The key that will route the messages to the queue
	 */
	public RabbitSink(String exchange, String host, int port, String routingKey) {
		Assert.hasText(exchange, "exchange must not be empty nor null");
		Assert.hasText(routingKey, "routingKey must not be empty nor null");
		Assert.hasText(host, "host must not be empty nor null");

		this.host = host;
		this.routingKey = routingKey;
		this.exchange = exchange;
		this.port = port;
	}

	/**
	 * Renders the DSL for this fixture.
	 */
	@Override
	protected String toDSL() {
		return String.format("rabbit --host=%s  --exchange=%s --routingKey='\"%s\"' --port=%s ",
				host,
				exchange, routingKey,
				port);
	}

	/**
	 * Creates an instance of the Rabbit Sink using defaults
	 *
	 * @return An instance of the Rabbit Key Fixture
	 */
	public static RabbitSink withDefaults() {
		return new RabbitSink(DEFAULT_EXCHANGE, DEFAULT_HOST, DEFAULT_PORT, DEFAULT_ROUTING_KEY);
	}

	/**
	 * Ensure that the Rabbit broker socket is available by polling it for up to 2 seconds
	 * 
	 * @return RabbitSink to use in fluent API chaining
	 * @throws IllegalStateException if can not connect in 2 seconds.
	 */
	public RabbitSink ensureReady() {
		AvailableSocketPorts.ensureReady(this, host, port, 2000);
		return this;
	}


	/**
	 * Sets the host for the fixture
	 *
	 * @param host The host where the rabbit broker is deployed
	 * @return current instance of fixture
	 */
	public RabbitSink host(String host) {
		this.host = host;
		return this;
	}

	/**
	 * Sets the port for which data will be sent to the rabbit broker
	 *
	 * @param port The port that the rabbit broker is monitoring
	 * @return current instance of fixture
	 */
	public RabbitSink port(int port) {
		this.port = port;
		return this;
	}

	/**
	 * Sets the routing key that will be used to route messages to the appropriate key.
	 *
	 * @param routingKey the routing key to be used by the fixture.
	 * @return current instance of fixture
	 */
	public RabbitSink routingKey(String routingKey) {
		this.routingKey = routingKey;
		return this;
	}


	/**
	 * Sets the exchange for the fixture
	 *
	 * @param exchange The exchange associated with the Rabbit sink
	 * @return current instance of fixture
	 */
	public RabbitSink exchange(String exchange) {
		this.exchange = exchange;
		return this;
	}

}
