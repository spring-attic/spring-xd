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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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
import org.hamcrest.collection.IsMapContaining;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.kafka.core.KafkaMessage;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.listener.AcknowledgingMessageListener;
import org.springframework.integration.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.integration.kafka.listener.MessageListener;
import org.springframework.integration.kafka.support.ProducerConfiguration;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.xd.dirt.integration.bus.Binding;
import org.springframework.xd.dirt.integration.bus.BusProperties;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.integration.bus.PartitionCapableBusTests;
import org.springframework.xd.dirt.integration.bus.XdHeaders;
import org.springframework.xd.dirt.integration.kafka.KafkaMessageBus;
import org.springframework.xd.test.kafka.KafkaTestSupport;

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
				new Properties(), UUID.randomUUID().toString(), 1, topic, OffsetRequest.EarliestTime());

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
	@Ignore("Kafka message bus does not support direct binding")
	@Override
	public void testDirectBinding() throws Exception {
	}
}
