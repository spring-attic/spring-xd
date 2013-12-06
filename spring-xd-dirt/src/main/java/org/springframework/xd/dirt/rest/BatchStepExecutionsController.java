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

import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.admin.web.StepExecutionInfo;
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
import org.springframework.xd.rest.client.domain.StepExecutionInfoResource;

/**
 * Controller for returning Batch {@link StepExecution}s.
 * 
 * @author Gunnar Hillert
 * @since 1.0
 * 
 */
@Controller
@RequestMapping("/batch/executions/{jobExecutionId}/steps")
@ExposesResourceFor(StepExecutionInfoResource.class)
public class BatchStepExecutionsController {

	private final JobService jobService;

	private final StepExecutionInfoResourceAssembler stepExecutionInfoResourceAssembler;

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
	public Collection<StepExecutionInfoResource> list(@PathVariable("jobExecutionId") Long jobExecutionId) {

		final Collection<StepExecution> stepExecutions;

		try {
			stepExecutions = jobService.getStepExecutions(jobExecutionId);
		}
		catch (org.springframework.batch.core.launch.NoSuchJobExecutionException e) {
			throw new NoSuchJobExecutionException(jobExecutionId);
		}

		final Collection<StepExecutionInfoResource> result = new ArrayList<StepExecutionInfoResource>();

		for (StepExecution stepExecution : stepExecutions) {
			result.add(stepExecutionInfoResourceAssembler.toResource(new StepExecutionInfo(stepExecution, timeZone)));
		}

		return result;
	}

}
