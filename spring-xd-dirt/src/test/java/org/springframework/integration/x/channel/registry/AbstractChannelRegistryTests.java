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
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;
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
		registry.createOutbound("foo.0", moduleOutputChannel, false);
		registry.createInbound("foo.0", moduleInputChannel, ALL, false);
		moduleOutputChannel.send(new GenericMessage<String>("foo"));
		Message<?> inbound = moduleInputChannel.receive(5000);
		assertNotNull(inbound);
		assertEquals("foo", inbound.getPayload());
	}

	protected abstract Collection<?> getBridges(ChannelRegistry registry);

	protected abstract ChannelRegistry getRegistry() throws Exception;

}
