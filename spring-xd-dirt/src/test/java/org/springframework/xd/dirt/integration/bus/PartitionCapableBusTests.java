/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.integration.bus;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Properties;

import org.junit.Test;

import org.springframework.amqp.utils.test.TestUtils;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;


/**
 * 
 * @author Gary Russell
 */
abstract public class PartitionCapableBusTests extends AbstractMessageBusTests {

	@Test
	public void testBadProperties() throws Exception {
		MessageBus bus = getMessageBus();
		Properties properties = new Properties();
		properties.put("foo", "bar");
		properties.put("baz", "qux");

		DirectChannel output = new DirectChannel();
		try {
			bus.bindProducer("badprops.0", output, properties);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), equalTo(bus.getClass().getSimpleName().replace("Test", "")
					+ " does not support producer properties: baz,foo"));
		}

		properties.remove("baz");
		try {
			bus.bindConsumer("badprops.0", output, properties);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), equalTo(bus.getClass().getSimpleName().replace("Test", "")
					+ " does not support consumer property: foo"));
		}
	}

	@Test
	public void testPartitionedModuleSpEL() throws Exception {
		MessageBus bus = getMessageBus();
		Properties properties = new Properties();
		properties.put("partitionKeyExpression", "payload");
		properties.put("partitionSelectorExpression", "hashCode()");
		properties.put("partitionCount", "3");

		DirectChannel output = new DirectChannel();
		bus.bindProducer("part.0", output, properties);
		@SuppressWarnings("unchecked")
		List<Binding> bindings = TestUtils.getPropertyValue(bus, "messageBus.bindings", List.class);
		assertEquals(1, bindings.size());
		AbstractEndpoint endpoint = bindings.get(0).getEndpoint();
		assertThat(getEndpointRouting(endpoint), containsString("part.0-' + headers['partition']"));

		properties.clear();
		properties.put("concurrency", "2");
		properties.put("partitionIndex", "0");
		QueueChannel input0 = new QueueChannel();
		bus.bindConsumer("part.0", input0, properties);
		properties.put("partitionIndex", "1");
		QueueChannel input1 = new QueueChannel();
		bus.bindConsumer("part.0", input1, properties);
		properties.put("partitionIndex", "2");
		QueueChannel input2 = new QueueChannel();
		bus.bindConsumer("part.0", input2, properties);

		output.send(new GenericMessage<Integer>(2));
		output.send(new GenericMessage<Integer>(1));
		output.send(new GenericMessage<Integer>(0));

		Message<?> receive0 = input0.receive(1000);
		assertNotNull(receive0);
		assertEquals(0, receive0.getPayload());
		Message<?> receive1 = input1.receive(1000);
		assertNotNull(receive1);
		assertEquals(1, receive1.getPayload());
		Message<?> receive2 = input2.receive(1000);
		assertNotNull(receive2);
		assertEquals(2, receive2.getPayload());

		bus.unbindConsumers("part.0");
		bus.unbindConsumers("part.0");
	}

	@Test
	public void testPartitionedModuleJava() throws Exception {
		MessageBus bus = getMessageBus();
		Properties properties = new Properties();
		properties.put("partitionKeyExtractorClass", "org.springframework.xd.dirt.integration.bus.PartitionTestSupport");
		properties.put("partitionSelectorClass", "org.springframework.xd.dirt.integration.bus.PartitionTestSupport");
		properties.put("partitionCount", "3");

		DirectChannel output = new DirectChannel();
		bus.bindProducer("part.0", output, properties);
		@SuppressWarnings("unchecked")
		List<Binding> bindings = TestUtils.getPropertyValue(bus, "messageBus.bindings", List.class);
		assertEquals(1, bindings.size());
		AbstractEndpoint endpoint = bindings.get(0).getEndpoint();
		assertThat(getEndpointRouting(endpoint), containsString("part.0-' + headers['partition']"));

		properties.clear();
		properties.put("partitionIndex", "0");
		QueueChannel input0 = new QueueChannel();
		bus.bindConsumer("part.0", input0, properties);
		properties.put("partitionIndex", "1");
		QueueChannel input1 = new QueueChannel();
		bus.bindConsumer("part.0", input1, properties);
		properties.put("partitionIndex", "2");
		QueueChannel input2 = new QueueChannel();
		bus.bindConsumer("part.0", input2, properties);

		output.send(new GenericMessage<Integer>(2));
		output.send(new GenericMessage<Integer>(1));
		output.send(new GenericMessage<Integer>(0));

		Message<?> receive0 = input0.receive(1000);
		assertNotNull(receive0);
		assertEquals(0, receive0.getPayload());
		Message<?> receive1 = input1.receive(1000);
		assertNotNull(receive1);
		assertEquals(1, receive1.getPayload());
		Message<?> receive2 = input2.receive(1000);
		assertNotNull(receive2);
		assertEquals(2, receive2.getPayload());

		bus.unbindConsumers("part.0");
		bus.unbindConsumers("part.0");
	}

	@Test
	public void testPartitionedPubSubModuleSpEL() throws Exception {
		MessageBus bus = getMessageBus();
		Properties properties = new Properties();
		properties.put("partitionKeyExpression", "payload");
		properties.put("partitionSelectorExpression", "hashCode()");
		properties.put("partitionCount", "3");

		DirectChannel output = new DirectChannel();
		bus.bindPubSubProducer("partpub.0", output, properties);
		@SuppressWarnings("unchecked")
		List<Binding> bindings = TestUtils.getPropertyValue(bus, "messageBus.bindings", List.class);
		assertEquals(1, bindings.size());
		AbstractEndpoint endpoint = bindings.get(0).getEndpoint();
		assertThat(getPubSubEndpointRouting(endpoint), containsString("partpub.0-' + headers['partition']"));

		properties.clear();
		properties.put("partitionIndex", "0");
		QueueChannel input0 = new QueueChannel();
		bus.bindPubSubConsumer("partpub.0", input0, properties);
		properties.put("partitionIndex", "1");
		QueueChannel input1 = new QueueChannel();
		bus.bindPubSubConsumer("partpub.0", input1, properties);
		properties.put("partitionIndex", "2");
		QueueChannel input2 = new QueueChannel();
		bus.bindPubSubConsumer("partpub.0", input2, properties);

		output.send(new GenericMessage<Integer>(2));
		output.send(new GenericMessage<Integer>(1));
		output.send(new GenericMessage<Integer>(0));

		Message<?> receive0 = input0.receive(1000);
		assertNotNull(receive0);
		assertEquals(0, receive0.getPayload());
		Message<?> receive1 = input1.receive(1000);
		assertNotNull(receive1);
		assertEquals(1, receive1.getPayload());
		Message<?> receive2 = input2.receive(1000);
		assertNotNull(receive2);
		assertEquals(2, receive2.getPayload());

		bus.unbindConsumers("partpub.0");
		bus.unbindConsumers("partpub.0");
	}

	protected abstract String getEndpointRouting(AbstractEndpoint endpoint);

	protected abstract String getPubSubEndpointRouting(AbstractEndpoint endpoint);

}
