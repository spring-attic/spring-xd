/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.plugins.job;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.Lifecycle;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * 
 * @author Gunnar Hillert
 * @since 1.0
 * 
 */
public class JobTriggerBean implements Lifecycle {

	protected final Log logger = LogFactory.getLog(getClass());

	private final MessageChannel requestChannel;

	private final String jobParametersAsJsonMap;

	private final boolean executeBatchJobOnStartup;

	private boolean isRunning = false;

	public JobTriggerBean(String jobName, String jobParametersAsJsonMap, boolean executeBatchJobOnStartup,
			MessageChannel requestChannel) {
		Assert.hasText(jobName, "The jobName must not be empty.");
		Assert.notNull(requestChannel, "The requestChannel must not be null.");
		this.executeBatchJobOnStartup = executeBatchJobOnStartup;
		this.jobParametersAsJsonMap = jobParametersAsJsonMap;
		this.requestChannel = requestChannel;
	}

	@Override
	public void stop() {
		isRunning = false;
	}

	@Override
	public void start() {
		isRunning = true;

		if (executeBatchJobOnStartup) {
			executeBatchJob();
		}

	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	public void executeBatchJob() {
		requestChannel.send(MessageBuilder.withPayload(this.jobParametersAsJsonMap).build());
	}

}
