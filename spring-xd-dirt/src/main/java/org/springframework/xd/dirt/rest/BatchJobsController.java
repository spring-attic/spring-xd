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

import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.admin.web.JobInfo;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.dirt.plugins.job.batch.NoSuchBatchJobException;

/**
 * Controller for batch jobs.
 * 
 * @author Dave Syer
 * @author Ilayaperumal Gopinathan
 * 
 */
@Controller
@RequestMapping("/batch/jobs")
@ExposesResourceFor(JobInfo.class)
public class BatchJobsController {

	private final JobService jobService;

	@Autowired
	public BatchJobsController(JobService jobService) {
		super();
		this.jobService = jobService;
	}

	@RequestMapping(value = "", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<JobInfo> jobs(@RequestParam(defaultValue = "0") int startJob,
			@RequestParam(defaultValue = "20") int pageSize) {
		Collection<String> names = jobService.listJobs(startJob, pageSize);
		List<JobInfo> jobs = new ArrayList<JobInfo>();
		for (String name : names) {
			int count = 0;
			try {
				count = jobService.countJobExecutionsForJob(name);
			}
			catch (NoSuchJobException e) {
				// shouldn't happen
			}
			boolean launchable = jobService.isLaunchable(name);
			boolean incrementable = jobService.isIncrementable(name);
			String simpleName = name.substring(0, name.length() - ".job".length());
			jobs.add(new JobInfo(simpleName, count, null, launchable, incrementable));
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
	public JobInfo jobinfo(ModelMap model, @PathVariable String jobName) {
		String fullName = jobName + ".job";
		boolean launchable = jobService.isLaunchable(fullName);
		JobInfo jobInfo;
		try {
			int count = jobService.countJobExecutionsForJob(fullName);
			jobInfo = new JobInfo(jobName, count, launchable, jobService.isIncrementable(fullName));
		}
		catch (NoSuchJobException e) {
			throw new NoSuchBatchJobException(jobName);
		}
		return jobInfo;
	}
}
