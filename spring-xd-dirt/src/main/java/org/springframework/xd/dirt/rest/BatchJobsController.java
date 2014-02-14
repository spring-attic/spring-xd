/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.dirt.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.xd.dirt.job.DetailedJobInfo;
import org.springframework.xd.dirt.job.JobExecutionInfo;
import org.springframework.xd.dirt.job.JobInstanceInfo;
import org.springframework.xd.dirt.job.NoSuchBatchJobException;
import org.springframework.xd.rest.client.domain.DetailedJobInfoResource;
import org.springframework.xd.rest.client.domain.JobExecutionInfoResource;
import org.springframework.xd.rest.client.domain.JobInstanceInfoResource;


/**
 * Controller for batch jobs and job instances, job executions on a given batch job.
 * 
 * @author Dave Syer
 * @author Ilayaperumal Gopinathan
 * @author Andrew Eisenberg
 * 
 */
@RestController
@RequestMapping("/batch/jobs")
@ExposesResourceFor(DetailedJobInfoResource.class)
public class BatchJobsController extends AbstractBatchJobsController {

	/**
	 * Get a list of JobInfo, in a given range.
	 * 
	 * @param startJob the start index of the job names to return
	 * @param pageSize page size for the list
	 */
	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public Collection<DetailedJobInfoResource> jobs(@RequestParam(defaultValue = "0") int startJob,
			@RequestParam(defaultValue = "20") int pageSize) {
		Collection<String> names = jobService.listJobs(startJob, pageSize);
		List<DetailedJobInfoResource> jobs = new ArrayList<DetailedJobInfoResource>();
		for (String name : names) {
			jobs.add(internalGetJobInfo(name));
		}
		return jobs;
	}

	/**
	 * Return a paged collection of job instances for a given job.
	 * 
	 * @param jobName name of the batch job
	 * @param startJobInstance start index for the job instance
	 * @param pageSize page size for the list
	 * @return collection of JobInstnaces by job name
	 */
	@RequestMapping(value = "/{jobName}/instances", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public Collection<JobInstanceInfoResource> instancesForJob(@PathVariable String jobName,
			@RequestParam(defaultValue = "0") int startJobInstance, @RequestParam(defaultValue = "20") int pageSize) {

		try {
			Collection<JobInstance> jobInstances = jobService.listJobInstances(jobName, startJobInstance, pageSize);
			List<JobInstanceInfoResource> result = new ArrayList<JobInstanceInfoResource>();
			for (JobInstance jobInstance : jobInstances) {
				List<JobExecution> jobExecutions = (List<JobExecution>) jobService.getJobExecutionsForJobInstance(
						jobName, jobInstance.getId());
				List<JobExecutionInfo> jobExecutionInfos = new ArrayList<JobExecutionInfo>();
				for (JobExecution jobExecution : jobExecutions) {
					jobExecutionInfos.add(new JobExecutionInfo(jobExecution, timeZone));
				}
				result.add(jobInstanceInfoResourceAssembler.toResource(new JobInstanceInfo(jobInstance, jobExecutionInfos)));
			}
			return result;
		}
		catch (NoSuchJobException e) {
			throw new NoSuchBatchJobException(jobName);
		}
	}

	/**
	 * @param jobName name of the job
	 * @return ExpandedJobInfo for the given job name
	 */
	@RequestMapping(value = "/{jobName}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public DetailedJobInfoResource jobinfo(@PathVariable String jobName) {
		return internalGetJobInfo(jobName);
	}

	/**
	 * @param jobName name of the job
	 * @return a job info for this job
	 */
	private DetailedJobInfoResource internalGetJobInfo(String jobName) {
		boolean launchable = jobService.isLaunchable(jobName);
		try {
			int count = jobService.countJobExecutionsForJob(jobName);
			DetailedJobInfo detailedJobInfo = new DetailedJobInfo(jobName, count, launchable,
					jobService.isIncrementable(jobName),
					getLastExecution(jobName));
			return jobInfoResourceAssembler.toResource(detailedJobInfo);
		}
		catch (NoSuchJobException e) {
			throw new NoSuchBatchJobException(jobName);
		}
	}

	/**
	 * @param jobName name of the batch job
	 * @return Last job execution info
	 * @throws NoSuchJobException
	 */
	private JobExecutionInfo getLastExecution(String jobName) throws NoSuchJobException {
		Collection<JobExecution> executions = jobService.listJobExecutionsForJob(jobName, 0, 1);
		if (executions.size() > 0) {
			return new JobExecutionInfo(executions.iterator().next(), timeZone);
		}
		else {
			return null;
		}
	}

	/**
	 * Return a paged collection of job executions for a given job.
	 * 
	 * @param jobName name of the job
	 * @param startJobExecution start index for the job execution list
	 * @param pageSize page size for the list
	 * @return collection of JobExecutionInfo
	 */
	@RequestMapping(value = "/{jobName}/executions", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public Collection<JobExecutionInfoResource> executionsForJob(@PathVariable String jobName,
			@RequestParam(defaultValue = "0") int startJobExecution,
			@RequestParam(defaultValue = "20") int pageSize) {

		Collection<JobExecutionInfoResource> result = new ArrayList<JobExecutionInfoResource>();
		try {
			for (JobExecution jobExecution : jobService.listJobExecutionsForJob(jobName, startJobExecution, pageSize)) {
				result.add(jobExecutionInfoResourceAssembler.toResource(new JobExecutionInfo(jobExecution, timeZone)));
			}
		}
		catch (NoSuchJobException e) {
			throw new NoSuchBatchJobException(jobName);
		}
		return result;
	}
}
