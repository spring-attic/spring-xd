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

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.xd.dirt.plugins.job.BatchJobHeaders;

/**
 * @author Gunnar Hillert
 * @since 1.0
 */
public class SimpleXdStepExecutionListener implements StepExecutionListener {

	private static final Log logger = LogFactory.getLog(SimpleXdStepExecutionListener.class);

	private MessageChannel notificationsChannel;

	public void setNotificationsChannel(MessageChannel notificationsChannel) {
		this.notificationsChannel = notificationsChannel;
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing beforeStep: " + stepExecution);
		}
		notificationsChannel.send(MessageBuilder.withPayload(stepExecution)
				.setHeader(BatchJobHeaders.BATCH_LISTENER_EVENT_TYPE,
						BatchListenerEventType.STEP_EXECUTION_LISTENER_BEFORE_STEP.name())
				.build());
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing afterStep: " + stepExecution);
		}
		notificationsChannel.send(MessageBuilder.withPayload(stepExecution)
				.setHeader(BatchJobHeaders.BATCH_LISTENER_EVENT_TYPE,
						BatchListenerEventType.STEP_EXECUTION_LISTENER_AFTER_STEP.name())
				.build());
		return null;
	}
}
