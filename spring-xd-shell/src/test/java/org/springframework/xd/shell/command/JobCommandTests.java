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

package org.springframework.xd.shell.command;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
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
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableRow;

/**
 * Test {@link JobCommands}.
 * 
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @since 1.0
 */
public class JobCommandTests extends AbstractJobIntegrationTest {

	private static final Log logger = LogFactory.getLog(JobCommandTests.class);

	@Test
	public void testJobLifecycleForMyJob() throws InterruptedException {

		logger.info("Starting Job Create for myTest");
		JobParametersHolder.reset();
		final JobParametersHolder jobParametersHolder = new JobParametersHolder();

		executeJobCreate(MY_TEST, JOB_WITH_PARAMETERS_DESCRIPTOR);

		checkForJobInList(MY_TEST, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
		executemyTestTriggerStream();
		assertTrue("Job did not complete within time alotted", jobParametersHolder.isDone());
		CommandResult cr = getShell().executeCommand("job undeploy --name myTest");
		checkForSuccess(cr);
		assertEquals("Un-deployed Job 'myTest'", cr.getResult());

	}

	@Test
	public void testJobCreateDuplicate() throws InterruptedException {
		logger.info("Create job myJob");
		JobParametersHolder.reset();
		final JobParametersHolder jobParametersHolder = new JobParametersHolder();

		executeJobCreate(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR);
		executemyJobTriggerStream();

		checkForJobInList(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
		assertTrue("Job did not complete within time alotted", jobParametersHolder.isDone());

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
		executeJobCreate(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR, false);

		checkForJobInList(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR, false);

		CommandResult cr = getShell().executeCommand("job create --definition \"job\" --name myJob --deploy false");
		checkForFail(cr);
		checkErrorMessages(cr, "There is already a job named 'myJob'");

		checkForJobInList(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR, false);
	}

	@Test
	public void testJobDeployUndeployFlow() throws InterruptedException {
		logger.info("Create batch job");
		JobParametersHolder.reset();
		final JobParametersHolder jobParametersHolder = new JobParametersHolder();

		executeJobCreate(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR, false);

		checkForJobInList(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR, false);
		CommandResult cr = getShell().executeCommand("job deploy --name myJob");
		checkForSuccess(cr);
		assertEquals("Deployed job 'myJob'", cr.getResult());
		executemyJobTriggerStream();
		assertTrue("Job did not complete within time alotted", jobParametersHolder.isDone());

		checkForJobInList(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR, true);

		cr = getShell().executeCommand("job undeploy --name myJob");
		checkForSuccess(cr);
		assertEquals("Un-deployed Job 'myJob'", cr.getResult());

		cr = getShell().executeCommand("job deploy --name myJob");
		checkForSuccess(cr);
		assertEquals("Deployed job 'myJob'", cr.getResult());

		checkForJobInList(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
	}

	@Test
	public void testInvalidJobDescriptor() throws InterruptedException {
		JobParametersHolder.reset();
		final JobParametersHolder jobParametersHolder = new JobParametersHolder();

		CommandResult cr = getShell().executeCommand("job create --definition \"barsdaf\" --name myJob ");
		checkForFail(cr);
		checkErrorMessages(cr, "Module definition is missing");
		assertFalse("Job did not complete within time alotted", jobParametersHolder.isDone());
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
		checkForJobInList(MY_JOB_WITH_PARAMETERS, JOB_WITH_PARAMETERS_DESCRIPTOR, false);

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
		checkForJobInList(MY_JOB_WITH_PARAMETERS, JOB_WITH_PARAMETERS_DESCRIPTOR, false);

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

	@Test
	public void testLaunchJob() {
		logger.info("Launch batch job");
		executeJobCreate(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR);
		checkForJobInList(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
		executeJobLaunch(MY_JOB);
	}

	@Test
	public void testLaunchNotDeployedJob() {
		logger.info("Launch batch job that is not deployed");
		executeJobCreate(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR, false);
		CommandResult result = executeCommandExpectingFailure("job launch --name " + MY_JOB);
		assertThat(result.getException().getMessage(),
				containsString(String.format("The job named '%s' is not currently deployed", MY_JOB)));
	}

	@Test
	public void testLaunchJobWithParameters() throws InterruptedException, ParseException {
		logger.info("Launch batch job with typed parameters");
		String myJobParams = "{\"-param1(long)\":\"12345\",\"param2(date)\":\"1990/10/03\"}";
		JobParametersHolder.reset();
		final JobParametersHolder jobParametersHolder = new JobParametersHolder();
		executeJobCreate(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR);
		checkForJobInList(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
		executeJobLaunch(MY_JOB, myJobParams);
		boolean done = jobParametersHolder.isDone();

		assertTrue("The countdown latch expired and did not count down.", done);
		// Make sure the job parameters are set when passing through job launch command
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

	@Test
	public void testLaunchJobTwiceWhereMakeUniqueIsImplicitlyTrue() {
		logger.info("Launch batch job twice (makeUnique is implicitly true)");

		executeJobCreate(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR);
		checkForJobInList(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
		executeJobLaunch(MY_JOB);
		executeJobLaunch(MY_JOB);
	}

	@Test
	public void testLaunchJobTwiceWhereMakeUniqueIsTrue() {
		logger.info("Launch batch job (makeUnique=true) twice");

		executeJobCreate(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR + " --makeUnique=true");
		checkForJobInList(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR + " --makeUnique=true", true);
		executeJobLaunch(MY_JOB);
		executeJobLaunch(MY_JOB);
	}

	@Test
	public void testLaunchJobTwiceWhereMakeUniqueIsFalse() {
		logger.info("Launch batch job (makeUnique=false) twice");

		executeJobCreate(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR + " --makeUnique=false");
		checkForJobInList(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR + " --makeUnique=false", true);
		executeJobLaunch(MY_JOB);

		CommandResult result = executeCommandExpectingFailure("job launch --name " + MY_JOB);
		assertThat(
				result.getException().getMessage(),
				containsString("A job instance already exists and is complete for parameters={}.  If you want to run this job again, change the parameters."));
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

	@Test
	public void testListJobExecutions() {
		executeJobCreate(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR);
		checkForJobInList(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
		executeJobLaunch(MY_JOB);
		listJobExecutions();
	}

	@Test
	public void testDisplaySpecificJobExecution() {
		executeJobCreate(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR);
		checkForJobInList(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
		executeJobLaunch(MY_JOB);
		final Table jobExecutions = listJobExecutions();
		String id = jobExecutions.getRows().get(0).getValue(1);
		displayJobExecution(id);
	}

	@Test
	public void testDisplaySpecificJobExecutionWithDateParam() {
		executeJobCreate(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR);
		checkForJobInList(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
		executeJobLaunch(MY_JOB, "{\"param1\":\"fixedDelayKenny\",\"param2(date)\":\"2013/12/28\"}");
		final Table jobExecutions = listJobExecutions();
		String id = jobExecutions.getRows().get(0).getValue(1);
		displayJobExecution(id);
	}

	@Test
	public void testListStepExecutionsForSpecificJobExecution() {
		executeJobCreate(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR);
		checkForJobInList(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
		executeJobLaunch(MY_JOB);
		final Table jobExecutions = listJobExecutions();
		String jobExecutionId = jobExecutions.getRows().get(0).getValue(1);

		final Table stepExecutions = listStepExecutions(jobExecutionId);
		String stepExecutionId = stepExecutions.getRows().get(0).getValue(1);

		assertNotNull(stepExecutionId);
	}

	@Test
	public void testStopJobExecution() throws Exception {
		executeJobCreate(MY_JOB, JOB_WITH_STEP_EXECUTIONS);
		checkForJobInList(MY_JOB, JOB_WITH_STEP_EXECUTIONS, true);
		executemyJobFixedDelayStream("5");
		Thread.sleep(5000);
		Table table = (Table) executeCommand("job execution list").getResult();
		assertTrue(!table.getRows().isEmpty());
		String executionId = table.getRows().get(0).getValue(1);
		String executionStatus = table.getRows().get(0).getValue(5);
		assertTrue(executionStatus.equals("STARTING") || executionStatus.equals("STARTED"));
		// Stop the execution by the given executionId.
		executeCommand("job execution stop " + executionId);
		// sleep for stop() until the step2 is invoked.
		Thread.sleep(3000);
		table = (Table) executeCommand("job execution list").getResult();
		for (TableRow tr : table.getRows()) {
			// Match by above executionId
			if (tr.getValue(1).equals(executionId)) {
				executionStatus = tr.getValue(5);
				break;
			}
		}
		assertEquals("STOPPED", executionStatus);
	}

	@Test
	public void testStopAllJobExecutions() throws Exception {
		executeJobCreate(MY_JOB, JOB_WITH_STEP_EXECUTIONS);
		checkForJobInList(MY_JOB, JOB_WITH_STEP_EXECUTIONS, true);
		executemyJobFixedDelayStream("5");
		Thread.sleep(5000);
		Table table = (Table) executeCommand("job execution list").getResult();
		assertTrue(!table.getRows().isEmpty());
		String executionId = table.getRows().get(0).getValue(1);
		String executionStatus = table.getRows().get(0).getValue(5);
		assertTrue(executionStatus.equals("STARTING") || executionStatus.equals("STARTED"));
		// Stop the execution by the given executionId.
		executeCommand("job execution all stop --force true");
		// sleep for stop() until the step2 is invoked.
		Thread.sleep(3000);
		table = (Table) executeCommand("job execution list").getResult();
		for (TableRow tr : table.getRows()) {
			// Match by above executionId
			if (tr.getValue(1).equals(executionId)) {
				executionStatus = tr.getValue(5);
				break;
			}
		}
		assertEquals("STOPPED", executionStatus);
	}

	public void testStepExecutionProgress() {
		executeJobCreate(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR);
		checkForJobInList(MY_JOB, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
		executeJobLaunch(MY_JOB);
		final Table jobExecutions = listJobExecutions();
		String jobExecutionId = jobExecutions.getRows().get(0).getValue(1);

		final Table stepExecutions = listStepExecutions(jobExecutionId);
		String stepExecutionId = stepExecutions.getRows().get(0).getValue(1);

		final Table stepExecutionProgress = getStepExecutionProgress(jobExecutionId, stepExecutionId);
		String id = stepExecutionProgress.getRows().get(0).getValue(1);
		String percentageComplete = stepExecutionProgress.getRows().get(0).getValue(3);
		String duration = stepExecutionProgress.getRows().get(0).getValue(4);
		assertEquals(stepExecutionId, id);
		assertNotNull(percentageComplete);
		assertNotNull(duration);
	}
}
