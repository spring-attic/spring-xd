/*
 * Copyright 2013-2015 the original author or authors.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.xd.module.options.spi.ModulePlaceholders.XD_STREAM_NAME_KEY;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.validation.BindException;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.integration.bus.XdHeaders;
import org.springframework.xd.dirt.integration.bus.local.LocalMessageBus;
import org.springframework.xd.dirt.zookeeper.EmbeddedZooKeeper;
import org.springframework.xd.dirt.zookeeper.Paths;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.module.ModuleDefinition;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.TestModuleDefinitions;
import org.springframework.xd.module.core.Module;
import org.springframework.xd.module.core.ResourceConfiguredModule;

/**
 * @author Mark Fisher
 * @author Jennifer Hickey
 * @author Gary Russell
 * @author David Turanski
 */
public class StreamPluginTests {

	@Mock
	private MessageBus bus;

	private StreamPlugin plugin;

	private MessageChannel input = new DirectChannel();

	private MessageChannel output = new DirectChannel();

	private ZooKeeperConnection zkConnection;

	@Before
	public void setup() throws BindException {
		System.setProperty("XD_TRANSPORT", "local");
		MockitoAnnotations.initMocks(this);
		EmbeddedZooKeeper embeddedZooKeeper = new EmbeddedZooKeeper();
		embeddedZooKeeper.start();
		this.zkConnection = new ZooKeeperConnection("localhost:" + embeddedZooKeeper.getClientPort());
		zkConnection.start();
		while (!zkConnection.isConnected()) {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("interrupted while waiting for ZK connection");
			}
		}
		plugin = new StreamPlugin(bus, zkConnection);
	}

	@After
	public void clearContextProperties() {
		System.clearProperty("XD_TRANSPORT");
	}

	@Test
	public void streamPropertiesAdded() {
		ModuleDescriptor moduleDescriptor = new ModuleDescriptor.Builder()
				.setModuleDefinition(TestModuleDefinitions.dummy("testsource", ModuleType.source))
				.setGroup("foo")
				.setIndex(0)
				.build();

		Module module = new ResourceConfiguredModule(moduleDescriptor,
				new ModuleDeploymentProperties());
		module.initialize();
		assertEquals(0, module.getProperties().size());
		plugin.preProcessModule(module);
		assertEquals(1, module.getProperties().size());
		assertEquals("foo", module.getProperties().getProperty(XD_STREAM_NAME_KEY));
	}

	@Test
	public void streamChannelTests() throws InterruptedException {
		ModuleDefinition moduleDefinition = TestModuleDefinitions.dummy("testing", ModuleType.processor);
		Module module = mock(Module.class);
		when(module.getDescriptor()).thenReturn(new ModuleDescriptor.Builder()
				.setGroup("foo")
				.setIndex(1)
				.setModuleDefinition(moduleDefinition)
				.build());
		when(module.getType()).thenReturn(moduleDefinition.getType());
		when(module.getName()).thenReturn(moduleDefinition.getName());
		when(module.getComponent(MessageBus.class)).thenReturn(bus);
		when(module.getComponent("input", MessageChannel.class)).thenReturn(input);
		when(module.getComponent("output", MessageChannel.class)).thenReturn(output);
		plugin.preProcessModule(module);
		plugin.postProcessModule(module);
		verify(bus).bindConsumer(eq("foo.0"), same(input), any(Properties.class));
		verify(bus).bindProducer(eq("foo.1"), same(output), any(Properties.class));
		plugin.beforeShutdown(module);
		plugin.removeModule(module);
		verify(bus).unbindConsumer("foo.0", input);
		verify(bus).unbindProducer("foo.1", output);
		verify(bus).unbindProducers("tap:stream:foo.testing.1");
	}

	@Test
	public void testTapOnProxy() throws Exception {
		ModuleDefinition moduleDefinition = TestModuleDefinitions.dummy("testing", ModuleType.processor);
		Module module = mock(Module.class);
		when(module.getDescriptor()).thenReturn(new ModuleDescriptor.Builder()
				.setGroup("foo")
				.setIndex(1)
				.setModuleDefinition(moduleDefinition)
				.build());
		when(module.getComponent(MessageBus.class)).thenReturn(bus);
		when(module.getName()).thenReturn(moduleDefinition.getName());
		DirectChannel output = new DirectChannel();
		MessageChannel proxy = (MessageChannel) new ProxyFactory(output).getProxy();
		when(module.getComponent("output", MessageChannel.class)).thenReturn(proxy);
		plugin.postProcessModule(module);
		List<?> interceptors = TestUtils.getPropertyValue(output, "interceptors.interceptors", List.class);
		assertEquals(0, interceptors.size());

		// simulate addition of a tap consumer
		zkConnection.getClient().create().creatingParentsIfNeeded().forPath(
				Paths.build(Paths.TAPS, "stream:foo.testing.1"));
		Thread.sleep(1000);

		assertEquals(1, interceptors.size());
		assertTrue(interceptors.get(0) instanceof WireTap);
	}

	@Test
	// XD 2429
	public void testBindOrder() {
		ModuleDefinition moduleDefinition = TestModuleDefinitions.dummy("testing", ModuleType.processor);
		Module module = mock(Module.class);
		when(module.getDescriptor()).thenReturn(new ModuleDescriptor.Builder()
				.setGroup("foo")
				.setIndex(1)
				.setModuleDefinition(moduleDefinition)
				.build());
		when(module.getType()).thenReturn(moduleDefinition.getType());
		when(module.getName()).thenReturn(moduleDefinition.getName());
		final AtomicReference<Message<?>> messageReceived = new AtomicReference<>();
		LocalMessageBus bus = new LocalMessageBus() {

			final AtomicBoolean consumerBound = new AtomicBoolean();

			@Override
			public void bindProducer(String name, final MessageChannel moduleOutputChannel, Properties properties) {

				final MessageHandler handler = new MessageHandler() {

					@Override
					public void handleMessage(Message<?> message) throws MessagingException {
						messageReceived.set(message);
					}

				};

				if (consumerBound.get()) {
					Executors.newSingleThreadExecutor().execute(new Runnable() {

						@Override
						public void run() {
							try {
								Thread.sleep(1000); //delay the bind to exhibit the XD-2429 behavior
							}
							catch (InterruptedException e) {
								Thread.currentThread().interrupt();
							}
							((SubscribableChannel) moduleOutputChannel).subscribe(handler);
						}
					});
				}
				else {
					((SubscribableChannel) moduleOutputChannel).subscribe(handler);
				}
			}

			@Override
			public void bindConsumer(String name, MessageChannel moduleInputChannel, Properties properties) {
				this.consumerBound.set(true);
			}

		};
		when(module.getComponent(MessageBus.class)).thenReturn(bus);
		DirectChannel input = new DirectChannel();
		input.setBeanName("test.input.channel");
		DirectChannel output = new DirectChannel();
		output.setBeanName("test.output.channel");
		BridgeHandler handler = new BridgeHandler();
		handler.setOutputChannel(output);
		EventDrivenConsumer bridge = new EventDrivenConsumer(input, handler);
		bridge.start();
		when(module.getComponent("input", MessageChannel.class)).thenReturn(input);
		when(module.getComponent("output", MessageChannel.class)).thenReturn(output);
		ModuleDeploymentProperties props = new ModuleDeploymentProperties();
		props.setTrackHistory(true);
		when(module.getDeploymentProperties()).thenReturn(props);
		when(module.getComponent(MessageBuilderFactory.class)).thenReturn(new DefaultMessageBuilderFactory());
		StreamPlugin plugin = new StreamPlugin(bus, zkConnection);
		plugin.preProcessModule(module);
		plugin.postProcessModule(module);
		input.send(new GenericMessage<String>("foo"));
		assertNotNull(messageReceived.get());
		@SuppressWarnings("unchecked")
		Collection<Map<String, Object>> xdHistory =
				(Collection<Map<String, Object>>) messageReceived.get().getHeaders().get(XdHeaders.XD_HISTORY);
		assertTrue(xdHistory != null);
		assertEquals("testing", xdHistory.iterator().next().get("module"));
	}

}
