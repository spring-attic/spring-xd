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

package org.springframework.xd.dirt.plugins.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.messaging.MessageChannel;
import org.springframework.xd.dirt.server.options.XDPropertyKeys;
import org.springframework.xd.module.DeploymentMetadata;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.SimpleModule;
import org.springframework.xd.module.support.BeanDefinitionAddingPostProcessor;

/**
 * @author Mark Fisher
 * @author Jennifer Hickey
 * @author Gary Russell
 */
public class StreamPluginTests {

	private StreamPlugin plugin = new StreamPlugin();

	private MessageChannel input = new DirectChannel();

	private MessageChannel output = new DirectChannel();

	@Before
	public void setup() {
		System.setProperty("XD_TRANSPORT", "local");
	}

	@After
	public void clearContextProperties() {
		System.clearProperty(XDPropertyKeys.XD_TRANSPORT);
	}

	@Test
	public void streamPropertiesAdded() {
		Module module = new SimpleModule(new ModuleDefinition("testsource", ModuleType.source),
				new DeploymentMetadata("foo", 0));
		module.initialize();
		assertEquals(0, module.getProperties().size());
		plugin.preProcessModule(module);
		plugin.postProcessModule(module);
		assertEquals(2, module.getProperties().size());
		assertEquals("foo", module.getProperties().getProperty("xd.stream.name"));
		assertEquals("0", module.getProperties().getProperty("xd.module.index"));
	}

	@Test
	public void streamChannelTests() {
		Module module = mock(Module.class);
		when(module.getDeploymentMetadata()).thenReturn(new DeploymentMetadata("foo", 1));
		when(module.getType()).thenReturn(ModuleType.processor);
		final MessageBus bus = mock(MessageBus.class);
		when(module.getName()).thenReturn("testing");
		when(module.getComponent(MessageBus.class)).thenReturn(bus);
		when(module.getComponent("input", MessageChannel.class)).thenReturn(input);
		when(module.getComponent("output", MessageChannel.class)).thenReturn(output);
		plugin.preProcessModule(module);
		plugin.postProcessModule(module);
		verify(bus).bindConsumer("foo.0", input, Collections.singletonList(MediaType.ALL), false);
		verify(bus).bindProducer("foo.1", output, false);
		verify(bus).bindPubSubProducer(eq("tap:foo.testing"), any(DirectChannel.class));
		plugin.beforeShutdown(module);
		plugin.removeModule(module);
		verify(bus).unbindConsumer("foo.0", input);
		verify(bus).unbindProducer("foo.1", output);
		verify(bus).unbindProducers("tap:foo.testing");
	}

	@Test
	public void sharedComponentsAdded() {
		GenericApplicationContext context = new GenericApplicationContext();
		plugin.preProcessSharedContext(context);
		List<BeanFactoryPostProcessor> sharedBeans = context.getBeanFactoryPostProcessors();
		assertEquals(1, sharedBeans.size());
		assertTrue(sharedBeans.get(0) instanceof BeanDefinitionAddingPostProcessor);
	}

	@Test
	public void testTapOnProxy() {
		Module module = mock(Module.class);
		when(module.getDeploymentMetadata()).thenReturn(new DeploymentMetadata("foo", 1));
		MessageBus messageBus = mock(MessageBus.class);
		when(module.getComponent(MessageBus.class)).thenReturn(messageBus);
		DirectChannel output = new DirectChannel();
		MessageChannel proxy = (MessageChannel) new ProxyFactory(output).getProxy();
		when(module.getComponent("output", MessageChannel.class)).thenReturn(proxy);
		StreamPlugin plugin = new StreamPlugin();
		plugin.postProcessModule(module);
		List<?> interceptors = TestUtils.getPropertyValue(output, "interceptors.interceptors", List.class);
		assertEquals(1, interceptors.size());
		assertTrue(interceptors.get(0) instanceof WireTap);
	}
}
