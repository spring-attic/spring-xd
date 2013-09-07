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
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;

/**
 * A {@link org.springframework.integration.support.channel.ChannelResolver} implementation that first checks for any
 * channel whose name begins with a colon in the {@link MessageBus}.
 * 
 * @author Mark Fisher
 */
public class MessageBusAwareChannelResolver extends BeanFactoryChannelResolver {

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
	public MessageChannel resolveChannelName(String name) {
		MessageChannel channel = null;
		if (name.startsWith(":")) {
			String channelName = name.substring(1);
			channel = channels.get(channelName);
			if (channel == null && messageBus != null) {
				channel = new DirectChannel();
				messageBus.bindProducer(channelName, channel, true);
				channels.put(channelName, channel);
			}
		}
		if (channel == null) {
			channel = super.resolveChannelName(name);
		}
		return channel;
	}

}
