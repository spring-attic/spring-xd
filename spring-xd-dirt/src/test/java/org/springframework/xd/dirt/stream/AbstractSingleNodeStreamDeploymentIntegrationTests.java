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

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.xd.module.Module;


/**
 * Base class that contains the tests but does not provide the transport. Each subclass should implement
 * {@link AbstractStreamDeploymentIntegrationTests#getTransport()} in order to execute the test methods defined here for
 * that transport.
 * 
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Mark Fisher
 */
public abstract class AbstractSingleNodeStreamDeploymentIntegrationTests extends
		AbstractStreamDeploymentIntegrationTests {

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
		final StreamDefinition routerDefinition = new StreamDefinition("routerDefinition",
				"queue:routeit > router --expression=payload.contains('a')?'queue:foo':'queue:bar'");
		doTest(routerDefinition);
	}

	@Test
	public final void testRoutingWithGroovy() throws InterruptedException {
		StreamDefinition routerDefinition = new StreamDefinition("routerDefinition",
				"queue:routeit > router --script='org/springframework/xd/dirt/stream/router.groovy'");
		doTest(routerDefinition);
	}

	private void doTest(StreamDefinition routerDefinition) throws InterruptedException {
		assertEquals(0, streamRepository.count());
		streamDefinitionRepository.save(routerDefinition);
		streamDeployer.deploy("routerDefinition");
		Thread.sleep(1000);
		assertEquals(1, streamRepository.count());
		assertModuleRequest("router", false);

		final Module module = getModule("router", 0, moduleDeployer);
		MessageBus bus = module.getComponent(MessageBus.class);

		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		bus.bindConsumer("foo", fooChannel, null, true);
		bus.bindConsumer("bar", barChannel, null, true);

		DirectChannel testChannel = new DirectChannel();
		bus.bindProducer("routeit", testChannel, true);
		testChannel.send(MessageBuilder.withPayload("a").build());
		Thread.sleep(2000);

		testChannel.send(MessageBuilder.withPayload("b").build());
		Thread.sleep(2000);

		// assertTrue(fooChannel.getQueueSize() == 1);
		// assertTrue(barChannel.getQueueSize() == 1);
		final Message<?> fooMessage = fooChannel.receive(2000);
		final Message<?> barMessage = barChannel.receive(2000);
		assertEquals("a", fooMessage.getPayload());
		assertEquals("b", barMessage.getPayload());

		bus.unbindProducer("routeit", testChannel);
		bus.unbindConsumer("foo", fooChannel);
		bus.unbindConsumer("bar", barChannel);
	}

}
