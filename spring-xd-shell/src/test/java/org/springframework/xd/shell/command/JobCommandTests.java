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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

	@Test
	public void testJobDeployWithParameters() throws InterruptedException {
		logger.info("Create batch job with parameters");

		JobParametersHolder.reset();
		executeJobCreate(MY_JOB_WITH_PARAMETERS, JOB_WITH_PARAMETERS_DESCRIPTOR, false);
		checkForJobInList(MY_JOB_WITH_PARAMETERS, JOB_WITH_PARAMETERS_DESCRIPTOR);

		final JobParametersHolder jobParametersHolder = new JobParametersHolder();

		final String commandString =
				"job deploy --name myJobWithParameters";

		CommandResult cr = getShell().executeCommand(commandString);
		checkForSuccess(cr);
		assertEquals("Deployed job 'myJobWithParameters'", cr.getResult());
		executemyjobWithParametersTriggerStream("{\"param1\":\"spring rocks!\"}");
		boolean done = jobParametersHolder.isDone();

		assertTrue("The countdown latch expired and did not count down.", done);

		int numberOfJobParameters = JobParametersHolder.getJobParameters().size();
		assertTrue("Expecting 2 parameters but got " + numberOfJobParameters, numberOfJobParameters == 2);

		assertNotNull(JobParametersHolder.getJobParameters().get("random"));

		final JobParameter parameter1 = JobParametersHolder.getJobParameters().get("param1");

		assertNotNull(parameter1);
		assertEquals("spring rocks!", parameter1.getValue());
	}

	@Test
	public void testJobDeployWithTypedParameters() throws InterruptedException, ParseException {
		logger.info("Create batch job with typed parameters");
		JobParametersHolder.reset();
		executeJobCreate(MY_JOB_WITH_PARAMETERS, JOB_WITH_PARAMETERS_DESCRIPTOR, false);
		checkForJobInList(MY_JOB_WITH_PARAMETERS, JOB_WITH_PARAMETERS_DESCRIPTOR);

		final JobParametersHolder jobParametersHolder = new JobParametersHolder();

		final String commandString = "job deploy --name myJobWithParameters ";

		logger.info(commandString);

		final CommandResult cr = getShell().executeCommand(commandString);
		checkForSuccess(cr);
		assertEquals("Deployed job 'myJobWithParameters'", cr.getResult());
		executemyjobWithParametersTriggerStream("{\"-param1(long)\":\"12345\",\"param2(date)\":\"1990/10/03\"}");

		boolean done = jobParametersHolder.isDone();

		assertTrue("The countdown latch expired and did not count down.", done);
		assertTrue("Expecting 3 parameters.", JobParametersHolder.getJobParameters().size() == 3);
		assertNotNull(JobParametersHolder.getJobParameters().get("random"));

		final JobParameter parameter1 = JobParametersHolder.getJobParameters().get("param1");
		final JobParameter parameter2 = JobParametersHolder.getJobParameters().get("param2");

		assertNotNull(parameter1);
		assertNotNull(parameter2);
		assertTrue("parameter1 should be a Long", parameter1.getValue() instanceof Long);
		assertTrue("parameter2 should be a java.util.Date", parameter2.getValue() instanceof Date);

		final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
		final Date expectedDate = dateFormat.parse("1990/10/03");

		assertEquals("Was expecting the Long value 12345", Long.valueOf(12345), parameter1.getValue());
		assertEquals("Should be the same dates", expectedDate, parameter2.getValue());

		assertFalse("parameter1 should be non-identifying", parameter1.isIdentifying());
		assertTrue("parameter2 should be identifying", parameter2.isIdentifying());
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
