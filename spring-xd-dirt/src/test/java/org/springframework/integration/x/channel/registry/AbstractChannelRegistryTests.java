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

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.xd.dirt.stream.Tap;

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
		registry.tap("bar", "foo.0", new DirectChannel());
		Collection<?> bridges = getBridges(registry);
		assertEquals(6, bridges.size());
		registry.deleteOutbound("foo.0");
		assertEquals(5, bridges.size());
		registry.deleteInbound("foo.0");
		registry.deleteOutbound("foo.1");
		assertEquals(3, bridges.size());
		registry.deleteInbound("foo.1");
		registry.deleteOutbound("foo.2");
		assertEquals(1, bridges.size()); // tap remains
	}

	@Test
	public void testCleanTap() throws Exception {
		ChannelRegistry registry = getRegistry();
		registry.createOutbound("foo.0", new DirectChannel(), false);
		registry.createInbound("foo.0", new DirectChannel(), ALL, false);

		MessageChannel output = new DirectChannel();

		Tap tap = new Tap("foo.0", "bar.0", registry);
		tap.setOutputChannel(output);
		tap.afterPropertiesSet();

		registry.createInbound("bar.0", new DirectChannel(), ALL, false);
		registry.createOutbound("bar.0", output, false);
		Collection<?> bridges = getBridges(registry);
		assertEquals(5, bridges.size()); // 2 each stream + tap
		registry.deleteOutbound("bar.0");
		registry.deleteInbound("bar.0");
		assertEquals(2, bridges.size()); // tap completely gone
		registry.deleteOutbound("foo.0");
		registry.deleteInbound("foo.0");
		assertEquals(0, bridges.size());
	}

	@Test
	public void testSendAndReceive() throws Exception {
		ChannelRegistry registry = getRegistry();
		DirectChannel moduleOutputChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		QueueChannel tapChannel = new QueueChannel();
		registry.createOutbound("foo.0", moduleOutputChannel, false);
		registry.createInbound("foo.0", moduleInputChannel, ALL, false);
		registry.tap("tapper", "foo.0", tapChannel);
		Message<?> message = MessageBuilder.withPayload("foo").setHeader(MessageHeaders.CONTENT_TYPE, "foo/bar").build();
		boolean tapSuccess = false;
		boolean retried = false;
		while (!tapSuccess) {
			moduleOutputChannel.send(message);
			Message<?> inbound = moduleInputChannel.receive(5000);
			assertNotNull(inbound);
			assertEquals("foo", inbound.getPayload());
			assertNull(inbound.getHeaders().get(ChannelRegistrySupport.ORIGINAL_CONTENT_TYPE_HEADER));
			assertEquals("foo/bar", inbound.getHeaders().get(MessageHeaders.CONTENT_TYPE));
			Message<?> tapped = tapChannel.receive(5000);
			if (tapped == null) {
				// tap listener might not have started
				assertFalse("Failed to receive tap after retry", retried);
				retried = true;
				continue;
			}
			tapSuccess = true;
			assertEquals("foo", tapped.getPayload());
			assertNull(tapped.getHeaders().get(ChannelRegistrySupport.ORIGINAL_CONTENT_TYPE_HEADER));
			assertEquals("foo/bar", tapped.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		}
	}

	@Test
	public void testSendAndReceiveNoOriginalContentType() throws Exception {
		ChannelRegistry registry = getRegistry();
		DirectChannel moduleOutputChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		QueueChannel tapChannel = new QueueChannel();
		registry.createOutbound("bar.0", moduleOutputChannel, false);
		registry.createInbound("bar.0", moduleInputChannel, ALL, false);
		registry.tap("tapper", "bar.0", tapChannel);
		boolean tapSuccess = false;
		boolean retried = false;
		while (!tapSuccess) {
			Message<?> message = MessageBuilder.withPayload("foo").build();
			moduleOutputChannel.send(message);
			Message<?> inbound = moduleInputChannel.receive(5000);
			assertNotNull(inbound);
			assertEquals("foo", inbound.getPayload());
			assertNull(inbound.getHeaders().get(ChannelRegistrySupport.ORIGINAL_CONTENT_TYPE_HEADER));
			assertNull(inbound.getHeaders().get(MessageHeaders.CONTENT_TYPE));
			Message<?> tapped = tapChannel.receive(5000);
			if (tapped == null) {
				// tap listener might not have started
				assertFalse("Failed to receive tap after retry", retried);
				retried = true;
				continue;
			}
			tapSuccess = true;
			assertEquals("foo", tapped.getPayload());
			assertNull(tapped.getHeaders().get(ChannelRegistrySupport.ORIGINAL_CONTENT_TYPE_HEADER));
			assertNull(tapped.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		}
	}

	protected abstract Collection<?> getBridges(ChannelRegistry registry);

	protected abstract ChannelRegistry getRegistry() throws Exception;

}
