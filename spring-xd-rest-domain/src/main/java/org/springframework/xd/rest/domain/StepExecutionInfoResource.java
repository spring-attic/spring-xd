/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.rest.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.batch.core.StepExecution;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.Assert;

import javax.xml.bind.annotation.XmlRootElement;


/**
 * Represents the step execution info resource.
 * 
 * @author Gunnar Hillert
 * @since 1.0
 */
@XmlRootElement
public class StepExecutionInfoResource extends ResourceSupport {

	private final Long jobExecutionId;

	private final StepExecution stepExecution;

	private final String stepType;

	/**
	 * 
	 * @param jobExecutionId Must not be null
	 * @param stepExecution Must not be null
	 */
	public StepExecutionInfoResource(
			@JsonProperty("jobExecutionId") Long jobExecutionId,
			@JsonProperty("stepExecution") StepExecution stepExecution,
			@JsonProperty("stepType") String stepType) {

		Assert.notNull(jobExecutionId, "jobExecutionId must not be null.");
		Assert.notNull(stepExecution, "stepExecution must not be null.");

		this.stepExecution = stepExecution;
		this.jobExecutionId = jobExecutionId;
		this.stepType = stepType;
	}

	public StepExecutionInfoResource() {
		this.stepExecution = null;
		this.jobExecutionId = null;
		this.stepType = null;
	}

	/**
	 * @return The jobExecutionId, which will never be null
	 */
	public Long getJobExecutionId() {
		return this.jobExecutionId;
	}

	/**
	 * @return The stepExecution, which will never be null
	 */
	public StepExecution getStepExecution() {
		return stepExecution;
	}

	public String getStepType() { return this.stepType; }

}
