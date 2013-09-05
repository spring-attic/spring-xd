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
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;


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
public class BatchJobController {

	private final JobService jobService;

	@Autowired
	public BatchJobController(JobService jobService) {
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
			jobs.add(new JobInfo(name, count, null, launchable, incrementable));
		}
		return jobs;
	}

	// TODO: Currently the job name has "." separator which doesn't work as path variable
	// @RequestMapping(value = "/{jobName}", method = RequestMethod.GET)
	// @ResponseBody
	// @ResponseStatus(HttpStatus.OK)
	// public ModelMap details(ModelMap model, @PathVariable String jobName,
	// @RequestParam(defaultValue = "0") int startJobInstance, @RequestParam(defaultValue = "20") int pageSize) {
	//
	// boolean launchable = jobService.isLaunchable(jobName);
	//
	// try {
	// Collection<JobInstance> result = jobService.listJobInstances(jobName, startJobInstance, pageSize);
	// Collection<JobInstanceInfo> jobInstances = new ArrayList<JobInstanceInfo>();
	// model.addAttribute("jobParameters", "");
	// for (JobInstance jobInstance : result) {
	// jobInstances.add(new JobInstanceInfo(jobInstance, jobService.getJobExecutionsForJobInstance(jobName,
	// jobInstance.getId())));
	// }
	//
	// model.addAttribute("jobInstances", jobInstances);
	// int count = jobService.countJobExecutionsForJob(jobName);
	// model.addAttribute("jobInfo", new JobInfo(jobName, count, launchable, jobService.isIncrementable(jobName)));
	//
	// }
	// catch (NoSuchJobException e) {
	// throw new NoSuchBatchJobException(jobName);
	// }
	//
	// return model;
	//
	// }
}
