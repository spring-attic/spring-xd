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

package org.springframework.xd.dirt.plugins.job;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;

/**
 * 
 * @author Gunnar Hillert
 * @author Dave Syer
 * @since 1.0
 * 
 */
public class JobFactoryBean implements FactoryBean<Job> {

	protected final Log logger = LogFactory.getLog(getClass());

	private JobRegistry registry;

	private String jobName;

	private String jobSuffix;

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}


	public void setJobSuffix(String jobSuffix) {
		this.jobSuffix = jobSuffix;
	}


	public void setRegistry(JobRegistry registry) {
		this.registry = registry;
	}

	/**
	 * Return the {@link Job} for the corresponding {@link #jobName}. An {@link IllegalArgumentException} will be thrown
	 * if the {@link Job} was not found in the {@link JobRegistry}.
	 */
	@Override
	public Job getObject() throws Exception {

		// Delay validation otherwise early instantiation can prang the application context (see SPR-11049)
		Assert.notNull(registry, "A JobRegistry is required");
		Assert.hasText(jobName, "The jobName must not be empty.");
		Assert.hasText(jobSuffix, "The jobSuffix must not be empty.");

		final Job job;

		String jobKeyInRegistry = jobName + JobPlugin.JOB_NAME_DELIMITER + jobSuffix;

		try {
			job = registry.getJob(jobKeyInRegistry);
		}
		catch (NoSuchJobException e) {
			throw new IllegalStateException(String.format("No Batch Job found in registry "
					+ "for the provided key '%s'.", jobKeyInRegistry));
		}

		return job;
	}

	@Override
	public Class<?> getObjectType() {
		return Job.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
