/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.job;

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.xd.rest.domain.util.TimeUtils;

/**
 * Represents Batch step execution info.
 *
 * @author Dave Syer
 * @author Gunnar Hillert
 * @author Michael Minella
 * @since 1.0
 */
public class StepExecutionInfo {

	private DateFormat dateFormat = TimeUtils.getDefaultDateFormat();

	private DateFormat timeFormat = TimeUtils.getDefaultTimeFormat();

	private DateFormat durationFormat = TimeUtils.getDefaultDurationFormat();

	private Long id;

	private Long jobExecutionId;

	private String jobName;

	private String name;

	private String startDate = "-";

	private String startTime = "-";

	private String duration = "-";

	private StepExecution stepExecution;

	private long durationMillis;

	private String stepType = StepType.UNKNOWN.getDisplayName();

	public StepExecutionInfo(String jobName, Long jobExecutionId, String name, TimeZone timeZone) {
		this.jobName = jobName;
		this.jobExecutionId = jobExecutionId;
		this.name = name;
		this.stepExecution = new StepExecution(name, new JobExecution(jobExecutionId));
	}

	public StepExecutionInfo(StepExecution stepExecution, TimeZone timeZone) {

		this.stepExecution = stepExecution;
		this.id = stepExecution.getId();
		this.name = stepExecution.getStepName();
		this.jobName = stepExecution.getJobExecution() == null
				|| stepExecution.getJobExecution().getJobInstance() == null ? "?" : stepExecution.getJobExecution()
				.getJobInstance().getJobName();
		this.jobExecutionId = stepExecution.getJobExecutionId();
		// Duration is always in GMT
		durationFormat.setTimeZone(TimeUtils.getDefaultTimeZone());
		timeFormat.setTimeZone(timeZone);
		dateFormat.setTimeZone(timeZone);
		if (stepExecution.getStartTime() != null) {
			this.startDate = dateFormat.format(stepExecution.getStartTime());
			this.startTime = timeFormat.format(stepExecution.getStartTime());
			Date endTime = stepExecution.getEndTime() != null ? stepExecution.getEndTime() : new Date();
			this.durationMillis = endTime.getTime() - stepExecution.getStartTime().getTime();
			this.duration = durationFormat.format(new Date(durationMillis));
		}

		if(stepExecution.getExecutionContext().containsKey(TaskletStep.TASKLET_TYPE_KEY)) {
			String taskletClassName = stepExecution.getExecutionContext().getString(TaskletStep.TASKLET_TYPE_KEY);
			TaskletType type = TaskletType.fromClassName(taskletClassName);

			if(type == TaskletType.UNKNOWN) {
				this.stepType = taskletClassName;
			}
			else {
				this.stepType = type.getDisplayName();
			}
		}
		else if(stepExecution.getExecutionContext().containsKey(Step.STEP_TYPE_KEY)) {
			String stepClassName = stepExecution.getExecutionContext().getString(Step.STEP_TYPE_KEY);
			StepType type = StepType.fromClassName(stepClassName);

			if(type == StepType.UNKNOWN) {
				this.stepType = stepClassName;
			}
			else {
				this.stepType = type.getDisplayName();
			}
		}
	}

	public Long getId() {
		return id;
	}

	public Long getJobExecutionId() {
		return jobExecutionId;
	}

	public String getName() {
		return name;
	}

	public String getJobName() {
		return jobName;
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

	public long getDurationMillis() {
		return durationMillis;
	}

	public String getStatus() {
		if (id != null) {
			return stepExecution.getStatus().toString();
		}
		return "NONE";
	}

	public String getExitCode() {
		if (id != null) {
			return stepExecution.getExitStatus().getExitCode();
		}
		return "NONE";
	}

	public StepExecution getStepExecution() {
		return stepExecution;
	}

	public String getStepType() { return this.stepType; }

}
