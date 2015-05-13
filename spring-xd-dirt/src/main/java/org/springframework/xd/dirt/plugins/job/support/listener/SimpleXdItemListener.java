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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.messaging.SubscribableChannel;

/**
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @since 1.0
 */
public final class SimpleXdItemListener<I, O> extends BatchJobListener<Object> implements ItemReadListener<I>,
		ItemProcessListener<I, Object>,
		ItemWriteListener<Object> {

	private static final Logger logger = LoggerFactory.getLogger(SimpleXdItemListener.class);

	public SimpleXdItemListener(SubscribableChannel itemEventsChannel, SubscribableChannel aggregatedEventsChannel) {
		super(itemEventsChannel, aggregatedEventsChannel);
	}

	@Override
	public void onReadError(Exception exception) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing onReadError: " + exception.getMessage(), exception);
		}
		publish(exception.getMessage());
	}

	@Override
	public void onProcessError(I item, Exception exception) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing onProcessError: " + exception.getMessage(), exception);
		}
		publishWithThrowableHeader(item, exception.getMessage());
	}

	@Override
	public void afterWrite(List<? extends Object> items) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing afterWrite: " + items);
		}
		publish(items.size() + " items have been written.");
	}

	@Override
	public void onWriteError(Exception exception, List<? extends Object> items) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing onWriteError: " + exception.getMessage(), exception);
		}
		String payload = "Exception while " + items.size() + " items are attempted to be written.";
		publishWithThrowableHeader(payload, exception.getMessage());
	}

	@Override
	public void beforeWrite(List<? extends Object> items) {
		publish(items.size() + " items to be written.");
	}

	// TODO: Currently these are NO-OP and the notification messages are TBD.
	@Override
	public void beforeProcess(I item) {
	}

	@Override
	public void afterProcess(I item, Object result) {
	}

	@Override
	public void beforeRead() {
	}

	@Override
	public void afterRead(I item) {
	}

}
