/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.integration.redis;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.MediaType;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.redis.inbound.RedisInboundChannelAdapter;
import org.springframework.integration.redis.inbound.RedisQueueMessageDrivenEndpoint;
import org.springframework.integration.redis.outbound.RedisPublishingMessageHandler;
import org.springframework.integration.redis.outbound.RedisQueueOutboundChannelAdapter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.integration.bus.AbstractBusPropertiesAccessor;
import org.springframework.xd.dirt.integration.bus.Binding;
import org.springframework.xd.dirt.integration.bus.BusProperties;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.integration.bus.MessageBusSupport;
import org.springframework.xd.dirt.integration.bus.serializer.MultiTypeCodec;

/**
 * A {@link MessageBus} implementation backed by Redis.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author David Turanski
 * @author Jennifer Hickey
 */
public class RedisMessageBus extends MessageBusSupport implements DisposableBean {

	private static final String REPLY_TO = "replyTo";

	private static final SpelExpressionParser parser = new SpelExpressionParser();

	private RedisConnectionFactory connectionFactory;

	private final EmbeddedHeadersMessageConverter embeddedHeadersMessageConverter = new EmbeddedHeadersMessageConverter();

	private static final Set<Object> SUPPORTED_CONSUMER_PROPERTIES = new HashSet<Object>(Arrays.asList(new String[] {
		BusProperties.CONCURRENCY,
		BusProperties.PARTITION_INDEX
	}));

	private static final Set<Object> SUPPORTED_PUBSUB_CONSUMER_PROPERTIES = new HashSet<Object>(
			Arrays.asList(new String[] {
				BusProperties.PARTITION_INDEX
			}));

	private static final Set<Object> SUPPORTED_PRODUCER_PROPERTIES = new HashSet<Object>(Arrays.asList(new String[] {
		BusProperties.PARTITION_COUNT,
		BusProperties.PARTITION_KEY_EXPRESSION,
		BusProperties.PARTITION_KEY_EXTRACTOR_CLASS,
		BusProperties.PARTITION_SELECTOR_CLASS,
		BusProperties.PARTITION_SELECTOR_EXPRESSION
	}));

	public RedisMessageBus(RedisConnectionFactory connectionFactory, MultiTypeCodec<Object> codec) {
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		Assert.notNull(codec, "codec must not be null");
		this.connectionFactory = connectionFactory;
		setCodec(codec);
	}

	@Override
	public void bindConsumer(final String name, MessageChannel moduleInputChannel, Properties properties) {
		validateConsumerProperties(properties, SUPPORTED_CONSUMER_PROPERTIES);
		RedisPropertiesAccessor accessor = new RedisPropertiesAccessor(properties);
		String queueName = "queue." + name;
		int partitionIndex = accessor.getPartitionIndex();
		if (partitionIndex >= 0) {
			queueName += "-" + partitionIndex;
		}
		MessageProducerSupport adapter;
		int concurrency = accessor.getConcurrency(this.defaultConcurrentConsumers);
		concurrency = concurrency > 0 ? concurrency : 1;
		if (concurrency == 1) {
			RedisQueueMessageDrivenEndpoint single = new RedisQueueMessageDrivenEndpoint(queueName,
					this.connectionFactory);
			single.setBeanFactory(this.getBeanFactory());
			single.setSerializer(null);
			adapter = single;
		}
		else {
			adapter = new CompositeRedisQueueMessageDrivenEndpoint(queueName, concurrency);
		}
		registerNamedChannelForConsumerIfNecessary(name, false);
		doRegisterConsumer(name, moduleInputChannel, adapter);
	}

	@Override
	public void bindPubSubConsumer(final String name, MessageChannel moduleInputChannel,
			Properties properties) {
		if (logger.isInfoEnabled()) {
			logger.info("declaring pubsub for inbound: " + name);
		}
		validateConsumerProperties(properties, SUPPORTED_PUBSUB_CONSUMER_PROPERTIES);
		registerNamedChannelForConsumerIfNecessary(name, true);
		RedisInboundChannelAdapter adapter = new RedisInboundChannelAdapter(this.connectionFactory);
		adapter.setBeanFactory(this.getBeanFactory());
		adapter.setSerializer(null);
		String topicName = "topic." + name;
		RedisPropertiesAccessor accessor = new RedisPropertiesAccessor(properties);
		int partitionIndex = accessor.getPartitionIndex();
		if (partitionIndex >= 0) {
			topicName += "-" + partitionIndex;
		}
		adapter.setTopics(topicName);
		doRegisterConsumer(name, moduleInputChannel, adapter);
	}

	private void doRegisterConsumer(String name, MessageChannel moduleInputChannel, MessageProducerSupport adapter) {
		DirectChannel bridgeToModuleChannel = new DirectChannel();
		bridgeToModuleChannel.setBeanFactory(this.getBeanFactory());
		bridgeToModuleChannel.setBeanName(name + ".bridge");
		adapter.setOutputChannel(bridgeToModuleChannel);
		adapter.setBeanName("inbound." + name);
		adapter.afterPropertiesSet();
		Binding consumerBinding = Binding.forConsumer(adapter, moduleInputChannel);
		addBinding(consumerBinding);
		ReceivingHandler convertingBridge = new ReceivingHandler();
		convertingBridge.setOutputChannel(moduleInputChannel);
		convertingBridge.setBeanName(name + ".convert.bridge");
		convertingBridge.afterPropertiesSet();
		bridgeToModuleChannel.subscribe(convertingBridge);
		consumerBinding.start();
	}

	@Override
	public void bindProducer(final String name, MessageChannel moduleOutputChannel,
			Properties properties) {
		Assert.isInstanceOf(SubscribableChannel.class, moduleOutputChannel);
		validateProducerProperties(properties, SUPPORTED_PRODUCER_PROPERTIES);
		RedisPropertiesAccessor accessor = new RedisPropertiesAccessor(properties);
		String partitionKeyExtractorClass = accessor.getPartitionKeyExtractorClass();
		Expression partitionKeyExpression = accessor.getPartitionKeyExpression();
		RedisQueueOutboundChannelAdapter queue;
		String queueName = "queue." + name;
		if (partitionKeyExpression == null && !StringUtils.hasText(partitionKeyExtractorClass)) {
			queue = new RedisQueueOutboundChannelAdapter(queueName, this.connectionFactory);
		}
		else {
			queue = new RedisQueueOutboundChannelAdapter(
					parser.parseExpression(buildPartitionRoutingExpression(queueName)), this.connectionFactory);
		}
		queue.setIntegrationEvaluationContext(this.evaluationContext);
		queue.setBeanFactory(this.getBeanFactory());
		queue.afterPropertiesSet();
		doRegisterProducer(name, moduleOutputChannel, queue, accessor);
	}

	@Override
	public void bindPubSubProducer(final String name, MessageChannel moduleOutputChannel,
			Properties properties) {
		validateProducerProperties(properties, SUPPORTED_PRODUCER_PROPERTIES);
		RedisPropertiesAccessor accessor = new RedisPropertiesAccessor(properties);
		String partitionKeyExtractorClass = accessor.getPartitionKeyExtractorClass();
		Expression partitionKeyExpression = accessor.getPartitionKeyExpression();
		RedisPublishingMessageHandler topic = new RedisPublishingMessageHandler(connectionFactory);
		topic.setBeanFactory(this.getBeanFactory());
		if (partitionKeyExpression == null && !StringUtils.hasText(partitionKeyExtractorClass)) {
			topic.setTopic("topic." + name);
		}
		else {
			topic.setTopicExpression(parser.parseExpression(buildPartitionRoutingExpression("topic." + name)));
		}
		topic.setIntegrationEvaluationContext(this.evaluationContext);
		topic.afterPropertiesSet();
		doRegisterProducer(name, moduleOutputChannel, topic, accessor);
	}

	private void doRegisterProducer(final String name, MessageChannel moduleOutputChannel, MessageHandler delegate,
			RedisPropertiesAccessor properties) {
		this.doRegisterProducer(name, moduleOutputChannel, delegate, null, properties);
	}

	private void doRegisterProducer(final String name, MessageChannel moduleOutputChannel, MessageHandler delegate,
			String replyTo, RedisPropertiesAccessor properties) {
		Assert.isInstanceOf(SubscribableChannel.class, moduleOutputChannel);
		MessageHandler handler = new SendingHandler(delegate, replyTo, properties);
		EventDrivenConsumer consumer = new EventDrivenConsumer((SubscribableChannel) moduleOutputChannel, handler);
		consumer.setBeanFactory(this.getBeanFactory());
		consumer.setBeanName("outbound." + name);
		consumer.afterPropertiesSet();
		Binding producerBinding = Binding.forProducer(moduleOutputChannel, consumer);
		addBinding(producerBinding);
		producerBinding.start();
	}

	@Override
	public void bindRequestor(String name, MessageChannel requests, MessageChannel replies,
			Properties properties) {
		if (logger.isInfoEnabled()) {
			logger.info("binding requestor: " + name);
		}
		Assert.isInstanceOf(SubscribableChannel.class, requests);
		validateProducerProperties(properties, SUPPORTED_PRODUCER_PROPERTIES);
		RedisQueueOutboundChannelAdapter queue = new RedisQueueOutboundChannelAdapter("queue." + name + ".requests",
				this.connectionFactory);
		queue.setBeanFactory(this.getBeanFactory());
		queue.afterPropertiesSet();
		String replyQueueName = name + ".replies." + this.getIdGenerator().generateId();
		this.doRegisterProducer(name, requests, queue, replyQueueName, new RedisPropertiesAccessor(properties));
		RedisQueueMessageDrivenEndpoint adapter = new RedisQueueMessageDrivenEndpoint(
				replyQueueName, this.connectionFactory);
		adapter.setSerializer(null);
		this.doRegisterConsumer(name, replies, adapter);
	}

	@Override
	public void bindReplier(String name, MessageChannel requests, MessageChannel replies,
			Properties properties) {
		if (logger.isInfoEnabled()) {
			logger.info("binding replier: " + name);
		}
		validateConsumerProperties(properties, SUPPORTED_CONSUMER_PROPERTIES);
		RedisQueueMessageDrivenEndpoint adapter = new RedisQueueMessageDrivenEndpoint(
				"queue." + name + ".requests",
				this.connectionFactory);
		adapter.setBeanFactory(this.getBeanFactory());
		adapter.setSerializer(null);
		this.doRegisterConsumer(name, requests, adapter);

		RedisQueueOutboundChannelAdapter replyQueue = new RedisQueueOutboundChannelAdapter(
				RedisMessageBus.parser.parseExpression("headers['" + REPLY_TO + "']"),
				this.connectionFactory);
		replyQueue.setBeanFactory(this.getBeanFactory());
		replyQueue.setIntegrationEvaluationContext(this.evaluationContext);
		replyQueue.afterPropertiesSet();
		this.doRegisterProducer(name, replies, replyQueue, new RedisPropertiesAccessor(properties));
	}

	@Override
	public void destroy() {
		stopBindings();
	}

	private class SendingHandler extends AbstractMessageHandler {

		private final MessageHandler delegate;

		private final String replyTo;

		private final PartitioningMetadata partitioningMetadata;


		private SendingHandler(MessageHandler delegate, String replyTo, RedisPropertiesAccessor properties) {
			this.delegate = delegate;
			this.replyTo = replyTo;
			this.partitioningMetadata = new PartitioningMetadata(properties);
			this.setBeanFactory(RedisMessageBus.this.getBeanFactory());
		}

		@Override
		protected void handleMessageInternal(Message<?> message) throws Exception {
			@SuppressWarnings("unchecked")
			Message<byte[]> transformed = (Message<byte[]>) serializePayloadIfNecessary(message,
					MediaType.APPLICATION_OCTET_STREAM);
			Map<String, Object> additionalHeaders = null;
			if (replyTo != null) {
				additionalHeaders = new HashMap<String, Object>();
				additionalHeaders.put(REPLY_TO, this.replyTo);
			}
			if (this.partitioningMetadata.isPartitionedModule()) {
				if (additionalHeaders == null) {
					additionalHeaders = new HashMap<String, Object>();
				}
				additionalHeaders.put(PARTITION_HEADER, determinePartition(message, this.partitioningMetadata));
			}
			if (additionalHeaders != null) {
				transformed = getMessageBuilderFactory().fromMessage(transformed)
						.copyHeaders(additionalHeaders)
						.build();
			}
			Message<?> messageToSend = embeddedHeadersMessageConverter.embedHeaders(transformed,
					MessageHeaders.CONTENT_TYPE, ORIGINAL_CONTENT_TYPE_HEADER, REPLY_TO);
			Assert.isInstanceOf(byte[].class, messageToSend.getPayload());
			delegate.handleMessage(messageToSend);
		}

	}

	private class ReceivingHandler extends AbstractReplyProducingMessageHandler {

		public ReceivingHandler() {
			super();
			this.setBeanFactory(RedisMessageBus.this.getBeanFactory());
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			Message<?> theRequestMessage = requestMessage;
			try {
				theRequestMessage = embeddedHeadersMessageConverter.extractHeaders((Message<byte[]>) requestMessage);
			}
			catch (UnsupportedEncodingException e) {
				logger.error("Could not convert message", e);
			}
			return deserializePayloadIfNecessary(theRequestMessage);
		}

	};

	static class EmbeddedHeadersMessageConverter {

		/**
		 * Encodes requested headers into payload; max headers = 255; max header name length = 255; max header value
		 * length = 255.
		 *
		 * @throws UnsupportedEncodingException
		 */
		Message<byte[]> embedHeaders(Message<byte[]> message, String... headers) throws UnsupportedEncodingException {
			String[] headerValues = new String[headers.length];
			int n = 0;
			int headerCount = 0;
			int headersLength = 0;
			for (String header : headers) {
				String value = message.getHeaders().get(header) == null ? null
						: message.getHeaders().get(header).toString();
				headerValues[n++] = value;
				if (value != null) {
					headerCount++;
					headersLength += header.length() + value.length();
				}
			}
			byte[] newPayload = new byte[message.getPayload().length + headersLength + headerCount * 2 + 1];
			ByteBuffer byteBuffer = ByteBuffer.wrap(newPayload);
			byteBuffer.put((byte) headerCount);
			for (int i = 0; i < headers.length; i++) {
				if (headerValues[i] != null) {
					byteBuffer.put((byte) headers[i].length());
					byteBuffer.put(headers[i].getBytes("UTF-8"));
					byteBuffer.put((byte) headerValues[i].length());
					byteBuffer.put(headerValues[i].getBytes("UTF-8"));
				}
			}
			byteBuffer.put(message.getPayload());
			return MessageBuilder.withPayload(newPayload).copyHeaders(message.getHeaders()).build();
		}

		Message<byte[]> extractHeaders(Message<byte[]> message) throws UnsupportedEncodingException {
			byte[] bytes = message.getPayload();
			ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
			int headerCount = byteBuffer.get();
			Map<String, String> headers = new HashMap<String, String>();
			for (int i = 0; i < headerCount; i++) {
				int len = byteBuffer.get();
				String headerName = new String(bytes, byteBuffer.position(), len, "UTF-8");
				byteBuffer.position(byteBuffer.position() + len);
				len = byteBuffer.get();
				String headerValue = new String(bytes, byteBuffer.position(), len, "UTF-8");
				byteBuffer.position(byteBuffer.position() + len);
				headers.put(headerName, headerValue);
			}
			byte[] newPayload = new byte[byteBuffer.remaining()];
			byteBuffer.get(newPayload);
			return MessageBuilder.withPayload(newPayload).copyHeaders(headers).build();
		}

	}

	private class RedisPropertiesAccessor extends AbstractBusPropertiesAccessor {

		public RedisPropertiesAccessor(Properties properties) {
			super(properties);
		}

	}

	private class CompositeRedisQueueMessageDrivenEndpoint extends MessageProducerSupport {

		private final List<RedisQueueMessageDrivenEndpoint> consumers = new ArrayList<RedisQueueMessageDrivenEndpoint>();

		public CompositeRedisQueueMessageDrivenEndpoint(String queueName, int concurrency) {
			for (int i = 0; i < concurrency; i++) {
				RedisQueueMessageDrivenEndpoint adapter = new RedisQueueMessageDrivenEndpoint(queueName,
						connectionFactory);
				adapter.setBeanFactory(RedisMessageBus.this.getBeanFactory());
				adapter.setSerializer(null);
				adapter.setBeanName("inbound." + queueName + "." + i);
				this.consumers.add(adapter);
			}
		}

		@Override
		protected void onInit() {
			for (RedisQueueMessageDrivenEndpoint consumer : consumers) {
				consumer.afterPropertiesSet();
			}
		}

		@Override
		protected void doStart() {
			for (RedisQueueMessageDrivenEndpoint consumer : consumers) {
				consumer.start();
			}
		}

		@Override
		protected void doStop() {
			for (RedisQueueMessageDrivenEndpoint consumer : consumers) {
				consumer.stop();
			}
		}

		@Override
		public void setOutputChannel(MessageChannel outputChannel) {
			for (RedisQueueMessageDrivenEndpoint consumer : consumers) {
				consumer.setOutputChannel(outputChannel);
			}
		}

		@Override
		public void setErrorChannel(MessageChannel errorChannel) {
			for (RedisQueueMessageDrivenEndpoint consumer : consumers) {
				consumer.setErrorChannel(errorChannel);
			}
		}

	}

}
