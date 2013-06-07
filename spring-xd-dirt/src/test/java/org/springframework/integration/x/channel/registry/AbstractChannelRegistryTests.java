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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

import org.junit.Test;

import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.xd.dirt.plugins.StreamPlugin;
import org.springframework.xd.module.Module;
import org.springframework.xd.module.Plugin;
import org.springframework.xd.module.SimpleModule;

/**
 * @author Gary Russell
 */
public abstract class AbstractChannelRegistryTests {

	@Test
	public void testClean() throws Exception {
		ChannelRegistry registry = getRegistry();
		registry.outbound("foo.0", new DirectChannel());
		registry.inbound("foo.0", new DirectChannel());
		registry.outbound("foo.1", new DirectChannel());
		registry.inbound("foo.1", new DirectChannel());
		registry.outbound("foo.2", new DirectChannel());
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
		registry.outbound("foo.0", new DirectChannel());
		registry.inbound("foo.0", new DirectChannel());
		Module module = new SimpleModule("tap", "source");
		Properties properties = new Properties();
		properties.put("channel", "foo.0");
		module.addProperties(properties);
		module = spy(module);
		MessageChannel output = new DirectChannel();
		when(module.getComponents(MessageChannel.class)).thenReturn(
				Collections.singletonMap("output", output));
		Plugin plugin = new StreamPlugin(registry);
		plugin.processModule(module, "bar", 0);
		registry.inbound("bar.0", new DirectChannel());
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
