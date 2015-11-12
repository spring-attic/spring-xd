/*
 * Copyright 2013-2015 the original author or authors.
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
import java.util.List;

import org.springframework.batch.admin.service.SearchableJobInstanceDao;
import org.springframework.batch.admin.service.SearchableStepExecutionDao;
import org.springframework.batch.admin.service.SimpleJobService;
import org.springframework.batch.core.Job;
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
import org.springframework.xd.dirt.job.dao.XdJdbcSearchableJobExecutionDao;

/**
 * SimpleJobService in distributed mode
 *
 * @author Ilayaperumal Gopinathan
 * @author Andrew Eisenberg
 * @author Gunnar Hillert
 */
public class DistributedJobService extends SimpleJobService {

	private DistributedJobLocator distributedJobLocator;

	private XdJdbcSearchableJobExecutionDao xdJdbcSearchableJobExecutionDao;

	public DistributedJobService(SearchableJobInstanceDao jobInstanceDao,
			XdJdbcSearchableJobExecutionDao xdJdbcSearchableJobExecutionDao,
			SearchableStepExecutionDao stepExecutionDao, JobRepository jobRepository, JobLauncher jobLauncher,
			DistributedJobLocator batchJobLocator, ExecutionContextDao executionContextDao) {
		super(jobInstanceDao, xdJdbcSearchableJobExecutionDao, stepExecutionDao, jobRepository, jobLauncher,
				batchJobLocator,
				executionContextDao);
		this.distributedJobLocator = batchJobLocator;
		this.xdJdbcSearchableJobExecutionDao = xdJdbcSearchableJobExecutionDao;
	}

	@Override
	public JobExecution launch(String jobName, JobParameters params) throws NoSuchJobException,
			JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException,
			JobParametersInvalidException {
		// TODO: make sure to use XD way of launching the job instead of {@link SimpleJobService}'s launch
		// as it uses JobParametersIncrementer that is not available in Job that the {@link DistributedJobLocator}
		// returns.
		throw new UnsupportedOperationException("Job Launch");
	}

	@Override
	public JobExecution restart(Long jobExecutionId) throws NoSuchJobExecutionException,
			JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException,
			NoSuchJobException, JobParametersInvalidException {
		throw new UnsupportedOperationException("Restart");
	}

	@Override
	public JobExecution abandon(Long jobExecutionId) throws NoSuchJobExecutionException,
			JobExecutionAlreadyRunningException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Abandon");
	}

	@Override
	public boolean isIncrementable(String jobName) {
		// if the batch job is not launchable (the job is not deployed) then return false
		// as the persistent job locator wouldn't have entries for the job.
		return (isLaunchable(jobName) ? distributedJobLocator.isIncrementable(jobName) : false);
	}

	public Job getJob(String jobName) throws NoSuchJobException {
		return distributedJobLocator.getJob(jobName);
	}

	/**
	 * Get a list of all {@link JobExecution}s that do not have any parent {@link JobExecution}s.
	 *
	 * @param start
	 * @param count
	 *
	 * @return {@link List} of {@link JobExecution}s. Will never return null.
	 */
	public Collection<JobExecution> getTopLevelJobExecutions(int start, int count) {
		return this.xdJdbcSearchableJobExecutionDao.getTopLevelJobExecutions(start, count);
	}

	public int countTopLevelJobExecutions() {
		return this.xdJdbcSearchableJobExecutionDao.countTopLevelJobExecutions();
	}

	/**
	 * Get a list of all {@link JobExecution}s that are direct children to the
	 * provided {@link JobExecution} ID.
	 *
	 * @param jobExecutionId
	 * @return {@link List} of {@link JobExecution}s. Will never return null.
	 */
	public Collection<JobExecution> getChildJobExecutions(long jobExecutionId) {
		return this.xdJdbcSearchableJobExecutionDao.getChildJobExecutions(jobExecutionId);
	}

	/**
	 * Determines, if the Job Execution represents a composed job.
	 *
	 * @param jobExecutionId
	 * @return Returns {@code true} if the Job Execution represents a composed job.
	 */
	public boolean isComposedJobExecution(long jobExecutionId) {
		return this.xdJdbcSearchableJobExecutionDao.isComposedJobExecution(jobExecutionId);
	}

}
