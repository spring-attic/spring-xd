/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.x.bus;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.x.bus.serializer.MultiTypeCodec;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;

/**
 * A simple implementation of {@link MessageBus} for in-process use. For inbound and outbound, creates a
 * {@link DirectChannel} or a {@link QueueChannel} depending on whether the binding is aliased or not then bridges the
 * passed {@link MessageChannel} to the channel which is registered in the given application context. If that channel
 * does not yet exist, it will be created.
 * 
 * @author David Turanski
 * @author Mark Fisher
 * @author Gary Russell
 * @author Jennifer Hickey
 * @since 1.0
 */
public class LocalMessageBus extends MessageBusSupport implements ApplicationContextAware, InitializingBean {

	private volatile AbstractApplicationContext applicationContext;

	private volatile boolean convertWithinTransport = false;

	private int queueSize = Integer.MAX_VALUE;

	private PollerMetadata poller;

	private final Map<String, ExecutorChannel> requestReplyChannels = new HashMap<String, ExecutorChannel>();

	private volatile ExecutorService executor = Executors.newCachedThreadPool();

	/**
	 * Used in the canonical case, when the binding does not involve an alias name.
	 */
	private SharedChannelProvider<DirectChannel> directChannelProvider = new SharedChannelProvider<DirectChannel>(
			DirectChannel.class) {

		@Override
		protected DirectChannel createSharedChannel(String name) {
			return new DirectChannel();
		}
	};

	/**
	 * Used to create and customize {@link QueueChannel}s when the binding operation involves aliased names.
	 */
	private SharedChannelProvider<QueueChannel> queueChannelProvider = new SharedChannelProvider<QueueChannel>(
			QueueChannel.class) {

		@Override
		protected QueueChannel createSharedChannel(String name) {
			QueueChannel queueChannel = new QueueChannel(queueSize);
			return queueChannel;
		}
	};

	private SharedChannelProvider<PublishSubscribeChannel> pubsubChannelProvider = new SharedChannelProvider<PublishSubscribeChannel>(
			PublishSubscribeChannel.class) {

		@Override
		protected PublishSubscribeChannel createSharedChannel(String name) {
			return new PublishSubscribeChannel();
		}
	};

	private boolean hasCodec;

	/**
	 * Set the size of the queue when using {@link QueueChannel}s.
	 */
	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}

	/**
	 * Set the poller to use when QueueChannels are used.
	 */
	public void setPoller(PollerMetadata poller) {
		this.poller = poller;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Assert.isInstanceOf(AbstractApplicationContext.class, applicationContext);
		this.applicationContext = (AbstractApplicationContext) applicationContext;
	}

	/**
	 * Determines whether any conversion logic is applied within the local transport. When false, objects pass through
	 * without any modification; default true.
	 */
	public void setConvertWithinTransport(boolean convertWithinTransport) {
		this.convertWithinTransport = convertWithinTransport;
	}

	@Override
	public void setCodec(MultiTypeCodec<Object> codec) {
		super.setCodec(codec);
		this.hasCodec = codec != null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(applicationContext, "The 'applicationContext' property cannot be null");
		if (convertWithinTransport) {
			Assert.isTrue(hasCodec, "The 'codec' property cannot be null if 'convertWithinTransport' is true");
		}
	}

	/**
	 * Looks up or creates a DirectChannel with the given name and creates a bridge from that channel to the provided
	 * channel instance.
	 */
	@Override
	public void bindConsumer(String name, MessageChannel moduleInputChannel, Collection<MediaType> acceptedMediaTypes,
			boolean aliasHint) {
		SharedChannelProvider<?> channelProvider = aliasHint ? queueChannelProvider
				: directChannelProvider;
		doRegisterConsumer(name, moduleInputChannel, acceptedMediaTypes, channelProvider);
	}

	@Override
	public void bindPubSubConsumer(String name, MessageChannel moduleInputChannel,
			Collection<MediaType> acceptedMediaTypes) {
		doRegisterConsumer(name, moduleInputChannel, acceptedMediaTypes, pubsubChannelProvider);
	}

	private void doRegisterConsumer(String name, MessageChannel moduleInputChannel,
			Collection<MediaType> acceptedMediaTypes,
			SharedChannelProvider<?> channelProvider) {
		Assert.hasText(name, "a valid name is required to register an inbound channel");
		Assert.notNull(moduleInputChannel, "channel must not be null");
		AbstractMessageChannel registeredChannel = channelProvider.lookupOrCreateSharedChannel(name);
		bridge(registeredChannel, moduleInputChannel, "inbound." + registeredChannel.getComponentName(),
				acceptedMediaTypes);
	}

	/**
	 * Looks up or creates a DirectChannel with the given name and creates a bridge to that channel from the provided
	 * channel instance.
	 */
	@Override
	public void bindProducer(String name, MessageChannel moduleOutputChannel, boolean aliasHint) {
		SharedChannelProvider<?> channelProvider = aliasHint ? queueChannelProvider
				: directChannelProvider;
		doRegisterProducer(name, moduleOutputChannel, channelProvider);
	}

	@Override
	public void bindPubSubProducer(String name, MessageChannel moduleOutputChannel) {
		doRegisterProducer(name, moduleOutputChannel, pubsubChannelProvider);
	}

	private void doRegisterProducer(String name, MessageChannel moduleOutputChannel,
			SharedChannelProvider<?> channelProvider) {
		Assert.hasText(name, "a valid name is required to register an outbound channel");
		Assert.notNull(moduleOutputChannel, "channel must not be null");
		AbstractMessageChannel registeredChannel = channelProvider.lookupOrCreateSharedChannel(name);
		bridge(moduleOutputChannel, registeredChannel, "outbound." + registeredChannel.getComponentName());
	}

	@Override
	public void bindRequestor(final String name, MessageChannel requests, final MessageChannel replies) {
		final MessageChannel requestChannel = this.findOrCreateRequestReplyChannel("requestor." + name);
		// TODO: handle Pollable ?
		Assert.isInstanceOf(SubscribableChannel.class, requests);
		((SubscribableChannel) requests).subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				requestChannel.send(message);
			}
		});

		ExecutorChannel replyChannel = this.findOrCreateRequestReplyChannel("replier." + name);
		replyChannel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				replies.send(message);
			}
		});
	}

	@Override
	public void bindReplier(String name, final MessageChannel requests, MessageChannel replies) {
		SubscribableChannel requestChannel = this.findOrCreateRequestReplyChannel("requestor." + name);
		requestChannel.subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				requests.send(message);
			}
		});

		// TODO: handle Pollable ?
		Assert.isInstanceOf(SubscribableChannel.class, replies);
		final SubscribableChannel replyChannel = this.findOrCreateRequestReplyChannel("replier." + name);
		((SubscribableChannel) replies).subscribe(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				replyChannel.send(message);
			}
		});
	}

	private synchronized ExecutorChannel findOrCreateRequestReplyChannel(String name) {
		ExecutorChannel channel = this.requestReplyChannels.get(name);
		if (channel == null) {
			channel = new ExecutorChannel(this.executor);
			this.requestReplyChannels.put(name, channel);
		}
		return channel;
	}

	@Override
	public void unbindProducer(String name, MessageChannel channel) {
		this.requestReplyChannels.remove("replier." + name);
		MessageChannel requestChannel = this.requestReplyChannels.remove("requestor." + name);
		if (requestChannel == null) {
			super.unbindProducer(name, channel);
		}
	}

	protected <T extends AbstractMessageChannel> T createSharedChannel(String name, Class<T> requiredType) {
		try {
			T channel = requiredType.newInstance();
			channel.setComponentName(name);
			channel.setBeanFactory(applicationContext);
			channel.setBeanName(name);
			channel.afterPropertiesSet();
			applicationContext.getBeanFactory().registerSingleton(name, channel);
			return channel;
		}
		catch (Exception e) {
			throw new IllegalArgumentException("failed to create channel: " + name, e);
		}
	}

	protected BridgeHandler bridge(MessageChannel from, MessageChannel to, String bridgeName) {
		return bridge(from, to, bridgeName, null);
	}


	protected BridgeHandler bridge(MessageChannel from, MessageChannel to, String bridgeName,
			final Collection<MediaType> acceptedMediaTypes) {

		final boolean isInbound = bridgeName.startsWith("inbound.");

		BridgeHandler handler = new BridgeHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				/*
				 * optimization for local transport, just pass through if false
				 */
				if (convertWithinTransport) {
					if (acceptedMediaTypes != null) {
						if (isInbound) {
							return transformPayloadForConsumerIfNecessary(requestMessage, acceptedMediaTypes);
						}
					}
				}
				return requestMessage;
			}

		};

		handler.setOutputChannel(to);
		handler.setBeanName(bridgeName);
		handler.afterPropertiesSet();

		// Usage of a CEFB allows to handle both Subscribable & Pollable channels the same way
		ConsumerEndpointFactoryBean cefb = new ConsumerEndpointFactoryBean();
		cefb.setInputChannel(from);
		cefb.setHandler(handler);
		cefb.setBeanFactory(applicationContext.getBeanFactory());
		if (from instanceof PollableChannel) {
			cefb.setPollerMetadata(poller);
		}
		try {
			cefb.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}

		try {
			cefb.getObject().setComponentName(handler.getComponentName());
			Binding binding = isInbound ? Binding.forConsumer(cefb.getObject(), to)
					: Binding.forProducer(from, cefb.getObject());
			addBinding(binding);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}

		cefb.start();
		return handler;
	}

	protected <T> T getBean(String name, Class<T> requiredType) {
		return this.applicationContext.getBean(name, requiredType);
	}

	/**
	 * Looks up or optionally creates a new channel to use.
	 * 
	 * @author Eric Bottard
	 */
	private abstract class SharedChannelProvider<T extends AbstractMessageChannel> {

		private final Class<T> requiredType;

		private SharedChannelProvider(Class<T> clazz) {
			this.requiredType = clazz;
		}

		private final T lookupOrCreateSharedChannel(String name) {
			T channel = lookupSharedChannel(name);
			if (channel == null) {
				channel = createSharedChannel(name);
				channel.setComponentName(name);
				channel.setBeanFactory(applicationContext);
				channel.setBeanName(name);
				channel.afterPropertiesSet();
				applicationContext.getBeanFactory().registerSingleton(name, channel);
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
