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

package org.springframework.xd.rest.client;

import java.util.List;

import org.springframework.hateoas.PagedResources;
import org.springframework.xd.rest.client.domain.JobDefinitionResource;
import org.springframework.xd.rest.client.domain.JobExecutionInfoResource;
import org.springframework.xd.rest.client.domain.StepExecutionInfoResource;
import org.springframework.xd.rest.client.domain.StepExecutionProgressInfoResource;

/**
 * Interface defining operations available against jobs.
 * 
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 * @author Gunnar Hillert
 * 
 */
public interface JobOperations extends ResourceOperations {

	/**
	 * Create a new Job, optionally deploying it.
	 */
	public JobDefinitionResource createJob(String name, String defintion, boolean deploy);

	/**
	 * Launch a job that is already deployed.
	 * 
	 * @param name the name of the job to launch
	 * @param jobParameters the JSON string as jobParameters
	 */
	public void launchJob(String name, String jobParameters);

	/**
	 * List jobs known to the system.
	 */
	public PagedResources<JobDefinitionResource> list();

	/**
	 * List all Job Executions.
	 */
	public List<JobExecutionInfoResource> listJobExecutions();

	/**
	 * Retrieve a specific Job Execution for the provided {@code jobExecutionId}.
	 * 
	 */
	public JobExecutionInfoResource displayJobExecution(long jobExecutionId);

	/**
	 * Retrieve a {@link List} of {@link StepExecutionInfoResource}s for the provided {@code jobExecutionId}.
	 * 
	 */
	public List<StepExecutionInfoResource> listStepExecutions(long jobExecutionId);

	/**
	 * Stop job execution that is running.
	 */
	public void stopJobExecution(long jobExecutionId);

	/**
	 * Stop all job executions.
	 */
	public void stopAllJobExecutions();

	/**
	 * Retrieve step execution progress with the given {@code jobExecutionId} and {@code stepExecutionId}.
	 */
	public StepExecutionProgressInfoResource stepExecutionProgress(long jobExecutionId, long stepExecutionId);
}
