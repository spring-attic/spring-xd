/*
 * Copyright 2013-2015 the original author or authors.
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.PagedResources.PageMetadata;
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
import org.springframework.xd.dirt.stream.JobDefinition;
import org.springframework.xd.dirt.stream.JobDeployer;
import org.springframework.xd.rest.domain.JobExecutionInfoResource;

/**
 * Controller for batch job executions.
 *
 * @author Dave Syer
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 *
 */
@RestController
@RequestMapping("/jobs/executions")
@ExposesResourceFor(JobExecutionInfoResource.class)
public class BatchJobExecutionsController extends AbstractBatchJobsController {

	@Autowired
	private JobDeployer jobDeployer;

	@Autowired
	private DistributedJobLocator jobLocator;

	@Autowired
	private StepExecutionDao stepExecutionDao;

	/**
	 * List all job executions in a given range. If no pagination is provided,
	 * the default {@code PageRequest(0, 20)} is passed in. See {@link PageableHandlerMethodArgumentResolver}
	 * for details.
	 *
	 * @param pageable If not provided will default to page 0 and a page size of 20
	 * @return Collection of JobExecutionInfoResource
	 */
	@RequestMapping(value = { "" }, method = RequestMethod.GET, produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<JobExecutionInfoResource> list(Pageable pageable) {

		final List<JobExecutionInfoResource> resources = new ArrayList<JobExecutionInfoResource>();
		final Set<String> restartableJobs = new HashSet<String>(jobLocator.getAllRestartableJobs());
		final Set<String> deployedJobs = new HashSet<String>(jobLocator.getJobNames());
		final Set<String> jobDefinitionNames = new HashSet<String>(getJobDefinitionNames());

		for (JobExecution jobExecution : jobService.getTopLevelJobExecutions(pageable.getOffset(),
				pageable.getPageSize())) {

			stepExecutionDao.addStepExecutions(jobExecution);
			final JobExecutionInfoResource jobExecutionInfoResource = getJobExecutionInfoResource(jobExecution,
					restartableJobs, deployedJobs, jobDefinitionNames);
			boolean isComposed = jobService.isComposedJobExecution(jobExecution.getId());
			jobExecutionInfoResource.setComposedJob(isComposed);

			resources.add(jobExecutionInfoResource);
		}

		final PagedResources<JobExecutionInfoResource> pagedResources = new PagedResources<JobExecutionInfoResource>(
				resources,
				new PageMetadata(pageable.getPageSize(), pageable.getPageNumber(),
						Long.valueOf(jobService.countTopLevelJobExecutions())));
		return pagedResources;
	}

	/**
	 * Return a paged collection of job executions for a given job.
	 *
	 * @param jobName name of the job
	 * @param startJobExecution start index for the job execution list
	 * @param pageSize page size for the list
	 * @return collection of JobExecutionInfo
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, params = "jobname", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public Collection<JobExecutionInfoResource> executionsForJob(@RequestParam("jobname") String jobName,
			@RequestParam(defaultValue = "0") int startJobExecution,
			@RequestParam(defaultValue = "20") int pageSize) {

		Collection<JobExecutionInfoResource> result = new ArrayList<JobExecutionInfoResource>();
		try {
			for (JobExecution jobExecution : jobService.listJobExecutionsForJob(jobName, startJobExecution, pageSize)) {
				result.add(jobExecutionInfoResourceAssembler.toResource(new JobExecutionInfo(jobExecution, timeZone)));
			}
		}
		catch (NoSuchJobException e) {
			throw new NoSuchBatchJobException(jobName);
		}
		return result;
	}

	/**
	 * Send the request to launch Job. Job has to be deployed first.
	 *
	 * @param name the name of the job
	 * @param jobParameters the job parameters in JSON string
	 */
	@RequestMapping(value = "", method = RequestMethod.POST, params = "jobname")
	@ResponseStatus(HttpStatus.CREATED)
	public void launchJob(@RequestParam("jobname") String name, @RequestParam(required = false) String jobParameters) {
		jobDeployer.launch(name, jobParameters);
	}


	/**
	 * Get all existing job definition names.
	 *
	 * @return the collection of job definition names
	 */
	private Collection<String> getJobDefinitionNames() {
		Collection<String> jobDefinitionNames = new ArrayList<String>();
		Iterable<JobDefinition> jobDefinitions = xdJobDefinitionRepository.findAll();
		for (JobDefinition definition : jobDefinitions) {
			jobDefinitionNames.add(definition.getName());
		}
		return jobDefinitionNames;
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

		final Set<String> restartableJobs = new HashSet<String>(jobLocator.getAllRestartableJobs());
		final Set<String> deployedJobs = new HashSet<String>(jobLocator.getJobNames());
		final Set<String> jobDefinitionNames = new HashSet<String>(getJobDefinitionNames());

		final JobExecutionInfoResource jobExecutionInfoResource = getJobExecutionInfoResource(jobExecution,
				restartableJobs, deployedJobs, jobDefinitionNames);

		for (JobExecution childJobExecution : jobService.getChildJobExecutions(jobExecution.getId())) {
			jobExecutionInfoResource.setComposedJob(true);
			stepExecutionDao.addStepExecutions(childJobExecution);
			final JobExecutionInfoResource childJobExecutionInfoResource = getJobExecutionInfoResource(
					childJobExecution,
					restartableJobs, deployedJobs, jobDefinitionNames);
			boolean isComposed = jobService.isComposedJobExecution(childJobExecution.getId());
			childJobExecutionInfoResource.setComposedJob(isComposed);

			jobExecutionInfoResource.getChildJobExecutions().add(childJobExecutionInfoResource);
		}

		return jobExecutionInfoResource;
	}

	private JobExecutionInfoResource getJobExecutionInfoResource(JobExecution jobExecution,
			Set<String> restartableJobs,
			Set<String> deployedJobs,
			Set<String> jobDefinitionNames) {

		final JobExecutionInfoResource jobExecutionInfoResource = jobExecutionInfoResourceAssembler.toResource(
				new JobExecutionInfo(
						jobExecution,
						timeZone));
		final String jobName = jobExecution.getJobInstance().getJobName();
		jobExecutionInfoResource.setDeleted(!jobDefinitionNames.contains(jobName));
		jobExecutionInfoResource.setDeployed(deployedJobs.contains(jobName));
		if (restartableJobs.contains(jobName)) {
			// Set restartable flag for the JobExecutionResource based on the actual JobInstance
			// If any one of the jobExecutions for the jobInstance is complete, set the restartable flag for
			// all the jobExecutions to false.
			if (jobExecution.getStatus() != BatchStatus.COMPLETED) {
				jobExecutionInfoResource.setRestartable(isJobExecutionRestartable(jobExecution));
			}
		}
		else {
			// Set false for this job execution irrespective its status.
			jobExecutionInfoResource.setRestartable(false);
		}

		return jobExecutionInfoResource;
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
			throw new org.springframework.xd.dirt.job.JobExecutionAlreadyRunningException(
					"Job Execution for this job is already running: " + jobExecution.getJobInstance());
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

		final String jobParametersAsString = expandedJobParametersConverter.getJobParametersAsString(jobParameters,
				true);
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
