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
import org.springframework.xd.dirt.job.DetailedJobInfo;
import org.springframework.xd.dirt.job.JobExecutionInfo;
import org.springframework.xd.rest.domain.DetailedJobInfoResource;
import org.springframework.xd.rest.domain.JobExecutionInfoResource;


/**
 * Knows how to build a REST resource out of our domain model {@link DetailedJobInfo}.
 * 
 * @author Ilayaperumal Gopinathan
 */
public class DetailedJobInfoResourceAssembler extends
		ResourceAssemblerSupport<DetailedJobInfo, DetailedJobInfoResource> {

	private JobExecutionInfoResourceAssembler jobExecutionInfoResourceAssembler = new JobExecutionInfoResourceAssembler();

	public DetailedJobInfoResourceAssembler() {
		super(BatchJobsController.class, DetailedJobInfoResource.class);
	}

	@Override
	public DetailedJobInfoResource toResource(DetailedJobInfo entity) {
		return createResourceWithId(entity.getName(), entity);
	}

	@Override
	protected DetailedJobInfoResource instantiateResource(DetailedJobInfo entity) {
		JobExecutionInfoResource jobExecutionInfoResource;
		if (entity.getLastExecutionInfo() != null) {
			JobExecutionInfo jobExecutionInfo = new JobExecutionInfo(
					entity.getLastExecutionInfo().getJobExecution(),
					entity.getLastExecutionInfo().getTimeZone());
			jobExecutionInfoResource = jobExecutionInfoResourceAssembler.instantiateResource(jobExecutionInfo);
		}
		else {
			jobExecutionInfoResource = null;
		}
		return new DetailedJobInfoResource(entity.getName(), entity.getExecutionCount(),
				entity.isLaunchable(), entity.isIncrementable(), jobExecutionInfoResource, entity.isDeployed());
	}
}
