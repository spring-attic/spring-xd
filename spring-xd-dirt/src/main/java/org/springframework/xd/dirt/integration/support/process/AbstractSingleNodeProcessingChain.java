/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.dirt.integration.support.process;

import org.springframework.integration.x.bus.MessageBus;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.integration.support.sink.NamedChannelSink;
import org.springframework.xd.dirt.integration.support.sink.SingleNodeNamedQueueSink;
import org.springframework.xd.dirt.integration.support.source.NamedChannelSource;
import org.springframework.xd.dirt.integration.support.source.SingleNodeNamedQueueSource;
import org.springframework.xd.dirt.server.SingleNodeApplication;
import org.springframework.xd.dirt.stream.StreamDefinition;


/**
 * A helper class for building single node streams that use a {@link NamedChannelSource} and {@link NamedChannelSink}
 * 
 * @author David Turanski
 */
public abstract class AbstractSingleNodeProcessingChain {

	private static final String QUEUE_CONSUMER = "queue:consumer";

	private static final String QUEUE_PRODUCER = "queue:producer";

	private final StreamDefinition stream;

	private final MessageBus messageBus;

	protected final NamedChannelSource source;

	protected final NamedChannelSink sink;

	protected final SingleNodeApplication application;

	protected AbstractSingleNodeProcessingChain(SingleNodeApplication application, String streamName,
			String processingChain) {
		Assert.notNull(application, "application cannot be null");
		Assert.hasText(processingChain, "processingChain cannot be null or empty");
		Assert.hasText(streamName, "streamName cannot be null or empty");
		this.application = application;
		String streamDefinition = buildStreamDefinition(processingChain);
		stream = new StreamDefinition(streamName, streamDefinition);

		application.createAndDeployStream(stream);

		messageBus = application.messageBus();
		this.sink = createSink() ? new SingleNodeNamedQueueSink(messageBus, QUEUE_CONSUMER) : null;
		this.source = createSource() ? new SingleNodeNamedQueueSource(messageBus, QUEUE_PRODUCER) : null;

	}

	/**
	 * @return
	 */
	private String buildStreamDefinition(String processingChain) {
		StringBuilder sb = new StringBuilder();
		if (createSource()) {
			sb.append(QUEUE_PRODUCER).append(">");
		}
		sb.append(processingChain);
		if (createSink()) {
			sb.append(">").append(QUEUE_CONSUMER);
		}
		return sb.toString();
	}

	public void unbind() {
		if (sink != null) {
			sink.unbind();
		}
		if (source != null) {
			source.unbind();
		}
	}

	public void destroy() {
		unbind();
		application.undeployAndDestroyStream(stream);
	}

	protected abstract boolean createSink();

	protected abstract boolean createSource();
}
