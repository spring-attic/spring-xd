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

package org.springframework.xd.dirt.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TimeZone;

import org.springframework.batch.admin.history.StepExecutionHistory;
import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.job.NoSuchJobExecutionException;
import org.springframework.xd.dirt.job.NoSuchStepExecutionException;
import org.springframework.xd.dirt.job.StepExecutionInfo;
import org.springframework.xd.dirt.job.StepExecutionProgressInfo;
import org.springframework.xd.rest.client.domain.StepExecutionInfoResource;
import org.springframework.xd.rest.client.domain.StepExecutionProgressInfoResource;

/**
 * Controller for returning Batch {@link StepExecution}s.
 * 
 * @author Gunnar Hillert
 * @author Dave Syer
 * @author Ilayaperumal Gopinathan
 * @since 1.0
 * 
 */
@Controller
@RequestMapping("/batch/executions/{jobExecutionId}/steps")
@ExposesResourceFor(StepExecutionInfoResource.class)
public class BatchStepExecutionsController {

	private final JobService jobService;

	private final StepExecutionInfoResourceAssembler stepExecutionInfoResourceAssembler;

	private final StepExecutionProgressInfoResourceAssembler progressInfoResourceAssembler;

	private TimeZone timeZone = TimeZone.getDefault();

	/**
	 * @param timeZone the timeZone to set
	 */
	@Autowired(required = false)
	@Qualifier("userTimeZone")
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	@Autowired
	public BatchStepExecutionsController(JobService jobService) {
		super();
		this.jobService = jobService;
		this.stepExecutionInfoResourceAssembler = new StepExecutionInfoResourceAssembler();
		this.progressInfoResourceAssembler = new StepExecutionProgressInfoResourceAssembler();
	}

	/**
	 * List all step executions.
	 * 
	 * @param jobExecutionId Id of the {@link JobExecution}, must not be null
	 * @return Collection of {@link StepExecutionInfoResource} for the given jobExecutionId
	 * @throws NoSuchJobExecutionException Thrown if the respective {@link JobExecution} does not exist
	 */
	@RequestMapping(value = { "" }, method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<StepExecutionInfoResource> list(@PathVariable("jobExecutionId") long jobExecutionId) {

		final Collection<StepExecution> stepExecutions;

		try {
			stepExecutions = jobService.getStepExecutions(jobExecutionId);
		}
		catch (org.springframework.batch.core.launch.NoSuchJobExecutionException e) {
			throw new NoSuchJobExecutionException(jobExecutionId);
		}

		final Collection<StepExecutionInfoResource> result = new ArrayList<StepExecutionInfoResource>();

		for (StepExecution stepExecution : stepExecutions) {
			// Band-Aid to prevent Hateos crash - see XD-1206
			if (stepExecution.getId() != null) {
				result.add(stepExecutionInfoResourceAssembler.toResource(new StepExecutionInfo(stepExecution, timeZone)));
			}
		}

		return result;
	}

	/**
	 * Get the step execution progress for the given jobExecutions step.
	 * 
	 * @param jobExecutionId Id of the {@link JobExecution}, must not be null
	 * @param stepExecutionId Id of the {@link StepExecution}, must not be null
	 * @return {@link StepExecutionInfoResource} that has the progress info on the given {@link StepExecution}.
	 * @throws NoSuchJobExecutionException Thrown if the respective {@link JobExecution} does not exist
	 * @throws NoSuchStepExecutionException Thrown if the respective {@link StepExecution} does not exist
	 */
	@RequestMapping(value = "/{stepExecutionId}/progress", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public StepExecutionProgressInfoResource progress(@PathVariable long jobExecutionId,
			@PathVariable long stepExecutionId) {
		StepExecutionProgressInfoResource result;
		try {
			StepExecution stepExecution = jobService.getStepExecution(jobExecutionId, stepExecutionId);
			String stepName = stepExecution.getStepName();
			if (stepName.contains(":partition")) {
				// assume we want to compare all partitions
				stepName = stepName.replaceAll("(:partition).*", "$1*");
			}
			String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
			StepExecutionHistory stepExecutionHistory = computeHistory(jobName, stepName);
			result = progressInfoResourceAssembler.toResource(new StepExecutionProgressInfo(stepExecution,
					stepExecutionHistory));
		}
		catch (org.springframework.batch.admin.service.NoSuchStepExecutionException e) {
			throw new NoSuchStepExecutionException(stepExecutionId);
		}
		catch (org.springframework.batch.core.launch.NoSuchJobExecutionException e) {
			throw new NoSuchJobExecutionException(jobExecutionId);
		}
		return result;
	}

	/**
	 * Compute step execution history for the given jobs step.
	 * 
	 * @param jobName the name of the job
	 * @param stepName the name of the step
	 * @return the step execution history for the given step
	 */
	private StepExecutionHistory computeHistory(String jobName, String stepName) {
		int total = jobService.countStepExecutionsForStep(jobName, stepName);
		StepExecutionHistory stepExecutionHistory = new StepExecutionHistory(stepName);
		for (int i = 0; i < total; i += 1000) {
			for (StepExecution stepExecution : jobService.listStepExecutionsForStep(jobName, stepName, i, 1000)) {
				stepExecutionHistory.append(stepExecution);
			}
		}
		return stepExecutionHistory;
	}
}
