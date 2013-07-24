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

package org.springframework.integration.x.channel.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.util.Assert;
import org.springframework.xd.module.Module;

/**
 * A simple implementation of {@link ChannelRegistry} for in-process use. For inbound and
 * outbound, creates a {@link DirectChannel} and bridges the passed
 * {@link MessageChannel} to the channel which is registered in the given application
 * context. If that channel does not yet exist, it will be created. For tap, it adds a
 * {@link WireTap} for an inbound channel whose name matches the one provided. If no such
 * inbound channel exists at the time of the method invocation, it will throw an
 * Exception. Otherwise the provided channel instance will receive messages from the wire
 * tap on that inbound channel.
 *
 * @author David Turanski
 * @author Mark Fisher
 * @author Gary Russell
 * @since 1.0
 */
public class LocalChannelRegistry extends ChannelRegistrySupport implements ApplicationContextAware, InitializingBean {

	private volatile AbstractApplicationContext applicationContext;

	private final List<BridgeMetadata> bridges = Collections.synchronizedList(new ArrayList<BridgeMetadata>());

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Assert.isInstanceOf(AbstractApplicationContext.class, applicationContext);
		this.applicationContext = (AbstractApplicationContext) applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(applicationContext, "The 'applicationContext' property cannot be null");
	}

	/**
	 * Looks up or creates a DirectChannel with the given name and creates a bridge from
	 * that channel to the provided channel instance. Also registers a wire tap if the
	 * channel for the given name had been created. The target of the wire tap is a
	 * publish-subscribe channel.
	 */
	@Override
	public void inbound(String name, MessageChannel channel, Module module) {
		Assert.hasText(name, "a valid name is required to register an inbound channel");
		Assert.notNull(channel, "channel must not be null");
		DirectChannel registeredChannel = lookupOrCreateSharedChannel(name, DirectChannel.class);
		bridge(registeredChannel, channel, registeredChannel.getComponentName() + ".in.bridge", module);
		createSharedTapChannelIfNecessary(registeredChannel);
	}

	/**
	 * Looks up or creates a DirectChannel with the given name and creates a bridge to
	 * that channel from the provided channel instance.
	 */
	@Override
	public void outbound(String name, MessageChannel channel, Module module) {
		Assert.hasText(name, "a valid name is required to register an outbound channel");
		Assert.notNull(channel, "channel must not be null");
		Assert.isTrue(channel instanceof SubscribableChannel,
				"channel must be of type " + SubscribableChannel.class.getName());
		DirectChannel registeredChannel = lookupOrCreateSharedChannel(name, DirectChannel.class);
		bridge((SubscribableChannel) channel, registeredChannel, registeredChannel.getComponentName() + ".out.bridge", module);
	}

	/**
	 * Looks up a wiretap for the inbound channel with the given name and creates a
	 * bridge from that wiretap's output channel to the provided channel instance.
	 * Will throw an Exception if no such wiretap exists.
	 */
	@Override
	public void tap(String tapModule, String name, MessageChannel channel) {
		Assert.hasText(name, "a valid name is required to register a tap channel");
		Assert.notNull(channel, "channel must not be null");
		SubscribableChannel tapChannel = null;
		String tapName = "tap." + name;
		try {
			tapChannel = applicationContext.getBean(tapName, SubscribableChannel.class);
		} catch (Exception e) {
			throw new IllegalArgumentException("No tap channel exists for '" + name
					+ "'. A tap is only valid for a registered inbound channel.");
		}
		bridge(tapChannel, channel, tapName + ".bridge", tapModule, null);
	}

	@Override
	public void cleanAll(String name) {
		Assert.hasText(name, "a valid name is required to clean a module");
		synchronized (this.bridges) {
			Iterator<BridgeMetadata> iterator = this.bridges.iterator();
			while (iterator.hasNext()) {
				BridgeMetadata bridge = iterator.next();
				if (bridge.handler.getComponentName().startsWith(name) || name.equals(bridge.tapModule)) {
					bridge.channel.unsubscribe(bridge.handler);
					iterator.remove();
				}
			}
		}
	}

	protected synchronized <T extends AbstractMessageChannel> T lookupOrCreateSharedChannel(String name,
			Class<T> requiredType) {
		T channel = lookupSharedChannel(name, requiredType);
		if (channel == null) {
			channel = createSharedChannel(name, requiredType);
		}
		return channel;
	}

	protected synchronized <T extends AbstractMessageChannel> T lookupSharedChannel(String name, Class<T> requiredType) {
		T channel = null;
		if (applicationContext.containsBean(name)) {
			try {
				channel = applicationContext.getBean(name, requiredType);
			} catch (Exception e) {
				throw new IllegalArgumentException("bean '" + name
						+ "' is already registered but does not match the required type");
			}
		}
		return channel;
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
		} catch (Exception e) {
			throw new IllegalArgumentException("failed to create channel: " + name, e);
		}
	}

	private synchronized void createSharedTapChannelIfNecessary(AbstractMessageChannel channel) {
		String tapName = "tap." + channel.getComponentName();
		PublishSubscribeChannel tapChannel = null;
		if (!applicationContext.containsBean(tapName)) {
			tapChannel = createSharedChannel(tapName, PublishSubscribeChannel.class);
			WireTap wireTap = new WireTap(tapChannel);
			channel.addInterceptor(wireTap);
			bridge(tapChannel, new NullChannel(), channel.getComponentName() + ".to.null");
		} else {
			try {
				tapChannel = applicationContext.getBean(tapName, PublishSubscribeChannel.class);
			} catch (Exception e) {
				throw new IllegalArgumentException("bean '" + tapName
						+ "' is already registered but does not match the required type");
			}
		}
	}

	protected BridgeHandler bridge(SubscribableChannel from, MessageChannel to, String bridgeName) {
		return bridge(from, to, bridgeName, null, null);
	}

	protected BridgeHandler bridge(SubscribableChannel from, MessageChannel to, String bridgeName, Module module) {
		return bridge(from, to, bridgeName, null, module);
	}

	protected BridgeHandler bridge(SubscribableChannel from, MessageChannel to, String bridgeName, String tapModule, final Module module) {

		final boolean isInbound = bridgeName.endsWith("in.bridge");

		BridgeHandler handler = new BridgeHandler() {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				if (module != null) {
					if (isInbound) {
						// TODO: Optimize for never convert case (RTI)
						return transformInboundIfNecessary(requestMessage, module);
					}
				}
				return requestMessage;
			}

		};

		handler.setOutputChannel(to);
		handler.setBeanName(bridgeName);
		handler.afterPropertiesSet();
		from.subscribe(handler);
		if (!(to instanceof NullChannel)) {
			this.bridges.add(new BridgeMetadata(handler, from, tapModule));
		}
		return handler;
	}

	private class BridgeMetadata {
		private final BridgeHandler handler;
		private final SubscribableChannel channel;
		private final String tapModule;

		public BridgeMetadata(BridgeHandler handler, SubscribableChannel channel, String tapModule) {
			this.handler = handler;
			this.channel = channel;
			this.tapModule = tapModule;
		}

		@Override
		public String toString() {
			return "BridgeMetadata [handler=" + handler + ", channel=" + channel + ", tapModule=" + tapModule + "]";
		}

	}
}
