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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.admin.web.JobExecutionInfo;
import org.springframework.batch.core.JobExecution;
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
import org.springframework.xd.dirt.plugins.job.batch.NoSuchBatchJobException;


/**
 * Controller for job executions.
 * 
 * @author Dave Syer
 * @author Ilayaperumal Gopinathan
 * 
 */
@Controller
@RequestMapping("/batch/jobs")
@ExposesResourceFor(JobExecutionInfo.class)
public class BatchJobExecutionController {

	private static Log logger = LogFactory.getLog(BatchJobExecutionController.class);

	public static class StopRequest {

		private Long jobExecutionId;

		public Long getJobExecutionId() {
			return jobExecutionId;
		}

		public void setJobExecutionId(Long jobExecutionId) {
			this.jobExecutionId = jobExecutionId;
		}

	}

	private JobService jobService;

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
	public BatchJobExecutionController(JobService jobService) {
		super();
		this.jobService = jobService;
	}

	@RequestMapping(value = { "/executions", "/executions.*" }, method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<JobExecutionInfo> list(@RequestParam(defaultValue = "0") int startJobExecution,
			@RequestParam(defaultValue = "20") int pageSize) {

		Collection<JobExecutionInfo> result = new ArrayList<JobExecutionInfo>();
		for (JobExecution jobExecution : jobService.listJobExecutions(startJobExecution, pageSize)) {
			result.add(new JobExecutionInfo(jobExecution, timeZone));
		}
		return result;
	}

	@RequestMapping(value = "/{jobName}/executions", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<JobExecutionInfo> listForJob(@PathVariable String jobName,
			@RequestParam(defaultValue = "0") int startJobExecution,
			@RequestParam(defaultValue = "20") int pageSize) {

		Collection<JobExecutionInfo> result = new ArrayList<JobExecutionInfo>();
		try {
			for (JobExecution jobExecution : jobService.listJobExecutionsForJob(jobName, startJobExecution, pageSize)) {
				result.add(new JobExecutionInfo(jobExecution, timeZone));
			}
		}
		catch (NoSuchJobException e) {
			logger.warn("Could not locate Job with name=" + jobName);
			throw new NoSuchBatchJobException(jobName);
		}
		return result;
	}

}
