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

package org.springframework.xd.dirt.integration.test.sink;

import org.springframework.xd.dirt.integration.bus.MessageBus;


/**
 * A {@link NamedChannelSink} implementation that binds to a Queue Channel.
 * 
 * @author David Turanski
 * 
 */
public class SingleNodeNamedQueueSink extends AbstractSingleNodeNamedChannelSink {

	public SingleNodeNamedQueueSink(MessageBus messageBus, String sharedChannelName) {
		super(messageBus, sharedChannelName);
	}


	@Override
	protected void bind() {
		this.messageBus.bindConsumer(this.sharedChannelName, this.messageChannel);
	}


}
