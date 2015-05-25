/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.xd.integration.test;

import org.junit.After;
import org.junit.Before;

import org.springframework.batch.core.BatchStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.PagedResources;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.xd.integration.fixtures.Jobs;
import org.springframework.xd.integration.fixtures.ModuleType;
import org.springframework.xd.integration.util.StreamUtils;
import org.springframework.xd.rest.client.impl.SpringXDTemplate;
import org.springframework.xd.rest.domain.JobDefinitionResource;
import org.springframework.xd.rest.domain.JobExecutionInfoResource;
import org.springframework.xd.rest.domain.StepExecutionInfoResource;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertTrue;


/**
 * /**
 * Base Class for Spring XD Integration Job test classes
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 */
public abstract class AbstractJobTest extends AbstractIntegrationTest {

	protected final static String JOB_NAME = "ec2Job3";
	protected final static int DEFAULT_JOB_COMPLETE_COUNT = 1;

	@Autowired
	protected Jobs jobs;

	private SpringXDTemplate springXDTemplate;

	@Override
	@Before
	public void setup() {
		initializer();
		springXDTemplate = createSpringXDTemplate();
		destroyAllJobs();
		waitForEmptyJobList(WAIT_TIME);
		StreamUtils.destroyAllStreams(adminServer);
		waitForXD();

	}

	/**
	 * Destroys all streams created in the test.
	 */
	@Override
	@After
	public void tearDown() {
		destroyAllJobs();
		waitForEmptyJobList(WAIT_TIME);
	}

	/**
	 * Creates a job on the XD cluster defined by the test's
	 * Artifact or Environment variables Uses JOB_NAME as default job name.
	 *
	 * @param job the job definition
	 */
	public void job(String job) {
		Assert.hasText(job, "job needs to be populated with a definition and can not be null");
		job(JOB_NAME, job, true);
	}

	/**
	 * Launches a job with the test's JOB_NAME on the XD instance.
	 */
	public void jobLaunch() {
		jobLaunch(JOB_NAME);
	}

	/**
	 * Launches a job on the XD instance
	 *
	 * @param jobName The name of the job to be launched
	 */
	public void jobLaunch(String jobName) {
		launchJob(jobName, "");
	}

	/**
	 * Gets the host of the container where the job was deployed
	 *
	 * @return The host that contains the job.
	 */
	public String getContainerHostForJob() {
		return getContainerHostForJob(JOB_NAME);
	}

	/**
	 * Gets the host of the container where the job was deployed
	 *
	 * @param jobName Used to find the container that contains the job.
	 * @return The host that contains the job.
	 */
	public String getContainerHostForJob(String jobName) {
		return this.getContainerResolver().getContainerHostForModulePrefix(jobName, ModuleType.job);
	}

	/*
	* Launches a job on the XD instance
	*
	* @param jobName The name of the job to be launched
	* @param jobParameters the job parameters
	*/
	public void launchJob(String jobName, String jobParameters) {
		Assert.hasText(jobName, "The jobName must not be empty nor null");
		springXDTemplate.jobOperations().launchJob(jobName, jobParameters);
	}

	/**
	 * Deploys the job.
	 */
	public void deployJob() {
		deployJob(JOB_NAME);
	}

	/**
	 * Deploys the job with the jobName.
	 *
	 * @param jobName The name of the job to deploy.
	 */
	public void deployJob(String jobName) {
		Assert.hasText(jobName, "jobName must not be empty nor null");
		springXDTemplate.jobOperations().deploy(jobName, Collections.<String, String>emptyMap());
	}

	/**
	 * Verifies that the description and status for the job name is as expected.
	 *
	 * @param jobDefinition the definition that the job should have.
	 * @param deployed      if true the job should be deployed.  if false the job should not be deployed.
	 * @return true if the job is as expected.   false if the job is not.
	 */
	public boolean checkJob(String jobDefinition, boolean deployed) {
		return checkJob(JOB_NAME, jobDefinition, deployed);
	}

	/**
	 * Verifies that the description and status for the job name is as expected.
	 *
	 * @param jobName       The name of the job to evaluate.
	 * @param jobDefinition the definition that the job should have.
	 * @param deployed      if true the job should be deployed.  if false the job should not be deployed.
	 * @return true if the job is as expected.   false if the job is not.
	 */
	public boolean checkJob(String jobName, String jobDefinition, boolean deployed) {
		Assert.hasText(jobName, "jobName must not be empty nor null");
		Assert.hasText(jobDefinition, "jobDefinition must not be empty nor null");
		JobDefinitionResource resource = getJobDefinitionResource(jobName);
		long timeout = System.currentTimeMillis() + WAIT_TIME;
		String deployedStatus = (deployed) ? "deployed" : "undeployed";
		boolean status = resource != null && deployedStatus.equals(resource.getStatus())
				&& jobDefinition.equals(resource.getDefinition());
		while (!status && System.currentTimeMillis() < timeout) {
			sleepOneSecond();
			resource = getJobDefinitionResource(jobName);
			status = resource != null && deployedStatus.equals(resource.getStatus())
					&& jobDefinition.equals(resource.getDefinition());
		}
		return status;
	}

	/**
	 * Waits until the number of "Complete" job executions reaches the jobCompleteCount.
	 *
	 * @param jobName          The name of the job to evaluate.
	 * @param jobCompleteCount the number of job executions marked complete to return true.
	 * @param waitTime         The milliseconds that the method should wait for the job execution to complete.
	 * @return True if the number of Complete Jobs is reached before waitTime, else false.
	 */
	protected boolean waitForJobToComplete(String jobName, long waitTime, int jobCompleteCount) {
		Assert.hasText(jobName, "The job name must be specified.");

		boolean isJobComplete = isJobComplete(jobName, jobCompleteCount);
		long timeout = System.currentTimeMillis() + waitTime;
		while (!isJobComplete && System.currentTimeMillis() < timeout) {
			sleepOneSecond();
			isJobComplete = isJobComplete(jobName, jobCompleteCount);
		}
		return isJobComplete;
	}

	/**
	 * Wait until the job is complete up to default WAIT_TIME. The method will return true
	 * if any of the job executions are marked complete.
	 * Hint: use a unique jobName, to guarantee that you will get zero or one jobExecution.
	 *
	 * @param jobName The name of the job to evaluate.
	 */
	protected boolean waitForJobToComplete(String jobName) {
		return waitForJobToComplete(jobName, DEFAULT_JOB_COMPLETE_COUNT);
	}

	/**
	 * Waits until the number of "Complete" job executions reaches the jobCompleteCount.
	 *
	 * @param jobName          The name of the job to evaluate.
	 * @param jobCompleteCount The number of job executions marked complete before returning true.
	 */
	protected boolean waitForJobToComplete(String jobName, int jobCompleteCount) {
		Assert.hasText(jobName, "The job name must be specified.");
		return waitForJobToComplete(jobName, WAIT_TIME, jobCompleteCount);
	}

	/**
	 * Checks to see if the execution for the job is complete.
	 * Returns true when the number of job complete executions matches the job completeCount.
	 *
	 * @param jobName          The name of the job to be evaluated.
	 * @param jobCompleteCount The number of executions that reached complete to return true.
	 * @return true if the job is deployed else false
	 */
	private boolean isJobComplete(String jobName, int jobCompleteCount) {
		List<JobExecutionInfoResource> resources = getJobExecInfoByName(jobName);
		Iterator<JobExecutionInfoResource> resourceIter = resources.iterator();
		int count = 0;
		while (resourceIter.hasNext()) {
			JobExecutionInfoResource resource = resourceIter.next();

			if (jobName.equals(resource.getName())) {
				if (BatchStatus.COMPLETED.equals(resource.getJobExecution().getStatus())) {
					count++;
					break;
				} else {
					break;
				}
			}
		}
		return jobCompleteCount == count;
	}

	/**
	 * Creates the job definition and deploys it to the cluster being tested.
	 *
	 * @param jobName       The name of the job
	 * @param jobDefinition The definition that needs to be deployed for this job.
	 * @param isDeploy      true deploy the job.  False do not deploy the job.
	 */
	protected void job(final String jobName, final String jobDefinition, boolean isDeploy) {
		Assert.hasText(jobName, "The job name must be specified.");
		Assert.notNull(jobDefinition, "a job definition must not be empty.");
		springXDTemplate.jobOperations().createJob(jobName, jobDefinition, isDeploy);
		String action = (isDeploy) ? "deploy" : "undeploy";
		assertTrue("The job did not " + action + ". ",
				waitForJobDeploymentChange(jobName, WAIT_TIME, isDeploy));

	}


	/**
	 * Removes all the jobs from the cluster. Used to guarantee a clean acceptance test.
	 */
	protected void destroyAllJobs() {
		springXDTemplate.jobOperations().destroyAll();
	}

	/**
	 * Undeploys the job
	 */
	protected void undeployJob() {
		undeployJob(JOB_NAME);
	}

	/*
	 * Destroys the job
	 */
	protected void destroyJob() {
		destroyJob(JOB_NAME);
	}

	/**
	 * Destroys the specified job
	 *
	 * @param jobName The name of the job to destroy
	 */
	protected void destroyJob(final String jobName) {
		Assert.hasText(jobName, "The jobName must not be empty nor null");
		springXDTemplate.jobOperations().destroy(jobName);
	}

	/**
	 * Undeploys the specified job
	 *
	 * @param jobName The name of the job to undeploy
	 */
	protected void undeployJob(final String jobName) {
		Assert.hasText(jobName, "The jobName must not be empty nor null");
		springXDTemplate.jobOperations().undeploy(jobName);
	}

	/**
	 * Retrieve a list of JobExecutionInfoResources that have the name contained in the jobName parameter
	 *
	 * @param jobName The search name
	 * @return a list of JobExecutionInfoResources
	 */
	protected List<JobExecutionInfoResource> getJobExecInfoByName(String jobName) {
		PagedResources<JobExecutionInfoResource> jobExecutions = springXDTemplate.jobOperations().listJobExecutions();
		Iterator<JobExecutionInfoResource> iter = jobExecutions.iterator();
		List<JobExecutionInfoResource> result = new ArrayList<JobExecutionInfoResource>();
		while (iter.hasNext()) {
			JobExecutionInfoResource resource = iter.next();
			if (resource.getName().equals(jobName)) {
				result.add(resource);
			}
		}
		return result;
	}

	/**
	 * Retrieves a list of step executions for the execution Id.
	 *
	 * @param executionId The executionId that will be searched.
	 * @return a list if StepExecutionInfoResources
	 */
	protected List<StepExecutionInfoResource> getStepExecutions(long executionId) {
		return springXDTemplate.jobOperations().listStepExecutions(executionId);
	}

	/**
	 * Waits up to the wait time for a job to be deployed or undeployed.
	 *
	 * @param jobName    The name of the job to be evaluated.
	 * @param waitTime   the amount of time in millis to wait.
	 * @param isDeployed if true the method will wait for the job to be deployed.  If false it will wait for the job to become undeployed.
	 * @return true if the job is deployed else false.
	 */
	protected boolean waitForJobDeploymentChange(String jobName, int waitTime, boolean isDeployed) {
		boolean result = isJobDeployed(jobName);
		long timeout = System.currentTimeMillis() + waitTime;
		while (!result && System.currentTimeMillis() < timeout) {
			sleepOneSecond();
			result = isDeployed ? isJobDeployed(jobName) : isJobUndeployed(jobName);
		}

		return result;
	}

	/**
	 * Checks to see if the specified job is deployed on the XD cluster.
	 *
	 * @param jobName The name of the job to be evaluated.
	 * @return true if the job is deployed else false
	 */
	protected boolean isJobDeployed(String jobName) {
		Assert.hasText(jobName, "The job name must be specified.");
		JobDefinitionResource resource = getJobDefinitionResource(jobName);
		boolean result = false;
		if ("deployed".equals(resource.getStatus())) {
			result = true;
		}
		return result;
	}

	/**
	 * Checks to see if the specified job is undeployed on the XD cluster.
	 *
	 * @param jobName The name of the job to be evaluated.
	 * @return true if the job is deployed else false
	 */
	protected boolean isJobUndeployed(String jobName) {
		Assert.hasText(jobName, "The job name must be specified.");
		JobDefinitionResource resource = getJobDefinitionResource(jobName);
		boolean result = false;
		if ("undeployed".equals(resource.getStatus())) {
			result = true;
		}
		return result;
	}

	/**
	 * Wait up to the specified time for the job list to become empty.
	 *
	 * @param waitTime The time in Millis to wait.
	 */
	protected void waitForEmptyJobList(int waitTime) {
		PagedResources<JobDefinitionResource> resources = springXDTemplate.jobOperations().list();
		int jobSize = resources.getContent().size();
		long timeout = System.currentTimeMillis() + waitTime;
		while (jobSize > 0 && System.currentTimeMillis() < timeout) {
			sleepOneSecond();
			jobSize = resources.getContent().size();
		}
	}


	/**
	 * Create an new instance of the SpringXDTemplate given the Admin Server URL
	 *
	 * @return A new instance of SpringXDTemplate
	 */
	private SpringXDTemplate createSpringXDTemplate() {
		try {
			return new SpringXDTemplate(adminServer.toURI());
		} catch (URISyntaxException uriException) {
			throw new IllegalStateException(uriException.getMessage(), uriException);
		}
	}

	private JobDefinitionResource getJobDefinitionResource(String jobName) {
		PagedResources<JobDefinitionResource> resources = springXDTemplate.jobOperations().list();
		long timeout = System.currentTimeMillis() + WAIT_TIME;
		while (!resources.iterator().hasNext() && System.currentTimeMillis() < timeout) {
			sleepOneSecond();
			resources = springXDTemplate.jobOperations().list();
		}

		JobDefinitionResource result = null;
		Iterator<JobDefinitionResource> resourceIter = resources.iterator();
		while (resourceIter.hasNext()) {
			JobDefinitionResource resource = resourceIter.next();
			if (jobName.equals(resource.getName())) {
				result = resource;
				break;
			}
		}
		return result;
	}

	private void sleepOneSecond() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	/**
	 * Retrieves the first job execution from a list of job executions for the job name.
	 *
	 * @param jobName a unique job name.
	 */
	protected BatchStatus getJobExecutionStatus(String jobName) {
		List<JobExecutionInfoResource> results = getJobExecInfoByName(jobName);
		Assert.isTrue(results.size() > 0, "No Job execution available for the job.");
		return results.get(0).getJobExecution().getStatus();
	}

	/**
	 * Requests the steps execution details for a specific step from the admin server.
	 *
	 * @param jobExecutionId  The job execution id of the step to be interrogated.
	 * @param stepExecutionId The step execution id of the step that will be interrogated.
	 * @return The JSon Returned from the step execution request.
	 */
	protected String getStepResultJson(long jobExecutionId, long stepExecutionId) {
		RestTemplate restTemplate = new RestTemplate();
		return restTemplate.getForObject(
				"{server}/jobs/executions/{jobexecutionid}/steps/{stepExecutionID}",
				String.class, getEnvironment().getAdminServerUrl(), jobExecutionId,
				stepExecutionId);

	}

}
