/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.plugins.job.support.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.messaging.SubscribableChannel;

/**
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @since 1.0
 */
public class SimpleXdChunkListener extends BatchJobListener<ChunkContextInfo> implements ChunkListener {

	private static final Logger logger = LoggerFactory.getLogger(SimpleXdChunkListener.class);

	public SimpleXdChunkListener(SubscribableChannel chunkEventsChannel, SubscribableChannel aggregatedEventsChannel) {
		super(chunkEventsChannel, aggregatedEventsChannel);
	}

	@Override
	public void afterChunk(ChunkContext context) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing afterChunk: " + context);
		}
		publish(convertChunkContext(context));
	}

	@Override
	public void beforeChunk(ChunkContext context) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing beforeChunk: " + context);
		}
		publish(convertChunkContext(context));
	}

	@Override
	public void afterChunkError(ChunkContext context) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing afterChunkError: " + context);
		}
		publish(convertChunkContext(context));
	}

	private ChunkContextInfo convertChunkContext(ChunkContext context) {

		final ChunkContextInfo chunkContextInfo = new ChunkContextInfo();
		chunkContextInfo.setComplete(context.isComplete());
		chunkContextInfo.setStepExecution(context.getStepContext().getStepExecution());

		final String[] attributeNames = context.attributeNames();

		for (String attributeName : attributeNames) {
			final Object attribute = context.getAttribute(attributeName);
			chunkContextInfo.getAttributes().put(attributeName, attribute);
		}

		return chunkContextInfo;
	}
}
