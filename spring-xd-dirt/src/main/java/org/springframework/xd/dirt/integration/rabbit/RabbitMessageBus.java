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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.aopalliance.aop.Advice;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.expression.Expression;
import org.springframework.http.MediaType;
import org.springframework.integration.amqp.AmqpHeaders;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.integration.bus.AbstractBusPropertiesAccessor;
import org.springframework.xd.dirt.integration.bus.Binding;
import org.springframework.xd.dirt.integration.bus.BusProperties;
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

	private static final MessageDeliveryMode DEFAULT_DEFAULT_DELIVERY_MODE = MessageDeliveryMode.PERSISTENT;

	private static final boolean DEFAULT_DEFAULT_REQUEUE_REJECTED = true;

	private static final int DEFAULT_MAX_CONCURRENCY = 1;

	private static final int DEFAULT_PREFETCH_COUNT = 1;

	private static final String DEFAULT_RABBIT_PREFIX = "xdbus.";

	private static final int DEFAULT_TX_SIZE = 1;

	private static final String[] DEFAULT_REQUEST_HEADER_PATTERNS = new String[] { "STANDARD_REQUEST_HEADERS", "*" };

	private static final String[] DEFAULT_REPLY_HEADER_PATTERNS = new String[] { "STANDARD_REPLY_HEADERS", "*" };

	private static final Set<Object> RABBIT_CONSUMER_PROPERTIES = new HashSet<Object>(Arrays.asList(new String[] {
		BusProperties.MAX_CONCURRENCY,
		RabbitPropertiesAccessor.ACK_MODE,
		RabbitPropertiesAccessor.PREFETCH,
		RabbitPropertiesAccessor.PREFIX,
		RabbitPropertiesAccessor.REQUEST_HEADER_PATTERNS,
		RabbitPropertiesAccessor.REQUEUE,
		RabbitPropertiesAccessor.TRANSACTED,
		RabbitPropertiesAccessor.TX_SIZE
	}));

	/**
	 * Retry + rabbit consumer properties.
	 */
	private static final Set<Object> SUPPORTED_BASIC_CONSUMER_PROPERTIES = new SetBuilder()
			.addAll(CONSUMER_RETRY_PROPERTIES)
			.addAll(RABBIT_CONSUMER_PROPERTIES)
			.build();

	private static final Set<Object> SUPPORTED_PUBSUB_CONSUMER_PROPERTIES = SUPPORTED_BASIC_CONSUMER_PROPERTIES;

	/**
	 * Basic + concurrency.
	 */
	private static final Set<Object> SUPPORTED_NAMED_CONSUMER_PROPERTIES = new SetBuilder()
			.addAll(SUPPORTED_BASIC_CONSUMER_PROPERTIES)
			.add(BusProperties.CONCURRENCY)
			.build();

	/**
	 * Basic + concurrency + partitioning.
	 */
	private static final Set<Object> SUPPORTED_CONSUMER_PROPERTIES = new SetBuilder()
			.addAll(SUPPORTED_BASIC_CONSUMER_PROPERTIES)
			.add(BusProperties.CONCURRENCY)
			.add(BusProperties.PARTITION_INDEX)
			.build();

	/**
	 * Basic + concurrency + reply headers + delivery mode (reply).
	 */
	private static final Set<Object> SUPPORTED_REPLYING_CONSUMER_PROPERTIES = new SetBuilder()
			// request
			.addAll(SUPPORTED_BASIC_CONSUMER_PROPERTIES)
			.add(BusProperties.CONCURRENCY)
			// reply
			.add(RabbitPropertiesAccessor.REPLY_HEADER_PATTERNS)
			.add(RabbitPropertiesAccessor.DELIVERY_MODE)
			.build();

	/**
	 * Rabbit producer properties.
	 */
	private static final Set<Object> SUPPORTED_BASIC_PRODUCER_PROPERTIES = new SetBuilder()
			.addAll(PRODUCER_STANDARD_PROPERTIES)
			.add(RabbitPropertiesAccessor.DELIVERY_MODE)
			.add(RabbitPropertiesAccessor.PREFIX)
			.add(RabbitPropertiesAccessor.REQUEST_HEADER_PATTERNS)
			.build();

	private static final Set<Object> SUPPORTED_PUBSUB_PRODUCER_PROPERTIES = SUPPORTED_BASIC_PRODUCER_PROPERTIES;

	private static final Set<Object> SUPPORTED_NAMED_PRODUCER_PROPERTIES = SUPPORTED_BASIC_PRODUCER_PROPERTIES;

	/**
	 * Partitioning + rabbit producer properties.
	 */
	private static final Set<Object> SUPPORTED_PRODUCER_PROPERTIES = new SetBuilder()
			.addAll(PRODUCER_PARTITIONING_PROPERTIES)
			.addAll(SUPPORTED_BASIC_PRODUCER_PROPERTIES)
			.add(BusProperties.DIRECT_BINDING_ALLOWED)
			.build();

	/**
	 * Basic producer + basic consumer + concurrency + reply headers.
	 */
	private static final Set<Object> SUPPORTED_REQUESTING_PRODUCER_PROPERTIES = new SetBuilder()
			// request
			.addAll(SUPPORTED_BASIC_PRODUCER_PROPERTIES)
			// reply
			.addAll(SUPPORTED_BASIC_CONSUMER_PROPERTIES)
			.add(BusProperties.CONCURRENCY)
			.add(RabbitPropertiesAccessor.REPLY_HEADER_PATTERNS)
			.build();

	private final Log logger = LogFactory.getLog(this.getClass());

	private final RabbitAdmin rabbitAdmin;

	private final RabbitTemplate rabbitTemplate = new RabbitTemplate();

	private final ConnectionFactory connectionFactory;

	private final GenericApplicationContext autoDeclareContext = new GenericApplicationContext();

	// Default RabbitMQ Container properties

	private volatile AcknowledgeMode defaultAcknowledgeMode = DEFAULT_ACKNOWLEDGE_MODE;

	private volatile boolean defaultChannelTransacted;

	private volatile MessageDeliveryMode defaultDefaultDeliveryMode = DEFAULT_DEFAULT_DELIVERY_MODE;

	private volatile boolean defaultDefaultRequeueRejected = DEFAULT_DEFAULT_REQUEUE_REJECTED;

	private volatile int defaultMaxConcurrency = DEFAULT_MAX_CONCURRENCY;

	private volatile int defaultPrefetchCount = DEFAULT_PREFETCH_COUNT;

	private volatile int defaultTxSize = DEFAULT_TX_SIZE;

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
		Assert.notNull(defaultAcknowledgeMode, "'defaultAcknowledgeMode' cannot be null");
		this.defaultAcknowledgeMode = defaultAcknowledgeMode;
	}

	public void setDefaultChannelTransacted(boolean defaultChannelTransacted) {
		this.defaultChannelTransacted = defaultChannelTransacted;
	}

	public void setDefaultDefaultDeliveryMode(MessageDeliveryMode defaultDefaultDeliveryMode) {
		Assert.notNull(defaultDefaultDeliveryMode, "'defaultDeliveryMode' cannot be null");
		this.defaultDefaultDeliveryMode = defaultDefaultDeliveryMode;
	}

	public void setDefaultDefaultRequeueRejected(boolean defaultDefaultRequeueRejected) {
		this.defaultDefaultRequeueRejected = defaultDefaultRequeueRejected;
	}

	/**
	 * Set the bus's default max consumers; can be overridden by consumer.maxConcurrency. Values
	 * less than 'concurrency' will be coerced to be equal to concurrency.
	 * @param defaultMaxConcurrency The default max concurrency.
	 */
	public void setDefaultMaxConcurrency(int defaultMaxConcurrency) {
		this.defaultMaxConcurrency = defaultMaxConcurrency;
	}

	public void setDefaultPrefetchCount(int defaultPrefetchCount) {
		this.defaultPrefetchCount = defaultPrefetchCount;
	}

	public void setDefaultTxSize(int defaultTxSize) {
		this.defaultTxSize = defaultTxSize;
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
	public void bindConsumer(final String name, MessageChannel moduleInputChannel, Properties properties) {
		if (logger.isInfoEnabled()) {
			logger.info("declaring queue for inbound: " + name);
		}
		if (name.startsWith(P2P_NAMED_CHANNEL_TYPE_PREFIX)) {
			validateConsumerProperties(name, properties, SUPPORTED_NAMED_CONSUMER_PROPERTIES);
		}
		else {
			validateConsumerProperties(name, properties, SUPPORTED_CONSUMER_PROPERTIES);
		}
		RabbitPropertiesAccessor accessor = new RabbitPropertiesAccessor(properties);
		String queueName = accessor.getPrefix(this.defaultPrefix) + name;
		int partitionIndex = accessor.getPartitionIndex();
		if (partitionIndex >= 0) {
			queueName += "-" + partitionIndex;
		}
		Queue queue = new Queue(queueName);
		this.rabbitAdmin.declareQueue(queue);
		doRegisterConsumer(name, moduleInputChannel, queue, accessor, false);
		bindExistingProducerDirectlyIfPossible(name, moduleInputChannel);
	}

	@Override
	public void bindPubSubConsumer(String name, MessageChannel moduleInputChannel, Properties properties) {
		if (logger.isInfoEnabled()) {
			logger.info("declaring pubsub for inbound: " + name);
		}
		RabbitPropertiesAccessor accessor = new RabbitPropertiesAccessor(properties);
		validateConsumerProperties(name, properties, SUPPORTED_PUBSUB_CONSUMER_PROPERTIES);
		String prefix = accessor.getPrefix(this.defaultPrefix);
		FanoutExchange exchange = new FanoutExchange(prefix + "topic." + name);
		rabbitAdmin.declareExchange(exchange);
		Queue queue = new Queue(prefix + name + "." + UUID.randomUUID().toString(),
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
		doRegisterConsumer(name, moduleInputChannel, queue, accessor, true);
	}

	private void doRegisterConsumer(String name, MessageChannel moduleInputChannel, Queue queue,
			RabbitPropertiesAccessor properties, boolean isPubSub) {
		SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer(this.connectionFactory);
		listenerContainer.setAcknowledgeMode(properties.getAcknowledgeMode(this.defaultAcknowledgeMode));
		listenerContainer.setChannelTransacted(properties.getTransacted(this.defaultChannelTransacted));
		listenerContainer.setDefaultRequeueRejected(properties.getRequeueRejected(this.defaultDefaultRequeueRejected));
		if (!isPubSub) {
			int concurrency = properties.getConcurrency(this.defaultConcurrency);
			concurrency = concurrency > 0 ? concurrency : 1;
			listenerContainer.setConcurrentConsumers(concurrency);
			int maxConcurrency = properties.getMaxConcurrency(this.defaultMaxConcurrency);
			if (maxConcurrency > concurrency) {
				listenerContainer.setMaxConcurrentConsumers(maxConcurrency);
			}
		}
		listenerContainer.setPrefetchCount(properties.getPrefetchCount(this.defaultPrefetchCount));
		listenerContainer.setTxSize(properties.getTxSize(this.defaultTxSize));
		listenerContainer.setTaskExecutor(new SimpleAsyncTaskExecutor(queue.getName() + "-"));
		listenerContainer.setQueues(queue);
		int maxAttempts = properties.getMaxAttempts(this.defaultMaxAttempts);
		if (maxAttempts > 1) {
			RetryOperationsInterceptor retryInterceptor = RetryInterceptorBuilder.stateless()
					.maxAttempts(maxAttempts)
					.backOffOptions(properties.getBackOffInitialInterval(this.defaultBackOffInitialInterval),
							properties.getBackOffMultiplier(this.defaultBackOffMultiplier),
							properties.getBackOffMaxInterval(this.defaultBackOffMaxInterval))
					.recoverer(new RejectAndDontRequeueRecoverer())
					.build();
			listenerContainer.setAdviceChain(new Advice[] { retryInterceptor });
		}
		listenerContainer.afterPropertiesSet();
		AmqpInboundChannelAdapter adapter = new AmqpInboundChannelAdapter(listenerContainer);
		adapter.setBeanFactory(this.getBeanFactory());
		DirectChannel bridgeToModuleChannel = new DirectChannel();
		bridgeToModuleChannel.setBeanFactory(this.getBeanFactory());
		bridgeToModuleChannel.setBeanName(name + ".bridge");
		adapter.setOutputChannel(bridgeToModuleChannel);
		adapter.setBeanName("inbound." + name);
		DefaultAmqpHeaderMapper mapper = new DefaultAmqpHeaderMapper();
		mapper.setRequestHeaderNames(properties.getRequestHeaderPattens(this.defaultRequestHeaderPatterns));
		mapper.setReplyHeaderNames(properties.getReplyHeaderPattens(this.defaultReplyHeaderPatterns));
		adapter.setHeaderMapper(mapper);
		adapter.afterPropertiesSet();
		Binding consumerBinding = Binding.forConsumer(name, adapter, moduleInputChannel, properties);
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
		RabbitPropertiesAccessor accessor = new RabbitPropertiesAccessor(properties);
		if (name.startsWith(P2P_NAMED_CHANNEL_TYPE_PREFIX)) {
			validateProducerProperties(name, properties, SUPPORTED_NAMED_PRODUCER_PROPERTIES);
		}
		else {
			validateProducerProperties(name, properties, SUPPORTED_PRODUCER_PROPERTIES);
		}
		if (!bindNewProducerDirectlyIfPossible(name, (SubscribableChannel) moduleOutputChannel, accessor)) {
			if (logger.isInfoEnabled()) {
				logger.info("declaring queue for outbound: " + name);
			}
			AmqpOutboundEndpoint queue = this.buildOutboundEndpoint(name, accessor);
			doRegisterProducer(name, moduleOutputChannel, queue, accessor);
		}
	}

	private AmqpOutboundEndpoint buildOutboundEndpoint(final String name, RabbitPropertiesAccessor properties) {
		String queueName = properties.getPrefix(this.defaultPrefix) + name;
		String partitionKeyExtractorClass = properties.getPartitionKeyExtractorClass();
		Expression partitionKeyExpression = properties.getPartitionKeyExpression();
		AmqpOutboundEndpoint queue = new AmqpOutboundEndpoint(rabbitTemplate);
		if (partitionKeyExpression == null && !StringUtils.hasText(partitionKeyExtractorClass)) {
			rabbitAdmin.declareQueue(new Queue(queueName));
			queue.setRoutingKey(queueName); // uses default exchange
		}
		else {
			queue.setRoutingKeyExpression(buildPartitionRoutingExpression(queueName));
			for (int i = 0; i < properties.getPartitionCount(); i++) {
				this.rabbitAdmin.declareQueue(new Queue(queueName + "-" + i));
			}
		}
		configureOutboundHandler(queue, properties);
		return queue;
	}

	private void configureOutboundHandler(AmqpOutboundEndpoint handler, RabbitPropertiesAccessor properties) {
		DefaultAmqpHeaderMapper mapper = new DefaultAmqpHeaderMapper();
		mapper.setRequestHeaderNames(properties.getRequestHeaderPattens(this.defaultRequestHeaderPatterns));
		mapper.setReplyHeaderNames(properties.getReplyHeaderPattens(this.defaultReplyHeaderPatterns));
		handler.setHeaderMapper(mapper);
		handler.setDefaultDeliveryMode(properties.getDeliveryMode(this.defaultDefaultDeliveryMode));
		handler.setBeanFactory(this.getBeanFactory());
		handler.afterPropertiesSet();
	}

	@Override
	public void bindPubSubProducer(String name, MessageChannel moduleOutputChannel,
			Properties properties) {
		validateProducerProperties(name, properties, SUPPORTED_PUBSUB_PRODUCER_PROPERTIES);
		RabbitPropertiesAccessor accessor = new RabbitPropertiesAccessor(properties);
		String exchangeName = accessor.getPrefix(this.defaultPrefix) + "topic." + name;
		this.rabbitAdmin.declareExchange(new FanoutExchange(exchangeName));
		AmqpOutboundEndpoint fanout = new AmqpOutboundEndpoint(rabbitTemplate);
		fanout.setExchangeName(exchangeName);
		configureOutboundHandler(fanout, accessor);
		doRegisterProducer(name, moduleOutputChannel, fanout, accessor);
	}

	private void doRegisterProducer(final String name, MessageChannel moduleOutputChannel,
			AmqpOutboundEndpoint delegate, RabbitPropertiesAccessor properties) {
		this.doRegisterProducer(name, moduleOutputChannel, delegate, null, properties);
	}

	private void doRegisterProducer(final String name, MessageChannel moduleOutputChannel,
			AmqpOutboundEndpoint delegate, String replyTo, RabbitPropertiesAccessor properties) {
		Assert.isInstanceOf(SubscribableChannel.class, moduleOutputChannel);
		MessageHandler handler = new SendingHandler(delegate, replyTo, properties);
		EventDrivenConsumer consumer = new EventDrivenConsumer((SubscribableChannel) moduleOutputChannel, handler);
		consumer.setBeanFactory(getBeanFactory());
		consumer.setBeanName("outbound." + name);
		consumer.afterPropertiesSet();
		Binding producerBinding = Binding.forProducer(name, moduleOutputChannel, consumer, properties);
		addBinding(producerBinding);
		producerBinding.start();
	}

	@Override
	public void bindRequestor(String name, MessageChannel requests, MessageChannel replies,
			Properties properties) {
		if (logger.isInfoEnabled()) {
			logger.info("binding requestor: " + name);
		}
		validateProducerProperties(name, properties, SUPPORTED_REQUESTING_PRODUCER_PROPERTIES);
		Assert.isInstanceOf(SubscribableChannel.class, requests);
		RabbitPropertiesAccessor accessor = new RabbitPropertiesAccessor(properties);
		String queueName = name + ".requests";
		AmqpOutboundEndpoint queue = this.buildOutboundEndpoint(queueName, accessor);
		queue.setBeanFactory(this.getBeanFactory());

		String replyQueueName = accessor.getPrefix(this.defaultPrefix) + name + ".replies."
				+ this.getIdGenerator().generateId();
		this.doRegisterProducer(name, requests, queue, replyQueueName, accessor);
		Queue replyQueue = new Queue(replyQueueName, false, false, true); // auto-delete
		this.rabbitAdmin.declareQueue(replyQueue);
		// register with context so it will be redeclared after a connection failure
		this.autoDeclareContext.getBeanFactory().registerSingleton(replyQueueName, replyQueue);
		this.doRegisterConsumer(name, replies, replyQueue, accessor, false);
	}

	@Override
	public void bindReplier(String name, MessageChannel requests, MessageChannel replies,
			Properties properties) {
		if (logger.isInfoEnabled()) {
			logger.info("binding replier: " + name);
		}
		validateConsumerProperties(name, properties, SUPPORTED_REPLYING_CONSUMER_PROPERTIES);
		RabbitPropertiesAccessor accessor = new RabbitPropertiesAccessor(properties);
		Queue requestQueue = new Queue(accessor.getPrefix(this.defaultPrefix) + name + ".requests");
		this.rabbitAdmin.declareQueue(requestQueue);
		this.doRegisterConsumer(name, requests, requestQueue, accessor, false);

		AmqpOutboundEndpoint replyQueue = new AmqpOutboundEndpoint(rabbitTemplate);
		replyQueue.setRoutingKeyExpression("headers['" + AmqpHeaders.REPLY_TO + "']");
		configureOutboundHandler(replyQueue, accessor);
		doRegisterProducer(name, replies, replyQueue, accessor);
	}

	@Override
	public void destroy() {
		stopBindings();
	}

	private class SendingHandler extends AbstractMessageHandler {

		private final MessageHandler delegate;

		private final String replyTo;

		private final PartitioningMetadata partitioningMetadata;

		private SendingHandler(MessageHandler delegate, String replyTo, RabbitPropertiesAccessor properties) {
			this.delegate = delegate;
			this.replyTo = replyTo;
			this.partitioningMetadata = new PartitioningMetadata(properties);
			this.setBeanFactory(RabbitMessageBus.this.getBeanFactory());
		}

		@Override
		protected void handleMessageInternal(Message<?> message) throws Exception {
			Message<?> messageToSend = serializePayloadIfNecessary(message,
					MediaType.APPLICATION_OCTET_STREAM);
			Map<String, Object> additionalHeaders = null;
			if (replyTo != null) {
				additionalHeaders = new HashMap<String, Object>();
				additionalHeaders.put(AmqpHeaders.REPLY_TO, this.replyTo);
			}
			if (this.partitioningMetadata.isPartitionedModule()) {
				if (additionalHeaders == null) {
					additionalHeaders = new HashMap<String, Object>();
				}
				additionalHeaders.put(PARTITION_HEADER, determinePartition(message, this.partitioningMetadata));
			}
			if (additionalHeaders != null) {
				messageToSend = getMessageBuilderFactory().fromMessage(messageToSend)
						.copyHeaders(additionalHeaders)
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

	/**
	 * Property accessor for the RabbitMessageBus. Refer to the Spring-AMQP
	 * documentation for information on the specific properties.
	 */
	private static class RabbitPropertiesAccessor extends AbstractBusPropertiesAccessor {

		/**
		 * The acknowledge mode.
		 */
		private static final String ACK_MODE = "ackMode";

		/**
		 * The delivery mode.
		 */
		private static final String DELIVERY_MODE = "deliveryMode";

		/**
		 * The prefetch count (basic qos).
		 */
		private static final String PREFETCH = "prefetch";

		/**
		 * The prefix for queues, exchanges.
		 */
		private static final String PREFIX = "prefix";

		/**
		 * The reply header patterns.
		 */
		private static final String REPLY_HEADER_PATTERNS = "replyHeaderPatterns";

		/**
		 * The request header patterns.
		 */
		private static final String REQUEST_HEADER_PATTERNS = "requestHeaderPatterns";

		/**
		 * Whether delivery failures should be requeued.
		 */
		private static final String REQUEUE = "requeue";

		/**
		 * Whether to use transacted channels.
		 */
		private static final String TRANSACTED = "transacted";

		/**
		 * The number of deliveries between acks.
		 */
		private static final String TX_SIZE = "txSize";

		public RabbitPropertiesAccessor(Properties properties) {
			super(properties);
		}

		public AcknowledgeMode getAcknowledgeMode(AcknowledgeMode defaultValue) {
			String ackknowledgeMode = getProperty(ACK_MODE);
			if (StringUtils.hasText(ackknowledgeMode)) {
				return AcknowledgeMode.valueOf(ackknowledgeMode);
			}
			else {
				return defaultValue;
			}
		}

		public MessageDeliveryMode getDeliveryMode(MessageDeliveryMode defaultValue) {
			String deliveryMode = getProperty(DELIVERY_MODE);
			if (StringUtils.hasText(deliveryMode)) {
				return MessageDeliveryMode.valueOf(deliveryMode);
			}
			else {
				return defaultValue;
			}
		}

		public int getPrefetchCount(int defaultValue) {
			return getProperty(PREFETCH, defaultValue);
		}

		public String getPrefix(String defaultValue) {
			return getProperty(PREFIX, defaultValue);
		}

		public String[] getReplyHeaderPattens(String[] defaultValue) {
			return asStringArray(getProperty(REPLY_HEADER_PATTERNS), defaultValue);
		}

		public String[] getRequestHeaderPattens(String[] defaultValue) {
			return asStringArray(getProperty(REQUEST_HEADER_PATTERNS), defaultValue);
		}

		public boolean getRequeueRejected(boolean defaultValue) {
			return getProperty(REQUEUE, defaultValue);
		}

		public boolean getTransacted(boolean defaultValue) {
			return getProperty(TRANSACTED, defaultValue);
		}

		public int getTxSize(int defaultValue) {
			return getProperty(TX_SIZE, defaultValue);
		}

	}

}
