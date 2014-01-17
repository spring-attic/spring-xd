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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;

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
 * 
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

	private List<String> jobs = new ArrayList<String>();

	@Before
	public void before() {
		copyTaskletDescriptorsToServer(MODULE_RESOURCE_DIR + TEST_TASKLET, MODULE_TARGET_DIR + TEST_TASKLET);
		copyTaskletDescriptorsToServer(MODULE_RESOURCE_DIR + JOB_WITH_PARAMETERS_TASKLET, MODULE_TARGET_DIR
				+ JOB_WITH_PARAMETERS_TASKLET);
		copyTaskletDescriptorsToServer(MODULE_RESOURCE_DIR + JOB_WITH_STEP_EXECUTIONS_TASKLET, MODULE_TARGET_DIR
				+ JOB_WITH_STEP_EXECUTIONS_TASKLET);
		copyTaskletDescriptorsToServer(MODULE_RESOURCE_DIR + JOB_WITH_PARTITIONS_TASKLET, MODULE_TARGET_DIR
				+ JOB_WITH_PARTITIONS_TASKLET);
		// clear any test jobs that may still exist
		try {
			executeJobDestroy(MY_JOB);
		}
		catch (Throwable t) {
			// don't worry if it is thrown
		}
		try {
			executeJobDestroy(MY_TEST);
		}
		catch (Throwable t) {
			// don't worry if it is thrown
		}
		try {
			executeJobDestroy(MY_JOB_WITH_PARAMETERS);
		}
		catch (Throwable t) {
			// don't worry if it is thrown
		}
		try {
			executeJobDestroy(MY_JOB_WITH_PARTITIONS);
		}
		catch (Throwable t) {
			// don't worry if it is thrown
		}
	}

	@After
	public void after() {
		executeJobDestroy(jobs.toArray(new String[jobs.size()]));
		getShell().executeCommand("stream destroy " + "me-Try2");
	}

	/**
	 * Execute 'job destroy' for the supplied stream names
	 */
	protected void executeJobDestroy(String... jobNames) {
		for (String jobName : jobNames) {
			CommandResult cr = executeCommand("job destroy --name " + jobName);
			assertTrue("Failure to destroy job " + jobName + ".  CommandResult = " + cr.toString(), cr.isSuccess());
		}
	}

	protected void executeJobCreate(String jobName, String jobDefinition) {
		executeJobCreate(jobName, jobDefinition, true);
	}

	/**
	 * Execute job create for the supplied job name/definition, and verify the command result.
	 */
	protected void executeJobCreate(String jobName, String jobDefinition, boolean deploy) {
		CommandResult cr = executeCommand("job create --definition \"" + jobDefinition + "\" --name " + jobName
				+ (deploy ? "" : " --deploy false"));
		String prefix = (deploy) ? "Successfully created and deployed job '" : "Successfully created job '";
		assertEquals(prefix + jobName + "'", cr.getResult());
		jobs.add(jobName);
	}

	/**
	 * Launch a job that is already deployed
	 */
	protected void executeJobLaunch(String jobName, String jobParameters) {
		CommandResult cr = executeCommand("job launch --name " + jobName + " --params " + jobParameters);
		String prefix = "Successfully launched the job '";
		assertEquals(prefix + jobName + "'", cr.getResult());
	}

	/**
	 * Launch a job that is already deployed
	 */
	protected void executeJobLaunch(String jobName) {
		CommandResult cr = executeCommand("job launch --name " + jobName);
		String prefix = "Successfully launched the job '";
		assertEquals(prefix + jobName + "'", cr.getResult());
	}

	protected void checkForJobInList(String jobName, String jobDescriptor, boolean shouldBeDeployed) {
		Table t = listJobs();
		assertTrue(t.getRows().contains(
				new TableRow().addValue(1, jobName).addValue(2, jobDescriptor).addValue(3,
						shouldBeDeployed ? "deployed" : "")));
	}

	protected void checkForFail(CommandResult cr) {
		assertTrue("Failure.  CommandResult = " + cr.toString(), !cr.isSuccess());
	}

	protected void checkForSuccess(CommandResult cr) {
		assertTrue("Failure.  CommandResult = " + cr.toString(), cr.isSuccess());
	}

	protected void checkErrorMessages(CommandResult cr, String expectedMessage) {
		assertTrue("Failure.  CommandResult = " + cr.toString(),
				cr.getException().getMessage().contains(expectedMessage));
	}

	protected void executemyJobFixedDelayStream(String fixedDelay) {
		CommandResult cr = getShell().executeCommand(
				"stream create --name me-Try2 --definition \"trigger --fixedDelay=" + fixedDelay
						+ " > queue:job:myJob\"");
		checkForSuccess(cr);
	}

	protected void executemyJobTriggerStream() {
		CommandResult cr = getShell().executeCommand(
				"stream create --name me-Try2 --definition \"trigger > queue:job:myJob\"");
		checkForSuccess(cr);
	}

	protected void executemyTestTriggerStream() {
		CommandResult cr = getShell().executeCommand(
				"stream create --name me-Try2 --definition \"trigger > queue:job:myTest\"");
		checkForSuccess(cr);
	}

	protected void executemyjobWithParametersTriggerStream(String params) {
		String commandString = String.format(
				"stream create --name me-Try2 --definition \"trigger --payload='%s' > queue:job:myJobWithParameters\"",
				params);
		CommandResult cr = getShell().executeCommand(commandString);
		checkForSuccess(cr);
	}

	private Table listJobs() {
		return (Table) getShell().executeCommand("job list").getResult();
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
}
