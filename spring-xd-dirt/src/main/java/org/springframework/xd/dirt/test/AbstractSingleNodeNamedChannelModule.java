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

package org.springframework.xd.dirt.test;

import java.util.Collections;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.integration.bus.MessageBus;


/**
 * Base class for SingleNodeNamedChannel resources used to bind a {@link MessageChannel} to a shared channel using a
 * {@link MessageBus}.
 * 
 * @author David Turanski
 * 
 */
public abstract class AbstractSingleNodeNamedChannelModule<T extends MessageChannel> implements NamedChannelModule {

	protected final static List<MediaType> ALL_MEDIA_TYPES = Collections.singletonList(MediaType.ALL);

	protected final T messageChannel;

	protected final MessageBus messageBus;

	protected final String sharedChannelName;

	protected AbstractSingleNodeNamedChannelModule(MessageBus messageBus, T messageChannel, String sharedChannelName) {
		Assert.notNull(messageBus, "messageBus cannot be null");
		Assert.notNull(messageChannel, "messageChannel cannot be null");
		Assert.hasText(sharedChannelName, "sharedChannelName cannot be null");
		this.messageBus = messageBus;
		this.messageChannel = messageChannel;
		this.sharedChannelName = sharedChannelName;
		bind();
	}

	protected abstract void bind();
}
