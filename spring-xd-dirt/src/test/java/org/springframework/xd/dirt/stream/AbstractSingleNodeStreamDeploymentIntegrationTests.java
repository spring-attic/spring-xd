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

import java.util.Collections;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.x.bus.MessageBus;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.xd.module.core.Module;


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

	@Test
	public final void testTopicChannel() throws InterruptedException {

		StreamDefinition bar1Definition = new StreamDefinition("bar1Definition",
				"topic:foo > queue:bar1");
		StreamDefinition bar2Definition = new StreamDefinition("bar2Definition",
				"topic:foo > queue:bar2");
		assertEquals(0, streamRepository.count());
		streamDeployer.save(bar1Definition);
		deploy(bar1Definition);

		streamDeployer.save(bar2Definition);
		deploy(bar2Definition);
		Thread.sleep(1000);
		assertEquals(2, streamRepository.count());

		final Module module = getModule("bridge", 0);

		MessageBus bus = module.getComponent(MessageBus.class);

		QueueChannel bar1Channel = new QueueChannel();
		QueueChannel bar2Channel = new QueueChannel();

		bus.bindConsumer("queue:bar1", bar1Channel, Collections.singletonList(MediaType.ALL), true);
		bus.bindConsumer("queue:bar2", bar2Channel, Collections.singletonList(MediaType.ALL), true);

		DirectChannel testChannel = new DirectChannel();
		bus.bindPubSubProducer("topic:foo", testChannel);

		testChannel.send(new GenericMessage<String>("hello"));

		final Message<?> bar1Message = bar1Channel.receive(10000);
		final Message<?> bar2Message = bar2Channel.receive(10000);
		assertEquals("hello", bar1Message.getPayload());
		assertEquals("hello", bar2Message.getPayload());

		bus.unbindProducer("topic:foo", testChannel);
		bus.unbindConsumer("queue:bar1", bar1Channel);
		bus.unbindConsumer("queue:bar2", bar2Channel);

	}

	private void doTest(StreamDefinition routerDefinition) throws InterruptedException {
		assertEquals(0, streamRepository.count());
		streamDeployer.save(routerDefinition);
		deploy(routerDefinition);
		assertEquals(1, streamRepository.count());
		assertModuleRequest("router", false);

		final Module module = getModule("router", 0);
		MessageBus bus = module.getComponent(MessageBus.class);

		QueueChannel fooChannel = new QueueChannel();
		QueueChannel barChannel = new QueueChannel();
		bus.bindConsumer("queue:foo", fooChannel, Collections.singletonList(MediaType.ALL), true);
		bus.bindConsumer("queue:bar", barChannel, Collections.singletonList(MediaType.ALL), true);

		DirectChannel testChannel = new DirectChannel();
		bus.bindProducer("queue:routeit", testChannel, true);
		testChannel.send(MessageBuilder.withPayload("a").build());

		testChannel.send(MessageBuilder.withPayload("b").build());

		final Message<?> fooMessage = fooChannel.receive(10000);
		final Message<?> barMessage = barChannel.receive(10000);
		assertEquals("a", fooMessage.getPayload());
		assertEquals("b", barMessage.getPayload());

		bus.unbindProducer("queue:routeit", testChannel);
		bus.unbindConsumer("queue:foo", fooChannel);
		bus.unbindConsumer("queue:bar", barChannel);
	}


}
