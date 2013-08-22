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

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 
 * @author Gunnar Hillert
 * @since 1.0
 * 
 */
public class JobFactoryBean implements FactoryBean<Job> {

	protected final Log logger = LogFactory.getLog(getClass());

	private String jobName;

	private JobRegistry registry;

	private static String JOB_NAME_DELIMITER = ".";

	private static int NAME_INDEX = 1;

	/**
	 * Instantiate the {@link JobFactoryBean} with the provided {@link JobRegistry} and the name of the {@link Job}.
	 * 
	 * @param registry Must not be null
	 * @param jobName Must not be empty
	 */
	public JobFactoryBean(JobRegistry registry, String jobName) {
		Assert.notNull(registry, "A JobRegistry is required");
		Assert.hasText(jobName, "The jobName must not be empty.");
		this.registry = registry;
		this.jobName = jobName;
	}

	/**
	 * Return the {@link Job} for the corresponding {@link #jobName}. An {@link IllegalArgumentException} will be thrown
	 * if the {@link Job} was not found in the {@link JobRegistry}.
	 */
	@Override
	public Job getObject() throws Exception {

		final Collection<String> names = registry.getJobNames();

		Job job = null;

		for (String curName : names) {
			String[] jobNames = StringUtils.split(curName, JOB_NAME_DELIMITER);
			if (jobNames[NAME_INDEX].equals(jobName)) {
				job = registry.getJob(curName);
				break;
			}
		}

		if (job == null) {
			throw new IllegalArgumentException(String.format("No Batch Job found "
					+ "for the provided Job Name '%s'.", jobName));
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
