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

package org.springframework.xd.dirt.plugins.job.batch;

import java.util.Collection;
import java.util.List;

import org.springframework.batch.admin.service.SearchableJobExecutionDao;
import org.springframework.batch.admin.service.SearchableJobInstanceDao;
import org.springframework.batch.admin.service.SearchableStepExecutionDao;
import org.springframework.batch.admin.service.SimpleJobService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
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
 */
public class DistributedJobService extends SimpleJobService {

	private BatchJobLocator batchJobLocator;

	private SearchableJobInstanceDao jobInstanceDao;

	private SearchableJobExecutionDao jobExecutionDao;

	private JobRegistry jobRegistry;

	private JobRepository jobRepository;

	public DistributedJobService(SearchableJobInstanceDao jobInstanceDao, SearchableJobExecutionDao jobExecutionDao,
			SearchableStepExecutionDao stepExecutionDao, JobRepository jobRepository, JobLauncher jobLauncher,
			BatchJobLocator batchJobLocator, ExecutionContextDao executionContextDao) {
		super(jobInstanceDao, jobExecutionDao, stepExecutionDao, jobRepository, jobLauncher, batchJobLocator,
				executionContextDao);
		this.jobRepository = jobRepository;
		this.batchJobLocator = batchJobLocator;
		this.jobInstanceDao = jobInstanceDao;
		this.jobExecutionDao = jobExecutionDao;
		this.jobRegistry = jobRegistry;
	}

	@Override
	public JobExecution launch(String jobName, JobParameters params) throws NoSuchJobException,
			JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException,
			JobParametersInvalidException {
		JobExecution execution = jobRepository.createJobExecution(jobName, params);
		jobRegistry.getJob(jobName).execute(execution);
		return execution;
	}

	@Override
	public JobExecution restart(Long jobExecutionId) throws NoSuchJobExecutionException,
			JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException,
			NoSuchJobException, JobParametersInvalidException {
		// TODO
		throw new UnsupportedOperationException("Job Restart");
	}

	@Override
	public JobExecution stop(Long jobExecutionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Stop Job Execution");
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
	public Collection<JobExecution> listJobExecutionsForJob(String jobName, int start, int count)
			throws NoSuchJobException {
		checkJobExists(jobName);
		List<JobExecution> jobExecutions = jobExecutionDao.getJobExecutions(jobName, start, count);
		return jobExecutions;
	}

	// TODO probably not needed. OK to delete?
	// public JobParameters getJobParameters(Long jobExecutionId) throws
	// NoSuchJobExecutionException {
	// try {
	// // TODO extract to constant
	// Method getParametersMethod =
	// JdbcJobExecutionDao.class.getDeclaredMethod("getJobParameters", Long.class);
	// ReflectionUtils.makeAccessible(getParametersMethod);
	// JobParameters parameters = (JobParameters)
	// ReflectionUtils.invokeMethod(getParametersMethod,
	// jobExecutionDao, jobExecutionId);
	// return parameters == null ? new JobParameters() : parameters;
	// }
	// catch (Exception e) {
	// throw new NoSuchJobExecutionException("Could not get job execution for id " +
	// jobExecutionId, e);
	// }
	// }

	private void checkJobExists(String jobName) throws NoSuchJobException {
		if (batchJobLocator.getJobNames().contains(jobName)) {
			return;
		}
		if (jobInstanceDao.countJobInstances(jobName) > 0) {
			return;
		}
		throw new NoSuchJobException("No Job with that name either current or historic: [" + jobName + "]");
	}

}
