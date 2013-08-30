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

package org.springframework.xd.shell.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import org.springframework.batch.core.JobParameter;
import org.springframework.shell.core.CommandResult;

/**
 * Test stream commands
 * 
 * @author Glenn Renfro
 * @author Gunnar Hillert
 */
public class JobCommandTests extends AbstractJobIntegrationTest {

	private static final Log logger = LogFactory.getLog(JobCommandTests.class);

	@Test
	public void testJobLifecycleForMyJob() throws InterruptedException {

		logger.info("Starting Job Create for myTest");
		executeJobCreate(MY_TEST, TEST_DESCRIPTOR);

		checkForJobInList(MY_TEST, TEST_DESCRIPTOR);
		executemyTestTriggerStream();

		CommandResult cr = getShell().executeCommand("job undeploy --name myTest");
		checkForSuccess(cr);
		assertEquals("Un-deployed Job 'myTest'", cr.getResult());
		waitForResult();
		assertTrue("Batch Script did not complete successfully", fileExists(TEST_FILE));
	}

	@Test
	public void testJobCreateDuplicate() throws InterruptedException {
		logger.info("Create job myJob");
		executeJobCreate(MY_JOB, JOB_DESCRIPTOR);
		executemyJobTriggerStream();

		checkForJobInList(MY_JOB, JOB_DESCRIPTOR);
		waitForResult();
		assertTrue(fileExists(TMP_FILE));

		CommandResult cr = getShell().executeCommand("job create --definition \"job\" --name myJob");
		checkForFail(cr);
		checkErrorMessages(cr, "There is already a job named 'myJob'");
	}

	@Test
	public void testStreamDestroyMissing() {
		logger.info("Destroy a job that doesn't exist");
		CommandResult cr = getShell().executeCommand("job destroy --name myJob");
		checkForFail(cr);
		checkErrorMessages(cr, "There is no job definition named 'myJob'");
	}

	@Test
	public void testJobCreateDuplicateWithDeployFalse() {
		logger.info("Create 2 myJobs with --deploy = false");
		executeJobCreate(MY_JOB, JOB_DESCRIPTOR, false);

		checkForJobInList(MY_JOB, JOB_DESCRIPTOR);

		CommandResult cr = getShell().executeCommand("job create --definition \"job\" --name myJob --deploy false");
		checkForFail(cr);
		checkErrorMessages(cr, "There is already a job named 'myJob'");

		checkForJobInList(MY_JOB, JOB_DESCRIPTOR);
	}

	@Test
	public void testJobDeployUndeployFlow() {
		logger.info("Create batch job");
		executeJobCreate(MY_JOB, JOB_DESCRIPTOR, false);

		checkForJobInList(MY_JOB, JOB_DESCRIPTOR);
		CommandResult cr = getShell().executeCommand("job deploy --name myJob");
		checkForSuccess(cr);
		assertEquals("Deployed job 'myJob'", cr.getResult());
		executemyJobTriggerStream();
		waitForResult();
		assertTrue(fileExists(TMP_FILE));

		checkForJobInList(MY_JOB, JOB_DESCRIPTOR);

		cr = getShell().executeCommand("job undeploy --name myJob");
		checkForSuccess(cr);
		assertEquals("Un-deployed Job 'myJob'", cr.getResult());

		cr = getShell().executeCommand("job deploy --name myJob");
		checkForSuccess(cr);
		assertEquals("Deployed job 'myJob'", cr.getResult());

		checkForJobInList(MY_JOB, JOB_DESCRIPTOR);
	}

	@Test
	public void testInvalidJobDescriptor() {
		CommandResult cr = getShell().executeCommand("job create --definition \"barsdaf\" --name myJob ");
		checkForFail(cr);
		checkErrorMessages(cr, "Module definition is missing");
		assertFalse(fileExists(TMP_FILE));
	}

	@Test
	public void testMissingJobDescriptor() {
		CommandResult cr = getShell().executeCommand("job create --name myJob ");
		checkForFail(cr);
	}


	public static class JobParametersHolder {

		private static Map<String, JobParameter> jobParameters = new ConcurrentHashMap<String, JobParameter>();

		private static CountDownLatch countDownLatch = new CountDownLatch(1);

		public boolean isDone() throws InterruptedException {
			return countDownLatch.await(10, TimeUnit.SECONDS);
		}

		public void countDown() throws InterruptedException {
			countDownLatch.countDown();
		}

		public void addParameter(String parameterName, JobParameter jobParameter) {
			jobParameters.put(parameterName, jobParameter);
		}

		protected static Map<String, JobParameter> getJobParameters() {
			return jobParameters;
		}

		public static void reset() {
			jobParameters.clear();
			countDownLatch = new CountDownLatch(1);
		}
	}
}
