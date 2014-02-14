/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.job;

import java.util.List;

import org.springframework.batch.core.JobInstance;
import org.springframework.util.Assert;


/**
 * Represents Batch job instance info.
 * 
 * @author Ilayaperumal Gopinathan
 */
public class JobInstanceInfo {

	private final JobInstance jobInstance;

	private final List<JobExecutionInfo> jobExecutions;

	public JobInstanceInfo(JobInstance jobInstance, List<JobExecutionInfo> jobExecutions) {
		Assert.notNull(jobInstance, "jobInstance must not null");
		Assert.notNull(jobExecutions, "jobExecutions must not null");
		this.jobInstance = jobInstance;
		this.jobExecutions = jobExecutions;
	}

	public JobInstance getJobInstance() {
		return jobInstance;
	}

	public List<JobExecutionInfo> getJobExecutions() {
		return jobExecutions;
	}

	public long getInstanceId() {
		return jobInstance.getInstanceId();
	}

}
