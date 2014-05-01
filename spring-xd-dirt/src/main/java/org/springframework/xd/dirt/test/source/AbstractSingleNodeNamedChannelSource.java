/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.xd.dirt.test.source;

import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.test.AbstractSingleNodeNamedChannelModule;


/**
 * Base class for SingleNode named channel source types.
 * 
 * @author David Turanski
 * 
 */
public abstract class AbstractSingleNodeNamedChannelSource extends
		AbstractSingleNodeNamedChannelModule<SubscribableChannel> implements
		NamedChannelSource {

	protected AbstractSingleNodeNamedChannelSource(MessageBus messageBus, SubscribableChannel messageChannel,
			String sharedChannelName) {
		super(messageBus, messageChannel, sharedChannelName);
	}

	@Override
	public void unbind() {
		this.messageBus.unbindProducer(this.sharedChannelName, this.messageChannel);
	}

	@Override
	public void send(Message<?> message) {
		this.messageChannel.send(message);
	}

	@Override
	public void sendPayload(Object payload) {
		this.messageChannel.send(MessageBuilder.withPayload(payload).build());
	}

}
