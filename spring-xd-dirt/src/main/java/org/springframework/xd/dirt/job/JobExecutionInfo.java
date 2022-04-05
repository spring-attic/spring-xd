/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.job;

import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import org.springframework.batch.admin.web.JobParametersExtractor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.xd.rest.domain.util.TimeUtils;


/**
 * Represents Batch job execution info.
 *
 * @author Dave Syer
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 * @since 1.0
 */
public class JobExecutionInfo {

	private DateFormat dateFormat = TimeUtils.getDefaultDateFormat();

	private DateFormat timeFormat = TimeUtils.getDefaultTimeFormat();

	private DateFormat durationFormat = TimeUtils.getDefaultDurationFormat();

	private Long executionId;

	private int stepExecutionCount;

	private Long jobId;

	private String jobName;

	private String startDate = "";

	private String startTime = "";

	private String duration = "";

	private JobExecution jobExecution;

	private Properties jobParameters;

	private String jobParametersString;

	private boolean restartable = false;

	private boolean abandonable = false;

	private boolean stoppable = false;

	private JobParametersConverter converter = new DefaultJobParametersConverter();

	private final TimeZone timeZone;

	public JobExecutionInfo(JobExecution jobExecution, TimeZone timeZone) {

		this.jobExecution = jobExecution;
		this.timeZone = timeZone;
		this.executionId = jobExecution.getId();
		this.jobId = jobExecution.getJobId();
		this.stepExecutionCount = jobExecution.getStepExecutions().size();
		this.jobParameters = converter.getProperties(jobExecution.getJobParameters());
		this.jobParametersString = new JobParametersExtractor().fromJobParameters(jobExecution.getJobParameters());

		JobInstance jobInstance = jobExecution.getJobInstance();
		if (jobInstance != null) {
			this.jobName = jobInstance.getJobName();
			BatchStatus status = jobExecution.getStatus();
			this.restartable = status.isGreaterThan(BatchStatus.STOPPING) && status.isLessThan(BatchStatus.ABANDONED);
			this.abandonable = status.isGreaterThan(BatchStatus.STARTED) && status != BatchStatus.ABANDONED;
			this.stoppable = status.isLessThan(BatchStatus.STOPPING);
		}
		else {
			this.jobName = "?";
		}

		// Duration is always in GMT
		durationFormat.setTimeZone(TimeUtils.getDefaultTimeZone());
		// The others can be localized
		timeFormat.setTimeZone(timeZone);
		dateFormat.setTimeZone(timeZone);
		if (jobExecution.getStartTime() != null) {
			this.startDate = dateFormat.format(jobExecution.getStartTime());
			this.startTime = timeFormat.format(jobExecution.getStartTime());
			Date endTime = jobExecution.getEndTime() != null ? jobExecution.getEndTime() : new Date();
			this.duration = durationFormat.format(new Date(endTime.getTime() - jobExecution.getStartTime().getTime()));
		}

	}

	public TimeZone getTimeZone() {
		return timeZone;
	}

	public String getName() {
		return jobName;
	}

	public Long getExecutionId() {
		return executionId;
	}

	public int getStepExecutionCount() {
		return stepExecutionCount;
	}

	public Long getJobId() {
		return jobId;
	}

	public String getStartDate() {
		return startDate;
	}

	public String getStartTime() {
		return startTime;
	}

	public String getDuration() {
		return duration;
	}

	public JobExecution getJobExecution() {
		return jobExecution;
	}

	public boolean isRestartable() {
		return restartable;
	}

	public boolean isAbandonable() {
		return abandonable;
	}

	public boolean isStoppable() {
		return stoppable;
	}

	public String getJobParametersString() {
		return jobParametersString;
	}

	public Properties getJobParameters() {
		return jobParameters;
	}
}
