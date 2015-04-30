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

import java.net.URI;
import java.net.URISyntaxException;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.test.fixtures.util.AvailableSocketPorts;


/**
 * A test fixture that allows testing of the rabbit sink module.
 *
 * @author Glenn Renfro
 */
public class RabbitSink extends AbstractModuleFixture<RabbitSink> {

	public static final String DEFAULT_EXCHANGE = "RabbitSource";

	public static final String DEFAULT_ROUTING_KEY = "rabbitfixture.rabbit_sink";

	public static final String DEFAULT_ADDRESSES = "localhost:5672";

	private String exchange;

	private String routingKey;

	private String addresses;

	/**
	 * Initialize and instance of the RabbitSink
	 *
	 * @param exchange The exchange the sink associate
	 * @param addresses Comma delimited list of addresses where the brokers are deployed
	 * @param routingKey The key that will route the messages to the queue
	 */
	public RabbitSink(String exchange, String addresses, String routingKey) {
		Assert.hasText(exchange, "exchange must not be empty nor null");
		Assert.hasText(routingKey, "routingKey must not be empty nor null");
		Assert.hasText(addresses, "addresses must not be empty nor null");

		this.routingKey = routingKey;
		this.exchange = exchange;
		this.addresses = addresses;
	}

	/**
	 * Renders the DSL for this fixture.
	 */
	@Override
	protected String toDSL() {
		return String.format("rabbit --addresses=%s  --exchange=%s --routingKey='\"%s\"' ",
				addresses, exchange, routingKey);
	}

	/**
	 * Creates an instance of the Rabbit Sink using defaults
	 *
	 * @return An instance of the Rabbit Key Fixture
	 */
	public static RabbitSink withDefaults() {
		return new RabbitSink(DEFAULT_EXCHANGE, DEFAULT_ADDRESSES, DEFAULT_ROUTING_KEY);
	}

	/**
	 * Ensure that the each Rabbit broker socket is available by polling it for up to 2 seconds
	 * 
	 * @return RabbitSink to use in fluent API chaining
	 * @throws IllegalStateException if can not connect in 2 seconds.
	 */
	public RabbitSink ensureReady() {
		String[] addressArray = StringUtils.commaDelimitedListToStringArray(addresses);
		try {
			for (String address : addressArray) {
				URI uri = new URI(address);
				AvailableSocketPorts.ensureReady(this.getClass().getName(), uri.getHost(), uri.getPort(), 2000);
			}
		}
		catch (URISyntaxException uriSyntaxException) {
			throw new IllegalStateException(uriSyntaxException.getMessage(), uriSyntaxException);
		}
		return this;
	}


	/**
	 * Sets the addresses for the fixture
	 *
	 * @param addresses The addresses where the rabbit brokers are deployed
	 * @return current instance of fixture
	 */
	public RabbitSink addresses(String addresses) {
		this.addresses = addresses;
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
