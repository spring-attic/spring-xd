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

import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.xd.dirt.job.JobInstanceInfo;
import org.springframework.xd.rest.domain.JobInstanceInfoResource;


/**
 * Resource assembler that builds the REST resource {@link JobInstanceInfoResource} out of domain model
 * {@link JobInstanceInfo}.
 * 
 * @author Ilayaperumal Gopinathan
 */
public class JobInstanceInfoResourceAssembler extends
		ResourceAssemblerSupport<JobInstanceInfo, JobInstanceInfoResource> {

	JobExecutionInfoResourceAssembler jobExecutionInfoResourceAssembler = new JobExecutionInfoResourceAssembler();

	public JobInstanceInfoResourceAssembler() {
		super(BatchJobInstancesController.class, JobInstanceInfoResource.class);
	}

	@Override
	public JobInstanceInfoResource toResource(JobInstanceInfo entity) {
		return createResourceWithId(entity.getInstanceId(), entity);
	}

	@Override
	protected JobInstanceInfoResource instantiateResource(JobInstanceInfo entity) {
		return new JobInstanceInfoResource(entity.getJobInstance(),
				jobExecutionInfoResourceAssembler.toResources(entity.getJobExecutions()));
	}
}
