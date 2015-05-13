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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.Lifecycle;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Executes all jobs defined within a given stream once the context has been started. This really should be replaced
 * once we have the concept of triggers built in.
 *
 * @author Michael Minella
 * @author Gunnar Hillert
 * @since 1.0
 *
 */
public class ModuleJobLauncher implements Lifecycle {

	protected final Logger logger = LoggerFactory.getLogger(getClass());

	private JobLauncher launcher;

	private String groupName;

	private JobRegistry registry;

	private static String JOB_NAME_DELIMITER = ".";

	private static int NAME_INDEX = 0;

	private boolean isRunning = false;

	private final boolean executeBatchJobOnStartup;

	private final JobParameters jobParameters;

	public ModuleJobLauncher(JobLauncher launcher, JobRegistry registry, boolean executeBatchJobOnStartup,
			JobParametersBean jobParametersBean) {
		Assert.notNull(launcher, "A JobLauncher is required");

		this.launcher = launcher;
		this.registry = registry;
		this.executeBatchJobOnStartup = executeBatchJobOnStartup;
		this.jobParameters = jobParametersBean.getJobParameters();
	}

	@Override
	public void start() {
		isRunning = true;

		if (executeBatchJobOnStartup) {
			executeBatchJob();
		}

	}

	public void executeBatchJob() {
		Collection<String> names = registry.getJobNames();

		for (String curName : names) {
			String[] jobNames = StringUtils.split(curName, JOB_NAME_DELIMITER);
			if (jobNames[NAME_INDEX].equals(groupName)) {
				try {
					launcher.run(registry.getJob(curName), jobParameters);
				}
				catch (Exception e) {
					logger.error("An error occured while starting job " + curName, e);
				}
			}
		}
	}

	@Override
	public void stop() {
		isRunning = false;
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
}
