/*
 * Copyright 2013-2014 the original author or authors.
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

import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.batch.core.ExitStatus;


/**
 * Represents Expanded Batch job info that has more details on batch jobs.
 * 
 * @author Ilayaperumal Gopinathan
 */
@XmlRootElement
public class DetailedJobInfoResource extends JobInfoResource {

	private String jobParameters;

	private String duration;

	private String startTime;

	private String startDate;

	private int stepExecutionCount;

	private ExitStatus exitStatus;

	/**
	 * Default constructor for serialization frameworks.
	 */
	@SuppressWarnings("unused")
	private DetailedJobInfoResource() {
		super();
	}

	public DetailedJobInfoResource(String name, int executionCount, boolean launchable, boolean incrementable,
			JobExecutionInfoResource lastExecution, boolean deployed) {
		super(name, executionCount, null, launchable, incrementable, deployed);
		if (lastExecution != null) {
			jobParameters = lastExecution.getJobParametersString();
			duration = lastExecution.getDuration();
			startTime = lastExecution.getStartTime();
			startDate = lastExecution.getStartDate();
			stepExecutionCount = lastExecution.getStepExecutionCount();
			exitStatus = lastExecution.getJobExecution().getExitStatus();
		}
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
