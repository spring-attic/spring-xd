/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.xd.integration.util;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;

import org.springframework.hateoas.PagedResources;
import org.springframework.util.Assert;
import org.springframework.xd.rest.client.domain.JobDefinitionResource;
import org.springframework.xd.rest.client.impl.SpringXDTemplate;


/**
 * Utilities for managing jobs.
 *
 * @author Glenn Renfro
 */
public class JobUtils {


	/**
	 * Creates the job definition and deploys it to the cluster being tested.
	 *
	 * @param jobName The name of the job
	 * @param jobDefinition The definition that needs to be deployed for this job.
	 * @param adminServer The admin server that this job will be deployed against.
	 */
	public static void job(final String jobName, final String jobDefinition,
			final URL adminServer) {
		Assert.hasText(jobName, "The job name must be specified.");
		Assert.hasText(jobDefinition, "a job definition must be supplied.");
		Assert.notNull(adminServer, "The admin server must be specified.");
		createSpringXDTemplate(adminServer).jobOperations().createJob(jobName, jobDefinition, true);
	}


	/**
	 * Removes all the jobs from the cluster. Used to guarantee a clean acceptance test.
	 *
	 * @param adminServer The admin server that the command will be executed against.
	 */
	public static void destroyAllJobs(final URL adminServer) {
		Assert.notNull(adminServer, "The admin server must be specified.");
		createSpringXDTemplate(adminServer).jobOperations().destroyAll();

	}

	/**
	 * Undeploys the specified job
	 *
	 * @param adminServer The admin server that the command will be executed against.
	 * @param jobName The name of the job to undeploy
	 */
	public static void undeployJob(final URL adminServer, final String jobName)
	{
		Assert.notNull(adminServer, "The admin server must be specified.");
		Assert.hasText(jobName, "The jobName must not be empty nor null");
		createSpringXDTemplate(adminServer).jobOperations().undeploy(jobName);
	}

	/**
	 * Launches the specified job
	 *
	 * @param adminServer The admin server that the command will be executed against.
	 * @param jobName The name of the job to launch
	 */
	public static void launch(final URL adminServer, final String jobName) {
		Assert.notNull(adminServer, "The admin server must be specified.");
		Assert.hasText(jobName, "The jobName must not be empty nor null");
		createSpringXDTemplate(adminServer).jobOperations().launchJob(jobName, "");// should this be empty or null?
	}

	/**
	 * Waits up to the wait time for a job to be deployed.
	 *
	 * @param jobName The name of the job to be evaluated.
	 * @param adminServer The admin server URL that will be queried.
	 * @param waitTime the amount of time in millis to wait.
	 * @return true if the job is deployed else false.
	 */
	public static boolean waitForJobDeployment(String jobName, URL adminServer, int waitTime) {
		boolean result = isJobDeployed(jobName, adminServer);
		long timeout = System.currentTimeMillis() + waitTime;
		while (!result && System.currentTimeMillis() < timeout) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
			result = isJobDeployed(jobName, adminServer);
		}

		return result;
	}

	/**
	 * Checks to see if the specified job is deployed on the XD cluster.
	 *
	 * @param jobName The name of the job to be evaluated.
	 * @param adminServer The admin server URL that will be queried.
	 * @return true if the job is deployed else false
	 */
	public static boolean isJobDeployed(String jobName, URL adminServer) {
		Assert.hasText(jobName, "The job name must be specified.");
		Assert.notNull(adminServer, "The admin server must be specified.");

		boolean result = false;
		SpringXDTemplate xdTemplate = createSpringXDTemplate(adminServer);
		PagedResources<JobDefinitionResource> resources = xdTemplate.jobOperations().list();
		Iterator<JobDefinitionResource> resourceIter = resources.iterator();
		while (resourceIter.hasNext()) {
			JobDefinitionResource resource = resourceIter.next();
			if (jobName.equals(resource.getName())) {
				if (resource.isDeployed()) {
					result = true;
					break;
				}
				else {
					result = false;
					break;
				}
			}
		}
		return result;
	}

	/**
	 * Create an new instance of the SpringXDTemplate given the Admin Server URL
	 * 
	 * @param adminServer URL of the Admin Server
	 * @return A new instance of SpringXDTemplate
	 */
	private static SpringXDTemplate createSpringXDTemplate(URL adminServer) {
		try {
			return new SpringXDTemplate(adminServer.toURI());
		}
		catch (URISyntaxException uriException) {
			throw new IllegalStateException(uriException.getMessage(), uriException);
		}
	}

}
