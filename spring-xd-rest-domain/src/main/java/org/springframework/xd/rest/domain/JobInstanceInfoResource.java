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

package org.springframework.xd.rest.domain;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.springframework.batch.core.JobInstance;
import org.springframework.hateoas.ResourceSupport;


/**
 * Represents JobInstance info resource.
 * 
 * @author Ilayaperumal Gopinathan
 */
@XmlRootElement
public class JobInstanceInfoResource extends ResourceSupport {

	private String jobName;

	private long instanceId;

	private List<JobExecutionInfoResource> jobExecutions;

	/**
	 * Default constructor for serialization frameworks.
	 */
	protected JobInstanceInfoResource() {

	}

	public JobInstanceInfoResource(JobInstance jobInstance, List<JobExecutionInfoResource> jobExecutionInfoResources) {
		this.jobName = jobInstance.getJobName();
		this.instanceId = jobInstance.getInstanceId();
		this.jobExecutions = jobExecutionInfoResources;
	}

	public String getJobName() {
		return jobName;
	}

	public List<JobExecutionInfoResource> getJobExecutions() {
		return this.jobExecutions;
	}

	public long getInstanceId() {
		return instanceId;
	}
}
