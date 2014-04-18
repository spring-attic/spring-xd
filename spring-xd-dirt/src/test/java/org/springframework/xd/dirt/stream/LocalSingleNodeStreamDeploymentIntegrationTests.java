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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * @author Mark Fisher
 * @author David Turanski
 */
public class LocalSingleNodeStreamDeploymentIntegrationTests extends AbstractSingleNodeStreamDeploymentIntegrationTests {

	@BeforeClass
	public static void setUp() {
		setUp("local");
	}

	@Test
	public void verifyChannelsRegisteredOnDemand() throws InterruptedException {
		final StreamDefinition routerDefinition = new StreamDefinition("routerDefinition",
				"queue:x > router --expression=payload.contains('y')?'queue:y':'queue:z'");
		integrationSupport.streamDefinitionRepository().save(routerDefinition);
		integrationSupport.deployStream(routerDefinition);
		Thread.sleep(1000);

		singleNodeApplication.pluginContext().getBean("queue:x", MessageChannel.class);
		assertFalse(singleNodeApplication.pluginContext().containsBean("queue:y"));
		assertFalse(singleNodeApplication.pluginContext().containsBean("queue:z"));


		DirectChannel testChannel = new DirectChannel();
		MessageBus bus = integrationSupport.messageBus();
		bus.bindProducer("queue:x", testChannel);
		testChannel.send(MessageBuilder.withPayload("y").build());
		Thread.sleep(2000);

		singleNodeApplication.pluginContext().getBean("queue:y", MessageChannel.class);
		assertFalse(singleNodeApplication.pluginContext().containsBean("queue:z"));

		testChannel.send(MessageBuilder.withPayload("z").build());
		Thread.sleep(2000);
		QueueChannel y3 = singleNodeApplication.pluginContext().getBean("queue:y", QueueChannel.class);
		QueueChannel z3 = singleNodeApplication.pluginContext().getBean("queue:z", QueueChannel.class);
		assertNotNull(y3);
		assertNotNull(z3);

		assertTrue(y3.getQueueSize() == 1);
		assertTrue(z3.getQueueSize() == 1);
		final Message<?> yMessage = y3.receive(2000);
		final Message<?> zMessage = z3.receive(2000);
		assertEquals("y", yMessage.getPayload());
		assertEquals("z", zMessage.getPayload());

		bus.unbindProducer("queue:x", testChannel);
		bus.unbindConsumer("queue:y", y3);
		bus.unbindConsumer("queue:z", z3);
	}
}
