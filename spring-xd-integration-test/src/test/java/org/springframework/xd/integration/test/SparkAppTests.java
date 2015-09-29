/*
 *
 *  * Copyright 2014-2015 the original author or authors.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.springframework.xd.integration.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.core.io.FileSystemResource;
import org.springframework.xd.rest.domain.JobExecutionInfoResource;
import org.springframework.xd.rest.domain.StepExecutionInfoResource;
import org.springframework.xd.test.fixtures.SparkAppJob;

/**
 * Verifies that SparkApp job executes on a Spark App Server and on the VM.
 *
 * @author Glenn Renfro
 */
public class SparkAppTests extends AbstractJobTest {

	public static final long WAIT_TIME_FOR_SPARK_SERVER = 30000;
	SparkAppJob sparkAppJob;
	String jobName;

	/**
	 * Initialize Fixture and copy the SparkApp Jar to the XD Instance.
	 */
	@Before
	public void initialize() {
		sparkAppJob = jobs.sparkAppJob();
		jobName = "SA" + UUID.randomUUID().toString();
		FileSystemResource jarSystemResource =
				new FileSystemResource(sparkAppJob.getSparkAppJar());
		URI jarDestination = sparkAppJob.getJarURI(jarSystemResource.getFile().getParent());
		List<File> jars = new ArrayList<>();
		jars.add(new File(sparkAppJob.getSparkAppJarSource()));
		copyFileToCluster(jarDestination, jars);
	}

	@Test
	public void localSparkAppTest() {
		job(jobName, sparkAppJob.sparkMaster("local[1]").toDSL(), true);
		jobLaunch(jobName);

		waitForJobToComplete(jobName);
		verifySparkAppResults(jobName);
	}

	@Test
	@Ignore("Ignoring the test until the spark standalone cluster is setup with correct version")
	public void sparkAppTest() {
		job(jobName, sparkAppJob.toDSL(), true);
		jobLaunch(jobName);

		waitForJobToComplete(jobName, WAIT_TIME_FOR_SPARK_SERVER, DEFAULT_JOB_COMPLETE_COUNT);
		verifySparkAppResults(jobName);
	}

	private void verifySparkAppResults(String jobName) {
		List<JobExecutionInfoResource> jobList = this.getJobExecInfoByName(jobName);
		long executionId = jobList.get(0).getExecutionId();
		List<StepExecutionInfoResource> stepExecutions = getStepExecutions(executionId);
		StepExecutionInfoResource stepInfo = stepExecutions.get(0);
		String result = getStepResultJson(executionId,
				stepInfo.getStepExecution().getId());
		assertTrue("The String Pi is roughly 3. is not contained in " + result,
				result.indexOf("Pi is roughly 3.") > -1);

	}
}
