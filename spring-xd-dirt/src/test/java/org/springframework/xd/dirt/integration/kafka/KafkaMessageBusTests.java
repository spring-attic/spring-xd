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

package org.springframework.xd.dirt.integration.kafka;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.serializer.Decoder;
import kafka.serializer.DefaultDecoder;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.xd.dirt.integration.bus.EmbeddedHeadersMessageConverter;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.integration.bus.MessageBusSupport;
import org.springframework.xd.dirt.integration.bus.PartitionCapableBusTests;


/**
 * Integration tests for the {@link KafkaMessageBus}.
 *
 * @author Eric Bottard
 */
public class KafkaMessageBusTests extends PartitionCapableBusTests {

	private final EmbeddedHeadersMessageConverter embeddedHeadersMessageConverter = new EmbeddedHeadersMessageConverter();

	private static ExecutorService executorService = Executors.newCachedThreadPool();

	private KafkaTestMessageBus messageBus;

	@Override
	protected void busBindUnbindLatency() throws InterruptedException {
		Thread.sleep(500);
	}

	@Override
	protected MessageBus getMessageBus() {
		if (messageBus == null) {
			messageBus = new KafkaTestMessageBus(getCodec());
		}
		return messageBus;
	}

	@Override
	protected boolean usesExplicitRouting() {
		return false;
	}

	@Override
	public Spy spyOn(final String name) {
		String topic = KafkaMessageBus.escapeTopicName(name);

		Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
		int numThreads = 1;
		topicCountMap.put(topic, numThreads);


		Decoder<byte[]> valueDecoder = new DefaultDecoder(null);
		Decoder<Integer> keyDecoder = new IntegerEncoderDecoder();

		KafkaTestMessageBus busWrapper = (KafkaTestMessageBus) getMessageBus();
		// Rewind offset, as tests will have typically already sent the messages we're trying to consume
		ConsumerConnector connector = busWrapper.getCoreMessageBus().createConsumerConnector(
				UUID.randomUUID().toString(), "auto.offset.reset", "smallest");

		Map<String, List<KafkaStream<Integer, byte[]>>> map = connector.createMessageStreams(
				topicCountMap, keyDecoder, valueDecoder);

		final ConsumerIterator<Integer, byte[]> iterator = map.get(topic).iterator().next().iterator();


		return new Spy() {

			@Override
			public Object receive(boolean expectNull) throws Exception {
				final Future<String> submit = executorService.submit(new Callable<String>() {

					@Override
					public String call() throws Exception {
						iterator.hasNext();
						byte[] raw = iterator.next().message();
						Message<byte[]> theRequestMessage = embeddedHeadersMessageConverter.extractHeaders(MessageBuilder.withPayload(
								raw).build());

						return new String(theRequestMessage.getPayload(), "UTF-8");
					}

				});
				try {
					return submit.get(expectNull ? 50 : 5000, TimeUnit.MILLISECONDS);
				}
				catch (TimeoutException e) {
					return null;
				}
			}
		};

	}

	@Test
	public void testCompression() throws Exception {
		final String[] codecs = new String[] {null, "default", "gzip", "snappy"};

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
	@Ignore("XD-2293 Revisit later")
	public void testPartitionedModuleSpEL() throws Exception {

	}

	@Test
	@Ignore("XD-2293 Revisit later")
	public void testPartitionedModuleJava() throws Exception {

	}



	}
