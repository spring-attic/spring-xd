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

package org.springframework.integration.x.channel.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.http.MediaType;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Gary Russell
 */
public abstract class AbstractChannelRegistryTests {

	private static final Collection<MediaType> ALL = Collections.singletonList(MediaType.ALL);

	@Test
	public void testClean() throws Exception {
		ChannelRegistry registry = getRegistry();
		registry.createOutbound("foo.0", new DirectChannel(), false);
		registry.createInbound("foo.0", new DirectChannel(), ALL, false);
		registry.createOutbound("foo.1", new DirectChannel(), false);
		registry.createInbound("foo.1", new DirectChannel(), ALL, false);
		registry.createOutbound("foo.2", new DirectChannel(), false);
		Collection<?> bridges = getBridges(registry);
		assertEquals(5, bridges.size());
		registry.deleteOutbound("foo.0");
		assertEquals(4, bridges.size());
		registry.deleteInbound("foo.0");
		registry.deleteOutbound("foo.1");
		assertEquals(2, bridges.size());
		registry.deleteInbound("foo.1");
		registry.deleteOutbound("foo.2");
		assertTrue(bridges.isEmpty());
	}

	@Test
	public void testSendAndReceive() throws Exception {
		ChannelRegistry registry = getRegistry();
		DirectChannel moduleOutputChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		registry.createOutbound("foo.0", moduleOutputChannel, false);
		registry.createInbound("foo.0", moduleInputChannel, ALL, false);
		Message<?> message = MessageBuilder.withPayload("foo").setHeader(MessageHeaders.CONTENT_TYPE, "foo/bar").build();
		moduleOutputChannel.send(message);
		Message<?> inbound = moduleInputChannel.receive(5000);
		assertNotNull(inbound);
		assertEquals("foo", inbound.getPayload());
		assertNull(inbound.getHeaders().get(ChannelRegistrySupport.ORIGINAL_CONTENT_TYPE_HEADER));
		assertEquals("foo/bar", inbound.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void testSendAndReceiveNoOriginalContentType() throws Exception {
		ChannelRegistry registry = getRegistry();
		DirectChannel moduleOutputChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		registry.createOutbound("bar.0", moduleOutputChannel, false);
		registry.createInbound("bar.0", moduleInputChannel, ALL, false);

		Message<?> message = MessageBuilder.withPayload("foo").build();
		moduleOutputChannel.send(message);
		Message<?> inbound = moduleInputChannel.receive(5000);
		assertNotNull(inbound);
		assertEquals("foo", inbound.getPayload());
		assertNull(inbound.getHeaders().get(ChannelRegistrySupport.ORIGINAL_CONTENT_TYPE_HEADER));
		assertNull(inbound.getHeaders().get(MessageHeaders.CONTENT_TYPE));
	}

	@Test
	public void testSendAndReceivePubSub() throws Exception {
		ChannelRegistry registry = getRegistry();
		DirectChannel moduleOutputChannel = new DirectChannel();
		// Test pub/sub by emulating how StreamPlugin handles taps
		DirectChannel tapChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		QueueChannel module2InputChannel = new QueueChannel();
		QueueChannel module3InputChannel = new QueueChannel();
		registry.createOutbound("baz.0", moduleOutputChannel, false);
		registry.createInbound("baz.0", moduleInputChannel, ALL, false);
		moduleOutputChannel.addInterceptor(new WireTap(tapChannel));
		registry.createOutboundPubSub("tap:baz.http", tapChannel);
		// A new module is using the tap as an input channel
		registry.createInboundPubSub("tap:baz.http", module2InputChannel, ALL);
		// Another new module is using tap as an input channel
		registry.createInboundPubSub("tap:baz.http", module3InputChannel, ALL);
		Message<?> message = MessageBuilder.withPayload("foo").setHeader(MessageHeaders.CONTENT_TYPE, "foo/bar").build();
		boolean success = false;
		boolean retried = false;
		while (!success) {
			moduleOutputChannel.send(message);
			Message<?> inbound = moduleInputChannel.receive(5000);
			assertNotNull(inbound);
			assertEquals("foo", inbound.getPayload());
			assertNull(inbound.getHeaders().get(ChannelRegistrySupport.ORIGINAL_CONTENT_TYPE_HEADER));
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
			assertNull(tapped1.getHeaders().get(ChannelRegistrySupport.ORIGINAL_CONTENT_TYPE_HEADER));
			assertEquals("foo/bar", tapped1.getHeaders().get(MessageHeaders.CONTENT_TYPE));
			assertEquals("foo", tapped2.getPayload());
			assertNull(tapped2.getHeaders().get(ChannelRegistrySupport.ORIGINAL_CONTENT_TYPE_HEADER));
			assertEquals("foo/bar", tapped2.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		}
		// delete one tap stream is deleted
		registry.deleteInbound("tap:baz.http", module3InputChannel);
		Message<?> message2 = MessageBuilder.withPayload("bar").setHeader(MessageHeaders.CONTENT_TYPE, "foo/bar").build();
		moduleOutputChannel.send(message2);

		// other tap still receives messages
		Message<?> tapped = module2InputChannel.receive(5000);
		assertNotNull(tapped);

		// Removed tap does not
		assertNull(module3InputChannel.receive(1000));

		// when other tap stream is deleted
		registry.deleteInbound("tap:baz.http", module2InputChannel);
		// Clean up as StreamPlugin would
		registry.deleteInbound("baz.0", moduleInputChannel);
		registry.deleteOutbound("baz.0", moduleOutputChannel);
		registry.deleteOutbound("tap:baz.http");
		assertTrue(getBridges(registry).isEmpty());
	}

	@Test
	public void createInboundPubSubBeforeOutboundPubSub() throws Exception {
		ChannelRegistry registry = getRegistry();
		DirectChannel moduleOutputChannel = new DirectChannel();
		// Test pub/sub by emulating how StreamPlugin handles taps
		DirectChannel tapChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		QueueChannel module2InputChannel = new QueueChannel();
		QueueChannel module3InputChannel = new QueueChannel();
		// Create the tap first
		registry.createInboundPubSub("tap:baz.http", module2InputChannel, ALL);

		// Then create the stream
		registry.createOutbound("baz.0", moduleOutputChannel, false);
		registry.createInbound("baz.0", moduleInputChannel, ALL, false);
		moduleOutputChannel.addInterceptor(new WireTap(tapChannel));
		registry.createOutboundPubSub("tap:baz.http", tapChannel);

		// Another new module is using tap as an input channel
		registry.createInboundPubSub("tap:baz.http", module3InputChannel, ALL);
		Message<?> message = MessageBuilder.withPayload("foo").setHeader(MessageHeaders.CONTENT_TYPE, "foo/bar").build();
		boolean success = false;
		boolean retried = false;
		while (!success) {
			moduleOutputChannel.send(message);
			Message<?> inbound = moduleInputChannel.receive(5000);
			assertNotNull(inbound);
			assertEquals("foo", inbound.getPayload());
			assertNull(inbound.getHeaders().get(ChannelRegistrySupport.ORIGINAL_CONTENT_TYPE_HEADER));
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
			assertNull(tapped1.getHeaders().get(ChannelRegistrySupport.ORIGINAL_CONTENT_TYPE_HEADER));
			assertEquals("foo/bar", tapped1.getHeaders().get(MessageHeaders.CONTENT_TYPE));
			assertEquals("foo", tapped2.getPayload());
			assertNull(tapped2.getHeaders().get(ChannelRegistrySupport.ORIGINAL_CONTENT_TYPE_HEADER));
			assertEquals("foo/bar", tapped2.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		}
		// delete one tap stream is deleted
		registry.deleteInbound("tap:baz.http", module3InputChannel);
		Message<?> message2 = MessageBuilder.withPayload("bar").setHeader(MessageHeaders.CONTENT_TYPE, "foo/bar").build();
		moduleOutputChannel.send(message2);

		// other tap still receives messages
		Message<?> tapped = module2InputChannel.receive(5000);
		assertNotNull(tapped);

		// Removed tap does not
		assertNull(module3InputChannel.receive(1000));

		// when other tap stream is deleted
		registry.deleteInbound("tap:baz.http", module2InputChannel);
		// Clean up as StreamPlugin would
		registry.deleteInbound("baz.0", moduleInputChannel);
		registry.deleteOutbound("baz.0", moduleOutputChannel);
		registry.deleteOutbound("tap:baz.http");
		assertTrue(getBridges(registry).isEmpty());
	}

	protected Collection<?> getBridges(ChannelRegistry registry) {
		DirectFieldAccessor accessor = new DirectFieldAccessor(registry);
		List<?> bridges = (List<?>) accessor.getPropertyValue("bridges");
		return bridges;
	}

	protected abstract ChannelRegistry getRegistry() throws Exception;

}
