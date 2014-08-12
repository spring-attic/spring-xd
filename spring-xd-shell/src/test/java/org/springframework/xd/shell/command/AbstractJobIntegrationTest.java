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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.After;
import org.junit.Before;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.shell.core.CommandResult;
import org.springframework.util.FileCopyUtils;
import org.springframework.xd.shell.AbstractShellIntegrationTest;
import org.springframework.xd.shell.util.Table;
import org.springframework.xd.shell.util.TableRow;

/**
 * Provides an @After JUnit lifecycle method that will destroy the jobs that were created by calling executeJobCreate
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 */
public abstract class AbstractJobIntegrationTest extends AbstractShellIntegrationTest {

	private static final String MODULE_RESOURCE_DIR = "../spring-xd-shell/src/test/resources/spring-xd/xd/modules/job/";

	private static final String MODULE_TARGET_DIR = "../modules/job/";

	private static final String TEST_TASKLET = "test.xml";

	private static final String JOB_WITH_PARAMETERS_TASKLET = "jobWithParameters.xml";

	private static final String JOB_WITH_STEP_EXECUTIONS_TASKLET = "jobWithStepExecutions.xml";

	private static final String JOB_WITH_PARTITIONS_TASKLET = "jobWithPartitions.xml";

	public static final String MY_JOB = "myJob";

	public static final String MY_TEST = "myTest";

	public static final String MY_JOB_WITH_PARAMETERS = "myJobWithParameters";

	public static final String JOB_WITH_PARAMETERS_DESCRIPTOR = "jobWithParameters";

	public static final String JOB_WITH_STEP_EXECUTIONS = "jobWithStepExecutions";

	public static final String MY_JOB_WITH_PARTITIONS = "myJobWithPartitions";

	public static final String JOB_WITH_PARTITIONS_DESCRIPTOR = "jobWithPartitions";

	public static final String NESTED_JOB_DESCRIPTOR = "nestedJob";

	private static final String NESTED_JOB_TASKLET = "nestedJob.xml";

	private List<String> jobs = new ArrayList<String>();

	private List<String> streams = new ArrayList<String>();

	private final ConcurrentMap<String, PollableChannel> jobTapChannels = new ConcurrentHashMap<String, PollableChannel>();

	@Before
	public void before() {
		copyTaskletDescriptorsToServer(MODULE_RESOURCE_DIR + TEST_TASKLET, MODULE_TARGET_DIR + TEST_TASKLET);
		copyTaskletDescriptorsToServer(MODULE_RESOURCE_DIR + JOB_WITH_PARAMETERS_TASKLET, MODULE_TARGET_DIR
				+ JOB_WITH_PARAMETERS_TASKLET);
		copyTaskletDescriptorsToServer(MODULE_RESOURCE_DIR + JOB_WITH_STEP_EXECUTIONS_TASKLET, MODULE_TARGET_DIR
				+ JOB_WITH_STEP_EXECUTIONS_TASKLET);
		copyTaskletDescriptorsToServer(MODULE_RESOURCE_DIR + JOB_WITH_PARTITIONS_TASKLET, MODULE_TARGET_DIR
				+ JOB_WITH_PARTITIONS_TASKLET);
		copyTaskletDescriptorsToServer(MODULE_RESOURCE_DIR + NESTED_JOB_TASKLET, MODULE_TARGET_DIR
				+ NESTED_JOB_TASKLET);
	}

	@After
	public void after() {
		for (String job : jobs) {
			try {
				executeJobDestroy(job);
			}
			catch (Exception e) {
				// ignore
			}
		}
		for (String stream : streams) {
			try {
				executeStreamDestroy(stream);
			}
			catch (Exception e) {
				// ignore
			}
		}
	}

	/**
	 * Execute 'job destroy' for the supplied job names
	 */
	protected void executeJobDestroy(String... jobNames) {
		for (String jobName : jobNames) {
			CommandResult cr = executeCommand("job destroy --name " + jobName);
			assertTrue("Failure to destroy job " + jobName + ".  CommandResult = " + cr.toString(), cr.isSuccess());
			jobDeploymentVerifier.waitForDestroy(jobName);
		}
	}

	protected CommandResult jobDestroy(String jobName) {
		CommandResult result = getShell().executeCommand("job destroy --name " + jobName);
		jobDeploymentVerifier.waitForDestroy(jobName);
		return result;
	}

	protected void executeStreamDestroy(String... streamNames) {
		for (String streamName : streamNames) {
			CommandResult cr = executeCommand("stream destroy --name " + streamName);
			assertTrue("Failure to destroy stream " + streamName + ".  CommandResult = " + cr.toString(),
					cr.isSuccess());
			streamDeploymentVerifier.waitForDestroy(streamName);
		}
	}

	protected void executeJobCreate(String jobName, String jobDefinition) {
		executeJobCreate(jobName, jobDefinition, true);
	}

	protected String executeJobCreate(String jobDefinition) {
		String jobName = generateJobName();
		executeJobCreate(jobName, jobDefinition, true);
		return jobName;
	}

	/**
	 * Execute job create for the supplied job name/definition, and verify the command result.
	 */
	protected void executeJobCreate(String jobName, String jobDefinition, boolean deploy) {
		CommandResult cr = executeCommand("job create --definition \"" + jobDefinition + "\" --name " + jobName
				+ (deploy ? " --deploy" : ""));
		String prefix = (deploy) ? "Successfully created and deployed job '" : "Successfully created job '";
		assertEquals(prefix + jobName + "'", cr.getResult());
		jobs.add(jobName);
		if (deploy) {
			bindJobTap(jobName);
			jobDeploymentVerifier.waitForDeploy(jobName);
		}
		else {
			jobDeploymentVerifier.waitForCreate(jobName);
		}
	}

	protected CommandResult createJob(String jobName, String definition) {
		return createJob(jobName, definition, "true");
	}

	/**
	 * Execute job create without any assertions
	 */
	protected CommandResult createJob(String jobName, String definition, String deploy) {
		return getShell().executeCommand(
				"job create --definition \"" + definition + "\" --name " + jobName + " --deploy " + deploy);
	}

	/**
	 * Execute job deploy without any assertions
	 */
	protected CommandResult deployJob(String jobName) {
		CommandResult result = getShell().executeCommand("job deploy " + jobName);
		bindJobTap(jobName);
		jobDeploymentVerifier.waitForDeploy(jobName);
		return result;
	}

	/**
	 * Execute job undeploy without any assertions
	 */
	protected CommandResult undeployJob(String jobName) {
		CommandResult result = getShell().executeCommand("job undeploy " + jobName);
		unbindJobTap(jobName);
		jobDeploymentVerifier.waitForUndeploy(jobName);
		return result;
	}

	/**
	 * Launch a job that is already deployed
	 */
	protected void executeJobLaunch(String jobName, String jobParameters) {
		CommandResult cr = executeCommand("job launch --name " + jobName + " --params " + jobParameters);
		String prefix = "Successfully submitted launch request for job '";
		assertEquals(prefix + jobName + "'", cr.getResult());
		waitForJobCompletion(jobName);
	}

	/**
	 * Launch a job that is already deployed
	 */
	protected void executeJobLaunch(String jobName) {
		CommandResult cr = executeCommand("job launch --name " + jobName);
		String prefix = "Successfully submitted launch request for job '";
		assertEquals(prefix + jobName + "'", cr.getResult());
		waitForJobCompletion(jobName);
	}

	protected void checkForJobInList(String jobName, String jobDescriptor, boolean shouldBeDeployed) {
		Table t = listJobs();
		assertTrue(t.getRows().contains(
				new TableRow().addValue(1, jobName).addValue(2, jobDescriptor).addValue(3,
						shouldBeDeployed ? "deployed" : "undeployed")));
	}

	protected void checkForFail(CommandResult cr) {
		assertTrue("Failure.  CommandResult = " + cr.toString(), !cr.isSuccess());
	}

	protected void checkForSuccess(CommandResult cr) {
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
	}

	protected void checkDeployedJobMessage(CommandResult cr, String jobName) {
		assertEquals("Deployed job '" + jobName + "'", cr.getResult());
	}

	protected void checkUndeployedJobMessage(CommandResult cr, String jobName) {
		assertEquals("Un-deployed Job '" + jobName + "'", cr.getResult());
	}

	protected void checkErrorMessages(CommandResult cr, String expectedMessage) {
		assertTrue("Failure.  CommandResult = " + cr.toString(),
				cr.getException().getMessage().contains(expectedMessage));
	}

	protected void checkDuplicateJobErrorMessage(CommandResult cr, String jobName) {
		checkErrorMessages(cr, "There is already a job named '" + jobName + "'");
	}

	protected void checkBatchJobExistsErrorMessage(CommandResult cr, String jobName) {
		checkErrorMessages(cr, "Batch Job with the name " + jobName + " already exists");
	}

	protected void triggerJob(String jobName) {
		String streamName = generateStreamName();
		CommandResult cr = getShell().executeCommand(
				"stream create --name " + streamName + " --definition \"trigger > "
						+ getJobLaunchQueue(jobName) + "\" --deploy true");
		streams.add(streamName);
		waitForJobCompletion(jobName);
		checkForSuccess(cr);
	}

	protected void triggerJobWithParams(String jobName, String parameters) {
		String streamName = generateStreamName();
		CommandResult cr = getShell().executeCommand(
				String.format("stream create --name " + streamName
						+ " --definition \"trigger --payload='%s' > " + getJobLaunchQueue(jobName)
						+ "\" --deploy true", parameters.replaceAll("\"", "\\\\\"")));
		streams.add(streamName);
		waitForJobCompletion(jobName);
		checkForSuccess(cr);
	}

	protected void triggerJobWithDelay(String jobName, String fixedDelay) {
		String streamName = generateStreamName();
		CommandResult cr = getShell().executeCommand(
				"stream create --name " + streamName + " --definition \"trigger --fixedDelay=" + fixedDelay
				+ " > " + getJobLaunchQueue(jobName) + "\" --deploy true");
		streams.add(streamName);
		waitForJobCompletion(jobName);
		checkForSuccess(cr);
	}

	private Table listJobs() {
		Object result = getShell().executeCommand("job list").getResult();
		return (result instanceof Table) ? (Table) result : new Table();
	}

	private void copyTaskletDescriptorsToServer(String inFile, String outFile) {
		File out = new File(outFile);
		File in = new File(inFile);
		try {
			FileCopyUtils.copy(in, out);
		}
		catch (IOException ioe) {
			assertTrue("Unable to deploy Job descriptor to server directory", out.isFile());
		}
		out.deleteOnExit();

		// Copy the companion .properties file if it exists
		out = new File(outFile.replaceAll("\\.xml$", ".properties"));
		in = new File(inFile.replaceAll("\\.xml$", ".properties"));
		if (in.exists()) {
			try {
				FileCopyUtils.copy(in, out);
			}
			catch (IOException ioe) {
				assertTrue("Unable to deploy Job descriptor to server directory", out.isFile());
			}
			out.deleteOnExit();
		}

	}

	protected Table listJobExecutions() {
		return (Table) getShell().executeCommand("job execution list").getResult();
	}

	protected String displayJobExecution(String id) {
		final CommandResult commandResult = getShell().executeCommand("job execution display " + id);
		if (!commandResult.isSuccess()) {
			throw new IllegalStateException("Expected a successful command execution.", commandResult.getException());
		}
		return (String) commandResult.getResult();
	}

	protected TableRow getJobExecutionRow(String jobName) {
		for (TableRow row : listJobExecutions().getRows()) {
			if (jobName.equals(row.getValue(2))) {
				return row;
			}
		}
		return null;
	}

	protected String getJobExecutionId(TableRow jobExecution) {
		assertNotNull(jobExecution);
		return jobExecution.getValue(1);
	}

	protected String getJobExecutionId(String jobName) {
		return getJobExecutionId(getJobExecutionRow(jobName));

	}

	protected String getJobExecutionStatus(TableRow jobExecution) {
		assertNotNull(jobExecution);
		return jobExecution.getValue(5);
	}

	protected String getJobExecutionStatus(String jobName) {
		return getJobExecutionStatus(getJobExecutionRow(jobName));
	}

	protected Table listStepExecutions(String id) {
		final CommandResult commandResult = getShell().executeCommand("job execution step list " + id);
		if (!commandResult.isSuccess()) {
			throw new IllegalStateException("Expected a successful command execution.", commandResult.getException());
		}
		return (Table) commandResult.getResult();
	}

	protected Table getStepExecutionProgress(String jobExecutionId, String stepExecutionId) {
		final CommandResult commandResult = getShell().executeCommand(
				"job execution step progress " + stepExecutionId + " --jobExecutionId " + jobExecutionId);
		if (!commandResult.isSuccess()) {
			throw new IllegalStateException("Expected a successful command execution.", commandResult.getException());
		}
		return (Table) commandResult.getResult();
	}

	protected Table getDisplayStepExecution(String jobExecutionId, String stepExecutionId) {
		final CommandResult commandResult = getShell().executeCommand(
				"job execution step display " + stepExecutionId + " --jobExecutionId " + jobExecutionId);
		if (!commandResult.isSuccess()) {
			throw new IllegalStateException("Expected a successful command execution.", commandResult.getException());
		}
		return (Table) commandResult.getResult();
	}

	private void bindJobTap(String jobName) {
		MessageChannel alreadyBound = jobTapChannels.putIfAbsent(jobName, new QueueChannel());
		if (alreadyBound == null) {
			getMessageBus().bindPubSubConsumer("tap:job:" + jobName, jobTapChannels.get(jobName), null);
		}
	}

	private void unbindJobTap(String jobName) {
		jobTapChannels.remove(jobName);
		getMessageBus().unbindConsumers("tap:job:" + jobName);
	}

	private void waitForJobCompletion(String jobName) {
		// could match on parameters if necessary (for disambiguation of concurrent executions)
		PollableChannel channel = jobTapChannels.get(jobName);
		Message<?> message = null;
		do {
			message = channel.receive(10000);
			if (message != null) {
				Object payload = message.getPayload();
				if (payload instanceof JobExecution) {
					// could add a waitForJobLaunch that returns as soon as it sees STARTED status
					if (BatchStatus.COMPLETED.equals(((JobExecution) payload).getStatus())) {
						return;
					}
				}
			}
		}
		while (message != null);
		throw new IllegalStateException(String.format("timed out while waiting for job: %s", jobName));
	}

}
