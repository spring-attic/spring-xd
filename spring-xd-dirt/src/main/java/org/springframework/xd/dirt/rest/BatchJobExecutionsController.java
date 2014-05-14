/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.xd.dirt.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.xd.dirt.job.JobExecutionInfo;
import org.springframework.xd.dirt.job.JobExecutionNotRunningException;
import org.springframework.xd.dirt.job.NoSuchBatchJobException;
import org.springframework.xd.dirt.job.NoSuchJobExecutionException;
import org.springframework.xd.dirt.plugins.job.DistributedJobLocator;
import org.springframework.xd.dirt.plugins.job.ExpandedJobParametersConverter;
import org.springframework.xd.dirt.stream.JobDeployer;
import org.springframework.xd.rest.client.domain.JobExecutionInfoResource;

/**
 * Controller for batch job executions.
 *
 * @author Dave Syer
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 *
 */
@RestController
@RequestMapping("/batch/executions")
@ExposesResourceFor(JobExecutionInfoResource.class)
public class BatchJobExecutionsController extends AbstractBatchJobsController {

	@Autowired
	private JobDeployer jobDeployer;

	@Autowired
	private DistributedJobLocator jobLocator;

	/**
	 * List all job executions in a given range.
	 *
	 * @param startJobExecution index of the first job execution to get
	 * @param pageSize how many executions to return
	 * @return Collection of JobExecutionInfoResource
	 */
	@RequestMapping(value = { "" }, method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public Collection<JobExecutionInfoResource> list(@RequestParam(defaultValue = "0") int startJobExecution,
			@RequestParam(defaultValue = "20") int pageSize) {

		Collection<JobExecutionInfoResource> result = new ArrayList<JobExecutionInfoResource>();
		JobExecutionInfoResource jobExecutionInfoResource;
		for (JobExecution jobExecution : jobService.listJobExecutions(startJobExecution, pageSize)) {
			jobExecutionInfoResource = jobExecutionInfoResourceAssembler.toResource(new JobExecutionInfo(jobExecution,
					timeZone));
			// Set restartable flag for the JobExecutionResource based on the actual JobInstance
			// If any one of the jobExecutions for the jobInstance is complete, set the restartable flag for
			// all the jobExecutions to false.
			if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
				jobExecutionInfoResource.setRestartable(isJobExecutionRestartable(jobExecution));
			}
			result.add(jobExecutionInfoResource);
		}
		return result;
	}

	/**
	 * Check if the {@link JobInstance} corresponds to the given {@link JobExecution}
	 * has any of the JobExecutions in {@link BatchStatus.COMPLETED} status
	 * @param jobExecution the jobExecution to check for
	 * @return boolean flag to set if this job execution can be restarted
	 */
	private boolean isJobExecutionRestartable(JobExecution jobExecution) {
		JobInstance jobInstance = jobExecution.getJobInstance();
		BatchStatus status = jobExecution.getStatus();
		try {
			List<JobExecution> jobExecutionsForJobInstance = (List<JobExecution>) jobService.getJobExecutionsForJobInstance(
					jobInstance.getJobName(), jobInstance.getId());
			for (JobExecution jobExecutionForJobInstance : jobExecutionsForJobInstance) {
				if (jobExecutionForJobInstance.getStatus() == BatchStatus.COMPLETED) {
					return false;
				}
			}
		}
		catch (NoSuchJobException e) {
			throw new NoSuchBatchJobException(jobInstance.getJobName());
		}
		return status.isGreaterThan(BatchStatus.STOPPING) && status.isLessThan(BatchStatus.ABANDONED);
	}

	/**
	 * @param executionId Id of the {@link JobExecution}
	 * @return JobExecutionInfo for the given job name
	 * @throws NoSuchJobExecutionException Thrown if the {@link JobExecution} does not exist
	 */
	@RequestMapping(value = "/{executionId}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public JobExecutionInfoResource getJobExecutionInfo(@PathVariable long executionId) {

		final JobExecution jobExecution;

		try {
			jobExecution = jobService.getJobExecution(executionId);
		}
		catch (org.springframework.batch.core.launch.NoSuchJobExecutionException e) {
			throw new NoSuchJobExecutionException(executionId);
		}

		return jobExecutionInfoResourceAssembler.toResource(new JobExecutionInfo(jobExecution, timeZone));
	}

	/**
	 * Stop Job Execution by the given executionId.
	 *
	 * @param jobExecutionId the executionId of the job execution to stop
	 */
	@RequestMapping(value = { "/{executionId}" }, method = RequestMethod.PUT, params = "stop=true")
	@ResponseStatus(HttpStatus.OK)
	public void stopJobExecution(@PathVariable("executionId") long jobExecutionId) {
		try {
			jobService.stop(jobExecutionId);
		}
		catch (org.springframework.batch.core.launch.JobExecutionNotRunningException e) {
			throw new JobExecutionNotRunningException(jobExecutionId);
		}
		catch (org.springframework.batch.core.launch.NoSuchJobExecutionException e) {
			throw new NoSuchJobExecutionException(jobExecutionId);
		}
	}

	/**
	 * Restart the Job Execution with the given executionId.
	 *
	 * @param jobExecutionId the executionId of the job execution to restart
	 */
	@RequestMapping(value = { "/{executionId}" }, method = RequestMethod.PUT, params = "restart=true")
	@ResponseStatus(HttpStatus.OK)
	public void restartJobExecution(@PathVariable("executionId") long jobExecutionId) {

		final JobExecution jobExecution;
		try {
			jobExecution = jobService.getJobExecution(jobExecutionId);
		}
		catch (org.springframework.batch.core.launch.NoSuchJobExecutionException e) {
			throw new NoSuchJobExecutionException(jobExecutionId);
		}

		if (jobExecution.isRunning()) {
			throw new org.springframework.xd.dirt.job.JobExecutionAlreadyRunningException("Job Execution "
					+ jobExecution.getId()
					+ " is already running.");
		}

		final JobInstance lastInstance = jobExecution.getJobInstance();
		final ExpandedJobParametersConverter expandedJobParametersConverter = new ExpandedJobParametersConverter();
		final JobParameters jobParameters = jobExecution.getJobParameters();

		final Job job;
		try {
			job = jobLocator.getJob(lastInstance.getJobName());
		}
		catch (NoSuchJobException e1) {
			throw new org.springframework.xd.dirt.job.NoSuchBatchJobException("The job '" + lastInstance.getJobName()
					+ "' does not exist.");
		}
		try {
			job.getJobParametersValidator().validate(jobParameters);
		}
		catch (JobParametersInvalidException e) {
			throw new org.springframework.xd.dirt.job.JobParametersInvalidException(
					"The Job Parameters for Job Execution " + jobExecution.getId()
							+ " are invalid.");
		}

		final BatchStatus status = jobExecution.getStatus();

		if (status == BatchStatus.COMPLETED || status == BatchStatus.ABANDONED) {
			throw new org.springframework.xd.dirt.job.JobInstanceAlreadyCompleteException(
					"Job Execution " + jobExecution.getId() + " is already complete.");
		}

		if (!job.isRestartable()) {
			throw new org.springframework.xd.dirt.job.JobRestartException(
					"The job '" + lastInstance.getJobName() + "' is not restartable.");
		}

		final String jobParametersAsString = expandedJobParametersConverter
				.getJobParametersAsString(jobParameters, true);
		jobDeployer.launch(lastInstance.getJobName(), jobParametersAsString);
	}

	/**
	 * Stop all job executions.
	 */
	@RequestMapping(value = { "" }, method = RequestMethod.PUT, params = "stop=true")
	@ResponseStatus(HttpStatus.OK)
	public void stopAll() {
		jobService.stopAll();
	}
}
