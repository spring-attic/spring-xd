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

package org.springframework.xd.dirt.test.process;

import org.springframework.messaging.Message;
import org.springframework.xd.dirt.server.singlenode.SingleNodeApplication;
import org.springframework.xd.dirt.test.sink.NamedChannelSink;

/**
 * Creates a stream to test a processing chain including a source but no sink and adds a {@link NamedChannelSink} to
 * create a complete stream.
 * 
 * @author David Turanski
 * 
 */
public class SingleNodeProcessingChainConsumer extends AbstractSingleNodeProcessingChain implements NamedChannelSink {

	public SingleNodeProcessingChainConsumer(SingleNodeApplication application, String streamName,
			String processingChain) {
		super(application, streamName, processingChain);
	}

	public SingleNodeProcessingChainConsumer(SingleNodeApplication application, String streamName,
			String processingChain, String moduleResourceLocation) {
		super(application, streamName, processingChain, moduleResourceLocation);
	}

	@Override
	public Message<?> receive(int timeout) {
		return sink.receive(timeout);
	}

	@Override
	public Object receivePayload(int timeout) {
		return sink.receivePayload(timeout);
	}

	@Override
	protected boolean createSink() {
		return true;
	}

	@Override
	protected boolean createSource() {
		return false;
	}
}
