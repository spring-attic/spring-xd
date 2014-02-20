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

package org.springframework.xd.dirt.plugins.job;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.batch.core.job.flow.FlowJob;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.job.BatchJobAlreadyExistsException;


/**
 * JobRegistryBeanPostProcessor that processes batch job from the job module.
 * 
 * @author Ilayaperumal Gopinathan
 */
public class BatchJobRegistryBeanPostProcessor extends JobRegistryBeanPostProcessor {

	private JobRegistry jobRegistry;

	private DistributedJobLocator jobLocator;

	private String groupName;

	@Override
	public void setJobRegistry(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
		super.setJobRegistry(jobRegistry);
	}

	public void setJobLocator(DistributedJobLocator jobLocator) {
		this.jobLocator = jobLocator;
	}

	@Override
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		// Make sure we only post-process the Job bean from the job module's batch job
		if (bean instanceof FlowJob) {
			FlowJob job = (FlowJob) bean;
			job.setName(this.groupName);
			if (!jobRegistry.getJobNames().contains(groupName)) {
				// Add the job name & job parameters incrementer flag to {@link DistributedJobLocator}
				// Since, the Spring batch doesn't have persistent JobRegistry, the {@link DistributedJobLocator}
				// acts as the store to have jobName & incrementer flag to be used by {@link DistributedJobService}
				jobLocator.addJob(groupName, (job.getJobParametersIncrementer() != null) ? true : false);
				jobLocator.addStepNames(groupName, job.getStepNames());
				super.postProcessAfterInitialization(bean, beanName);
			}
			else {
				throw new BatchJobAlreadyExistsException(groupName);
			}
		}
		return bean;
	}

	@Override
	public void destroy() throws Exception {
		Assert.notNull(groupName, "JobName should not be null");
		jobLocator.deleteJobName(groupName);
		super.destroy();
	}
}
