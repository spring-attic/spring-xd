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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.shell.core.CommandResult;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableRow;

/**
 * Test {@link JobCommands}.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Michael Minella
 * @since 1.0
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AbstractJobCommandsTests extends AbstractJobIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(AbstractJobCommandsTests.class);

	@Test
	public void testJobLifecycleForMyJob() throws InterruptedException {
		JobParametersHolder.reset();
		final JobParametersHolder jobParametersHolder = new JobParametersHolder();

		String jobName = executeJobCreate(JOB_WITH_PARAMETERS_DESCRIPTOR);
		logger.info("Created Job " + jobName);
		checkForJobInList(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
		triggerJob(jobName);
		assertTrue("Job did not complete within time alotted", jobParametersHolder.isDone());
		CommandResult cr = getShell().executeCommand("job undeploy --name " + jobName);
		checkForSuccess(cr);
		checkUndeployedJobMessage(cr, jobName);
	}

	@Test
	public void testJobCreateDuplicate() throws InterruptedException {
		JobParametersHolder.reset();
		final JobParametersHolder jobParametersHolder = new JobParametersHolder();

		String jobName = executeJobCreate(JOB_WITH_PARAMETERS_DESCRIPTOR);
		logger.info("Create job " + jobName);
		triggerJob(jobName);

		checkForJobInList(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
		assertTrue("Job did not complete within time alotted", jobParametersHolder.isDone());

		CommandResult cr = createJob(jobName, "job");
		checkForFail(cr);
		checkBatchJobExistsErrorMessage(cr, jobName);
	}

	@Test
	public void testStreamDestroyMissing() {
		logger.info("Destroy a job that doesn't exist");
		String jobName = generateJobName();
		CommandResult cr = jobDestroy(jobName);
		checkForFail(cr);
		checkErrorMessages(cr, "There is no job definition named '" + jobName + "'");
	}

	@Test
	public void testJobCreateDuplicateWithDeployFalse() {
		logger.info("Create 2 myJobs with --deploy = false");
		String jobName = generateJobName();
		executeJobCreate(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, false);

		checkForJobInList(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, false);

		CommandResult cr = createJob(jobName, "job", "false");
		checkForFail(cr);
		checkDuplicateJobErrorMessage(cr, jobName);

		checkForJobInList(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, false);
	}

	@Test
	public void testJobDeployUndeployFlow() throws InterruptedException {
		logger.info("Create batch job");
		JobParametersHolder.reset();
		final JobParametersHolder jobParametersHolder = new JobParametersHolder();
		String jobName = generateJobName();
		executeJobCreate(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, false);

		checkForJobInList(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, false);
		CommandResult cr = deployJob(jobName);
		checkForSuccess(cr);
		checkDeployedJobMessage(cr, jobName);
		triggerJob(jobName);
		assertTrue("Job did not complete within time alotted", jobParametersHolder.isDone());

		checkForJobInList(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, true);

		cr = undeployJob(jobName);
		checkForSuccess(cr);
		checkUndeployedJobMessage(cr, jobName);

		cr = deployJob(jobName);
		checkForSuccess(cr);
		assertEquals("Deployed job '" + jobName + "'", cr.getResult());

		checkForJobInList(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
	}

	@Test
	public void testInvalidJobDescriptor() throws InterruptedException {
		JobParametersHolder.reset();
		final JobParametersHolder jobParametersHolder = new JobParametersHolder();
		CommandResult cr = getShell().executeCommand("job create --definition \"barsdaf\" --name " + generateJobName());
		checkForFail(cr);
		checkErrorMessages(cr, "Could not find module with name 'barsdaf'");
		assertFalse("Job did not complete within time alotted", jobParametersHolder.isDone());
	}

	@Test
	public void testMissingJobDescriptor() {
		CommandResult cr = getShell().executeCommand("job create --name " + generateJobName());
		checkForFail(cr);
	}

	@Test
	public void testJobDeployWithParameters() throws InterruptedException {
		logger.info("Create batch job with parameters");

		JobParametersHolder.reset();
		String jobName = generateJobName();
		executeJobCreate(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, false);
		checkForJobInList(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, false);

		final JobParametersHolder jobParametersHolder = new JobParametersHolder();

		CommandResult cr = deployJob(jobName);
		checkForSuccess(cr);
		checkDeployedJobMessage(cr, jobName);
		triggerJobWithParams(jobName, "{\"param1\":\"spring rocks!\"}");
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
		String jobName = generateJobName();
		executeJobCreate(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, false);
		checkForJobInList(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, false);

		final JobParametersHolder jobParametersHolder = new JobParametersHolder();

		final CommandResult cr = deployJob(jobName);
		checkForSuccess(cr);
		checkDeployedJobMessage(cr, jobName);
		triggerJobWithParams(jobName, "{\"-param1(long)\":\"12345\",\"param2(date)\":\"1990-10-03\"}");

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
		String jobName = generateJobName();
		executeJobCreate(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR);
		checkForJobInList(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
		executeJobLaunch(jobName);
	}

	@Test
	public void testNestedJobLaunch() {
		logger.info("Launch nested job");
		String jobName = generateJobName();
		executeJobCreate(jobName, NESTED_JOB_DESCRIPTOR);
		checkForJobInList(jobName, NESTED_JOB_DESCRIPTOR, true);
		executeJobLaunch(jobName);
	}

	@Test
	public void testInvalidNestedJobLaunch() {
		logger.info("Launch nested job");
		String jobName = generateJobName();
		CommandResult result = executeCommandExpectingFailure("job create --definition \"" +
				INVALID_NESTED_JOB_DESCRIPTOR + "\" --name " + jobName);
		assertTrue(result.getException().getMessage().contains(
				"Could not find module with name 'invalidJob' and type 'job'"));
	}

	@Test
	public void testLaunchPartitionedJob() {
		logger.info("Launch Partitioned batch job");
		String jobName = generateJobName();
		executeJobCreate(jobName, JOB_WITH_PARTITIONS_DESCRIPTOR);
		checkForJobInList(jobName, JOB_WITH_PARTITIONS_DESCRIPTOR, true);
		executeJobLaunch(jobName);
	}

	@Test
	public void testLaunchNotDeployedJob() {
		logger.info("Launch batch job that is not deployed");
		String jobName = generateJobName();
		executeJobCreate(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, false);
		CommandResult result = executeCommandExpectingFailure("job launch --name " + jobName);
		assertThat(result.getException().getMessage(),
				containsString(String.format("The job named '%s' is not currently deployed", jobName)));
	}

	@Test
	public void testLaunchJobWithParameters() throws InterruptedException, ParseException {
		logger.info("Launch batch job with typed parameters");
		String myJobParams = "{\"-param1(long)\":\"12345\",\"param2(date)\":\"1990-10-03\"}";
		JobParametersHolder.reset();
		final JobParametersHolder jobParametersHolder = new JobParametersHolder();
		String jobName = generateJobName();
		executeJobCreate(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR);
		checkForJobInList(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
		executeJobLaunch(jobName, myJobParams);
		boolean done = jobParametersHolder.isDone();

		assertTrue("The countdown latch expired and did not count down.", done);
		// Make sure the job parameters are set when passing through job launch command
		assertTrue("Expecting 3 parameters, but got: " + JobParametersHolder.getJobParameters(),
				JobParametersHolder.getJobParameters().size() == 3);
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
	public void testLaunchJobTwiceWhereMakeUniqueIsImplicitlyTrue() throws Exception {
		logger.info("Launch batch job twice (makeUnique is implicitly true)");
		String jobName = generateJobName();
		// Batch 3.0 requires at least one parameter to reject duplicate executions of an instance
		String myJobParams = "{\"-param(long)\":\"12345\"}";
		JobParametersHolder.reset();
		final JobParametersHolder jobParametersHolder = new JobParametersHolder();

		executeJobCreate(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR);
		checkForJobInList(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
		executeJobLaunch(jobName, myJobParams);
		assertTrue("The countdown latch expired and did not count down.", jobParametersHolder.isDone());
		executeJobLaunch(jobName, myJobParams);
	}

	@Test
	public void testLaunchJobTwiceWhereMakeUniqueIsTrue() throws Exception {
		logger.info("Launch batch job (makeUnique=true) twice");
		String jobName = generateJobName();
		// Batch 3.0 requires at least one parameter to reject duplicate executions of an instance
		String myJobParams = "{\"-param(long)\":\"12345\"}";
		JobParametersHolder.reset();
		final JobParametersHolder jobParametersHolder = new JobParametersHolder();

		executeJobCreate(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR + " --makeUnique=true");
		checkForJobInList(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR + " --makeUnique=true", true);
		executeJobLaunch(jobName, myJobParams);
		assertTrue("The countdown latch expired and did not count down.", jobParametersHolder.isDone());
		executeJobLaunch(jobName, myJobParams);
	}

	@Test
	public void testLaunchJobTwiceWhereMakeUniqueIsFalse() throws Exception {
		logger.info("Launch batch job (makeUnique=false) twice");
		String jobName = generateJobName();
		// Batch 3.0 requires at least one parameter to reject duplicate executions of an instance
		String myJobParams = "{\"-param(long)\":\"12345\"}";
		JobParametersHolder.reset();
		final JobParametersHolder jobParametersHolder = new JobParametersHolder();

		executeJobCreate(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR + " --makeUnique=false");
		checkForJobInList(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR + " --makeUnique=false", true);
		executeJobLaunch(jobName, myJobParams);
		assertTrue("The countdown latch expired and did not count down.", jobParametersHolder.isDone());

		final SynchronousQueue<Message<?>> rendezvous = new SynchronousQueue<Message<?>>();
		MessageHandler handler = new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				rendezvous.add(message);
			}
		};
		getErrorChannel().subscribe(handler);
		executeCommand("job launch --name " + jobName + " --params " + myJobParams);
		Message<?> error = rendezvous.poll(5, TimeUnit.SECONDS);
		getErrorChannel().unsubscribe(handler);
		assertNotNull("expected an error message", error);
		Object payload = error.getPayload();
		assertTrue("payload should be a MessagingException", payload instanceof MessagingException);
		assertEquals(JobInstanceAlreadyCompleteException.class,
				((MessagingException) payload).getCause().getClass());
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
		String jobName = generateJobName();
		executeJobCreate(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR);
		checkForJobInList(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
		executeJobLaunch(jobName);
		listJobExecutions();
	}

	@Test
	public void testDisplaySpecificJobExecution() {
		String jobName = generateJobName();
		executeJobCreate(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR);
		checkForJobInList(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
		executeJobLaunch(jobName);
		displayJobExecution(getJobExecutionId(jobName));
	}

	@Test
	public void testDisplaySpecificJobExecutionWithDateParam() {
		String jobName = generateJobName();
		String jobDefinition = JOB_WITH_PARAMETERS_DESCRIPTOR + " --dateFormat='yyyy/dd/MM' --numberFormat='###;(###)'";
		executeJobCreate(jobName, jobDefinition);
		checkForJobInList(jobName, jobDefinition, true);
		executeJobLaunch(jobName,
				"{\"param1\":\"fixedDelayKenny\",\"param2(date)\":\"2013/28/12\",\"param3(long)\":\"(123)\"}");
		String id = getJobExecutionId(jobName);
		String displayed = displayJobExecution(id);
		assertTrue("Date of job execution is not as expected. [" + displayed + "]",
				displayed.matches("(?s).*param2 +Sat Dec 28 00:00:00 [A-Z]{3} 2013 +DATE.*"));
		assertTrue("Long parameter of job execution is not as expected. [" + displayed + "]",
				displayed.matches("(?s).*param3 +-123 +LONG.*"));
	}

	@Test
	public void testListStepExecutionsForSpecificJobExecution() {
		String jobName = generateJobName();
		executeJobCreate(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR);
		checkForJobInList(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
		executeJobLaunch(jobName);
		final Table stepExecutions = listStepExecutions(getJobExecutionId(jobName));
		assertNotNull(stepExecutions.getRows().get(0).getValue(1));
	}

	@Test
	public void doStopJobExecution() throws Exception {
		String jobName = generateJobName();
		executeJobCreate(jobName, JOB_WITH_STEP_EXECUTIONS);
		checkForJobInList(jobName, JOB_WITH_STEP_EXECUTIONS, true);
		triggerJobWithDelay(jobName, "5");
		Thread.sleep(5000);
		String executionId = getJobExecutionId(jobName);
		String executionStatus = getJobExecutionStatus(jobName);
		long timeout = System.currentTimeMillis() + 20000;
		while ((executionStatus.equals("STARTING") || executionStatus.equals("STARTED"))
				&& System.currentTimeMillis() < timeout) {
			executionStatus = getJobExecutionStatus(jobName);
			executionId = getJobExecutionId(jobName);
		}
		// Stop the execution by the given executionId.
		executeCommand("job execution stop " + executionId);
		// sleep for stop() until the step2 is invoked.
		Thread.sleep(3000);
		Table table;
		int n = 0;
		do {
			table = (Table) executeCommand("job execution list").getResult();
			for (TableRow tr : table.getRows()) {
				// Match by above executionId
				if (tr.getValue(1).equals(executionId)) {
					executionStatus = tr.getValue(5);
					break;
				}
			}
			if (!"STOPPED".equals(executionStatus)) {
				Thread.sleep(100);
			}
		}
		while (!"STOPPED".equals(executionStatus) && n++ < 100);
		assertEquals("STOPPED", executionStatus);
	}

	@Test
	public void doStopAllJobExecutions() throws Exception {
		String jobName = generateJobName();
		executeJobCreate(jobName, JOB_WITH_STEP_EXECUTIONS);
		checkForJobInList(jobName, JOB_WITH_STEP_EXECUTIONS, true);
		triggerJobWithDelay(jobName, "5");
		Thread.sleep(5000);
		String executionId = getJobExecutionId(jobName);
		String executionStatus = getJobExecutionStatus(jobName);
		assertTrue(executionStatus.equals("STARTING") || executionStatus.equals("STARTED"));
		// Stop the execution by the given executionId.
		executeCommand("job execution all stop --force");
		// sleep for stop() until the step2 is invoked.
		Thread.sleep(3000);
		Table table = (Table) executeCommand("job execution list").getResult();
		for (TableRow tr : table.getRows()) {
			// Match by above executionId
			if (tr.getValue(1).equals(executionId)) {
				executionStatus = tr.getValue(5);
				break;
			}
		}
		assertTrue("Expected an executionStatus with Status 'STOPPING' or 'STOPPED' but got " + executionStatus,
				executionStatus.equals("STOPPING") || executionStatus.equals("STOPPED"));
	}

	public void testStepExecutionProgress() {
		String jobName = generateJobName();
		executeJobCreate(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR);
		checkForJobInList(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
		executeJobLaunch(jobName);
		String jobExecutionId = getJobExecutionId(jobName);

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

	@Test
	public void testDisplayStepExecution() {
		final String jobName = generateJobName();
		executeJobCreate(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR);
		checkForJobInList(jobName, JOB_WITH_PARAMETERS_DESCRIPTOR, true);
		executeJobLaunch(jobName);
		final String jobExecutionId = getJobExecutionId(jobName);
		final Table stepExecutions = listStepExecutions(jobExecutionId);
		String stepExecutionId = stepExecutions.getRows().get(0).getValue(1);

		final Table stepExecution = getDisplayStepExecution(jobExecutionId, stepExecutionId);
		final String stepExecutionIdFromTable = stepExecution.getRows().get(0).getValue(2);
		final String jobExecutionIdFromTable = stepExecution.getRows().get(1).getValue(2);

		final String stepNameFromTable = stepExecution.getRows().get(2).getValue(2);
		// start time
		final String duration = stepExecution.getRows().get(3).getValue(2);
		assertEquals(stepExecutionId, stepExecutionIdFromTable);
		assertEquals(jobExecutionId, jobExecutionIdFromTable);
		assertNotEquals(stepNameFromTable, "N/A");
		assertFalse(duration.isEmpty());
		assertNotEquals(duration, "N/A");
	}

	@Test
	public void testJavaConfigJob() {
		final String jobName = generateJobName();
		executeJobCreate(jobName, JAVA_CONFIG_JOB_DESCRIPTOR);
		checkForJobInList(jobName, JAVA_CONFIG_JOB_DESCRIPTOR, true);
		executeJobLaunch(jobName);
	}

}
