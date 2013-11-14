/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.x.bus;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.BeanFactoryMessageChannelDestinationResolver;

/**
 * A {@link org.springframework.integration.support.channel.ChannelResolver} implementation that first checks for any
 * channel whose name begins with a colon in the {@link MessageBus}.
 * 
 * @author Mark Fisher
 */
public class MessageBusAwareChannelResolver extends BeanFactoryMessageChannelDestinationResolver {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final Map<String, MessageChannel> channels = new HashMap<String, MessageChannel>();

	private volatile MessageBus messageBus;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		try {
			messageBus = beanFactory.getBean(MessageBus.class);
		}
		catch (Exception e) {
			logger.warn("failed to locate a MessageBus in the BeanFactory", e);
		}
	}

	@Override
	public MessageChannel resolveDestination(String name) {
		MessageChannel channel = null;
		if (name.indexOf(":") != -1) {
			channel = channels.get(name);
			if (channel == null && messageBus != null) {
				String[] tokens = name.split(":", 2);
				String type = tokens[0];
				String shortName = tokens[1];
				if ("queue".equals(type)) {
					channel = new DirectChannel();
					messageBus.bindProducer(shortName, channel, true);
				}
				else if ("topic".equals(type)) {
					channel = new PublishSubscribeChannel();
					messageBus.bindPubSubProducer(shortName, channel);
				}
				else {
					throw new IllegalArgumentException("unrecognized channel type: " + type);
				}
				channels.put(name, channel);
			}
		}
		if (channel == null) {
			channel = super.resolveDestination(name);
		}
		return channel;
	}

}
