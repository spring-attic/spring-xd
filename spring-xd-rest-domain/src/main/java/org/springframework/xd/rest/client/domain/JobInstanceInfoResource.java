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

package org.springframework.xd.rest.client.domain;

import java.util.List;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.hateoas.ResourceSupport;


/**
 * Represents JobInstance info resource.
 * 
 * @author Ilayaperumal Gopinathan
 */
public class JobInstanceInfoResource extends ResourceSupport {

	private final String jobName;

	private final long instanceId;

	private final List<JobExecution> jobExecutions;

	public JobInstanceInfoResource(JobInstance jobInstance, List<JobExecution> jobExecutions) {
		this.jobName = jobInstance.getJobName();
		this.instanceId = jobInstance.getInstanceId();
		this.jobExecutions = jobExecutions;
	}

	public String getJobName() {
		return jobName;
	}

	public List<JobExecution> getJobExecutions() {
		return this.jobExecutions;
	}

	public long getInstanceId() {
		return instanceId;
	}
}
