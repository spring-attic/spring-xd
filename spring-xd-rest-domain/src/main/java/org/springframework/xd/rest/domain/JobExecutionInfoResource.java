/*
 * Copyright 2013-2016 the original author or authors.
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

package org.springframework.xd.rest.domain;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.batch.admin.web.JobParametersExtractor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.xd.rest.domain.util.TimeUtils;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Represents job execution info resource.
 *
 * @author Dave Syer
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 */
@XmlRootElement
public class JobExecutionInfoResource extends ResourceSupport {

	private DateFormat dateFormat = TimeUtils.getDefaultDateFormat();

	private DateFormat timeFormat = TimeUtils.getDefaultTimeFormat();

	private DateFormat durationFormat = TimeUtils.getDefaultDurationFormat();

	private Long executionId;

	private int stepExecutionCount;

	private Long jobId;

	@JsonProperty("name")
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

	private boolean deployed = true;

	private boolean deleted = false;

	private boolean composedJob = false;

	private JobParametersConverter converter = new DefaultJobParametersConverter();

	private final TimeZone timeZone;

	final List<JobExecutionInfoResource> childJobExecutions = new ArrayList<JobExecutionInfoResource>(0);

	public JobExecutionInfoResource() {
		this.timeZone = TimeUtils.getDefaultTimeZone();
	}

	public JobExecutionInfoResource(JobExecution jobExecution, TimeZone timeZone) {

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
			this.stoppable = status.isLessThan(BatchStatus.STOPPING) && status != BatchStatus.COMPLETED;
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

	@JsonProperty
	public String getName() {
		return (jobName.endsWith(".job") ? jobName.substring(0, jobName.lastIndexOf(".job")) : jobName);
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

	/**
	 * Check if the job is deployed.
	 *
	 * @return the boolean value to specify if the job is deployed.
	 */
	public boolean isDeployed() {
		return deployed;
	}

	/**
	 * Check if the job definition is deleted/destroyed.
	 *
	 * @return the boolean value to specify if the job is deleted.
	 */
	public boolean isDeleted() {
		return deleted;
	}

	public String getJobParametersString() {
		return jobParametersString;
	}

	public Properties getJobParameters() {
		return jobParameters;
	}

	/**
	 * Set restartable flag explicitly based on the job executions status of the same job instance.
	 *
	 * @param restartable flag to identify if the job execution can be restarted
	 */
	public void setRestartable(boolean restartable) {
		this.restartable = restartable;
	}

	/**
	 * Set deployed flag
	 *
	 * @param deployed flag to identify if the job for the given job execution is deployed.
	 */
	public void setDeployed(boolean deployed) {
		this.deployed = deployed;
	}

	/**
	 * Set deleted flag explicitly if the job doesn't exist in job definition
	 *
	 * @param deleted flag to identify if the job for the given job execution is deleted.
	 */
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	/**
	 * Is this a composed Job? ?
	 *
	 * @return True if this is a composed job.
	 */
	public boolean isComposedJob() {
		return composedJob;
	}

	/**
	 * Set if this is a composed Job. If not specified, the underlying property
	 * will default to {@code false}.
	 */
	public void setComposedJob(boolean composedJob) {
		this.composedJob = composedJob;
	}

	/**
	 * Returns a {@link List} of {@link JobExecutionInfoResource}s, if any.
	 *
	 * @return Should never return null.
	 */
	public List<JobExecutionInfoResource> getChildJobExecutions() {
		return childJobExecutions;
	}

	/**
	 * Dedicated subclass to workaround type erasure.
	 *
	 * @author Gunnar Hillert
	 */
	public static class Page extends PagedResources<JobExecutionInfoResource> {

	}
}
