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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.Unmodifiable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.listener.ItemListenerSupport;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.xd.dirt.plugins.job.BatchJobHeaders;

/**
 * @author Gunnar Hillert
 * @since 1.0
 */
public class SimpleXdItemListener extends ItemListenerSupport<Object, Object> {

	private static final Log logger = LogFactory.getLog(SimpleXdItemListener.class);

	private MessageChannel notificationsChannel;

	public void setNotificationsChannel(MessageChannel notificationsChannel) {
		this.notificationsChannel = notificationsChannel;
	}

	@Override
	public void onReadError(Exception exception) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing onReadError: " + exception.getMessage(), exception);
		}
		final Message<Exception> message = MessageBuilder.withPayload(exception)
				.setHeader(BatchJobHeaders.BATCH_LISTENER_EVENT_TYPE,
						BatchListenerEventType.ITEM_LISTENER_ON_READ_ERROR)
				.build();
		notificationsChannel.send(message);
	}

	@Override
	public void onProcessError(Object item, Exception exception) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing onProcessError: " + exception.getMessage(), exception);
		}
		final Message<Object> message = MessageBuilder.withPayload(item)
				.setHeader(BatchJobHeaders.BATCH_EXCEPTION, exception)
				.setHeader(BatchJobHeaders.BATCH_LISTENER_EVENT_TYPE,
						BatchListenerEventType.ITEM_LISTENER_ON_PROCESS_ERROR.name())
				.build();
		notificationsChannel.send(message);
	}

	/**
	 * In order to support serialization with Kryo, the list of items is copied into a new List as the passed in List is
	 * {@link Unmodifiable}.
	 */
	@Override
	public void afterWrite(List<? extends Object> items) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing afterWrite: " + items);
		}
		final List<? extends Object> copiedItems = new ArrayList<Object>(items);
		notificationsChannel.send(MessageBuilder.withPayload(copiedItems)
				.setHeader(BatchJobHeaders.BATCH_LISTENER_EVENT_TYPE,
						BatchListenerEventType.ITEM_LISTENER_AFTER_WRITE.name())
				.build());
	}

	@Override
	public void onWriteError(Exception exception, List<? extends Object> items) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing onWriteError: " + exception.getMessage(), exception);
		}
		final Message<?> message = MessageBuilder.withPayload(items)
				.setHeader(BatchJobHeaders.BATCH_EXCEPTION, exception)
				.setHeader(BatchJobHeaders.BATCH_LISTENER_EVENT_TYPE,
						BatchListenerEventType.ITEM_LISTENER_ON_WRITE_ERROR.name())
				.build();
		notificationsChannel.send(message);
	}

}
