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

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.JobRegistryBeanPostProcessor;
import org.springframework.util.Assert;
import org.springframework.xd.dirt.job.BatchJobAlreadyExistsException;


/**
 * JobRegistryBeanPostProcessor that processes Job bean with name {@link JobPlugin.JOB_BEAN_ID}
 * 
 * @author Ilayaperumal Gopinathan
 */
public class BatchJobRegistryBeanPostProcessor extends JobRegistryBeanPostProcessor {

	private JobRegistry jobRegistry;

	private BatchJobLocator jobLocator;

	private String groupName;

	private String jobName;

	@Override
	public void setJobRegistry(JobRegistry jobRegistry) {
		this.jobRegistry = jobRegistry;
		super.setJobRegistry(jobRegistry);
	}

	public void setJobLocator(BatchJobLocator jobLocator) {
		this.jobLocator = jobLocator;
	}

	@Override
	public void setGroupName(String groupName) {
		this.groupName = groupName;
		super.setGroupName(groupName);
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) {
		// Make sure we only post-process the Job bean from the job module's batch job
		if (bean instanceof Job && beanName.equals(JobPlugin.JOB_BEAN_ID)) {
			Job job = (Job) bean;

			// the job name at the JobRegistry will be <groupName>.<batchJobId>
			jobName = this.groupName + JobPlugin.JOB_NAME_DELIMITER + job.getName();
			if (!jobRegistry.getJobNames().contains(jobName)) {
				// Add the job name & job parameters incrementer flag to BatchJobLocator
				// Since, the Spring batch doesn't have persistent JobRegistry, the BatchJobLocator
				// acts as the store to have jobName & incrementer flag to be used by {@DistributedJobService}
				jobLocator.addJob(jobName, (job.getJobParametersIncrementer() != null) ? true : false);
				super.postProcessAfterInitialization(bean, beanName);
			}
			else {
				// Currently there is no way to get to this as the job module's batch job configuration
				// schema won't allow multiple ids with JobPlugin.JOB_BEAN_ID ("job")
				throw new BatchJobAlreadyExistsException(jobName);
			}
		}
		return bean;
	}

	@Override
	public void destroy() throws Exception {
		Assert.notNull(jobName, "JobName should not be null");
		jobLocator.deleteJobName(jobName);
		super.destroy();
	}
}
