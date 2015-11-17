/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.xd.dirt.integration.bus.kafka;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.hamcrest.CoreMatchers;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.amqp.utils.test.TestUtils;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.kafka.support.KafkaHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.xd.dirt.integration.bus.Binding;
import org.springframework.xd.dirt.integration.bus.BusProperties;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.integration.bus.XdHeaders;
import org.springframework.xd.dirt.integration.kafka.KafkaMessageBus;

import kafka.admin.AdminUtils$;

/**
 * @author Marius Bogoevici
 */
public class RawModeKafkaMessageBusTests extends KafkaMessageBusTests {

	@Override
	protected KafkaTestMessageBus createKafkaTestMessageBus() {
		return new KafkaTestMessageBus(kafkaTestSupport, getCodec(), KafkaMessageBus.Mode.raw);
	}

	@Test
	@Override
	public void testPartitionedModuleJava() throws Exception {
		MessageBus bus = getMessageBus();
		Properties properties = new Properties();
		properties.put("partitionKeyExtractorClass", "org.springframework.xd.dirt.integration.bus.kafka.RawKafkaPartitionTestSupport");
		properties.put("partitionSelectorClass", "org.springframework.xd.dirt.integration.bus.kafka.RawKafkaPartitionTestSupport");
		properties.put(BusProperties.NEXT_MODULE_COUNT, "3");
		properties.put(BusProperties.NEXT_MODULE_CONCURRENCY, "2");

		DirectChannel output = new DirectChannel();
		output.setBeanName("test.output");
		bus.bindProducer("partJ.0", output, properties);
		@SuppressWarnings("unchecked")
		List<Binding> bindings = TestUtils.getPropertyValue(bus, "messageBus.bindings", List.class);
		assertEquals(1, bindings.size());

		properties.clear();
		properties.put("concurrency", "2");
		properties.put("count","3");
		properties.put("partitionIndex", "0");
		QueueChannel input0 = new QueueChannel();
		input0.setBeanName("test.input0J");
		bus.bindConsumer("partJ.0", input0, properties);
		properties.put("partitionIndex", "1");
		QueueChannel input1 = new QueueChannel();
		input1.setBeanName("test.input1J");
		bus.bindConsumer("partJ.0", input1, properties);
		properties.put("partitionIndex", "2");
		QueueChannel input2 = new QueueChannel();
		input2.setBeanName("test.input2J");
		bus.bindConsumer("partJ.0", input2, properties);

		output.send(new GenericMessage<>(new byte[]{(byte)0}));
		output.send(new GenericMessage<>(new byte[]{(byte)1}));
		output.send(new GenericMessage<>(new byte[]{(byte)2}));

		Message<?> receive0 = input0.receive(1000);
		assertNotNull(receive0);
		Message<?> receive1 = input1.receive(1000);
		assertNotNull(receive1);
		Message<?> receive2 = input2.receive(1000);
		assertNotNull(receive2);

		assertThat(Arrays.asList(
						((byte[]) receive0.getPayload())[0],
						((byte[]) receive1.getPayload())[0],
						((byte[]) receive2.getPayload())[0]),
				containsInAnyOrder((byte)0, (byte)1, (byte)2));

		bus.unbindConsumers("partJ.0");
		bus.unbindProducers("partJ.0");
	}

	@Test
	@Override
	public void testPartitionedModuleSpEL() throws Exception {
		MessageBus bus = getMessageBus();
		Properties properties = new Properties();
		properties.put("partitionKeyExpression", "payload[0]");
		properties.put("partitionSelectorExpression", "hashCode()");
		properties.put(BusProperties.NEXT_MODULE_COUNT, "3");
		properties.put(BusProperties.NEXT_MODULE_CONCURRENCY, "2");

		DirectChannel output = new DirectChannel();
		output.setBeanName("test.output");
		bus.bindProducer("part.0", output, properties);
		@SuppressWarnings("unchecked")
		List<Binding> bindings = TestUtils.getPropertyValue(bus, "messageBus.bindings", List.class);
		assertEquals(1, bindings.size());
		try {
			AbstractEndpoint endpoint = bindings.get(0).getEndpoint();
			assertThat(getEndpointRouting(endpoint), containsString("part.0-' + headers['partition']"));
		}
		catch (UnsupportedOperationException ignored) {

		}

		properties.clear();
		properties.put("concurrency", "2");
		properties.put("partitionIndex", "0");
		properties.put("count","3");
		QueueChannel input0 = new QueueChannel();
		input0.setBeanName("test.input0S");
		bus.bindConsumer("part.0", input0, properties);
		properties.put("partitionIndex", "1");
		QueueChannel input1 = new QueueChannel();
		input1.setBeanName("test.input1S");
		bus.bindConsumer("part.0", input1, properties);
		properties.put("partitionIndex", "2");
		QueueChannel input2 = new QueueChannel();
		input2.setBeanName("test.input2S");
		bus.bindConsumer("part.0", input2, properties);

		Message<byte[]> message2 = MessageBuilder.withPayload(new byte[]{2})
				.setHeader(IntegrationMessageHeaderAccessor.CORRELATION_ID, "foo")
				.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, 42)
				.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, 43)
				.setHeader("xdReplyChannel", "bar")
				.build();
		output.send(message2);
		output.send(new GenericMessage<>(new byte[]{1}));
		output.send(new GenericMessage<>(new byte[]{0}));

		Message<?> receive0 = input0.receive(1000);
		assertNotNull(receive0);
		Message<?> receive1 = input1.receive(1000);
		assertNotNull(receive1);
		Message<?> receive2 = input2.receive(1000);
		assertNotNull(receive2);


		assertThat(Arrays.asList(
						((byte[]) receive0.getPayload())[0],
						((byte[]) receive1.getPayload())[0],
						((byte[]) receive2.getPayload())[0]),
				containsInAnyOrder((byte)0, (byte)1, (byte)2));

		bus.unbindConsumers("part.0");
		bus.unbindProducers("part.0");
	}

	@Test
	@Override
	@SuppressWarnings("unchecked")
	public void testPartitioningWithSingleReceiver() throws Exception {
		MessageBus bus = getMessageBus();
		String topicName = "foo" + System.currentTimeMillis() + ".0";
		try {
			byte partitionCount = 4;
			Properties properties = new Properties();
			properties.put("partitionKeyExpression", "payload[0]");
			properties.put("partitionSelectorExpression", "hashCode()");
			properties.put(BusProperties.MIN_PARTITION_COUNT, Integer.toString(partitionCount));
			DirectChannel moduleOutputChannel = new DirectChannel();
			QueueChannel moduleInputChannel = new QueueChannel();
			bus.bindProducer(topicName, moduleOutputChannel, properties);
			bus.bindConsumer(topicName, moduleInputChannel, null);
			int totalSent = 0;
			for (byte i = 0; i < partitionCount; i++) {
				for (byte j = 0; j < partitionCount + 1; j++ ) {
					// the distribution is uneven across partitions, so that we can verify that the bus doesn't round robin
					moduleOutputChannel.send(new GenericMessage<>(new byte[] { (byte) (i * partitionCount + j) }));
					totalSent ++;
				}
			}
			List<Message<byte[]>> receivedMessages = new ArrayList<>();
			for (int i = 0; i < totalSent; i++) {
				assertTrue(receivedMessages.add((Message<byte[]>) moduleInputChannel.receive(2000)));
			}

			assertThat(receivedMessages, hasSize(totalSent));
			for (Message<byte[]> receivedMessage : receivedMessages) {
				int expectedPartition = receivedMessage.getPayload()[0] % partitionCount;
				assertThat(expectedPartition, equalTo(receivedMessage.getHeaders().get(KafkaHeaders.PARTITION_ID)));
			}
		}
		finally {
			bus.unbindConsumers(topicName);
			bus.unbindProducers(topicName);
			AdminUtils$.MODULE$.deleteTopic(kafkaTestSupport.getZkClient(),topicName);
		}
	}

	@Test
	@Override
	public void createInboundPubSubBeforeOutboundPubSub() throws Exception {
		MessageBus messageBus = getMessageBus();
		DirectChannel moduleOutputChannel = new DirectChannel();
		// Test pub/sub by emulating how StreamPlugin handles taps
		DirectChannel tapChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		QueueChannel module2InputChannel = new QueueChannel();
		QueueChannel module3InputChannel = new QueueChannel();
		// Create the tap first
		String fooTapName = messageBus.isCapable(MessageBus.Capability.DURABLE_PUBSUB) ? "foo.tap:baz.http" : "tap:baz.http";
		messageBus.bindPubSubConsumer(fooTapName, module2InputChannel, null);

		// Then create the stream
		messageBus.bindProducer("baz.0", moduleOutputChannel, null);
		messageBus.bindConsumer("baz.0", moduleInputChannel, null);
		moduleOutputChannel.addInterceptor(new WireTap(tapChannel));
		messageBus.bindPubSubProducer("tap:baz.http", tapChannel, null);

		// Another new module is using tap as an input channel
		String barTapName = messageBus.isCapable(MessageBus.Capability.DURABLE_PUBSUB) ? "bar.tap:baz.http" : "tap:baz.http";
		messageBus.bindPubSubConsumer(barTapName, module3InputChannel, null);
		Message<?> message = MessageBuilder.withPayload("foo".getBytes()).setHeader(MessageHeaders.CONTENT_TYPE, "foo/bar").build();
		boolean success = false;
		boolean retried = false;
		while (!success) {
			moduleOutputChannel.send(message);
			Message<?> inbound = moduleInputChannel.receive(5000);
			assertNotNull(inbound);
			assertEquals("foo", new String((byte[])inbound.getPayload()));
			Message<?> tapped1 = module2InputChannel.receive(5000);
			Message<?> tapped2 = module3InputChannel.receive(5000);
			if (tapped1 == null || tapped2 == null) {
				// listener may not have started
				assertFalse("Failed to receive tap after retry", retried);
				retried = true;
				continue;
			}
			success = true;
			assertEquals("foo", new String((byte[]) tapped1.getPayload()));
			assertNull(tapped1.getHeaders().get(XdHeaders.XD_ORIGINAL_CONTENT_TYPE));
			assertEquals("foo", new String((byte[])tapped2.getPayload()));
		}
		// delete one tap stream is deleted
		messageBus.unbindConsumer(barTapName, module3InputChannel);
		Message<?> message2 = MessageBuilder.withPayload("bar".getBytes()).setHeader(MessageHeaders.CONTENT_TYPE, "foo/bar").build();
		moduleOutputChannel.send(message2);

		// other tap still receives messages
		Message<?> tapped = module2InputChannel.receive(5000);
		assertNotNull(tapped);

		// Removed tap does not
		assertNull(module3InputChannel.receive(1000));

		// when other tap stream is deleted
		messageBus.unbindConsumer(fooTapName, module2InputChannel);
		// Clean up as StreamPlugin would
		messageBus.unbindConsumer("baz.0", moduleInputChannel);
		messageBus.unbindProducer("baz.0", moduleOutputChannel);
		messageBus.unbindProducers("tap:baz.http");
		assertTrue(getBindings(messageBus).isEmpty());
	}

	@Test
	@Override
	public void testSendAndReceive() throws Exception {
		MessageBus messageBus = getMessageBus();
		DirectChannel moduleOutputChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		messageBus.bindProducer("foo.0", moduleOutputChannel, null);
		messageBus.bindConsumer("foo.0", moduleInputChannel, null);
		Message<?> message = MessageBuilder.withPayload("foo".getBytes()).build();
		// Let the consumer actually bind to the producer before sending a msg
		busBindUnbindLatency();
		moduleOutputChannel.send(message);
		Message<?> inbound = moduleInputChannel.receive(5000);
		assertNotNull(inbound);
		assertEquals("foo", new String((byte[])inbound.getPayload()));
		messageBus.unbindProducers("foo.0");
		messageBus.unbindConsumers("foo.0");
	}

	// Ignored, since raw mode does not support headers
	@Test
	@Override
	@Ignore
	public void testSendAndReceiveNoOriginalContentType() throws Exception {

	}

	@Override
	@Test
	public void testSendAndReceivePubSub() throws Exception {
		MessageBus messageBus = getMessageBus();
		DirectChannel moduleOutputChannel = new DirectChannel();
		// Test pub/sub by emulating how StreamPlugin handles taps
		DirectChannel tapChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		QueueChannel module2InputChannel = new QueueChannel();
		QueueChannel module3InputChannel = new QueueChannel();
		messageBus.bindProducer("baz.0", moduleOutputChannel, null);
		messageBus.bindConsumer("baz.0", moduleInputChannel, null);
		moduleOutputChannel.addInterceptor(new WireTap(tapChannel));
		messageBus.bindPubSubProducer("tap:baz.http", tapChannel, null);
		// A new module is using the tap as an input channel
		String fooTapName = messageBus.isCapable(MessageBus.Capability.DURABLE_PUBSUB) ? "foo.tap:baz.http" : "tap:baz.http";
		messageBus.bindPubSubConsumer(fooTapName, module2InputChannel, null);
		// Another new module is using tap as an input channel
		String barTapName = messageBus.isCapable(MessageBus.Capability.DURABLE_PUBSUB) ? "bar.tap:baz.http" : "tap:baz.http";
		messageBus.bindPubSubConsumer(barTapName, module3InputChannel, null);
		Message<?> message = MessageBuilder.withPayload("foo".getBytes()).build();
		boolean success = false;
		boolean retried = false;
		while (!success) {
			moduleOutputChannel.send(message);
			Message<?> inbound = moduleInputChannel.receive(5000);
			assertNotNull(inbound);
			assertEquals("foo", new String((byte[])inbound.getPayload()));

			Message<?> tapped1 = module2InputChannel.receive(5000);
			Message<?> tapped2 = module3InputChannel.receive(5000);
			if (tapped1 == null || tapped2 == null) {
				// listener may not have started
				assertFalse("Failed to receive tap after retry", retried);
				retried = true;
				continue;
			}
			success = true;
			assertEquals("foo", new String((byte[])tapped1.getPayload()));
			assertEquals("foo", new String((byte[])tapped2.getPayload()));
		}
		// delete one tap stream is deleted
		messageBus.unbindConsumer(barTapName, module3InputChannel);
		Message<?> message2 = MessageBuilder.withPayload("bar".getBytes()).build();
		moduleOutputChannel.send(message2);

		// other tap still receives messages
		Message<?> tapped = module2InputChannel.receive(5000);
		assertNotNull(tapped);

		// Removed tap does not
		assertNull(module3InputChannel.receive(1000));

		// when other tap stream is deleted
		messageBus.unbindConsumer(fooTapName, module2InputChannel);
		// Clean up as StreamPlugin would
		messageBus.unbindConsumer("baz.0", moduleInputChannel);
		messageBus.unbindProducer("baz.0", moduleOutputChannel);
		messageBus.unbindProducers("tap:baz.http");
		assertTrue(getBindings(messageBus).isEmpty());
	}

	@Override
	@Test
	@SuppressWarnings("unchecked")
	public void testSendAndReceivePubSubWithMultipleConsumers() throws Exception {
		MessageBus messageBus = getMessageBus();
		DirectChannel quuxUpstreamModuleOutputChannel = new DirectChannel();
		// Test pub/sub by emulating how StreamPlugin handles taps
		DirectChannel tapChannel = new DirectChannel();
		QueueChannel basDownstreamModuleInputChannel = new QueueChannel();
		QueueChannel fooTapDownstreamInputChannel = new QueueChannel();
		QueueChannel barTapDowstreamInput1Channel = new QueueChannel();
		QueueChannel barTapDowstreamInput2Channel = new QueueChannel();
		String originalTopic = "quux.0";
		messageBus.bindProducer(originalTopic, quuxUpstreamModuleOutputChannel, null);
		messageBus.bindConsumer(originalTopic, basDownstreamModuleInputChannel, null);
		quuxUpstreamModuleOutputChannel.addInterceptor(new WireTap(tapChannel));
		Properties quuxTapProducerProperties = new Properties();
		// set the partition count to two, so that the tap has two partitions as well
		// this will be necessary to robin messages across the competing consumers
		quuxTapProducerProperties.setProperty(BusProperties.MIN_PARTITION_COUNT, "2");
		messageBus.bindPubSubProducer("tap:quux.http", tapChannel, quuxTapProducerProperties);
		// A new module is using the tap as an input channel
		String fooTapName = messageBus.isCapable(MessageBus.Capability.DURABLE_PUBSUB) ? "foo.tap:quux.http" : "tap:quux.http";
		messageBus.bindPubSubConsumer(fooTapName, fooTapDownstreamInputChannel, null);
		// Another new module is using tap as an input channel
		String barTapName = messageBus.isCapable(MessageBus.Capability.DURABLE_PUBSUB) ? "bar.tap:quux.http" : "tap:quux.http";
		Properties barTap1Properties = new Properties();
		barTap1Properties.setProperty(BusProperties.COUNT, "2");
		barTap1Properties.setProperty(BusProperties.SEQUENCE, "1");
		messageBus.bindPubSubConsumer(barTapName, barTapDowstreamInput1Channel, barTap1Properties);
		Properties barTap2Properties = new Properties();
		barTap2Properties.setProperty(BusProperties.COUNT, "2");
		barTap2Properties.setProperty(BusProperties.SEQUENCE, "2");
		messageBus.bindPubSubConsumer(barTapName, barTapDowstreamInput2Channel, barTap2Properties);
		Message<?> message1 = MessageBuilder.withPayload("foo1".getBytes()).build();
		Message<?> message2 = MessageBuilder.withPayload("foo2".getBytes()).build();

		quuxUpstreamModuleOutputChannel.send(message1);
		quuxUpstreamModuleOutputChannel.send(message2);
		Message<byte[]> inbound = (Message<byte[]>) basDownstreamModuleInputChannel.receive(5000);
		assertNotNull(inbound);
		assertEquals("foo1", new String(inbound.getPayload()));
		inbound = (Message<byte[]>) basDownstreamModuleInputChannel.receive(5000);
		assertNotNull(inbound);
		assertEquals("foo2", new String(inbound.getPayload()));
		List<Message<?>> tappedFoo = new ArrayList<>();
		tappedFoo.add(fooTapDownstreamInputChannel.receive(5000));
		tappedFoo.add(fooTapDownstreamInputChannel.receive(5000));
		Message<byte[]> tappedBar1 = (Message<byte[]>) barTapDowstreamInput1Channel.receive(5000);
		Message<byte[]> tappedBar2 = (Message<byte[]>) barTapDowstreamInput2Channel.receive(5000);
		assertThat(tappedFoo,
				CoreMatchers.<Message<?>>hasItems(hasProperty("payload", equalTo("foo1".getBytes())),
						hasProperty("payload", equalTo("foo2".getBytes()))));
		assertEquals("foo1", new String(tappedBar1.getPayload()));
		assertEquals("foo2", new String(tappedBar2.getPayload()));

		// when other tap stream is deleted
		messageBus.unbindConsumers(fooTapName);
		// Clean up as StreamPlugin would
		messageBus.unbindConsumers(originalTopic);
		messageBus.unbindProducers(originalTopic);
		messageBus.unbindProducers("tap:quux.http");
		messageBus.unbindConsumers(barTapName);
		assertTrue(getBindings(messageBus).isEmpty());
	}
}
