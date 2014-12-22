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

package org.springframework.xd.dirt.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.batch.core.JobExecution;
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
import org.springframework.xd.dirt.stream.Job;
import org.springframework.xd.rest.domain.DetailedJobInfoResource;


/**
 * Controller for batch jobs and job instances, job executions on a given batch job.
 * 
 * @author Dave Syer
 * @author Ilayaperumal Gopinathan
 * @author Andrew Eisenberg
 * 
 */
@RestController
@RequestMapping("/jobs/configurations")
@ExposesResourceFor(DetailedJobInfoResource.class)
public class BatchJobsController extends AbstractBatchJobsController {

	/**
	 * Get a list of JobInfo, in a given range.
	 * 
	 * @param startJob the start index of the job names to return
	 * @param pageSize page size for the list
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public Collection<DetailedJobInfoResource> jobs(@RequestParam(defaultValue = "0") int startJob,
			@RequestParam(defaultValue = "20") int pageSize) {
		Collection<String> names = jobService.listJobs(startJob, pageSize);
		List<DetailedJobInfoResource> jobs = new ArrayList<DetailedJobInfoResource>();
		// Check if the job is deployed into XD
		List<Job> deployedJobs = (List<Job>) xdJobrepository.findAll();
		for (String name : names) {
			boolean deployed = false;
			for (Job deployedJob : deployedJobs) {
				if (deployedJob.getDefinition().getName().equals(name)) {
					deployed = true;
					break;
				}
			}
			DetailedJobInfoResource jobInfoResource = getJobInfo(name, deployed);
			if (jobInfoResource != null) {
				jobs.add(jobInfoResource);
			}
		}
		return jobs;
	}

	/**
	 * @param jobName name of the job
	 * @return ExpandedJobInfo for the given job name
	 */
	@RequestMapping(value = "/{jobName}", method = RequestMethod.GET, produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public DetailedJobInfoResource jobinfo(@PathVariable String jobName) {
		return getJobInfo(jobName);
	}

	private DetailedJobInfoResource getJobInfo(String jobName) {
		Job deployedJob = xdJobrepository.findOne(jobName);
		return getJobInfo(jobName, (null != deployedJob));
	}

	/**
	 * Get detailed job info
	 *
	 * @param jobName name of the job
	 * @param deployed the deployment status of the job
	 * @return a job info for this job or null if job doesn't exist
	 */
	private DetailedJobInfoResource getJobInfo(String jobName, boolean deployed) {
		boolean launchable = jobService.isLaunchable(jobName);
		try {
			int count = jobService.countJobExecutionsForJob(jobName);
			DetailedJobInfo detailedJobInfo = new DetailedJobInfo(jobName, count, launchable,
					jobService.isIncrementable(jobName),
					getLastExecution(jobName), deployed);
			return jobInfoResourceAssembler.toResource(detailedJobInfo);
		}
		catch (NoSuchJobException e) {
			return null;
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

}
