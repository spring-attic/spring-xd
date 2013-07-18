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

import java.util.Collection;

import org.junit.Test;

import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.xd.dirt.stream.Tap;

/**
 * @author Gary Russell
 */
public abstract class AbstractChannelRegistryTests {

	@Test
	public void testClean() throws Exception {
		ChannelRegistry registry = getRegistry();
		registry.outbound("foo.0", new DirectChannel(), null);
		registry.inbound("foo.0", new DirectChannel(), null);
		registry.outbound("foo.1", new DirectChannel(), null);
		registry.inbound("foo.1", new DirectChannel(), null);
		registry.outbound("foo.2", new DirectChannel(), null);
		registry.tap("bar", "foo.0", new DirectChannel());
		Collection<?> bridges = getBridges(registry);
		assertEquals(6, bridges.size());
		registry.cleanAll("foo.0");
		assertEquals(4, bridges.size());
		registry.cleanAll("foo.1");
		assertEquals(2, bridges.size());
		registry.cleanAll("foo.2");
		assertEquals(1, bridges.size()); // tap remains
	}

	@Test
	public void testCleanTap() throws Exception {
		ChannelRegistry registry = getRegistry();
		registry.outbound("foo.0", new DirectChannel(), null);
		registry.inbound("foo.0", new DirectChannel(), null);

		MessageChannel output = new DirectChannel();

		Tap tap = new Tap("foo.0", "bar.0", registry);
		tap.setOutputChannel(output);
		tap.afterPropertiesSet();

		registry.inbound("bar.0", new DirectChannel(), null);
		registry.outbound("bar.0", output, null);
		Collection<?> bridges = getBridges(registry);
		assertEquals(5, bridges.size()); // 2 each stream + tap
		registry.cleanAll("bar.0");
		assertEquals(2, bridges.size()); // tap completely gone
		registry.cleanAll("foo.0");
		assertEquals(0, bridges.size());
	}

	protected abstract Collection<?> getBridges(ChannelRegistry registry);

	protected abstract ChannelRegistry getRegistry() throws Exception;

}
