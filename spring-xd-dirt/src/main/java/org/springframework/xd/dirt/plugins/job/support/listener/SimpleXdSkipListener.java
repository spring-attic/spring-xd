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

import org.springframework.batch.core.SkipListener;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.xd.dirt.plugins.job.BatchJobHeaders;

/**
 * @author Gunnar Hillert
 * @since 1.0
 */
public class SimpleXdSkipListener implements SkipListener<Object, Object> {

	private static final Log logger = LogFactory.getLog(SimpleXdSkipListener.class);

	private MessageChannel notificationsChannel;

	public void setNotificationsChannel(MessageChannel notificationsChannel) {
		this.notificationsChannel = notificationsChannel;
	}

	@Override
	public void onSkipInRead(Throwable t) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing onSkipInRead: " + t.getMessage(), t);
		}
		notificationsChannel.send(MessageBuilder.withPayload(t)
				.setHeader(BatchJobHeaders.BATCH_LISTENER_EVENT_TYPE,
						BatchListenerEventType.SKIP_LISTENER_ON_SKIP_IN_READ.name())
				.build());
	}

	@Override
	public void onSkipInWrite(Object item, Throwable throwable) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing onSkipInWrite: " + throwable.getMessage(), throwable);
		}
		final Message<Object> message = MessageBuilder.withPayload(item)
				.setHeader(BatchJobHeaders.BATCH_EXCEPTION, throwable)
				.setHeader(BatchJobHeaders.BATCH_LISTENER_EVENT_TYPE,
						BatchListenerEventType.SKIP_LISTENER_ON_SKIP_IN_WRITE.name())
				.build();
		notificationsChannel.send(message);
	}

	@Override
	public void onSkipInProcess(Object item, Throwable throwable) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing onSkipInProcess: " + throwable.getMessage(), throwable);
		}
		final Message<Object> message = MessageBuilder.withPayload(item)
				.setHeader(BatchJobHeaders.BATCH_EXCEPTION, throwable)
				.setHeader(BatchJobHeaders.BATCH_LISTENER_EVENT_TYPE,
						BatchListenerEventType.SKIP_LISTENER_ON_SKIP_IN_PROCESS.name())
				.build();
		notificationsChannel.send(message);
	}
}
