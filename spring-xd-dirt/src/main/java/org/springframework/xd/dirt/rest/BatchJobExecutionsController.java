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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.xd.rest.client.domain.BatchJobExecutionInfo;

/**
 * Controller for batch job executions.
 * 
 * @author Dave Syer
 * @author Ilayaperumal Gopinathan
 * 
 */
@Controller
@RequestMapping("/batch/executions")
@ExposesResourceFor(BatchJobExecutionInfo.class)
public class BatchJobExecutionsController {

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
	public BatchJobExecutionsController(JobService jobService) {
		super();
		this.jobService = jobService;
	}

	/**
	 * List all job executions in a given range.
	 * 
	 * @param startJobExecution index of the first job execution to get
	 * @param pageSize how many executions to return
	 */
	@RequestMapping(value = { "" }, method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<BatchJobExecutionInfo> list(@RequestParam(defaultValue = "0") int startJobExecution,
			@RequestParam(defaultValue = "20") int pageSize) {

		Collection<BatchJobExecutionInfo> result = new ArrayList<BatchJobExecutionInfo>();
		for (JobExecution jobExecution : jobService.listJobExecutions(startJobExecution, pageSize)) {
			result.add(new BatchJobExecutionInfo(jobExecution, timeZone));
		}
		return result;
	}
}
