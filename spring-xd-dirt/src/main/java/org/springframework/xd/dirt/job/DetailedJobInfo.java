/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.xd.dirt.job;

import org.springframework.batch.core.ExitStatus;

/**
 * A job info for batch jobs that provides more details than the base {@link JobInfo}
 * 
 * @author Andrew Eisenberg
 * @author Ilayaperumal Gopinathan
 */
public class DetailedJobInfo extends JobInfo {

	private String jobParameters;

	private String duration;

	private String startTime;

	private String startDate;

	private int stepExecutionCount;

	private ExitStatus exitStatus;

	private JobExecutionInfo lastExecutionInfo;

	public DetailedJobInfo(String name, int executionCount, boolean launchable, boolean incrementable,
			JobExecutionInfo lastExecution) {
		super(name, executionCount, launchable, incrementable);
		this.lastExecutionInfo = lastExecution;
		if (lastExecutionInfo != null) {
			jobParameters = lastExecution.getJobParametersString();
			duration = lastExecution.getDuration();
			startTime = lastExecution.getStartTime();
			startDate = lastExecution.getStartDate();
			stepExecutionCount = lastExecution.getStepExecutionCount();
			exitStatus = lastExecution.getJobExecution().getExitStatus();
		}
	}

	public JobExecutionInfo getLastExecutionInfo() {
		return lastExecutionInfo;
	}

	public String getJobParameters() {
		return jobParameters;
	}

	public String getDuration() {
		return duration;
	}

	public String getStartTime() {
		return startTime;
	}

	public String getStartDate() {
		return startDate;
	}

	public int getStepExecutionCount() {
		return stepExecutionCount;
	}

	public ExitStatus getExitStatus() {
		return exitStatus;
	}
}
