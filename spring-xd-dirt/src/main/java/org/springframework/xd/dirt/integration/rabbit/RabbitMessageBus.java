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

import java.util.UUID;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.integration.amqp.AmqpHeaders;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.integration.bus.Binding;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.integration.bus.MessageBusSupport;
import org.springframework.xd.dirt.integration.bus.serializer.MultiTypeCodec;

/**
 * A {@link MessageBus} implementation backed by RabbitMQ.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Jennifer Hickey
 */
public class RabbitMessageBus extends MessageBusSupport implements DisposableBean {

	private static final AcknowledgeMode DEFAULT_ACKNOWLEDGE_MODE = AcknowledgeMode.AUTO;

	private static final int DEFAULT_BACKOFF_INITIAL_INTERVAL = 1000;

	private static final int DEFAULT_BACKOFF_MAX_INTERVAL = 10000;

	private static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;

	private static final int DEFAULT_CONCURRENCY = 1;

	private static final boolean DEFAULT_DEFAULT_REQUEUE_REJECTED = true;

	private static final int DEFAULT_MAX_ATTEMPTS = 3;

	private static final int DEFAULT_MAX_CONCURRENCY = 1;

	private static final int DEFAULT_PREFETCH_COUNT = 1;

	private static final String DEFAULT_RABBIT_PREFIX = "xdbus.";

	private static final int DEFAULT_TX_SIZE = 1;

	private static final String[] DEFAULT_REQUEST_HEADER_PATTERNS = new String[] { "STANDARD_REQUEST_HEADERS", "*" };

	private static final String[] DEFAULT_REPLY_HEADER_PATTERNS = new String[] { "STANDARD_REPLY_HEADERS", "*" };

	private final Log logger = LogFactory.getLog(this.getClass());

	private final RabbitAdmin rabbitAdmin;

	private final RabbitTemplate rabbitTemplate = new RabbitTemplate();

	private final ConnectionFactory connectionFactory;

	private final GenericApplicationContext autoDeclareContext = new GenericApplicationContext();

	// Default RabbitMQ Container properties

	private volatile AcknowledgeMode defaultAcknowledgeMode = DEFAULT_ACKNOWLEDGE_MODE;

	private volatile boolean defaultChannelTransacted;

	private volatile int defaultConcurrentConsumers = DEFAULT_CONCURRENCY;

	private volatile boolean defaultDefaultRequeueRejected = DEFAULT_DEFAULT_REQUEUE_REJECTED;

	private volatile int defaultMaxConcurrentConsumers = DEFAULT_MAX_CONCURRENCY;

	private volatile int defaultPrefetchCount = DEFAULT_PREFETCH_COUNT;

	private volatile int defaultTxSize = DEFAULT_TX_SIZE;

	private volatile int defaultMaxAttempts = DEFAULT_MAX_ATTEMPTS;

	private volatile long defaultBackOffInitialInterval = DEFAULT_BACKOFF_INITIAL_INTERVAL;

	private volatile long defaultBackOffMaxInterval = DEFAULT_BACKOFF_MAX_INTERVAL;

	private volatile double defaultBackOffMultiplier = DEFAULT_BACKOFF_MULTIPLIER;

	private volatile String defaultPrefix = DEFAULT_RABBIT_PREFIX;

	private volatile String[] defaultRequestHeaderPatterns = DEFAULT_REQUEST_HEADER_PATTERNS;

	private volatile String[] defaultReplyHeaderPatterns = DEFAULT_REPLY_HEADER_PATTERNS;


	public RabbitMessageBus(ConnectionFactory connectionFactory, MultiTypeCodec<Object> codec) {
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		Assert.notNull(codec, "codec must not be null");
		this.connectionFactory = connectionFactory;
		this.rabbitTemplate.setConnectionFactory(connectionFactory);
		this.rabbitTemplate.afterPropertiesSet();
		this.rabbitAdmin = new RabbitAdmin(connectionFactory);
		this.autoDeclareContext.refresh();
		this.rabbitAdmin.setApplicationContext(this.autoDeclareContext);
		this.rabbitAdmin.afterPropertiesSet();
		this.setCodec(codec);
	}

	public void setDefaultAcknowledgeMode(AcknowledgeMode defaultAcknowledgeMode) {
		this.defaultAcknowledgeMode = defaultAcknowledgeMode;
	}

	public void setDefaultChannelTransacted(boolean defaultChannelTransacted) {
		this.defaultChannelTransacted = defaultChannelTransacted;
	}

	public void setDefaultConcurrentConsumers(int defaultConcurrentConsumers) {
		this.defaultConcurrentConsumers = defaultConcurrentConsumers;
	}

	public void setDefaultDefaultRequeueRejected(boolean defaultDefaultRequeueRejected) {
		this.defaultDefaultRequeueRejected = defaultDefaultRequeueRejected;
	}

	public void setDefaultMaxConcurrentConsumers(int defaultMaxConcurrentConsumers) {
		this.defaultMaxConcurrentConsumers = defaultMaxConcurrentConsumers;
	}

	public void setDefaultPrefetchCount(int defaultPrefetchCount) {
		this.defaultPrefetchCount = defaultPrefetchCount;
	}

	public void setDefaultTxSize(int defaultTxSize) {
		this.defaultTxSize = defaultTxSize;
	}

	public void setDefaultMaxAttempts(int defaultMaxAttempts) {
		this.defaultMaxAttempts = defaultMaxAttempts;
	}

	public void setDefaultBackOffInitialInterval(long defaultInitialBackOffInterval) {
		this.defaultBackOffInitialInterval = defaultInitialBackOffInterval;
	}

	public void setDefaultBackOffMultiplier(double defaultBackOffMultiplier) {
		this.defaultBackOffMultiplier = defaultBackOffMultiplier;
	}

	public void setDefaultBackOffMaxInterval(long defaultBackOffMaxInterval) {
		this.defaultBackOffMaxInterval = defaultBackOffMaxInterval;
	}

	public void setDefaultPrefix(String defaultPrefix) {
		Assert.notNull(defaultPrefix, "'defaultPrefix' cannot be null");
		this.defaultPrefix = defaultPrefix.trim();
	}

	public void setDefaultRequestHeaderPatterns(String[] defaultRequestHeaderPatterns) {
		this.defaultRequestHeaderPatterns = defaultRequestHeaderPatterns;
	}

	public void setDefaultReplyHeaderPatterns(String[] defaultReplyHeaderPatterns) {
		this.defaultReplyHeaderPatterns = defaultReplyHeaderPatterns;
	}

	@Override
	public void bindConsumer(final String name, MessageChannel moduleInputChannel) {
		if (logger.isInfoEnabled()) {
			logger.info("declaring queue for inbound: " + name);
		}
		registerNamedChannelForConsumerIfNecessary(name, false);
		Queue queue = new Queue(this.defaultPrefix + name);
		this.rabbitAdmin.declareQueue(queue);
		doRegisterConsumer(name, moduleInputChannel, queue);
	}

	@Override
	public void bindPubSubConsumer(String name, MessageChannel moduleInputChannel) {
		if (logger.isInfoEnabled()) {
			logger.info("declaring pubsub for inbound: " + name);
		}
		registerNamedChannelForConsumerIfNecessary(name, true);
		FanoutExchange exchange = new FanoutExchange(this.defaultPrefix + "topic." + name);
		rabbitAdmin.declareExchange(exchange);
		Queue queue = new Queue(this.defaultPrefix + name + "." + UUID.randomUUID().toString(),
				false, true, true);
		this.rabbitAdmin.declareQueue(queue);
		org.springframework.amqp.core.Binding binding = BindingBuilder.bind(queue).to(exchange);
		this.rabbitAdmin.declareBinding(binding);
		// register with context so they will be redeclared after a connection failure
		this.autoDeclareContext.getBeanFactory().registerSingleton(queue.getName(), queue);
		String bindingBeanName = exchange.getName() + "." + queue.getName() + ".binding";
		if (!autoDeclareContext.containsBean(bindingBeanName)) {
			this.autoDeclareContext.getBeanFactory().registerSingleton(bindingBeanName, binding);
		}
		doRegisterConsumer(name, moduleInputChannel, queue);
	}

	private void doRegisterConsumer(String name, MessageChannel moduleInputChannel, Queue queue) {
		SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer(this.connectionFactory);
		// TODO: override defaults with module properties if present
		listenerContainer.setAcknowledgeMode(this.defaultAcknowledgeMode);
		listenerContainer.setChannelTransacted(this.defaultChannelTransacted);
		listenerContainer.setConcurrentConsumers(this.defaultConcurrentConsumers);
		listenerContainer.setDefaultRequeueRejected(this.defaultDefaultRequeueRejected);
		listenerContainer.setMaxConcurrentConsumers(this.defaultMaxConcurrentConsumers);
		listenerContainer.setPrefetchCount(this.defaultPrefetchCount);
		listenerContainer.setTxSize(this.defaultTxSize);
		listenerContainer.setQueues(queue);
		RetryOperationsInterceptor retryInterceptor = RetryInterceptorBuilder.stateless()
				.maxAttempts(this.defaultMaxAttempts)
				.backOffOptions(this.defaultBackOffInitialInterval, this.defaultBackOffMultiplier,
						this.defaultBackOffMaxInterval)
						.recoverer(new RejectAndDontRequeueRecoverer() {

							@Override
							public void recover(org.springframework.amqp.core.Message message, Throwable cause) {
								if (logger.isWarnEnabled()) {
									logger.warn("Retries exhausted for message " + message, cause);
								}
							}

						})
						.build();
		listenerContainer.setAdviceChain(new Advice[] { retryInterceptor });
		listenerContainer.afterPropertiesSet();
		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(listenerContainer);
		adapter.setBeanFactory(this.getBeanFactory());
		DirectChannel bridgeToModuleChannel = new DirectChannel();
		bridgeToModuleChannel.setBeanFactory(this.getBeanFactory());
		bridgeToModuleChannel.setBeanName(name + ".bridge");
		adapter.setOutputChannel(bridgeToModuleChannel);
		adapter.setBeanName("inbound." + name);
		// TODO: module properties for mapped headers
		DefaultAmqpHeaderMapper mapper = new DefaultAmqpHeaderMapper();
		mapper.setRequestHeaderNames(this.defaultRequestHeaderPatterns);
		mapper.setReplyHeaderNames(this.defaultReplyHeaderPatterns);
		adapter.setHeaderMapper(mapper);
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
	public void bindProducer(final String name, MessageChannel moduleOutputChannel) {
		if (logger.isInfoEnabled()) {
			logger.info("declaring queue for outbound: " + name);
		}
		AmqpOutboundEndpoint queue = this.buildOutboundEndpoint(name);
		doRegisterProducer(name, moduleOutputChannel, queue);
	}

	private AmqpOutboundEndpoint buildOutboundEndpoint(final String name) {
		rabbitAdmin.declareQueue(new Queue(this.defaultPrefix + name));
		AmqpOutboundEndpoint queue = new AmqpOutboundEndpoint(rabbitTemplate);
		queue.setBeanFactory(this.getBeanFactory());
		queue.setRoutingKey(this.defaultPrefix + name); // uses default exchange
		// TODO: module properties for mapped headers
		DefaultAmqpHeaderMapper mapper = new DefaultAmqpHeaderMapper();
		mapper.setRequestHeaderNames(this.defaultRequestHeaderPatterns);
		mapper.setReplyHeaderNames(this.defaultReplyHeaderPatterns);
		queue.setHeaderMapper(mapper);
		queue.afterPropertiesSet();
		return queue;
	}

	@Override
	public void bindPubSubProducer(String name, MessageChannel moduleOutputChannel) {
		String exchangeName = this.defaultPrefix + "topic." + name;
		rabbitAdmin.declareExchange(new FanoutExchange(exchangeName));
		AmqpOutboundEndpoint fanout = new AmqpOutboundEndpoint(rabbitTemplate);
		fanout.setBeanFactory(this.getBeanFactory());
		fanout.setExchangeName(exchangeName);
		// TODO: module properties for mapped headers
		DefaultAmqpHeaderMapper mapper = new DefaultAmqpHeaderMapper();
		mapper.setRequestHeaderNames(this.defaultRequestHeaderPatterns);
		mapper.setReplyHeaderNames(this.defaultReplyHeaderPatterns);
		fanout.setHeaderMapper(mapper);
		fanout.afterPropertiesSet();
		doRegisterProducer(name, moduleOutputChannel, fanout);
	}

	private void doRegisterProducer(final String name, MessageChannel moduleOutputChannel,
			MessageHandler delegate) {
		this.doRegisterProducer(name, moduleOutputChannel, delegate, null);
	}

	private void doRegisterProducer(final String name, MessageChannel moduleOutputChannel,
			MessageHandler delegate, String replyTo) {
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
	public void bindRequestor(String name, MessageChannel requests, MessageChannel replies) {
		if (logger.isInfoEnabled()) {
			logger.info("binding requestor: " + name);
		}
		Assert.isInstanceOf(SubscribableChannel.class, requests);
		String queueName = name + ".requests";
		AmqpOutboundEndpoint queue = this.buildOutboundEndpoint(queueName);
		queue.setBeanFactory(this.getBeanFactory());

		String replyQueueName = this.defaultPrefix + name + ".replies." + this.getIdGenerator().generateId();
		this.doRegisterProducer(name, requests, queue, replyQueueName);
		Queue replyQueue = new Queue(replyQueueName, false, false, true); // auto-delete
		this.rabbitAdmin.declareQueue(replyQueue);
		// register with context so it will be redeclared after a connection failure
		this.autoDeclareContext.getBeanFactory().registerSingleton(replyQueueName, replyQueue);
		this.doRegisterConsumer(name, replies, replyQueue);
	}

	@Override
	public void bindReplier(String name, MessageChannel requests, MessageChannel replies) {
		if (logger.isInfoEnabled()) {
			logger.info("binding replier: " + name);
		}
		Queue requestQueue = new Queue(this.defaultPrefix + name + ".requests");
		this.rabbitAdmin.declareQueue(requestQueue);
		this.doRegisterConsumer(name, requests, requestQueue);

		AmqpOutboundEndpoint replyQueue = new AmqpOutboundEndpoint(rabbitTemplate);
		replyQueue.setBeanFactory(this.getBeanFactory());
		replyQueue.setBeanFactory(new DefaultListableBeanFactory());
		replyQueue.setRoutingKeyExpression("headers['" + AmqpHeaders.REPLY_TO + "']");
		// TODO: module properties for mapped headers
		DefaultAmqpHeaderMapper mapper = new DefaultAmqpHeaderMapper();
		mapper.setRequestHeaderNames(this.defaultRequestHeaderPatterns);
		mapper.setReplyHeaderNames(this.defaultReplyHeaderPatterns);
		replyQueue.setHeaderMapper(mapper);
		replyQueue.afterPropertiesSet();
		doRegisterProducer(name, replies, replyQueue);
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
			this.setBeanFactory(RabbitMessageBus.this.getBeanFactory());
		}

		@Override
		protected void handleMessageInternal(Message<?> message) throws Exception {
			Message<?> messageToSend = serializePayloadIfNecessary(message,
					MediaType.APPLICATION_OCTET_STREAM);
			if (replyTo != null) {
				messageToSend = MessageBuilder.fromMessage(messageToSend)
						.setHeader(AmqpHeaders.REPLY_TO, this.replyTo)
						.build();
			}
			this.delegate.handleMessage(messageToSend);
		}
	}

	private class ReceivingHandler extends AbstractReplyProducingMessageHandler {

		public ReceivingHandler() {
			super();
			this.setBeanFactory(RabbitMessageBus.this.getBeanFactory());
		}

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			return deserializePayloadIfNecessary(requestMessage);
		}

		@Override
		protected boolean shouldCopyRequestHeaders() {
			/*
			 * we've already copied the headers so no need for the ARPMH to do it, and we don't want the content-type
			 * restored if absent.
			 */
			return false;
		}

	};

}
