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

package org.springframework.xd.integration.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.xd.rest.client.impl.SpringXDException;
import org.springframework.xd.rest.domain.JobExecutionInfoResource;
import org.springframework.xd.rest.domain.StepExecutionInfoResource;
import org.springframework.xd.test.fixtures.FileJdbcJob;
import org.springframework.xd.test.fixtures.JdbcSink;


/**
 * Asserts that a job command has the expected results within the application.
 *
 * @author Glenn Renfro
 */
public class JobCommandTest extends AbstractJobTest {

	private static final Logger logger = LoggerFactory.getLogger(JobCommandTest.class);

	private final static String DEFAULT_FILE_NAME = "filejdbctest";

	private JdbcSink jdbcSink;

	private String tableName;

	/**
	 * Removes the table created from a previous test.
	 */
	@Before
	public void initialize() {
		jdbcSink = sinks.jdbc();
		tableName = "filejdbctest";
		jdbcSink.tableName(tableName);
		cleanup();
	}


	@Test
	public void testJobLifecycle() throws InterruptedException {
		FileJdbcJob job = new FileJdbcJob(FileJdbcJob.DEFAULT_DIRECTORY, FileJdbcJob.DEFAULT_FILE_NAME,
				FileJdbcJob.DEFAULT_TABLE_NAME, FileJdbcJob.DEFAULT_NAMES, FileJdbcJob.DEFAULT_DELETE_FILES);
		String jobName = "tjl" + UUID.randomUUID().toString();
		logger.info("Testing Job Lifecycle for: " + jobName);
		job(jobName, job.toDSL(),true);
		checkJob(jobName, job.toDSL(), true);
		undeployJob(jobName);
		checkJob(jobName, job.toDSL(), false);
	}

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void testJobCreateDuplicate() throws InterruptedException {
		exception.expect(SpringXDException.class);
		exception.expectMessage("Batch Job with the name " + JOB_NAME + " already exists");
		logger.info("Testing Job Create Duplicate");
		FileJdbcJob job = new FileJdbcJob(FileJdbcJob.DEFAULT_DIRECTORY, FileJdbcJob.DEFAULT_FILE_NAME,
				FileJdbcJob.DEFAULT_TABLE_NAME, FileJdbcJob.DEFAULT_NAMES, FileJdbcJob.DEFAULT_DELETE_FILES);
		job(job.toDSL());
		checkJob(job.toDSL(), true);
		job(job.toDSL());
	}

	@Test
	public void testJobDestroyMissing() {
		exception.expect(SpringXDException.class);
		exception.expectMessage("There is no job definition named '" + JOB_NAME + "'");
		logger.info("Testing Job Destroy Missing");
		destroyJob();
	}

	@Test
	public void testJobCreateDuplicateWithDeployFalse() {
		exception.expect(SpringXDException.class);
		exception.expectMessage("There is already a job named 'jobFalseDeploy'");
		logger.info("Testing Job False Deploy");

		FileJdbcJob job = new FileJdbcJob(FileJdbcJob.DEFAULT_DIRECTORY, FileJdbcJob.DEFAULT_FILE_NAME,
				FileJdbcJob.DEFAULT_TABLE_NAME, FileJdbcJob.DEFAULT_NAMES, FileJdbcJob.DEFAULT_DELETE_FILES);
		job("jobFalseDeploy", job.toDSL(),false);
		checkJob(job.toDSL(), false);
		job("jobFalseDeploy", job.toDSL(),false);
	}

	@Test
	public void testJobDeployUndeployFlow() throws InterruptedException {
		FileJdbcJob job = new FileJdbcJob(FileJdbcJob.DEFAULT_DIRECTORY, FileJdbcJob.DEFAULT_FILE_NAME,
				FileJdbcJob.DEFAULT_TABLE_NAME, FileJdbcJob.DEFAULT_NAMES, FileJdbcJob.DEFAULT_DELETE_FILES);
		String jobName = "tjduf" + UUID.randomUUID().toString();
		logger.info("Testing Job Deploy Undeploy Flow for: " + jobName);

		job(jobName, job.toDSL(),true);
		checkJob(jobName, job.toDSL(), true);
		undeployJob(jobName);
		checkJob(jobName, job.toDSL(), false);
		deployJob(jobName);
		checkJob(jobName, job.toDSL(), true);
		undeployJob(jobName);
		checkJob(jobName, job.toDSL(), false);
	}

	@Test
	public void testInvalidJobDescriptor() throws InterruptedException {
		exception.expect(SpringXDException.class);
		exception.expectMessage("Could not find module with name 'adfafadsf' and type 'job'");
		logger.info("Testing Invalid Job False Descriptor");
		job("FailJob", "adfafadsf",true);
	}

	@Test
	public void testMissingJobDescriptor() {
		exception.expect(SpringXDException.class);
		exception.expectMessage("definition cannot be blank or null");
		logger.info("Testing Missing job Descriptor");
		job("missingdescriptor", "",true);
	}

	@Test
	public void testListStepExecutions() {
		String data = UUID.randomUUID().toString();
		String jobName = "tsle" + UUID.randomUUID().toString();

		FileJdbcJob job = new FileJdbcJob(FileJdbcJob.DEFAULT_DIRECTORY, FileJdbcJob.DEFAULT_FILE_NAME,
				FileJdbcJob.DEFAULT_TABLE_NAME, FileJdbcJob.DEFAULT_NAMES, FileJdbcJob.DEFAULT_DELETE_FILES);

		// Create a stream that writes to a file. This file will be used by the job.
		stream("dataSender", sources.http() + XD_DELIMITER
				+ sinks.file(FileJdbcJob.DEFAULT_DIRECTORY, DEFAULT_FILE_NAME).toDSL());
		sources.httpSource("dataSender").postData(data);
		job(jobName, job.toDSL(),true);
		jobLaunch(jobName);
		String query = String.format("SELECT data FROM %s", tableName);
		waitForTablePopulation(query, jdbcSink.getJdbcTemplate(), 1);

		List<String> results = jdbcSink.getJdbcTemplate().queryForList(query, String.class);

		assertEquals(1, results.size());

		for (String result : results) {
			assertEquals(data, result);
		}
		List<JobExecutionInfoResource> jobList = this.getJobExecInfoByName(jobName);
		logger.info("testing job executions for: " + jobName);
		assertEquals(1, jobList.size());
		long executionId = jobList.get(0).getExecutionId();
		logger.info("testing step executions for: " + jobName);
		List<StepExecutionInfoResource> stepExecutions = getStepExecutions(executionId);
		Iterator<StepExecutionInfoResource> iter = stepExecutions.iterator();
		assertEquals(2, stepExecutions.size());
		boolean step1MasterFound = false;
		boolean step1MasterPartitionFound = false;
		while (iter.hasNext()) {
			StepExecutionInfoResource resource = iter.next();
			assertEquals(BatchStatus.COMPLETED, resource.getStepExecution().getStatus());
			assertEquals(ExitStatus.COMPLETED, resource.getStepExecution().getExitStatus());
			if (resource.getStepExecution().getStepName().equals("step1-master")) {
				step1MasterFound = true;
			}
			else if (resource.getStepExecution().getStepName().equals("step1-master:partition0")) {
				step1MasterPartitionFound = true;
			}
		}
		assertTrue("step1-master was not found in step execution list", step1MasterFound);
		assertTrue("step1-master:partition0 was not found in step execution list", step1MasterPartitionFound);

	}

	/**
	 * Being a good steward of the database remove the result table from the database.
	 */
	@After
	public void cleanup() {
		if (jdbcSink != null) {
			jdbcSink.dropTable(tableName);
		}
		destroyAllJobs();
	}
}
