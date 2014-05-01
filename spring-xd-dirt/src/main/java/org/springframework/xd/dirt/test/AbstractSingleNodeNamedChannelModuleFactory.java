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

import org.springframework.util.Assert;
import org.springframework.xd.dirt.integration.bus.MessageBus;


/**
 * Base class for factories used to create NamedChannelSource and NamedChannelSink. Factories are used to bind using p2p
 * or pub-sub {@link MessageBus} bindings for named channels prefixed by 'queue:' or 'topic:' respectively.
 * 
 * @author David Turanski
 * 
 */
public abstract class AbstractSingleNodeNamedChannelModuleFactory {

	protected final static String QUEUE_PREFIX = "queue:";

	protected final static String TOPIC_PREFIX = "topic:";

	protected final MessageBus messageBus;

	protected AbstractSingleNodeNamedChannelModuleFactory(MessageBus messageBus) {
		Assert.notNull(messageBus, "messageBus cannot be null");
		this.messageBus = messageBus;
	}

	protected void validateChannelName(String channelName) {
		Assert.notNull(channelName, "channelName cannot be null");
		Assert.isTrue(channelName.startsWith(QUEUE_PREFIX) || channelName.startsWith(TOPIC_PREFIX),
				String.format("channel name %s is not well formed. It must start with %s or %s", channelName,
						QUEUE_PREFIX, TOPIC_PREFIX));
	}
}
