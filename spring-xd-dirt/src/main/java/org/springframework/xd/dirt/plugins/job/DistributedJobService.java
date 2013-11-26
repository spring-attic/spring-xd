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

import java.util.Collection;

import org.springframework.batch.admin.service.SearchableJobExecutionDao;
import org.springframework.batch.admin.service.SearchableJobInstanceDao;
import org.springframework.batch.admin.service.SearchableStepExecutionDao;
import org.springframework.batch.admin.service.SimpleJobService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;

/**
 * SimpleJobService in distributed mode
 * 
 * @author Ilayaperumal Gopinathan
 * @author Andrew Eisenberg
 * @author Gunnar Hillert
 */
public class DistributedJobService extends SimpleJobService {

	private BatchJobLocator batchJobLocator;

	private SearchableJobInstanceDao jobInstanceDao;

	private SearchableJobExecutionDao jobExecutionDao;

	private SearchableStepExecutionDao stepExecutionDao;

	public DistributedJobService(SearchableJobInstanceDao jobInstanceDao, SearchableJobExecutionDao jobExecutionDao,
			SearchableStepExecutionDao stepExecutionDao, JobRepository jobRepository, JobLauncher jobLauncher,
			BatchJobLocator batchJobLocator, ExecutionContextDao executionContextDao) {
		super(jobInstanceDao, jobExecutionDao, stepExecutionDao, jobRepository, jobLauncher, batchJobLocator,
				executionContextDao);
		this.batchJobLocator = batchJobLocator;
		this.jobInstanceDao = jobInstanceDao;
		this.jobExecutionDao = jobExecutionDao;
		this.stepExecutionDao = stepExecutionDao;
	}

	@Override
	public JobExecution launch(String jobName, JobParameters params) throws NoSuchJobException,
			JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException,
			JobParametersInvalidException {
		// TODO
		throw new UnsupportedOperationException("Job Launch");
	}

	@Override
	public JobExecution restart(Long jobExecutionId) throws NoSuchJobExecutionException,
			JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException,
			NoSuchJobException, JobParametersInvalidException {
		// TODO
		throw new UnsupportedOperationException("Job Restart");
	}

	@Override
	public JobExecution abandon(Long jobExecutionId) throws NoSuchJobExecutionException,
			JobExecutionAlreadyRunningException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Abandon");
	}

	@Override
	public boolean isIncrementable(String jobName) {
		return batchJobLocator.isIncrementable(jobName);
	}

	@Override
	public Collection<JobExecution> listJobExecutions(int start, int count) {
		Collection<JobExecution> jobExecutions = jobExecutionDao.getJobExecutions(start, count);
		// Associate step executions for each of the job executions in the list.
		for (JobExecution jobExecution : jobExecutions) {
			stepExecutionDao.addStepExecutions(jobExecution);
		}
		return jobExecutions;
	}

}
