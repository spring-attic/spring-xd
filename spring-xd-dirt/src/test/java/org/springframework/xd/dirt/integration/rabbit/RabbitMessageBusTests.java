/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.integration.rabbit;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.aopalliance.aop.Advice;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.utils.test.TestUtils;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.xd.dirt.integration.bus.AbstractMessageBusTests;
import org.springframework.xd.dirt.integration.bus.Binding;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.integration.bus.RabbitTestMessageBus;
import org.springframework.xd.test.rabbit.RabbitTestSupport;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class RabbitMessageBusTests extends AbstractMessageBusTests {

	@Rule
	public RabbitTestSupport rabbitAvailableRule = new RabbitTestSupport();

	@Override
	protected MessageBus getMessageBus() {
		if (testMessageBus == null) {
			testMessageBus = new RabbitTestMessageBus(rabbitAvailableRule.getResource(), getCodec());
		}
		return testMessageBus;
	}

	@Test
	public void testSendAndReceiveBad() throws Exception {
		MessageBus messageBus = getMessageBus();
		DirectChannel moduleOutputChannel = new DirectChannel();
		DirectChannel moduleInputChannel = new DirectChannel();
		messageBus.bindProducer("bad.0", moduleOutputChannel, null);
		messageBus.bindConsumer("bad.0", moduleInputChannel, null);
		Message<?> message = MessageBuilder.withPayload("bad").setHeader(MessageHeaders.CONTENT_TYPE, "foo/bar").build();
		final CountDownLatch latch = new CountDownLatch(3);
		moduleInputChannel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				latch.countDown();
				throw new RuntimeException("bad");
			}
		});
		moduleOutputChannel.send(message);
		assertTrue(latch.await(10, TimeUnit.SECONDS));
	}

	@Test
	public void testConsumerProperties() throws Exception {
		MessageBus bus = getMessageBus();
		Properties properties = new Properties();
		properties.put("transacted", "true"); // test transacted with defaults; not allowed with ackmode NONE
		bus.bindConsumer("props.0", new DirectChannel(), properties);
		@SuppressWarnings("unchecked")
		List<Binding> bindings = TestUtils.getPropertyValue(bus, "messageBus.bindings", List.class);
		assertEquals(1, bindings.size());
		AbstractEndpoint endpoint = bindings.get(0).getEndpoint();
		SimpleMessageListenerContainer container = TestUtils.getPropertyValue(endpoint, "messageListenerContainer",
				SimpleMessageListenerContainer.class);
		assertEquals(AcknowledgeMode.AUTO, container.getAcknowledgeMode());
		assertEquals("xdbus.props.0", container.getQueueNames()[0]);
		assertTrue(TestUtils.getPropertyValue(container, "transactional", Boolean.class));
		assertEquals(1, TestUtils.getPropertyValue(container, "concurrentConsumers"));
		assertEquals(1, TestUtils.getPropertyValue(container, "maxConcurrentConsumers"));
		assertTrue(TestUtils.getPropertyValue(container, "defaultRequeueRejected", Boolean.class));
		assertEquals(1, TestUtils.getPropertyValue(container, "prefetchCount"));
		assertEquals(1, TestUtils.getPropertyValue(container, "txSize"));
		Advice retry = TestUtils.getPropertyValue(container, "adviceChain", Advice[].class)[0];
		assertEquals(3, TestUtils.getPropertyValue(retry, "retryOperations.retryPolicy.maxAttempts"));
		assertEquals(1000L, TestUtils.getPropertyValue(retry, "retryOperations.backOffPolicy.initialInterval"));
		assertEquals(10000L, TestUtils.getPropertyValue(retry, "retryOperations.backOffPolicy.maxInterval"));
		assertEquals(2.0, TestUtils.getPropertyValue(retry, "retryOperations.backOffPolicy.multiplier"));
		bus.unbindConsumers("props.0");
		assertEquals(0, bindings.size());

		properties = new Properties();
		properties.put("ackMode", "NONE");
		properties.put("backOffInitialInterval", "2000");
		properties.put("backOffMaxInterval", "20000");
		properties.put("backOffMultiplier", "5.0");
		properties.put("concurrency", "2");
		properties.put("maxAttempts", "23");
		properties.put("maxConcurrency", "3");
		properties.put("prefix", "foo.");
		properties.put("prefetch", "20");
		properties.put("requestHeaderPatterns", "foo");
		properties.put("requeue", "false");
		properties.put("txSize", "10");
		bus.bindConsumer("props.0", new DirectChannel(), properties);

		@SuppressWarnings("unchecked")
		List<Binding> bindingsNow = TestUtils.getPropertyValue(bus, "messageBus.bindings", List.class);
		assertEquals(1, bindings.size());
		endpoint = bindingsNow.get(0).getEndpoint();
		container = TestUtils.getPropertyValue(endpoint, "messageListenerContainer",
				SimpleMessageListenerContainer.class);
		assertEquals(AcknowledgeMode.NONE, container.getAcknowledgeMode());
		assertEquals("foo.props.0", container.getQueueNames()[0]);
		assertFalse(TestUtils.getPropertyValue(container, "transactional", Boolean.class));
		assertEquals(2, TestUtils.getPropertyValue(container, "concurrentConsumers"));
		assertEquals(3, TestUtils.getPropertyValue(container, "maxConcurrentConsumers"));
		assertFalse(TestUtils.getPropertyValue(container, "defaultRequeueRejected", Boolean.class));
		assertEquals(20, TestUtils.getPropertyValue(container, "prefetchCount"));
		assertEquals(10, TestUtils.getPropertyValue(container, "txSize"));
		retry = TestUtils.getPropertyValue(container, "adviceChain", Advice[].class)[0];
		assertEquals(23, TestUtils.getPropertyValue(retry, "retryOperations.retryPolicy.maxAttempts"));
		assertEquals(2000L, TestUtils.getPropertyValue(retry, "retryOperations.backOffPolicy.initialInterval"));
		assertEquals(20000L, TestUtils.getPropertyValue(retry, "retryOperations.backOffPolicy.maxInterval"));
		assertEquals(5.0, TestUtils.getPropertyValue(retry, "retryOperations.backOffPolicy.multiplier"));
		bus.unbindConsumers("props.0");
		assertEquals(0, bindingsNow.size());
	}

	@Test
	public void testProducerProperties() throws Exception {
		MessageBus bus = getMessageBus();
		bus.bindProducer("props.0", new DirectChannel(), null);
		@SuppressWarnings("unchecked")
		List<Binding> bindings = TestUtils.getPropertyValue(bus, "messageBus.bindings", List.class);
		assertEquals(1, bindings.size());
		AbstractEndpoint endpoint = bindings.get(0).getEndpoint();
		assertEquals("xdbus.props.0", TestUtils.getPropertyValue(endpoint, "handler.delegate.routingKey"));
		MessageDeliveryMode mode = TestUtils.getPropertyValue(endpoint, "handler.delegate.defaultDeliveryMode",
				MessageDeliveryMode.class);
		assertEquals(MessageDeliveryMode.PERSISTENT, mode);
		List<?> requestHeaders = TestUtils.getPropertyValue(endpoint,
				"handler.delegate.headerMapper.requestHeaderNames", List.class);
		assertEquals(2, requestHeaders.size());
		bus.unbindProducers("props.0");
		assertEquals(0, bindings.size());

		Properties properties = new Properties();
		properties.put("prefix", "foo.");
		properties.put("deliveryMode", "NON_PERSISTENT");
		properties.put("requestHeaderPatterns", "foo");
		bus.bindProducer("props.0", new DirectChannel(), properties);
		assertEquals(1, bindings.size());
		endpoint = bindings.get(0).getEndpoint();
		assertEquals("foo.props.0", TestUtils.getPropertyValue(endpoint, "handler.delegate.routingKey"));
		mode = TestUtils.getPropertyValue(endpoint, "handler.delegate.defaultDeliveryMode",
				MessageDeliveryMode.class);
		assertEquals(MessageDeliveryMode.NON_PERSISTENT, mode);
		requestHeaders = TestUtils.getPropertyValue(endpoint, "handler.delegate.headerMapper.requestHeaderNames",
				List.class);
		assertEquals(1, requestHeaders.size());
		assertEquals("foo", requestHeaders.get(0));
		bus.unbindProducers("props.0");
		assertEquals(0, bindings.size());
	}

	@Test
	public void testBadProperties() {
		MessageBus bus = getMessageBus();
		Properties properties = new Properties();
		properties.put("foo", "bar");
		properties.put("baz", "qux");

		DirectChannel output = new DirectChannel();
		try {
			bus.bindProducer("badprops.0", output, properties);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), equalTo("RabbitMessageBus does not support properties: baz,foo"));
		}

		properties.remove("baz");
		try {
			bus.bindConsumer("badprops.0", output, properties);
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), equalTo("RabbitMessageBus does not support property: foo"));
		}
	}

	@Test
	public void testPartitionedModuleSpEL() {
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
		assertEquals("'xdbus.part.0-' + headers['partition']",
				TestUtils.getPropertyValue(endpoint, "handler.delegate.routingKeyExpression"));

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
	public void testPartitionedModuleJava() {
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
		assertEquals("'xdbus.part.0-' + headers['partition']",
				TestUtils.getPropertyValue(endpoint, "handler.delegate.routingKeyExpression"));

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

}
