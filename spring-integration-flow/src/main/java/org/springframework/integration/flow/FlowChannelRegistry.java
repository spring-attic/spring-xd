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
package org.springframework.integration.flow;

import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.registry.LocalChannelRegistry;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.util.Assert;

/**
 * {@link ChannelRegistry} implementation for Spring Integration Flows used to create flow input and output channels used
 * by the declaring application context.
 * 
 * This varies from {@link LocalChannelRegistry} in that the input and output channels are 
 * not meant to be shared with other modules when composing modules (e.g., building streams).

 * @author David Turanski
 *
 */
public class FlowChannelRegistry extends LocalChannelRegistry {
	/**
	 * Register an inbound flow channel. This will always create a new {@link DirectChannel} and throw and exception if the
	 * channel, or bean with the same name already exists
	 */
	@Override
	public void inbound(String name, MessageChannel channel) {
		Assert.hasText(name, "a valid name is required to register an inbound channel");
		Assert.notNull(channel, "channel must not be null");
		DirectChannel registeredChannel = createSharedChannel(name, DirectChannel.class);
		bridge(registeredChannel, channel);
	}

	/**
	 * Register an outbound flow channel. This will always create a new {@link PublishSubscribeChannel} and throw and exception if the
	 * channel, or bean with the same name already exists
	 */
	@Override
	public void outbound(String name, MessageChannel channel) {
		Assert.hasText(name, "a valid name is required to register an outbound channel");
		Assert.notNull(channel, "channel must not be null");
		Assert.isTrue(channel instanceof SubscribableChannel,
				"channel must be of type " + SubscribableChannel.class.getName());
		PublishSubscribeChannel registeredChannel = this.lookupOrCreateSharedChannel(name, PublishSubscribeChannel.class);
		bridge((SubscribableChannel) channel, registeredChannel);
	}

	/**
	 * This implementation does not support taps. Throws an UnsupportedOperationException
	 */
	@Override
	public void tap(String name, MessageChannel channel) {
		throw new UnsupportedOperationException("tap is not supported by this ChannelRegistry implementation");
	}

}
