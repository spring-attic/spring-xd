/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.dirt.integration.bus;

import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.Lifecycle;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.http.MediaType;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.expression.IntegrationEvaluationContextAware;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.context.NamedComponent;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.ContentTypeResolver;
import org.springframework.util.AlternativeJdkIdGenerator;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.IdGenerator;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;
import org.springframework.xd.dirt.integration.bus.serializer.MultiTypeCodec;
import org.springframework.xd.dirt.integration.bus.serializer.SerializationException;


/**
 * @author David Turanski
 * @author Gary Russell
 */
public abstract class MessageBusSupport
		implements MessageBus, ApplicationContextAware, InitializingBean, IntegrationEvaluationContextAware {

	protected static final String P2P_NAMED_CHANNEL_TYPE_PREFIX = "queue:";

	protected static final String PUBSUB_NAMED_CHANNEL_TYPE_PREFIX = "topic:";

	protected static final String JOB_CHANNEL_TYPE_PREFIX = "job:";

	protected final Log logger = LogFactory.getLog(getClass());

	private volatile AbstractApplicationContext applicationContext;

	private int queueSize = Integer.MAX_VALUE;

	private volatile MultiTypeCodec<Object> codec;

	private final ContentTypeResolver contentTypeResolver = new StringConvertingContentTypeResolver();

	protected static final String ORIGINAL_CONTENT_TYPE_HEADER = "originalContentType";

	protected static final List<MediaType> MEDIATYPES_MEDIATYPE_ALL = Collections.singletonList(MediaType.ALL);

	private final List<Binding> bindings = Collections.synchronizedList(new ArrayList<Binding>());

	private final IdGenerator idGenerator = new AlternativeJdkIdGenerator();

	private final Set<MessageChannel> createdChannels = Collections.synchronizedSet(new HashSet<MessageChannel>());

	private volatile EvaluationContext evaluationContext;

	private volatile PartitionSelectorStrategy partitionSelector = new DefaultPartitionSelector();

	/**
	 * Used in the canonical case, when the binding does not involve an alias name.
	 */
	protected final SharedChannelProvider<DirectChannel> directChannelProvider = new SharedChannelProvider<DirectChannel>(
			DirectChannel.class) {

		@Override
		protected DirectChannel createSharedChannel(String name) {
			return new DirectChannel();
		}
	};

	/**
	 * Used to create and customize {@link QueueChannel}s when the binding operation involves aliased names.
	 */
	protected final SharedChannelProvider<QueueChannel> queueChannelProvider = new SharedChannelProvider<QueueChannel>(
			QueueChannel.class) {

		@Override
		protected QueueChannel createSharedChannel(String name) {
			QueueChannel queueChannel = new QueueChannel(queueSize);
			return queueChannel;
		}
	};

	protected final SharedChannelProvider<PublishSubscribeChannel> pubsubChannelProvider = new SharedChannelProvider<PublishSubscribeChannel>(
			PublishSubscribeChannel.class) {

		@Override
		protected PublishSubscribeChannel createSharedChannel(String name) {
			PublishSubscribeChannel publishSubscribeChannel = new PublishSubscribeChannel();
			return publishSubscribeChannel;
		}
	};

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Assert.isInstanceOf(AbstractApplicationContext.class, applicationContext);
		this.applicationContext = (AbstractApplicationContext) applicationContext;
	}

	protected AbstractApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	protected ConfigurableListableBeanFactory getBeanFactory() {
		return this.applicationContext.getBeanFactory();
	}

	/**
	 * Set the size of the queue when using {@link QueueChannel}s.
	 */
	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}

	public void setCodec(MultiTypeCodec<Object> codec) {
		this.codec = codec;
	}

	protected IdGenerator getIdGenerator() {
		return idGenerator;
	}

	@Override
	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}

	/**
	 * Set the partition strategy to be used by this bus if no partitionExpression
	 * is provided for a module.
	 * @param partitionSelector The selector.
	 */
	public void setPartitionSelector(PartitionSelectorStrategy partitionSelector) {
		this.partitionSelector = partitionSelector;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(applicationContext, "The 'applicationContext' property cannot be null");
	}

	/**
	 * For buses with an external broker, we can simply register a direct channel as the
	 * router output channel.
	 * @param name The name.
	 * @return The channel.
	 */
	@Override
	public synchronized MessageChannel bindDynamicProducer(String name) {
		MessageChannel channel = this.directChannelProvider.lookupSharedChannel(name);
		if (channel == null) {
			channel = this.directChannelProvider.createAndRegisterChannel(name);
			bindProducer(name, channel, null); // TODO: dynamic producer options
		}
		return channel;
	}

	/**
	 * For buses with an external broker, we can simply register a direct channel as the
	 * router output channel. Note: even though it's pub/sub, we still use a
	 * direct channel. It will be bridged to a pub/sub channel in the local
	 * bus and bound to an appropriate element for other buses.
	 * @param name The name.
	 * @return The channel.
	 */
	@Override
	public synchronized MessageChannel bindDynamicPubSubProducer(String name) {
		MessageChannel channel = this.directChannelProvider.lookupSharedChannel(name);
		if (channel == null) {
			channel = this.directChannelProvider.createAndRegisterChannel(name);
			bindPubSubProducer(name, channel, null); // TODO: dynamic producer options
		}
		return channel;
	}

	protected final void registerNamedChannelForConsumerIfNecessary(final String name, boolean pubSub) {
		if (isNamedChannel(name)) {
			if (pubSub) {
				bindDynamicPubSubProducer(name);
			}
			else {
				bindDynamicProducer(name);
			}
		}
	}

	protected boolean isNamedChannel(String name) {
		return name.startsWith(P2P_NAMED_CHANNEL_TYPE_PREFIX)
				|| name.startsWith(PUBSUB_NAMED_CHANNEL_TYPE_PREFIX)
				|| name.startsWith(JOB_CHANNEL_TYPE_PREFIX);
	}

	@Override
	public void unbindConsumers(String name) {
		deleteBindings("inbound." + name);
	}

	@Override
	public void unbindProducers(String name) {
		deleteBindings("outbound." + name);
	}

	@Override
	public void unbindConsumer(String name, MessageChannel channel) {
		deleteBinding("inbound." + name, channel);
	}

	@Override
	public void unbindProducer(String name, MessageChannel channel) {
		deleteBinding("outbound." + name, channel);
	}

	protected void addBinding(Binding binding) {
		this.bindings.add(binding);
	}

	protected void deleteBindings(String name) {
		Assert.hasText(name, "a valid name is required to remove bindings");
		synchronized (this.bindings) {
			Iterator<Binding> iterator = this.bindings.iterator();
			while (iterator.hasNext()) {
				Binding binding = iterator.next();
				if (binding.getEndpoint().getComponentName().equals(name)) {
					binding.stop();
					iterator.remove();
					destroyCreatedChannel(binding);
				}
			}
		}
	}

	protected void deleteBinding(String name, MessageChannel channel) {
		Assert.hasText(name, "a valid name is required to remove a binding");
		Assert.notNull(channel, "a valid channel is required to remove a binding");
		synchronized (this.bindings) {
			Iterator<Binding> iterator = this.bindings.iterator();
			while (iterator.hasNext()) {
				Binding binding = iterator.next();
				if (binding.getChannel().equals(channel) &&
						binding.getEndpoint().getComponentName().equals(name)) {
					binding.stop();
					iterator.remove();
					destroyCreatedChannel(binding);
					return;
				}
			}
		}
	}

	protected void destroyCreatedChannel(Binding binding) {
		MessageChannel channel = binding.getChannel();
		if (Binding.PRODUCER.equals(binding.getType()) && this.createdChannels.contains(channel)) {
			this.createdChannels.remove(channel);
			BeanFactory beanFactory = this.applicationContext.getBeanFactory();
			if (beanFactory instanceof DefaultListableBeanFactory) {
				String name = ((NamedComponent) channel).getComponentName();
				((DefaultListableBeanFactory) beanFactory).destroySingleton(name);
				if (logger.isDebugEnabled()) {
					logger.debug("Removed channel:" + name);
				}
			}
		}
	}

	protected void stopBindings() {
		for (Lifecycle bean : this.bindings) {
			try {
				bean.stop();
			}
			catch (Exception e) {
				if (logger.isWarnEnabled()) {
					logger.warn("failed to stop adapter", e);
				}
			}
		}
	}

	// TODO: Performs serialization currently no transformation
	protected final Message<?> serializePayloadIfNecessary(Message<?> message, MediaType to) {
		Object originalPayload = message.getPayload();
		Object originalContentType = message.getHeaders().get(MessageHeaders.CONTENT_TYPE);
		Object contentType = originalContentType;
		if (to.equals(ALL)) {
			return message;
		}
		else if (to.equals(APPLICATION_OCTET_STREAM)) {
			contentType = resolveContentType(originalPayload);
			Object payload = serializePayloadIfNecessary(originalPayload);
			MessageBuilder<Object> messageBuilder = MessageBuilder.withPayload(payload)
					.copyHeaders(message.getHeaders())
					.setHeader(MessageHeaders.CONTENT_TYPE, contentType);
			if (originalContentType != null) {
				messageBuilder.setHeader(ORIGINAL_CONTENT_TYPE_HEADER, originalContentType);
			}
			return messageBuilder.build();
		}
		else {
			throw new IllegalArgumentException("'to' can only be 'ALL' or 'APPLICATION_OCTET_STREAM'");
		}
	}

	private byte[] serializePayloadIfNecessary(Object originalPayload) {
		if (originalPayload instanceof byte[]) {
			return (byte[]) originalPayload;
		}
		else {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			try {
				if (originalPayload instanceof String) {
					return ((String) originalPayload).getBytes("UTF-8");
				}
				this.codec.serialize(originalPayload, bos);
				return bos.toByteArray();
			}
			catch (IOException e) {
				throw new SerializationException("unable to serialize payload ["
						+ originalPayload.getClass().getName() + "]", e);
			}
		}
	}

	protected final Message<?> deserializePayloadIfNecessary(Message<?> message) {
		Message<?> messageToSend = message;
		Object originalPayload = message.getPayload();
		MimeType contentType = contentTypeResolver.resolve(message.getHeaders());
		Object payload = deserializePayload(originalPayload, contentType);
		if (payload != null) {
			MessageBuilder<Object> transformed = MessageBuilder.withPayload(payload).copyHeaders(message.getHeaders());
			Object originalContentType = message.getHeaders().get(ORIGINAL_CONTENT_TYPE_HEADER);
			transformed.setHeader(MessageHeaders.CONTENT_TYPE, originalContentType);
			transformed.setHeader(ORIGINAL_CONTENT_TYPE_HEADER, null);
			messageToSend = transformed.build();
		}
		return messageToSend;
	}

	private Object deserializePayload(Object payload, MimeType contentType) {
		if (payload instanceof byte[]) {
			if (APPLICATION_OCTET_STREAM.equals(contentType)) {
				return payload;
			}
			else {
				return deserializePayload((byte[]) payload, contentType);
			}
		}
		return payload;
	}

	private Object deserializePayload(byte[] bytes, MimeType contentType) {
		Class<?> targetType = null;
		try {
			if (contentType.equals(TEXT_PLAIN)) {
				return new String(bytes, "UTF-8");
			}
			targetType = Class.forName(contentType.getParameter("type"));


			return codec.deserialize(bytes, targetType);
		}
		catch (ClassNotFoundException e) {
			throw new SerializationException("unable to deserialize [" + targetType + "]. Class not found.", e);
		}
		catch (IOException e) {
			throw new SerializationException("unable to deserialize [" + targetType + "]", e);
		}

	}

	private String resolveContentType(Object originalPayload) {
		if (originalPayload instanceof byte[]) {
			return APPLICATION_OCTET_STREAM_VALUE;
		}
		if (originalPayload instanceof String) {
			return TEXT_PLAIN_VALUE;
		}
		return "application/x-java-object;type=" + originalPayload.getClass().getName();
	}

	/**
	 * Determine the partition to which to send this message.
	 * If a partition key extractor class is provided, it is invoked to determine the key.
	 * Otherwise, the partition key expression is evaluated to obtain the
	 * key value.
	 * If a partition selector class is provided, it will be invoked to determine the
	 * partition. Otherwise,
	 * if the partition expression is not null, it is evaluated
	 * against the key and is expected to return an integer to which the modulo function
	 * will be applied, using the divisor.
	 * <p>
	 * If no partition expression is provided, the key will be passed to the bus partition
	 * strategy along with the divisor.
	 * The default partition strategy uses {@code key.hashCode()}, and the result will
	 * be the mod of that value.
	 *
	 * @param message the message.
	 * @param meta the partitioning metadata.
	 * @return the partition.
	 */
	protected int determinePartition(Message<?> message, PartitioningMetadata meta) {
		Object key = null;
		if (StringUtils.hasText(meta.partitionKeyExtractorClass)) {
			key = invokeExtractor(meta.partitionKeyExtractorClass, message);
		}
		else if (meta.partitionKeyExpression != null) {
			key = meta.partitionKeyExpression.getValue(this.evaluationContext, message);
		}
		Assert.notNull(key, "Partition key cannot be null");
		if (StringUtils.hasText(meta.partitionSelectorClass)) {
			int partition = invokePartitionSelector(meta.partitionSelectorClass, key, meta.divisor);
			Assert.isTrue(partition < meta.divisor, "The partition function returned " + partition
					+ "; it should be less than " + meta.divisor);
			return partition;
		}
		else if (meta.partitionSelectorExpression != null) {
			return meta.partitionSelectorExpression.getValue(this.evaluationContext, key, Integer.class) % meta.divisor;
		}
		else {
			int partition = this.partitionSelector.selectPartition(key, meta.divisor);
			Assert.isTrue(partition < meta.divisor, "The partition function returned " + partition
					+ "; it should be less than " + meta.divisor);
			return partition;
		}
	}

	private Object invokeExtractor(String partitionKeyExtractorClassName, Message<?> message) {
		if (this.applicationContext.containsBean(partitionKeyExtractorClassName)) {
			return this.applicationContext.getBean(partitionKeyExtractorClassName, PartitionKeyExtractorStrategy.class)
					.extractKey(message);
		}
		Class<?> clazz;
		try {
			clazz = ClassUtils.forName(partitionKeyExtractorClassName, this.applicationContext.getClassLoader());
		}
		catch (Exception e) {
			logger.error("Failed to load key extractor", e);
			throw new MessageBusException("Failed to load key extractor: " + partitionKeyExtractorClassName, e);
		}
		try {
			Object extractor = clazz.newInstance();
			Assert.isInstanceOf(PartitionKeyExtractorStrategy.class, extractor);
			this.applicationContext.getBeanFactory().registerSingleton(partitionKeyExtractorClassName, extractor);
			this.applicationContext.getBeanFactory().initializeBean(extractor, partitionKeyExtractorClassName);
			return ((PartitionKeyExtractorStrategy) extractor).extractKey(message);
		}
		catch (Exception e) {
			logger.error("Failed to instantiate key extractor", e);
			throw new MessageBusException("Failed to instantiate key extractor: " + partitionKeyExtractorClassName, e);
		}
	}

	private int invokePartitionSelector(String partitionSelectorClassName, Object key, int divisor) {
		if (this.applicationContext.containsBean(partitionSelectorClassName)) {
			return this.applicationContext.getBean(partitionSelectorClassName, PartitionSelectorStrategy.class)
					.selectPartition(key, divisor);
		}
		Class<?> clazz;
		try {
			clazz = ClassUtils.forName(partitionSelectorClassName, this.applicationContext.getClassLoader());
		}
		catch (Exception e) {
			logger.error("Failed to load partition selector", e);
			throw new MessageBusException("Failed to load partition selector: " + partitionSelectorClassName, e);
		}
		try {
			Object extractor = clazz.newInstance();
			Assert.isInstanceOf(PartitionKeyExtractorStrategy.class, extractor);
			this.applicationContext.getBeanFactory().registerSingleton(partitionSelectorClassName, extractor);
			this.applicationContext.getBeanFactory().initializeBean(extractor, partitionSelectorClassName);
			return ((PartitionSelectorStrategy) extractor).selectPartition(key, divisor);
		}
		catch (Exception e) {
			logger.error("Failed to instantiate partition selector", e);
			throw new MessageBusException("Failed to instantiate partition selector: " + partitionSelectorClassName, e);
		}
	}

	/**
	 * Default partition strategy; only works on keys with "real" hash codes, such as String.
	 */
	private class DefaultPartitionSelector implements PartitionSelectorStrategy {

		@Override
		public int selectPartition(Object key, int divisor) {
			return key.hashCode() % divisor;
		}

	}

	protected class PartitioningMetadata {

		private final String partitionKeyExtractorClass;

		private final Expression partitionKeyExpression;

		private final String partitionSelectorClass;

		private final Expression partitionSelectorExpression;

		private final int divisor;

		public PartitioningMetadata(AbstractBusPropertiesAccessor properties) {
			this.partitionKeyExtractorClass = properties.getPartitionKeyExtractorClass();
			this.partitionKeyExpression = properties.getPartitionKeyExpression();
			this.partitionSelectorClass = properties.getPartitionSelectorClass();
			this.partitionSelectorExpression = properties.getPartitionSelectorExpression();
			this.divisor = properties.getPartitionCount();
		}

		public boolean isPartitionedModule() {
			return StringUtils.hasText(this.partitionKeyExtractorClass) || this.partitionKeyExpression != null;
		}
	}

	/**
	 * Looks up or optionally creates a new channel to use.
	 *
	 * @author Eric Bottard
	 */
	protected abstract class SharedChannelProvider<T extends MessageChannel> {

		private final Class<T> requiredType;

		private SharedChannelProvider(Class<T> clazz) {
			this.requiredType = clazz;
		}

		protected synchronized final T lookupOrCreateSharedChannel(String name) {
			T channel = lookupSharedChannel(name);
			if (channel == null) {
				channel = createAndRegisterChannel(name);
			}
			return channel;
		}

		@SuppressWarnings("unchecked")
		protected T createAndRegisterChannel(String name) {
			T channel = createSharedChannel(name);
			ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
			beanFactory.registerSingleton(name, channel);
			channel = (T) beanFactory.initializeBean(channel, name);
			MessageBusSupport.this.createdChannels.add(channel);
			if (logger.isDebugEnabled()) {
				logger.debug("Registered channel:" + name);
			}
			return channel;
		}

		protected abstract T createSharedChannel(String name);

		protected T lookupSharedChannel(String name) {
			T channel = null;
			if (applicationContext.containsBean(name)) {
				try {
					channel = applicationContext.getBean(name, requiredType);
				}
				catch (Exception e) {
					throw new IllegalArgumentException("bean '" + name
							+ "' is already registered but does not match the required type");
				}
			}
			return channel;
		}
	}

}
