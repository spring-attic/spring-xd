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

import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.batch.admin.history.StepExecutionHistory;
import org.springframework.batch.core.StepExecution;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.Assert;


/**
 * Represents the step execution progress info resource.
 * 
 * @author Ilayaperumal Gopinathan
 * @since 1.0
 */
@XmlRootElement
public class StepExecutionProgressInfoResource extends ResourceSupport {

	private StepExecution stepExecution;

	private StepExecutionHistory stepExecutionHistory;

	private double percentageComplete;

	private boolean finished;

	private double duration;

	public StepExecutionProgressInfoResource() {
	}

	/**
	 * 
	 * @param stepExecution Must not be null
	 * @param stepExecutionHistory Must not be null
	 */
	public StepExecutionProgressInfoResource(StepExecution stepExecution, StepExecutionHistory stepExecutionHistory,
			double percentageComplete, boolean isFinished, double duration) {

		Assert.notNull(stepExecution, "stepExecution must not be null.");
		Assert.notNull(stepExecutionHistory, "stepExecution must not be null.");

		this.stepExecution = stepExecution;
		this.stepExecutionHistory = stepExecutionHistory;
		this.percentageComplete = percentageComplete;
		this.finished = isFinished;
		this.duration = duration;
	}

	public double getPercentageComplete() {
		return percentageComplete;
	}

	public boolean getFinished() {
		return finished;
	}

	public double getDuration() {
		return duration;
	}

	public StepExecution getStepExecution() {
		return stepExecution;
	}

	public StepExecutionHistory getStepExecutionHistory() {
		return stepExecutionHistory;
	}

}
