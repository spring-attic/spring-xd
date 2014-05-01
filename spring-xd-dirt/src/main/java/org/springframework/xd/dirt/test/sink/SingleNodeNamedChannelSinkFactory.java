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

package org.springframework.xd.dirt.test.sink;

import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.test.AbstractSingleNodeNamedChannelModuleFactory;


/**
 * A factory for creating {@link NamedChannelSink}s.
 * 
 * @author David Turanski
 * 
 */
public class SingleNodeNamedChannelSinkFactory extends AbstractSingleNodeNamedChannelModuleFactory {

	public SingleNodeNamedChannelSinkFactory(MessageBus messageBus) {
		super(messageBus);
	}

	public NamedChannelSink createNamedChannelSink(String channelName) {
		validateChannelName(channelName);
		if (channelName.startsWith(QUEUE_PREFIX)) {
			return new SingleNodeNamedQueueSink(this.messageBus, channelName);
		}
		return new SingleNodeNamedTopicSink(this.messageBus, channelName);

	}

}
