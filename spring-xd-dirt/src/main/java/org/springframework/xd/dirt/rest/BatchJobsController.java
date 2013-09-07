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
import java.util.TimeZone;

import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.admin.web.JobExecutionInfo;
import org.springframework.batch.admin.web.JobInfo;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.launch.NoSuchJobException;
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
import org.springframework.xd.dirt.plugins.job.batch.ExpandedJobInfo;
import org.springframework.xd.dirt.plugins.job.batch.NoSuchBatchJobException;

/**
 * Controller for batch jobs.
 * 
 * @author Dave Syer
 * @author Ilayaperumal Gopinathan
 * @author Andrew Eisenberg
 * 
 */
@Controller
@RequestMapping("/batch/jobs")
@ExposesResourceFor(JobInfo.class)
public class BatchJobsController {

	private final JobService jobService;

	private TimeZone timeZone = TimeZone.getDefault();

	@Autowired
	public BatchJobsController(JobService jobService) {
		super();
		this.jobService = jobService;
	}

	/**
	 * @param timeZone the timeZone to set
	 */
	@Autowired(required = false)
	@Qualifier("userTimeZone")
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<JobInfo> jobs(@RequestParam(defaultValue = "0") int startJob,
			@RequestParam(defaultValue = "20") int pageSize) {
		Collection<String> names = jobService.listJobs(startJob, pageSize);
		List<JobInfo> jobs = new ArrayList<JobInfo>();
		for (String name : names) {
			String displayName = name.substring(0, name.length() - ".job".length());
			jobs.add(internalGetJobInfo(displayName, name));
		}
		return jobs;
	}

	@RequestMapping(value = "/{jobName}/instances", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Collection<JobInstance> jobInstances(@PathVariable String jobName,
			@RequestParam(defaultValue = "0") int startJobInstance, @RequestParam(defaultValue = "20") int pageSize) {
		Collection<JobInstance> jobInstances = new ArrayList<JobInstance>();
		String fullName = jobName + ".job";

		try {
			jobInstances = jobService.listJobInstances(fullName, startJobInstance, pageSize);
			// TODO: Need to add the jobExecutions for each jobInstance
		}
		catch (NoSuchJobException e) {
			throw new NoSuchBatchJobException(jobName);
		}
		return jobInstances;
	}

	@RequestMapping(value = "/{jobName}", method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public ExpandedJobInfo jobinfo(@PathVariable String jobName) {
		String fullName = jobName + ".job";
		return internalGetJobInfo(jobName, fullName);
	}

	/**
	 * @param displayName Job name as displayed to endpoint
	 * @param fullName Job name as internal to the system
	 * @return a job info for this job
	 */
	private ExpandedJobInfo internalGetJobInfo(String displayName, String fullName) {
		boolean launchable = jobService.isLaunchable(fullName);
		ExpandedJobInfo jobInfo;
		try {
			int count = jobService.countJobExecutionsForJob(fullName);
			jobInfo = new ExpandedJobInfo(displayName, count, launchable, jobService.isIncrementable(fullName),
					getLastExecution(fullName));
		}
		catch (NoSuchJobException e) {
			throw new NoSuchBatchJobException(displayName);
		}
		return jobInfo;
	}

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
