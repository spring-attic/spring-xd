/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.dirt.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.xd.module.Module;


/**
 * @author David Turanski
 * @author Gunnar Hillert
 */
public class SingleNodeStreamDeploymentIntegrationTests extends AbstractStreamDeploymentIntegrationTests {

	@Override
	protected String getTransport() {
		return "local";
	}

	@Override
	protected void setupApplicationContext(ApplicationContext context) {
		MessageChannel containerControlChannel = context.getBean("containerControlChannel", MessageChannel.class);
		SubscribableChannel deployChannel = context.getBean("deployChannel", SubscribableChannel.class);
		SubscribableChannel undeployChannel = context.getBean("undeployChannel", SubscribableChannel.class);

		BridgeHandler handler = new BridgeHandler();
		handler.setOutputChannel(containerControlChannel);
		handler.setComponentName("xd.local.control.bridge");
		deployChannel.subscribe(handler);
		undeployChannel.subscribe(handler);
	}

	@Override
	protected void cleanup(ApplicationContext context) {

	}

	@Test
	public final void testRoutingWithSpel() throws InterruptedException {
		assertEquals(0, streamRepository.count());

		final StreamDefinition routerDefinition = new StreamDefinition("routerDefinition",
				":routeit > router --expression=payload.contains('a')?':foo':':bar'");
		streamDefinitionRepository.save(routerDefinition);
		streamDeployer.deploy("routerDefinition");

		assertEquals(1, streamRepository.count());
		assertModuleRequest("router", false);

		final Module module = getModule("router", 0, moduleDeployer);
		final MessageChannel inputChannel = module.getComponent("routeit", MessageChannel.class);
		assertNotNull(inputChannel);

		final MessageChannel outputChannelFoo = module.getComponent("foo", MessageChannel.class);
		final MessageChannel outputChannelBar = module.getComponent("bar", MessageChannel.class);
		assertNull(outputChannelFoo);
		assertNull(outputChannelBar);

		inputChannel.send(MessageBuilder.withPayload("a").build());
		Thread.sleep(1000);
		final MessageChannel outputChannelFoo2 = module.getComponent("foo", MessageChannel.class);
		final MessageChannel outputChannelBar2 = module.getComponent("bar", MessageChannel.class);
		assertNotNull(outputChannelFoo2);
		assertNull(outputChannelBar2);

		inputChannel.send(MessageBuilder.withPayload("b").build());
		Thread.sleep(1000);

		final QueueChannel outputChannelFoo3 = module.getComponent("foo", QueueChannel.class);
		final QueueChannel outputChannelBar3 = module.getComponent("bar", QueueChannel.class);
		assertNotNull(outputChannelFoo3);
		assertNotNull(outputChannelBar3);

		assertTrue(outputChannelFoo3.getQueueSize() == 1);
		assertTrue(outputChannelBar3.getQueueSize() == 1);

		final Message<?> fooMessage = outputChannelFoo3.receive(2000);
		final Message<?> barMessage = outputChannelBar3.receive(2000);

		assertEquals("a", fooMessage.getPayload());
		assertEquals("b", barMessage.getPayload());

	}

	@Test
	public final void testRoutingWithGroovy() throws InterruptedException {
		assertEquals(0, streamRepository.count());

		final StreamDefinition routerDefinition = new StreamDefinition("routerDefinition",
				":routeit > router --script='org/springframework/xd/dirt/stream/router.groovy'");
		streamDefinitionRepository.save(routerDefinition);
		streamDeployer.deploy("routerDefinition");
		Thread.sleep(1000);
		assertEquals(1, streamRepository.count());
		assertModuleRequest("router", false);

		final Module module = getModule("router", 0, moduleDeployer);
		final MessageChannel inputChannel = module.getComponent("routeit", MessageChannel.class);
		module.getComponent(MessageBus.class);
		assertNotNull(inputChannel);

		final MessageChannel outputChannelFoo = module.getComponent("foo", MessageChannel.class);
		final MessageChannel outputChannelBar = module.getComponent("bar", MessageChannel.class);
		assertNull(outputChannelFoo);
		assertNull(outputChannelBar);

		inputChannel.send(MessageBuilder.withPayload("a").build());
		Thread.sleep(2000);
		final MessageChannel outputChannelFoo2 = module.getComponent("foo", MessageChannel.class);
		final MessageChannel outputChannelBar2 = module.getComponent("bar", MessageChannel.class);
		assertNotNull(outputChannelFoo2);
		assertNull(outputChannelBar2);

		inputChannel.send(MessageBuilder.withPayload("b").build());
		Thread.sleep(1000);

		final QueueChannel outputChannelFoo3 = module.getComponent("foo", QueueChannel.class);
		final QueueChannel outputChannelBar3 = module.getComponent("bar", QueueChannel.class);
		assertNotNull(outputChannelFoo3);
		assertNotNull(outputChannelBar3);

		assertTrue(outputChannelFoo3.getQueueSize() == 1);
		assertTrue(outputChannelBar3.getQueueSize() == 1);

		final Message<?> fooMessage = outputChannelFoo3.receive(2000);
		final Message<?> barMessage = outputChannelBar3.receive(2000);

		assertEquals("a", fooMessage.getPayload());
		assertEquals("b", barMessage.getPayload());
	}

}
