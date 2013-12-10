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
import org.springframework.batch.core.JobExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.job.JobExecutionInfo;
import org.springframework.xd.dirt.job.JobExecutionNotRunningException;
import org.springframework.xd.dirt.job.NoSuchJobExecutionException;
import org.springframework.xd.rest.client.domain.JobExecutionInfoResource;

/**
 * Controller for batch job executions.
 * 
 * @author Dave Syer
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 * 
 */
@Controller
@RequestMapping("/batch/executions")
@ExposesResourceFor(JobExecutionInfoResource.class)
public class BatchJobExecutionsController {

	private JobService jobService;

	private TimeZone timeZone = TimeZone.getDefault();

	private final JobExecutionInfoResourceAssembler jobExecutionInfoResourceAssembler;

	/**
	 * @param timeZone the timeZone to set
	 */
	@Autowired(required = false)
	@Qualifier("userTimeZone")
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	@Autowired
	public BatchJobExecutionsController(JobService jobService) {
		super();
		this.jobService = jobService;
		this.jobExecutionInfoResourceAssembler = new JobExecutionInfoResourceAssembler();
	}

	/**
	 * List all job executions in a given range.
	 * 
	 * @param startJobExecution index of the first job execution to get
	 * @param pageSize how many executions to return
	 * @return Collection of JobExecutionInfoResource
	 */
	@RequestMapping(value = { "" }, method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<JobExecutionInfoResource> list(@RequestParam(defaultValue = "0") int startJobExecution,
			@RequestParam(defaultValue = "20") int pageSize) {

		Collection<JobExecutionInfoResource> result = new ArrayList<JobExecutionInfoResource>();
		for (JobExecution jobExecution : jobService.listJobExecutions(startJobExecution, pageSize)) {
			result.add(jobExecutionInfoResourceAssembler.toResource(new JobExecutionInfo(jobExecution, timeZone)));
		}
		return result;
	}

	/**
	 * @param jobExecutionId Id of the {@link JobExecution}
	 * @return JobExecutionInfo for the given job name
	 * @throws NoSuchJobExecutionException Thrown if the {@link JobExecution} does not exist
	 */
	@RequestMapping(value = "/{jobExecutionId}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public JobExecutionInfoResource getJobExecutionInfo(@PathVariable Long jobExecutionId) {

		final JobExecution jobExecution;

		try {
			jobExecution = jobService.getJobExecution(jobExecutionId);
		}
		catch (org.springframework.batch.core.launch.NoSuchJobExecutionException e) {
			throw new NoSuchJobExecutionException(jobExecutionId);
		}

		return jobExecutionInfoResourceAssembler.toResource(new JobExecutionInfo(jobExecution, timeZone));
	}

	/**
	 * Stop Job Execution by the given executionId.
	 * 
	 * @param jobExecutionId the executionId of the job execution to stop
	 */
	@RequestMapping(value = { "/{executionId}" }, method = RequestMethod.PUT)
	@ResponseStatus(HttpStatus.OK)
	public void stopJobExecution(@PathVariable("executionId") Long jobExecutionId) {
		try {
			jobService.stop(jobExecutionId);
		}
		catch (org.springframework.batch.core.launch.JobExecutionNotRunningException e) {
			throw new JobExecutionNotRunningException(jobExecutionId);
		}
		catch (org.springframework.batch.core.launch.NoSuchJobExecutionException e) {
			throw new NoSuchJobExecutionException(jobExecutionId);
		}
	}
}
