/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.x.bus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.http.MediaType;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.x.bus.serializer.AbstractCodec;
import org.springframework.integration.x.bus.serializer.CompositeCodec;
import org.springframework.integration.x.bus.serializer.MultiTypeCodec;
import org.springframework.integration.x.bus.serializer.kryo.PojoCodec;
import org.springframework.integration.x.bus.serializer.kryo.TupleCodec;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.xd.tuple.Tuple;

/**
 * @author Gary Russell
 * @author Ilayaperumal Gopinathan
 */
public abstract class AbstractMessageBusTests {

	protected static final Collection<MediaType> ALL = Collections.singletonList(MediaType.ALL);

	protected AbstractTestMessageBus testMessageBus;

	@Test
	public void testClean() throws Exception {
		MessageBus messageBus = getMessageBus();
		messageBus.bindProducer("foo.0", new DirectChannel(), false);
		messageBus.bindConsumer("foo.0", new DirectChannel(), ALL, false);
		messageBus.bindProducer("foo.1", new DirectChannel(), false);
		messageBus.bindConsumer("foo.1", new DirectChannel(), ALL, false);
		messageBus.bindProducer("foo.2", new DirectChannel(), false);
		Collection<?> bindings = getBindings(messageBus);
		assertEquals(5, bindings.size());
		messageBus.unbindProducers("foo.0");
		assertEquals(4, bindings.size());
		messageBus.unbindConsumers("foo.0");
		messageBus.unbindProducers("foo.1");
		assertEquals(2, bindings.size());
		messageBus.unbindConsumers("foo.1");
		messageBus.unbindProducers("foo.2");
		assertTrue(bindings.isEmpty());
	}

	@Test
	public void testSendAndReceive() throws Exception {
		MessageBus messageBus = getMessageBus();
		DirectChannel moduleOutputChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		messageBus.bindProducer("foo.0", moduleOutputChannel, false);
		messageBus.bindConsumer("foo.0", moduleInputChannel, ALL, false);
		Message<?> message = MessageBuilder.withPayload("foo").setHeader(MessageHeaders.CONTENT_TYPE, "foo/bar").build();
		moduleOutputChannel.send(message);
		Message<?> inbound = moduleInputChannel.receive(5000);
		assertNotNull(inbound);
		assertEquals("foo", inbound.getPayload());
		assertNull(inbound.getHeaders().get(MessageBusSupport.ORIGINAL_CONTENT_TYPE_HEADER));
		assertEquals("foo/bar", inbound.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		messageBus.unbindProducers("foo.0");
		messageBus.unbindConsumers("foo.0");
	}

	@Test
	public void testSendAndReceiveNoOriginalContentType() throws Exception {
		MessageBus messageBus = getMessageBus();
		DirectChannel moduleOutputChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		messageBus.bindProducer("bar.0", moduleOutputChannel, false);
		messageBus.bindConsumer("bar.0", moduleInputChannel, ALL, false);

		Message<?> message = MessageBuilder.withPayload("foo").build();
		moduleOutputChannel.send(message);
		Message<?> inbound = moduleInputChannel.receive(5000);
		assertNotNull(inbound);
		assertEquals("foo", inbound.getPayload());
		assertNull(inbound.getHeaders().get(MessageBusSupport.ORIGINAL_CONTENT_TYPE_HEADER));
		assertNull(inbound.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		messageBus.unbindProducers("bar.0");
		messageBus.unbindConsumers("bar.0");
	}

	@Test
	public void testSendAndReceivePubSub() throws Exception {
		MessageBus messageBus = getMessageBus();
		DirectChannel moduleOutputChannel = new DirectChannel();
		// Test pub/sub by emulating how StreamPlugin handles taps
		DirectChannel tapChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		QueueChannel module2InputChannel = new QueueChannel();
		QueueChannel module3InputChannel = new QueueChannel();
		messageBus.bindProducer("baz.0", moduleOutputChannel, false);
		messageBus.bindConsumer("baz.0", moduleInputChannel, ALL, false);
		moduleOutputChannel.addInterceptor(new WireTap(tapChannel));
		messageBus.bindPubSubProducer("tap:baz.http", tapChannel);
		// A new module is using the tap as an input channel
		messageBus.bindPubSubConsumer("tap:baz.http", module2InputChannel, ALL);
		// Another new module is using tap as an input channel
		messageBus.bindPubSubConsumer("tap:baz.http", module3InputChannel, ALL);
		Message<?> message = MessageBuilder.withPayload("foo").setHeader(MessageHeaders.CONTENT_TYPE, "foo/bar").build();
		boolean success = false;
		boolean retried = false;
		while (!success) {
			moduleOutputChannel.send(message);
			Message<?> inbound = moduleInputChannel.receive(5000);
			assertNotNull(inbound);
			assertEquals("foo", inbound.getPayload());
			assertNull(inbound.getHeaders().get(MessageBusSupport.ORIGINAL_CONTENT_TYPE_HEADER));
			assertEquals("foo/bar", inbound.getHeaders().get(MessageHeaders.CONTENT_TYPE));
			Message<?> tapped1 = module2InputChannel.receive(5000);
			Message<?> tapped2 = module3InputChannel.receive(5000);
			if (tapped1 == null || tapped2 == null) {
				// listener may not have started
				assertFalse("Failed to receive tap after retry", retried);
				retried = true;
				continue;
			}
			success = true;
			assertEquals("foo", tapped1.getPayload());
			assertNull(tapped1.getHeaders().get(MessageBusSupport.ORIGINAL_CONTENT_TYPE_HEADER));
			assertEquals("foo/bar", tapped1.getHeaders().get(MessageHeaders.CONTENT_TYPE));
			assertEquals("foo", tapped2.getPayload());
			assertNull(tapped2.getHeaders().get(MessageBusSupport.ORIGINAL_CONTENT_TYPE_HEADER));
			assertEquals("foo/bar", tapped2.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		}
		// delete one tap stream is deleted
		messageBus.unbindConsumer("tap:baz.http", module3InputChannel);
		Message<?> message2 = MessageBuilder.withPayload("bar").setHeader(MessageHeaders.CONTENT_TYPE, "foo/bar").build();
		moduleOutputChannel.send(message2);

		// other tap still receives messages
		Message<?> tapped = module2InputChannel.receive(5000);
		assertNotNull(tapped);

		// Removed tap does not
		assertNull(module3InputChannel.receive(1000));

		// when other tap stream is deleted
		messageBus.unbindConsumer("tap:baz.http", module2InputChannel);
		// Clean up as StreamPlugin would
		messageBus.unbindConsumer("baz.0", moduleInputChannel);
		messageBus.unbindProducer("baz.0", moduleOutputChannel);
		messageBus.unbindProducers("tap:baz.http");
		assertTrue(getBindings(messageBus).isEmpty());
	}

	@Test
	public void createInboundPubSubBeforeOutboundPubSub() throws Exception {
		MessageBus messageBus = getMessageBus();
		DirectChannel moduleOutputChannel = new DirectChannel();
		// Test pub/sub by emulating how StreamPlugin handles taps
		DirectChannel tapChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		QueueChannel module2InputChannel = new QueueChannel();
		QueueChannel module3InputChannel = new QueueChannel();
		// Create the tap first
		messageBus.bindPubSubConsumer("tap:baz.http", module2InputChannel, ALL);

		// Then create the stream
		messageBus.bindProducer("baz.0", moduleOutputChannel, false);
		messageBus.bindConsumer("baz.0", moduleInputChannel, ALL, false);
		moduleOutputChannel.addInterceptor(new WireTap(tapChannel));
		messageBus.bindPubSubProducer("tap:baz.http", tapChannel);

		// Another new module is using tap as an input channel
		messageBus.bindPubSubConsumer("tap:baz.http", module3InputChannel, ALL);
		Message<?> message = MessageBuilder.withPayload("foo").setHeader(MessageHeaders.CONTENT_TYPE, "foo/bar").build();
		boolean success = false;
		boolean retried = false;
		while (!success) {
			moduleOutputChannel.send(message);
			Message<?> inbound = moduleInputChannel.receive(5000);
			assertNotNull(inbound);
			assertEquals("foo", inbound.getPayload());
			assertNull(inbound.getHeaders().get(MessageBusSupport.ORIGINAL_CONTENT_TYPE_HEADER));
			assertEquals("foo/bar", inbound.getHeaders().get(MessageHeaders.CONTENT_TYPE));
			Message<?> tapped1 = module2InputChannel.receive(5000);
			Message<?> tapped2 = module3InputChannel.receive(5000);
			if (tapped1 == null || tapped2 == null) {
				// listener may not have started
				assertFalse("Failed to receive tap after retry", retried);
				retried = true;
				continue;
			}
			success = true;
			assertEquals("foo", tapped1.getPayload());
			assertNull(tapped1.getHeaders().get(MessageBusSupport.ORIGINAL_CONTENT_TYPE_HEADER));
			assertEquals("foo/bar", tapped1.getHeaders().get(MessageHeaders.CONTENT_TYPE));
			assertEquals("foo", tapped2.getPayload());
			assertNull(tapped2.getHeaders().get(MessageBusSupport.ORIGINAL_CONTENT_TYPE_HEADER));
			assertEquals("foo/bar", tapped2.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		}
		// delete one tap stream is deleted
		messageBus.unbindConsumer("tap:baz.http", module3InputChannel);
		Message<?> message2 = MessageBuilder.withPayload("bar").setHeader(MessageHeaders.CONTENT_TYPE, "foo/bar").build();
		moduleOutputChannel.send(message2);

		// other tap still receives messages
		Message<?> tapped = module2InputChannel.receive(5000);
		assertNotNull(tapped);

		// Removed tap does not
		assertNull(module3InputChannel.receive(1000));

		// when other tap stream is deleted
		messageBus.unbindConsumer("tap:baz.http", module2InputChannel);
		// Clean up as StreamPlugin would
		messageBus.unbindConsumer("baz.0", moduleInputChannel);
		messageBus.unbindProducer("baz.0", moduleOutputChannel);
		messageBus.unbindProducers("tap:baz.http");
		assertTrue(getBindings(messageBus).isEmpty());
	}

	protected Collection<?> getBindings(MessageBus testMessageBus) {
		if (testMessageBus instanceof AbstractTestMessageBus) {
			return getBindingsFromMsgBus(((AbstractTestMessageBus) testMessageBus).getCoreMessageBus());
		}
		else if (testMessageBus instanceof LocalMessageBus) {
			return getBindingsFromMsgBus(testMessageBus);
		}
		return Collections.EMPTY_LIST;
	}

	private Collection<?> getBindingsFromMsgBus(MessageBus messageBus) {
		DirectFieldAccessor accessor = new DirectFieldAccessor(messageBus);
		return (List<?>) accessor.getPropertyValue("bindings");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected MultiTypeCodec<Object> getCodec() {
		Map<Class<?>, AbstractCodec<?>> codecs = new HashMap<Class<?>, AbstractCodec<?>>();
		codecs.put(Tuple.class, new TupleCodec());
		return new CompositeCodec(codecs, new PojoCodec());
	}

	protected abstract MessageBus getMessageBus() throws Exception;

	@After
	public void cleanup() {
		if (testMessageBus != null) {
			testMessageBus.cleanup();
		}
	}

}
