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

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.messaging.SubscribableChannel;

/**
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @since 1.0
 */
public class SimpleXdJobExecutionListener extends BatchJobListener<JobExecution> implements JobExecutionListener {

	public SimpleXdJobExecutionListener(SubscribableChannel jobExecutionEventsChannel,
			SubscribableChannel aggregatedEventsChannel) {
		super(jobExecutionEventsChannel, aggregatedEventsChannel);
	}

	private static final Logger logger = LoggerFactory.getLogger(SimpleXdJobExecutionListener.class);

	@Override
	public void beforeJob(JobExecution jobExecution) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing beforeJob: " + jobExecution);
		}
		publish(jobExecution);
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		if (logger.isDebugEnabled()) {
			logger.debug("Executing afterJob: " + jobExecution);
		}
		publish(jobExecution);
	}
}
