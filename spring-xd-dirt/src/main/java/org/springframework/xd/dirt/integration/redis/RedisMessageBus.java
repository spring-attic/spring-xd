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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.http.MediaType;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.expression.IntegrationEvaluationContextAware;
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
import org.springframework.xd.dirt.integration.bus.Binding;
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
public class RedisMessageBus extends MessageBusSupport implements DisposableBean, IntegrationEvaluationContextAware {

	private static final String REPLY_TO = "replyTo";

	private static final SpelExpressionParser parser = new SpelExpressionParser();

	private RedisConnectionFactory connectionFactory;

	private final EmbeddedHeadersMessageConverter embeddedHeadersMessageConverter = new EmbeddedHeadersMessageConverter();

	private volatile EvaluationContext evaluationContext;

	public RedisMessageBus(RedisConnectionFactory connectionFactory, MultiTypeCodec<Object> codec) {
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		Assert.notNull(codec, "codec must not be null");
		this.connectionFactory = connectionFactory;
		setCodec(codec);
	}

	@Override
	public void setIntegrationEvaluationContext(EvaluationContext context) {
		this.evaluationContext = context;
	}

	@Override
	public void bindConsumer(final String name, MessageChannel moduleInputChannel, Properties properties) {
		RedisQueueMessageDrivenEndpoint adapter = new RedisQueueMessageDrivenEndpoint("queue." + name,
				this.connectionFactory);
		adapter.setBeanFactory(this.getBeanFactory());
		adapter.setSerializer(null);
		registerNamedChannelForConsumerIfNecessary(name, false);
		doRegisterConsumer(name, moduleInputChannel, adapter);
	}

	@Override
	public void bindPubSubConsumer(final String name, MessageChannel moduleInputChannel,
			Properties properties) {
		if (logger.isInfoEnabled()) {
			logger.info("declaring pubsub for inbound: " + name);
		}
		registerNamedChannelForConsumerIfNecessary(name, true);
		RedisInboundChannelAdapter adapter = new RedisInboundChannelAdapter(this.connectionFactory);
		adapter.setBeanFactory(this.getBeanFactory());
		adapter.setSerializer(null);
		adapter.setTopics("topic." + name);
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
		RedisQueueOutboundChannelAdapter queue = new RedisQueueOutboundChannelAdapter("queue." + name,
				connectionFactory);
		queue.setBeanFactory(this.getBeanFactory());
		queue.afterPropertiesSet();
		doRegisterProducer(name, moduleOutputChannel, queue);
	}

	@Override
	public void bindPubSubProducer(final String name, MessageChannel moduleOutputChannel,
			Properties properties) {
		RedisPublishingMessageHandler topic = new RedisPublishingMessageHandler(connectionFactory);
		topic.setBeanFactory(this.getBeanFactory());
		topic.setTopic("topic." + name);
		topic.afterPropertiesSet();
		doRegisterProducer(name, moduleOutputChannel, topic);
	}

	private void doRegisterProducer(final String name, MessageChannel moduleOutputChannel, MessageHandler delegate) {
		this.doRegisterProducer(name, moduleOutputChannel, delegate, null);
	}

	private void doRegisterProducer(final String name, MessageChannel moduleOutputChannel, MessageHandler delegate,
			String replyTo) {
		Assert.isInstanceOf(SubscribableChannel.class, moduleOutputChannel);
		MessageHandler handler = new SendingHandler(delegate, replyTo);
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
		RedisQueueOutboundChannelAdapter queue = new RedisQueueOutboundChannelAdapter("queue." + name + ".requests",
				this.connectionFactory);
		queue.setBeanFactory(this.getBeanFactory());
		queue.afterPropertiesSet();
		String replyQueueName = name + ".replies." + this.getIdGenerator().generateId();
		this.doRegisterProducer(name, requests, queue, replyQueueName);
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
		this.doRegisterProducer(name, replies, replyQueue);
	}

	@Override
	public void destroy() {
		stopBindings();
	}

	private class SendingHandler extends AbstractMessageHandler {

		private final MessageHandler delegate;

		private final String replyTo;


		private SendingHandler(MessageHandler delegate, String replyTo) {
			this.delegate = delegate;
			this.replyTo = replyTo;
			this.setBeanFactory(RedisMessageBus.this.getBeanFactory());
		}

		@Override
		protected void handleMessageInternal(Message<?> message) throws Exception {
			@SuppressWarnings("unchecked")
			Message<byte[]> transformed = (Message<byte[]>) serializePayloadIfNecessary(message,
					MediaType.APPLICATION_OCTET_STREAM);
			if (this.replyTo != null) {
				transformed = MessageBuilder.fromMessage(transformed)
						.setHeader(REPLY_TO, this.replyTo)
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
}
