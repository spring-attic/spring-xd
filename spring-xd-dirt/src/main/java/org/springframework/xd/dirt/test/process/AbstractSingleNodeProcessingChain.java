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

package org.springframework.xd.dirt.test.process;

import org.springframework.util.Assert;
import org.springframework.xd.dirt.integration.bus.MessageBus;
import org.springframework.xd.dirt.module.ResourceModuleRegistry;
import org.springframework.xd.dirt.server.singlenode.SingleNodeApplication;
import org.springframework.xd.dirt.stream.StreamDefinition;
import org.springframework.xd.dirt.test.SingleNodeIntegrationTestSupport;
import org.springframework.xd.dirt.test.sink.NamedChannelSink;
import org.springframework.xd.dirt.test.sink.SingleNodeNamedQueueSink;
import org.springframework.xd.dirt.test.source.NamedChannelSource;
import org.springframework.xd.dirt.test.source.SingleNodeNamedQueueSource;


/**
 * A helper class for building single node streams that use a {@link NamedChannelSource} and {@link NamedChannelSink}.
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

	protected final SingleNodeIntegrationTestSupport integrationSupport;

	protected AbstractSingleNodeProcessingChain(SingleNodeApplication application, String streamName,
			String processingChain) {
		this(application, streamName, processingChain, "file:./config");

	}

	protected AbstractSingleNodeProcessingChain(SingleNodeApplication application, String streamName,
			String processingChain, String moduleResourceLocation) {
		Assert.notNull(application, "application cannot be null");
		Assert.hasText(processingChain, "processingChain cannot be null or empty");
		Assert.hasText(streamName, "streamName cannot be null or empty");
		Assert.hasText(moduleResourceLocation, "moduleResourceLocation cannot be null or empty");
		this.integrationSupport = new SingleNodeIntegrationTestSupport(application);
		this.integrationSupport.addModuleRegistry(new ResourceModuleRegistry(moduleResourceLocation));
		String streamDefinition = buildStreamDefinition(processingChain);
		stream = new StreamDefinition(streamName, streamDefinition);

		integrationSupport.createAndDeployStream(stream);

		messageBus = integrationSupport.messageBus();
		this.sink = createSink() ? new SingleNodeNamedQueueSink(messageBus, QUEUE_CONSUMER) : null;
		this.source = createSource() ? new SingleNodeNamedQueueSource(messageBus, QUEUE_PRODUCER) : null;

	}

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
		integrationSupport.undeployAndDestroyStream(stream);
	}

	protected abstract boolean createSink();

	protected abstract boolean createSource();
}
