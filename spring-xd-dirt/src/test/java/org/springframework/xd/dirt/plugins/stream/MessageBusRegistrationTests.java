/*
 * Copyright 2013-2014 the original author or authors.
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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.zookeeper.EmbeddedZooKeeper;
import org.springframework.xd.dirt.zookeeper.ZooKeeperConnection;
import org.springframework.xd.module.ModuleDeploymentProperties;
import org.springframework.xd.module.ModuleDescriptor;
import org.springframework.xd.module.ModuleType;
import org.springframework.xd.module.TestModuleDefinitions;
import org.springframework.xd.module.core.Module;

/**
 * @author Jennifer Hickey
 * @author Gary Russell
 */
public class MessageBusRegistrationTests {

	@Mock
	private MessageBus bus;

	private StreamPlugin streamPlugin;

	@Mock
	private Module module;

	private ModuleDescriptor descriptor = new ModuleDescriptor.Builder()
			.setGroup("mystream")
			.setIndex(1)
			.setModuleDefinition(TestModuleDefinitions.dummy("messageBusRegistrationTests", ModuleType.processor))
			.build();

	private MessageChannel input = new DirectChannel();

	private MessageChannel output = new DirectChannel();

	private ModuleDeploymentProperties deploymentProperies;

	private Properties consumerProperties;

	private Properties producerProperties;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		EmbeddedZooKeeper embeddedZooKeeper = new EmbeddedZooKeeper();
		embeddedZooKeeper.start();
		ZooKeeperConnection zkConnection = new ZooKeeperConnection("localhost:" + embeddedZooKeeper.getClientPort());
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
		streamPlugin = new StreamPlugin(bus, zkConnection);
		when(module.getComponent("input", MessageChannel.class)).thenReturn(input);
		when(module.getComponent("output", MessageChannel.class)).thenReturn(output);
		when(module.getDescriptor()).thenReturn(descriptor);
		deploymentProperies = new ModuleDeploymentProperties();
		deploymentProperies.put("consumer.foo", "bar");
		deploymentProperies.put("producer.baz", "qux");
		when(module.getDeploymentProperties()).thenReturn(deploymentProperies);
		doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				consumerProperties = (Properties) invocation.getArguments()[2];
				return null;
			}

		}).when(bus).bindConsumer(eq("mystream.0"), same(input), any(Properties.class));
		doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				producerProperties = (Properties) invocation.getArguments()[2];
				return null;
			}

		}).when(bus).bindProducer(eq("mystream.1"), same(output), any(Properties.class));
	}

	@Test
	public void testRegistration() throws Exception {
		streamPlugin.postProcessModule(module);
		verify(bus).bindConsumer(eq("mystream.0"), same(input), any(Properties.class));
		verify(bus).bindProducer(eq("mystream.1"), same(output), any(Properties.class));
		assertNotNull(consumerProperties);
		assertEquals(1, consumerProperties.size());
		assertEquals("bar", consumerProperties.get("foo"));
		assertNotNull(producerProperties);
		assertEquals(1, producerProperties.size());
		assertEquals("qux", producerProperties.get("baz"));
	}

}
