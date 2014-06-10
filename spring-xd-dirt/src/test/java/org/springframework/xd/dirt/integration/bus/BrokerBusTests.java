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

package org.springframework.xd.dirt.integration.bus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;


/**
 * Tests for buses that use an external broker.
 * 
 * @author Gary Russell
 */
public abstract class BrokerBusTests extends
		AbstractMessageBusTests {

	@Test
	public void testShortCircuit() throws Exception {
		MessageBus bus = getMessageBus();
		Properties properties = new Properties();
		properties.setProperty(BusProperties.SHORT_CIRCUIT_ALLOWED, "true");

		DirectChannel moduleInputChannel = new DirectChannel();
		moduleInputChannel.setBeanName("short.input");
		DirectChannel moduleOutputChannel = new DirectChannel();
		moduleOutputChannel.setBeanName("short.output");
		bus.bindConsumer("short.0", moduleInputChannel, null);
		bus.bindProducer("short.0", moduleOutputChannel, properties);

		final AtomicReference<Thread> caller = new AtomicReference<Thread>();
		final AtomicInteger count = new AtomicInteger();
		moduleInputChannel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				caller.set(Thread.currentThread());
				count.incrementAndGet();
			}
		});

		moduleOutputChannel.send(new GenericMessage<String>("foo"));
		moduleOutputChannel.send(new GenericMessage<String>("foo"));

		assertNotNull(caller.get());
		assertSame(Thread.currentThread(), caller.get());
		assertEquals(2, count.get());
		assertNull(receive("short.0", true));

		// Remove short circuit and bind producer to bus
		bus.unbindConsumers("short.0");
		busUnbindLatency();

		count.set(0);
		moduleOutputChannel.send(new GenericMessage<String>("bar"));
		moduleOutputChannel.send(new GenericMessage<String>("baz"));
		Object bar = receive("short.0", false);
		assertEquals("bar", bar);
		Object baz = receive("short.0", false);
		assertEquals("baz", baz);
		assertEquals(0, count.get());

		// Unbind producer from bus and short-circuit again
		caller.set(null);
		bus.bindConsumer("short.0", moduleInputChannel, null);
		moduleOutputChannel.send(new GenericMessage<String>("foo"));
		moduleOutputChannel.send(new GenericMessage<String>("foo"));
		assertNotNull(caller.get());
		assertSame(Thread.currentThread(), caller.get());
		assertEquals(2, count.get());
		assertNull(receive("short.0", true));

		bus.unbindProducers("short.0");
		bus.unbindConsumers("short.0");
	}

	protected void busUnbindLatency() throws InterruptedException {
		// default none
	}

	protected abstract Object receive(String queue, boolean expectNull) throws Exception;

}
