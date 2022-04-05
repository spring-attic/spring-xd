/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.xd.dirt.test.sink;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.test.AbstractSingleNodeNamedChannelModule;


/**
 * Base class for SingleNode named channel sink types.
 * 
 * @author David Turanski
 * 
 */
public abstract class AbstractSingleNodeNamedChannelSink extends AbstractSingleNodeNamedChannelModule<QueueChannel>
		implements NamedChannelSink {

	protected AbstractSingleNodeNamedChannelSink(MessageBus messageBus, String sharedChannelName) {
		super(messageBus, new QueueChannel(), sharedChannelName);
	}

	@Override
	public Message<?> receive(int timeout) {
		return this.messageChannel.receive(timeout);
	}

	@Override
	public Object receivePayload(int timeout) {
		Message<?> message = this.messageChannel.receive(timeout);
		return message == null ? null : message.getPayload();
	}

	@Override
	public void unbind() {
		this.messageBus.unbindConsumer(this.sharedChannelName, this.messageChannel);
	}

}
