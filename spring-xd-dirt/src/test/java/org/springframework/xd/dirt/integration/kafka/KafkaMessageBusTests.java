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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.xd.dirt.integration.bus.EmbeddedHeadersMessageConverter;
import org.springframework.xd.dirt.integration.bus.MessageBus;
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
	protected MessageBus getMessageBus() throws Exception {
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
	protected Object receive(String name, boolean expectNull) throws Exception {
		Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
		int numThreads = 1;
		String topic = KafkaMessageBus.escapeTopicName(name);
		topicCountMap.put(topic, numThreads);


		Decoder<byte[]> valueDecoder = new DefaultDecoder(null);
		Decoder<Integer> keyDecoder = new IntegerEncoderDecoder();

		KafkaTestMessageBus busWrapper = (KafkaTestMessageBus) getMessageBus();
		ConsumerConnector connector = busWrapper.getCoreMessageBus().createConsumerConnector(
				UUID.randomUUID().toString());

		Map<String, List<KafkaStream<Integer, byte[]>>> consumerMap = connector.createMessageStreams(
				topicCountMap, keyDecoder, valueDecoder);

		final KafkaStream<Integer, byte[]> stream = consumerMap.get(topic).iterator().next();

		Future<String> submit = executorService.submit(new Callable<String>() {

			@Override
			public String call() throws Exception {
				ConsumerIterator<Integer, byte[]> iterator = stream.iterator();
				iterator.hasNext();
				byte[] raw = iterator.next().message();
				Message<byte[]> theRequestMessage = embeddedHeadersMessageConverter.extractHeaders(MessageBuilder.withPayload(
						raw).build());

				return new String(theRequestMessage.getPayload(), "UTF-8");
			}

		});

		try {
			return submit.get(expectNull ? 5000 : 5000, TimeUnit.MILLISECONDS);
		}
		catch (TimeoutException e) {
			return null;
		}


	}





}
