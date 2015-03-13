/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.xd.dirt.plugins.job.support;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.core.repository.support.SimpleJobRepository;

/**
 * This {@link org.springframework.batch.core.repository.JobRepository} implementation
 * provides the same functionality as Spring Batch's
 * {@link org.springframework.batch.core.repository.support.SimpleJobRepository} however
 * it does not re-hydrate a {@link org.springframework.batch.item.ExecutionContext} when
 * querying for a {@link org.springframework.batch.core.JobExecution} or a
 * {@link org.springframework.batch.core.StepExecution}.  This is done to prevent class
 * loading issues when launching jobs within Spring XD.
 *
 * @author Michael Minella
 */
public class JobLaunchingJobRepository extends SimpleJobRepository {

	private JobInstanceDao jobInstanceDao;
	private JobExecutionDao jobExecutionDao;
	private StepExecutionDao stepExecutionDao;

	public JobLaunchingJobRepository(JobInstanceDao jobInstanceDao,
			JobExecutionDao jobExecutionDao, StepExecutionDao stepExecutionDao,
			ExecutionContextDao executionContextDao) {
		super(jobInstanceDao, jobExecutionDao, stepExecutionDao, executionContextDao);
		this.jobInstanceDao = jobInstanceDao;
		this.jobExecutionDao = jobExecutionDao;
		this.stepExecutionDao = stepExecutionDao;
	}

	/**
	 * Returns the last {@link JobExecution} for the requested job <em>without</em>
	 * rehydrating the related {@link org.springframework.batch.item.ExecutionContext}.
	 *
	 * @param jobName The name of the job
	 * @param jobParameters The parameters of the job
	 * @return The related JobExecution
	 */
	@Override
	public JobExecution getLastJobExecution(String jobName, JobParameters jobParameters) {
		JobInstance jobInstance = jobInstanceDao.getJobInstance(jobName, jobParameters);

		if (jobInstance == null) {
			return null;
		}

		JobExecution jobExecution = jobExecutionDao.getLastJobExecution(jobInstance);

		if (jobExecution != null) {
			stepExecutionDao.addStepExecutions(jobExecution);
		}

		return jobExecution;

	}

	/**
	 * Returns the last {@link org.springframework.batch.core.StepExecution} for the
	 * requested step <em>without</em> rehydrating the related
	 * {@link org.springframework.batch.item.ExecutionContext}.
	 *
	 * @param jobInstance The {@link org.springframework.batch.core.JobInstance} the step
	 * 					  should come from
	 * @param stepName The name of the step to find
	 * @return The related StepExecution
	 */
	@Override
	public StepExecution getLastStepExecution(JobInstance jobInstance, String stepName) {
		List<JobExecution> jobExecutions = jobExecutionDao.findJobExecutions(jobInstance);
		List<StepExecution> stepExecutions = new ArrayList<StepExecution>(jobExecutions.size());

		for (JobExecution jobExecution : jobExecutions) {
			stepExecutionDao.addStepExecutions(jobExecution);
			for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
				if (stepName.equals(stepExecution.getStepName())) {
					stepExecutions.add(stepExecution);
				}
			}
		}

		StepExecution latest = null;
		for (StepExecution stepExecution : stepExecutions) {
			if (latest == null) {
				latest = stepExecution;
			}
			if (latest.getStartTime().getTime() < stepExecution.getStartTime().getTime()) {
				latest = stepExecution;
			}
		}

		return latest;
	}
}
