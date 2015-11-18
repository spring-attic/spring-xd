/*
 * Copyright 2014-2015 the original author or authors.
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

import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.hamcrest.CoreMatchers;
import org.hamcrest.beans.HasPropertyWithValue;
import org.hamcrest.collection.IsMapContaining;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.kafka.core.KafkaMessage;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.listener.AcknowledgingMessageListener;
import org.springframework.integration.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.integration.kafka.listener.KafkaNativeOffsetManager;
import org.springframework.integration.kafka.listener.KafkaTopicOffsetManager;
import org.springframework.integration.kafka.listener.MessageListener;
import org.springframework.integration.kafka.support.KafkaHeaders;
import org.springframework.integration.kafka.support.ProducerConfiguration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.xd.dirt.integration.bus.Binding;
import org.springframework.xd.dirt.integration.bus.BusProperties;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.integration.bus.PartitionCapableBusTests;
import org.springframework.xd.dirt.integration.bus.XdHeaders;
import org.springframework.xd.dirt.integration.kafka.KafkaMessageBus;
import org.springframework.xd.test.kafka.KafkaTestSupport;

import kafka.admin.AdminUtils$;
import kafka.api.OffsetRequest;

/**
 * Integration tests for the {@link KafkaMessageBus}.
 *
 * @author Eric Bottard
 * @author Marius Bogoevici
 */
public class KafkaMessageBusTests extends PartitionCapableBusTests {

	@Rule
	public KafkaTestSupport kafkaTestSupport = new KafkaTestSupport();

	private KafkaTestMessageBus messageBus;

	@Override
	protected void busBindUnbindLatency() throws InterruptedException {
		Thread.sleep(500);
	}

	@Override
	protected MessageBus getMessageBus() {
		if (messageBus == null) {
			messageBus = createKafkaTestMessageBus();
		}
		return messageBus;
	}

	protected KafkaTestMessageBus createKafkaTestMessageBus() {
		return new KafkaTestMessageBus(kafkaTestSupport, getCodec(), KafkaMessageBus.Mode.embeddedHeaders);
	}

	@Override
	protected boolean usesExplicitRouting() {
		return false;
	}

	@Override
	public Spy spyOn(final String name) {
		String topic = KafkaMessageBus.escapeTopicName(name);

		KafkaTestMessageBus busWrapper = (KafkaTestMessageBus) getMessageBus();
		// Rewind offset, as tests will have typically already sent the messages we're trying to consume

		KafkaMessageListenerContainer messageListenerContainer = busWrapper.getCoreMessageBus().createMessageListenerContainer(
				new Properties(), UUID.randomUUID().toString(), 1, topic, OffsetRequest.EarliestTime(), false);

		final BlockingQueue<KafkaMessage> messages = new ArrayBlockingQueue<KafkaMessage>(10);

		messageListenerContainer.setMessageListener(new MessageListener() {

			@Override
			public void onMessage(KafkaMessage message) {
				messages.offer(message);
			}
		});


		return new Spy() {

			@Override
			public Object receive(boolean expectNull) throws Exception {
				return messages.poll(expectNull ? 50 : 5000, TimeUnit.MILLISECONDS);
			}
		};

	}

	@Test
	@Override
	@SuppressWarnings("unchecked")
	public void testPartitioningWithSingleReceiver() throws Exception {
		MessageBus bus = getMessageBus();
		String topicName = "foo" + System.currentTimeMillis() + ".0";
		try {
			int partitionCount = 4;
			Properties properties = new Properties();
			properties.put("partitionKeyExtractorClass", "org.springframework.xd.dirt.integration.bus.PartitionTestSupport");
			properties.put("partitionSelectorClass", "org.springframework.xd.dirt.integration.bus.PartitionTestSupport");
			properties.put(BusProperties.MIN_PARTITION_COUNT, Integer.toString(partitionCount));
			DirectChannel moduleOutputChannel = new DirectChannel();
			QueueChannel moduleInputChannel = new QueueChannel();
			bus.bindProducer(topicName, moduleOutputChannel, properties);
			bus.bindConsumer(topicName, moduleInputChannel, null);
			int totalSent = 0;
			for (int i = 0; i < partitionCount; i++) {
				for (int j = 0; j < partitionCount + 1; j++ ) {
					// the distribution is uneven across partitions, so that we can verify that the bus doesn't round robin
					moduleOutputChannel.send(new GenericMessage<Object>(i*partitionCount + j));
					totalSent++;
				}
			}
			List<Message<Integer>> receivedMessages = new ArrayList<>();
			for (int i = 0; i < totalSent; i++) {
				assertTrue(receivedMessages.add((Message<Integer>) moduleInputChannel.receive(2000)));
			}

			assertThat(receivedMessages, hasSize(totalSent));
			for (Message<Integer> receivedMessage : receivedMessages) {
				int expectedPartition = receivedMessage.getPayload() % partitionCount;
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
	public void testCompression() throws Exception {
		final String[] codecs = new String[] { null, "none", "gzip", "snappy" };

		byte[] ratherBigPayload = new byte[2048];
		Arrays.fill(ratherBigPayload, (byte) 65);
		MessageBus messageBus = getMessageBus();

		for (String codec : codecs) {
			DirectChannel moduleOutputChannel = new DirectChannel();
			QueueChannel moduleInputChannel = new QueueChannel();
			Properties props = new Properties();
			if (codec != null) {
				props.put(KafkaMessageBus.COMPRESSION_CODEC, codec);
			}
			messageBus.bindProducer("foo.0", moduleOutputChannel, props);
			messageBus.bindConsumer("foo.0", moduleInputChannel, null);
			Message<?> message = org.springframework.integration.support.MessageBuilder.withPayload(ratherBigPayload).build();
			// Let the consumer actually bind to the producer before sending a msg
			busBindUnbindLatency();
			moduleOutputChannel.send(message);
			Message<?> inbound = moduleInputChannel.receive(2000);
			assertNotNull(inbound);
			assertArrayEquals(ratherBigPayload, (byte[]) inbound.getPayload());
			messageBus.unbindProducers("foo.0");
			messageBus.unbindConsumers("foo.0");
		}
	}

	@Test
	public void testCustomPartitionCountOverridesDefaultIfLarger() throws Exception {

		byte[] ratherBigPayload = new byte[2048];
		Arrays.fill(ratherBigPayload, (byte) 65);
		KafkaTestMessageBus messageBus = (KafkaTestMessageBus) getMessageBus();


		DirectChannel moduleOutputChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		Properties producerProperties = new Properties();
		producerProperties.put(BusProperties.MIN_PARTITION_COUNT, "10");
		Properties consumerProperties = new Properties();
		consumerProperties.put(BusProperties.MIN_PARTITION_COUNT, "10");
		long uniqueBindingId = System.currentTimeMillis();
		messageBus.bindProducer("foo" + uniqueBindingId + ".0", moduleOutputChannel, producerProperties);
		messageBus.bindConsumer("foo" + uniqueBindingId + ".0", moduleInputChannel, consumerProperties);
		Message<?> message = org.springframework.integration.support.MessageBuilder.withPayload(ratherBigPayload).build();
		// Let the consumer actually bind to the producer before sending a msg
		busBindUnbindLatency();
		moduleOutputChannel.send(message);
		Message<?> inbound = moduleInputChannel.receive(2000);
		assertNotNull(inbound);
		assertArrayEquals(ratherBigPayload, (byte[]) inbound.getPayload());
		Collection<Partition> partitions = messageBus.getCoreMessageBus().getConnectionFactory().getPartitions(
				"foo" + uniqueBindingId + ".0");
		assertThat(partitions, hasSize(10));
		messageBus.unbindProducers("foo" + uniqueBindingId + ".0");
		messageBus.unbindConsumers("foo" + uniqueBindingId + ".0");
	}

	@Test
	public void testCustomPartitionCountDoesNotOverrideModuleCountAndConcurrencyIfSmaller() throws Exception {

		byte[] ratherBigPayload = new byte[2048];
		Arrays.fill(ratherBigPayload, (byte) 65);
		KafkaTestMessageBus messageBus = (KafkaTestMessageBus) getMessageBus();


		DirectChannel moduleOutputChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		Properties producerProps = new Properties();
		producerProps.put(BusProperties.MIN_PARTITION_COUNT, "5");
		producerProps.put(BusProperties.NEXT_MODULE_CONCURRENCY, "6");
		Properties consumerProps = new Properties();
		consumerProps.put(BusProperties.MIN_PARTITION_COUNT, "5");
		consumerProps.put(BusProperties.CONCURRENCY, "6");
		long uniqueBindingId = System.currentTimeMillis();
		messageBus.bindProducer("foo" + uniqueBindingId + ".0", moduleOutputChannel, producerProps);
		messageBus.bindConsumer("foo" + uniqueBindingId + ".0", moduleInputChannel, consumerProps);
		Message<?> message = org.springframework.integration.support.MessageBuilder.withPayload(ratherBigPayload).build();
		// Let the consumer actually bind to the producer before sending a msg
		busBindUnbindLatency();
		moduleOutputChannel.send(message);
		Message<?> inbound = moduleInputChannel.receive(2000);
		assertNotNull(inbound);
		assertArrayEquals(ratherBigPayload, (byte[]) inbound.getPayload());
		Collection<Partition> partitions = messageBus.getCoreMessageBus().getConnectionFactory().getPartitions(
				"foo" + uniqueBindingId + ".0");
		assertThat(partitions, hasSize(6));
		messageBus.unbindProducers("foo" + uniqueBindingId + ".0");
		messageBus.unbindConsumers("foo" + uniqueBindingId + ".0");
	}

	@Test
	public void testCustomPartitionCountOverridesModuleCountAndConcurrencyIfLarger() throws Exception {

		byte[] ratherBigPayload = new byte[2048];
		Arrays.fill(ratherBigPayload, (byte) 65);
		KafkaTestMessageBus messageBus = (KafkaTestMessageBus) getMessageBus();

		DirectChannel moduleOutputChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		Properties producerProps = new Properties();
		producerProps.put(BusProperties.MIN_PARTITION_COUNT, "6");
		producerProps.put(BusProperties.NEXT_MODULE_CONCURRENCY, "5");
		Properties consumerProps = new Properties();
		consumerProps.put(BusProperties.MIN_PARTITION_COUNT, "6");
		consumerProps.put(BusProperties.CONCURRENCY, "5");
		long uniqueBindingId = System.currentTimeMillis();
		messageBus.bindProducer("foo" + uniqueBindingId + ".0", moduleOutputChannel, producerProps);
		messageBus.bindConsumer("foo" + uniqueBindingId + ".0", moduleInputChannel, consumerProps);
		Message<?> message = org.springframework.integration.support.MessageBuilder.withPayload(ratherBigPayload).build();
		// Let the consumer actually bind to the producer before sending a msg
		busBindUnbindLatency();
		moduleOutputChannel.send(message);
		Message<?> inbound = moduleInputChannel.receive(2000);
		assertNotNull(inbound);
		assertArrayEquals(ratherBigPayload, (byte[]) inbound.getPayload());
		Collection<Partition> partitions = messageBus.getCoreMessageBus().getConnectionFactory().getPartitions(
				"foo" + uniqueBindingId + ".0");
		assertThat(partitions, hasSize(6));
		messageBus.unbindProducers("foo" + uniqueBindingId + ".0");
		messageBus.unbindConsumers("foo" + uniqueBindingId + ".0");
	}

	@Test
	public void testCustomPartitionCountDoesNotOverridePartitioningIfSmaller() throws Exception {

		byte[] ratherBigPayload = new byte[2048];
		Arrays.fill(ratherBigPayload, (byte) 65);
		KafkaTestMessageBus messageBus = (KafkaTestMessageBus) getMessageBus();

		DirectChannel moduleOutputChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		Properties producerProperties = new Properties();
		producerProperties.put(BusProperties.MIN_PARTITION_COUNT, "3");
		producerProperties.put(BusProperties.NEXT_MODULE_COUNT, "5");
		producerProperties.put(BusProperties.PARTITION_KEY_EXPRESSION, "payload");
		Properties consumerProperties = new Properties();
		consumerProperties.put(BusProperties.MIN_PARTITION_COUNT, "3");
		long uniqueBindingId = System.currentTimeMillis();
		messageBus.bindProducer("foo" + uniqueBindingId + ".0", moduleOutputChannel, producerProperties);
		messageBus.bindConsumer("foo" + uniqueBindingId + ".0", moduleInputChannel, consumerProperties);
		Message<?> message = org.springframework.integration.support.MessageBuilder.withPayload(ratherBigPayload).build();
		// Let the consumer actually bind to the producer before sending a msg
		busBindUnbindLatency();
		moduleOutputChannel.send(message);
		Message<?> inbound = moduleInputChannel.receive(2000);
		assertNotNull(inbound);
		assertArrayEquals(ratherBigPayload, (byte[]) inbound.getPayload());
		Collection<Partition> partitions = messageBus.getCoreMessageBus().getConnectionFactory().getPartitions(
				"foo" + uniqueBindingId + ".0");
		assertThat(partitions, hasSize(5));
		messageBus.unbindProducers("foo" + uniqueBindingId + ".0");
		messageBus.unbindConsumers("foo" + uniqueBindingId + ".0");
	}

	@Test
	public void testCustomPartitionCountOverridesPartitioningIfLarger() throws Exception {

		byte[] ratherBigPayload = new byte[2048];
		Arrays.fill(ratherBigPayload, (byte) 65);
		KafkaTestMessageBus messageBus = (KafkaTestMessageBus) getMessageBus();

		DirectChannel moduleOutputChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		Properties producerProperties = new Properties();
		producerProperties.put(BusProperties.MIN_PARTITION_COUNT, "5");
		producerProperties.put(BusProperties.NEXT_MODULE_COUNT, "3");
		producerProperties.put(BusProperties.PARTITION_KEY_EXPRESSION, "payload");
		Properties consumerProperties = new Properties();
		consumerProperties.put(BusProperties.MIN_PARTITION_COUNT, "5");
		long uniqueBindingId = System.currentTimeMillis();
		messageBus.bindProducer("foo" + uniqueBindingId + ".0", moduleOutputChannel, producerProperties);
		messageBus.bindConsumer("foo" + uniqueBindingId + ".0", moduleInputChannel, consumerProperties);
		Message<?> message = org.springframework.integration.support.MessageBuilder.withPayload(ratherBigPayload).build();
		// Let the consumer actually bind to the producer before sending a msg
		busBindUnbindLatency();
		moduleOutputChannel.send(message);
		Message<?> inbound = moduleInputChannel.receive(2000);
		assertNotNull(inbound);
		assertArrayEquals(ratherBigPayload, (byte[]) inbound.getPayload());
		Collection<Partition> partitions = messageBus.getCoreMessageBus().getConnectionFactory().getPartitions(
				"foo" + uniqueBindingId + ".0");
		assertThat(partitions, hasSize(5));
		messageBus.unbindProducers("foo" + uniqueBindingId + ".0");
		messageBus.unbindConsumers("foo" + uniqueBindingId + ".0");
	}

	@Test
	public void testNamedChannelPartitionCount() throws Exception {

		byte[] ratherBigPayload = new byte[2048];
		Arrays.fill(ratherBigPayload, (byte) 65);
		KafkaTestMessageBus messageBus = (KafkaTestMessageBus) getMessageBus();

		DirectChannel moduleOutputChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		Properties producerProperties = new Properties();
		producerProperties.put(BusProperties.MIN_PARTITION_COUNT, "5");
		Properties consumerProperties = new Properties();
		consumerProperties.put(BusProperties.MIN_PARTITION_COUNT, "5");
		long uniqueBindingId = System.currentTimeMillis();
		messageBus.bindProducer("queue:foo" + uniqueBindingId + ".0", moduleOutputChannel, producerProperties);
		messageBus.bindConsumer("queue:foo" + uniqueBindingId + ".0", moduleInputChannel, consumerProperties);
		messageBus.bindProducer("topic:foo" + uniqueBindingId + ".0", moduleOutputChannel, producerProperties);
		Message<?> message = org.springframework.integration.support.MessageBuilder.withPayload(ratherBigPayload).build();
		// Let the consumer actually bind to the producer before sending a msg
		busBindUnbindLatency();
		moduleOutputChannel.send(message);
		Message<?> inbound = moduleInputChannel.receive(2000);
		assertNotNull(inbound);
		assertArrayEquals(ratherBigPayload, (byte[]) inbound.getPayload());
		Collection<Partition> partitions = messageBus.getCoreMessageBus().getConnectionFactory().getPartitions(
				"queue_3Afoo" + uniqueBindingId + ".0");
		assertThat(partitions, hasSize(5));
		partitions = messageBus.getCoreMessageBus().getConnectionFactory().getPartitions(
				"topic_3Afoo" + uniqueBindingId + ".0");
		assertThat(partitions, hasSize(5));
		messageBus.unbindProducers("queue:foo" + uniqueBindingId + ".0");
		messageBus.unbindConsumers("queue:foo" + uniqueBindingId + ".0");
		messageBus.unbindProducers("topic:foo" + uniqueBindingId + ".0");
	}

	@Test
	public void testKafkaSpecificConsumerPropertiesAreSet() {
		KafkaTestMessageBus messageBus = (KafkaTestMessageBus) getMessageBus();
		Properties consumerProperties = new Properties();
		consumerProperties.put(KafkaMessageBus.AUTO_COMMIT_OFFSET_ENABLED, "false");
		consumerProperties.put(KafkaMessageBus.FETCH_SIZE, "7");
		consumerProperties.put(KafkaMessageBus.QUEUE_SIZE, "128");
		long uniqueBindingId = System.currentTimeMillis();
		DirectChannel inputChannel = new DirectChannel();
		messageBus.bindConsumer("foo" + uniqueBindingId, inputChannel, consumerProperties);
		DirectFieldAccessor accessor = new DirectFieldAccessor(messageBus.getCoreMessageBus());
		@SuppressWarnings("unchecked")
		List<Binding> bindings = (List<Binding>) accessor.getPropertyValue("bindings");
		assertThat(bindings, hasSize(1));
		Binding consumerBinding = bindings.get(0);
		AbstractEndpoint bindingEndpoint = consumerBinding.getEndpoint();
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(bindingEndpoint);
		KafkaMessageListenerContainer messageListenerContainer = ((KafkaMessageListenerContainer) endpointAccessor.getPropertyValue("messageListenerContainer") );
		assertThat(messageListenerContainer.getQueueSize(), equalTo(128));
		assertThat(messageListenerContainer.getMaxFetch(), equalTo(7));
		assertThat(messageListenerContainer.getMessageListener(), instanceOf(AcknowledgingMessageListener.class));
		messageBus.unbindConsumers("foo" + uniqueBindingId);
	}

	@Test
	public void testKafkaSpecificProducerPropertiesAreSet() {
		KafkaTestMessageBus messageBus = (KafkaTestMessageBus) getMessageBus();
		Properties producerProperties = new Properties();
		producerProperties.put(BusProperties.BATCH_SIZE, "34");
		producerProperties.put(BusProperties.BATCH_TIMEOUT, "7");
		long uniqueBindingId = System.currentTimeMillis();
		DirectChannel inputChannel = new DirectChannel();
		messageBus.bindProducer("foo" + uniqueBindingId, inputChannel, producerProperties);
		DirectFieldAccessor accessor = new DirectFieldAccessor(messageBus.getCoreMessageBus());
		@SuppressWarnings("unchecked")
		List<Binding> bindings = (List<Binding>) accessor.getPropertyValue("bindings");
		assertThat(bindings, hasSize(1));
		Binding producerBinding = bindings.get(0);
		AbstractEndpoint bindingEndpoint = producerBinding.getEndpoint();
		DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(bindingEndpoint);
		MessageHandler messageHandler = ((MessageHandler) endpointAccessor.getPropertyValue("handler") );
		DirectFieldAccessor messageHandlerAccessor = new DirectFieldAccessor(messageHandler);
		ProducerConfiguration<?,?> producerConfiguration = (ProducerConfiguration<?, ?>) messageHandlerAccessor.getPropertyValue("producerConfiguration");
		DirectFieldAccessor producerConfigurationAccessor = new DirectFieldAccessor(producerConfiguration);
		@SuppressWarnings("rawtypes")
		KafkaProducer producer = (KafkaProducer) producerConfigurationAccessor.getPropertyValue("producer");
		DirectFieldAccessor producerAccessor = new DirectFieldAccessor(producer);
		ProducerConfig producerConfig = (ProducerConfig)producerAccessor.getPropertyValue("producerConfig");
		assertThat(producerConfig.getInt(ProducerConfig.BATCH_SIZE_CONFIG), equalTo(34));
		assertThat(producerConfig.getLong(ProducerConfig.LINGER_MS_CONFIG), equalTo(7L));
		messageBus.unbindProducers("foo" + uniqueBindingId);
	}

	@Test
	public void testMoreHeaders() throws Exception {
		KafkaTestMessageBus bus =
				new KafkaTestMessageBus(kafkaTestSupport, getCodec(), KafkaMessageBus.Mode.embeddedHeaders,
						"propagatedHeader");
		Collection<String> headers = Arrays.asList(TestUtils.getPropertyValue(bus.getCoreMessageBus(), "headersToMap",
				String[].class));
		assertTrue(headers.contains("propagatedHeader"));
		assertEquals(XdHeaders.STANDARD_HEADERS.length + 1, headers.size());
		DirectChannel moduleOutputChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		long uniqueBindingId = System.currentTimeMillis();
		Properties emptyProperties = new Properties();
		bus.bindProducer("foo" + uniqueBindingId + ".0", moduleOutputChannel, emptyProperties);
		bus.bindConsumer("foo" + uniqueBindingId + ".0", moduleInputChannel, emptyProperties);
		Message<?> message = org.springframework.integration.support.MessageBuilder.
				withPayload("payload").setHeader("propagatedHeader","propagatedValue").build();
		// Let the consumer actually bind to the producer before sending a msg
		busBindUnbindLatency();
		moduleOutputChannel.send(message);
		Message<?> inbound = moduleInputChannel.receive(2000);
		assertThat(inbound.getHeaders(), IsMapContaining.hasKey("propagatedHeader"));
		assertThat((String)inbound.getHeaders().get("propagatedHeader"), equalTo("propagatedValue"));
		bus.unbindProducers("foo" + uniqueBindingId + ".0");
		bus.unbindConsumers("foo" + uniqueBindingId + ".0");
		bus.cleanup();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testSendAndReceivePubSubWithMultipleConsumers() throws Exception {
		MessageBus messageBus = getMessageBus();
		DirectChannel graultUpstreamModuleOutputChannel = new DirectChannel();
		// Test pub/sub by emulating how StreamPlugin handles taps
		DirectChannel tapChannel = new DirectChannel();
		QueueChannel graultDownstreamModuleInputChannel = new QueueChannel();
		QueueChannel fooTapDownstreamInputChannel = new QueueChannel();
		QueueChannel barTapDowstreamInput1Channel = new QueueChannel();
		QueueChannel barTapDowstreamInput2Channel = new QueueChannel();
		String originalTopic = "grault.0";
		messageBus.bindProducer(originalTopic, graultUpstreamModuleOutputChannel, null);
		messageBus.bindConsumer(originalTopic, graultDownstreamModuleInputChannel, null);
		graultUpstreamModuleOutputChannel.addInterceptor(new WireTap(tapChannel));
		Properties graultTapProducerProperties = new Properties();
		// set the partition count to two, so that the tap has two partitions as well
		// this will be necessary to robin messages across the competing consumers
		graultTapProducerProperties.setProperty(BusProperties.MIN_PARTITION_COUNT, "2");
		messageBus.bindPubSubProducer("tap:grault.http", tapChannel, graultTapProducerProperties);
		// A new module is using the tap as an input channel
		String fooTapName = messageBus.isCapable(MessageBus.Capability.DURABLE_PUBSUB) ? "foo.tap:grault.http" : "tap:grault.http";
		messageBus.bindPubSubConsumer(fooTapName, fooTapDownstreamInputChannel, null);
		// Another new module is using tap as an input channel
		String barTapName = messageBus.isCapable(MessageBus.Capability.DURABLE_PUBSUB) ? "bar.tap:grault.http" : "tap:grault.http";
		Properties barTap1Properties = new Properties();
		barTap1Properties.setProperty(BusProperties.COUNT, "2");
		barTap1Properties.setProperty(BusProperties.SEQUENCE, "1");
		messageBus.bindPubSubConsumer(barTapName, barTapDowstreamInput1Channel, barTap1Properties);
		Properties barTap2Properties = new Properties();
		barTap2Properties.setProperty(BusProperties.COUNT, "2");
		barTap2Properties.setProperty(BusProperties.SEQUENCE, "2");
		messageBus.bindPubSubConsumer(barTapName, barTapDowstreamInput2Channel, barTap2Properties);
		Message<?> message1 = MessageBuilder.withPayload("foo1").setHeader(MessageHeaders.CONTENT_TYPE, "foo/bar")
				.build();
		Message<?> message2 = MessageBuilder.withPayload("foo2").setHeader(MessageHeaders.CONTENT_TYPE, "foo/bar")
				.build();

		graultUpstreamModuleOutputChannel.send(message1);
		graultUpstreamModuleOutputChannel.send(message2);
		Message<?> graultDownstreamInbound = graultDownstreamModuleInputChannel.receive(5000);
		assertNotNull(graultDownstreamInbound);
		assertEquals("foo1", graultDownstreamInbound.getPayload());
		assertNull(graultDownstreamInbound.getHeaders().get(XdHeaders.XD_ORIGINAL_CONTENT_TYPE));
		assertEquals("foo/bar", graultDownstreamInbound.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		graultDownstreamInbound = graultDownstreamModuleInputChannel.receive(5000);
		assertNotNull(graultDownstreamInbound);
		assertEquals("foo2", graultDownstreamInbound.getPayload());
		assertNull(graultDownstreamInbound.getHeaders().get(XdHeaders.XD_ORIGINAL_CONTENT_TYPE));
		assertEquals("foo/bar", graultDownstreamInbound.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		List<Message<?>> tappedFoo = new ArrayList<>();
		tappedFoo.add(fooTapDownstreamInputChannel.receive(5000));
		tappedFoo.add(fooTapDownstreamInputChannel.receive(5000));
		Message<?> tappedBar1 = barTapDowstreamInput1Channel.receive(5000);
		Message<?> tappedBar2 = barTapDowstreamInput2Channel.receive(5000);
		assertThat(tappedFoo,
				CoreMatchers.<Message<?>>hasItems(hasProperty("payload", equalTo("foo1")),
						hasProperty("payload", equalTo("foo2"))));
		assertThat(tappedFoo,
				everyItem(HasPropertyWithValue.<Message<?>>hasProperty("headers",
						hasEntry(XdHeaders.XD_ORIGINAL_CONTENT_TYPE, null))));
		assertThat(tappedFoo,
				everyItem(HasPropertyWithValue.<Message<?>>hasProperty("headers",
						hasEntry(MessageHeaders.CONTENT_TYPE, "foo/bar"))));
		assertEquals("foo1", tappedBar1.getPayload());
		assertNull(tappedBar1.getHeaders().get(XdHeaders.XD_ORIGINAL_CONTENT_TYPE));
		assertEquals("foo/bar", tappedBar1.getHeaders().get(MessageHeaders.CONTENT_TYPE));
		assertEquals("foo2", tappedBar2.getPayload());
		assertNull(tappedBar2.getHeaders().get(XdHeaders.XD_ORIGINAL_CONTENT_TYPE));
		assertEquals("foo/bar", tappedBar2.getHeaders().get(MessageHeaders.CONTENT_TYPE));

		messageBus.unbindConsumers(fooTapName);
		messageBus.unbindConsumers(originalTopic);
		messageBus.unbindProducers(originalTopic);
		messageBus.unbindProducers("tap:grault.http");
		messageBus.unbindConsumers(barTapName);
		assertTrue(getBindings(messageBus).isEmpty());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNativeOffsetManagementEnabled() {
		KafkaTestMessageBus bus =
				new KafkaTestMessageBus(kafkaTestSupport, getCodec(),
						KafkaMessageBus.OffsetManagement.kafkaNative);
		DirectChannel moduleOutputChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		long uniqueBindingId = System.currentTimeMillis();
		bus.bindProducer("foo" + uniqueBindingId + ".0", moduleOutputChannel, null);
		bus.bindConsumer("foo" + uniqueBindingId + ".0", moduleInputChannel, null);
		Collection<Binding> bindings = (Collection<Binding>) getBindings(bus);
		assertThat(bindings.size(),equalTo(2));
		for (Binding binding : bindings) {
			if ("consumer".equals(binding.getType())) {
				AbstractEndpoint endpoint = binding.getEndpoint();
				DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(endpoint);
				Object messageListenerContainer = endpointAccessor.getPropertyValue("messageListenerContainer");
				DirectFieldAccessor containerAccessor = new DirectFieldAccessor(messageListenerContainer);
				Object wrapperOffsetManager = containerAccessor.getPropertyValue("offsetManager");
				DirectFieldAccessor offsetManagerAccessor = new DirectFieldAccessor(wrapperOffsetManager);
				Object delegateOffsetManager = offsetManagerAccessor.getPropertyValue("delegate");
				assertThat(delegateOffsetManager, instanceOf(KafkaNativeOffsetManager.class));
			}
		}
		bus.unbindProducers("foo" + uniqueBindingId + ".0");
		bus.unbindConsumers("foo" + uniqueBindingId + ".0");
		bus.cleanup();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testTopicOffsetManagementEnabled() {
		KafkaTestMessageBus bus =
				new KafkaTestMessageBus(kafkaTestSupport, getCodec(),
						KafkaMessageBus.OffsetManagement.kafkaTopic);
		DirectChannel moduleOutputChannel = new DirectChannel();
		QueueChannel moduleInputChannel = new QueueChannel();
		long uniqueBindingId = System.currentTimeMillis();
		bus.bindProducer("foo" + uniqueBindingId + ".0", moduleOutputChannel, null);
		bus.bindConsumer("foo" + uniqueBindingId + ".0", moduleInputChannel, null);
		Collection<Binding> bindings = (Collection<Binding>) getBindings(bus);
		assertThat(bindings.size(),equalTo(2));
		for (Binding binding : bindings) {
			if ("consumer".equals(binding.getType())) {
				AbstractEndpoint endpoint = binding.getEndpoint();
				DirectFieldAccessor endpointAccessor = new DirectFieldAccessor(endpoint);
				Object messageListenerContainer = endpointAccessor.getPropertyValue("messageListenerContainer");
				DirectFieldAccessor containerAccessor = new DirectFieldAccessor(messageListenerContainer);
				Object wrapperOffsetManager = containerAccessor.getPropertyValue("offsetManager");
				DirectFieldAccessor offsetManagerAccessor = new DirectFieldAccessor(wrapperOffsetManager);
				Object delegateOffsetManager = offsetManagerAccessor.getPropertyValue("delegate");
				assertThat(delegateOffsetManager, instanceOf(KafkaTopicOffsetManager.class));
			}
		}
		bus.unbindProducers("foo" + uniqueBindingId + ".0");
		bus.unbindConsumers("foo" + uniqueBindingId + ".0");
		bus.cleanup();
	}

	@Test
	@Ignore("Kafka message bus does not support direct binding")
	@Override
	public void testDirectBinding() throws Exception {
	}
}
