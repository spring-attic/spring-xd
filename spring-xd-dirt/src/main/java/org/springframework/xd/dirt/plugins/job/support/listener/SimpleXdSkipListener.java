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

import org.springframework.batch.core.SkipListener;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.xd.dirt.plugins.job.BatchJobHeaders;

/**
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @since 1.0
 */
public class SimpleXdSkipListener extends BatchJobListener<Object> implements SkipListener<Object, Object> {

	private static final Logger logger = LoggerFactory.getLogger(SimpleXdSkipListener.class);

	public SimpleXdSkipListener(SubscribableChannel skipEventsChannel, SubscribableChannel aggregatedEventsChannel) {
		super(skipEventsChannel, aggregatedEventsChannel);
	}

	@Override
	public void onSkipInRead(Throwable t) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing onSkipInRead: " + t.getMessage(), t);
		}
		Message<String> message = MessageBuilder.withPayload("Skipped when reading.").setHeader(
				BatchJobHeaders.BATCH_EXCEPTION, t).build();
		publish(message);
	}

	@Override
	public void onSkipInWrite(Object item, Throwable t) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing onSkipInWrite: " + t.getMessage(), t);
		}
		publishWithThrowableHeader(item, t.getMessage());
	}

	@Override
	public void onSkipInProcess(Object item, Throwable t) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing onSkipInProcess: " + t.getMessage(), t);
		}
		publishWithThrowableHeader(item, t.getMessage());
	}
}
