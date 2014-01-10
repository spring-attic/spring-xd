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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.xd.dirt.plugins.job.BatchJobHeaders;

/**
 * @author Gunnar Hillert
 * @since 1.0
 */
public class SimpleXdChunkListener implements ChunkListener {

	private static final Log logger = LogFactory.getLog(SimpleXdChunkListener.class);

	private MessageChannel notificationsChannel;

	public void setNotificationsChannel(MessageChannel notificationsChannel) {
		this.notificationsChannel = notificationsChannel;
	}

	@Override
	public void afterChunk(ChunkContext context) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing afterChunk: " + context);
		}

		final ChunkContextInfo xdChunkContextInfo = convertChunkContext(context);

		notificationsChannel.send(MessageBuilder.withPayload(xdChunkContextInfo)
				.setHeader(BatchJobHeaders.BATCH_LISTENER_EVENT_TYPE,
						BatchListenerEventType.CHUNK_LISTENER_AFTER_CHUNK.name())
				.build());
	}

	@Override
	public void beforeChunk(ChunkContext context) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing beforeChunk: " + context);
		}

		final ChunkContextInfo xdChunkContextInfo = convertChunkContext(context);

		notificationsChannel.send(MessageBuilder.withPayload(xdChunkContextInfo)
				.setHeader(BatchJobHeaders.BATCH_LISTENER_EVENT_TYPE,
						BatchListenerEventType.CHUNK_LISTENER_BEFORE_CHUNK.name())
				.build());
	}

	@Override
	public void afterChunkError(ChunkContext context) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing afterChunkError: " + context);
		}

		final ChunkContextInfo xdChunkContextInfo = convertChunkContext(context);

		notificationsChannel.send(MessageBuilder.withPayload(xdChunkContextInfo)
				.setHeader(BatchJobHeaders.BATCH_LISTENER_EVENT_TYPE,
						BatchListenerEventType.CHUNK_LISTENER_AFTER_CHUNK_ERROR.name())
				.build());
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
