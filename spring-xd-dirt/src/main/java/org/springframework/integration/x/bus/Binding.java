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

package org.springframework.integration.x.bus;

import org.springframework.context.Lifecycle;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.util.Assert;

/**
 * Represents a binding between a module's channel and an adapter endpoint that connects to the MessageBus. The binding
 * could be for a consumer or a producer. A consumer binding represents a connection from an adapter on the bus to a
 * module's input channel. A producer binding represents a connection from a module's output channel to an adapter on
 * the bus.
 * 
 * @author Jennifer Hickey
 * @author Mark Fisher
 */
public class Binding implements Lifecycle {

	private final MessageChannel channel;

	private final AbstractEndpoint endpoint;

	private final String type;

	private Binding(MessageChannel channel, AbstractEndpoint endpoint, String type) {
		Assert.notNull(channel, "channel must not be null");
		Assert.notNull(endpoint, "endpoint must not be null");
		this.channel = channel;
		this.endpoint = endpoint;
		this.type = type;
	}

	public static Binding forConsumer(AbstractEndpoint adapterFromBus, MessageChannel moduleInputChannel) {
		return new Binding(moduleInputChannel, adapterFromBus, "consumer");
	}

	public static Binding forProducer(MessageChannel moduleOutputChannel, AbstractEndpoint adapterToBus) {
		return new Binding(moduleOutputChannel, adapterToBus, "producer");
	}

	public MessageChannel getChannel() {
		return channel;
	}

	public AbstractEndpoint getEndpoint() {
		return endpoint;
	}

	@Override
	public void start() {
		endpoint.start();
	}

	@Override
	public void stop() {
		endpoint.stop();
	}

	@Override
	public boolean isRunning() {
		return endpoint.isRunning();
	}

	@Override
	public String toString() {
		return type + "Binding[channel=" + channel + ", endpoint=" + endpoint.getComponentName() + "]";
	}

}
