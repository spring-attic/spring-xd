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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.xd.integration.util.XdEnvironment;
import org.springframework.xd.test.fixtures.FileJdbcJob;
import org.springframework.xd.test.fixtures.JdbcSink;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Asserts that this job will read the specified file and place the results into the database.
 *
 * @author Glenn Renfro
 */
public class FileJdbcTest extends AbstractJobTest {

	private final static String DEFAULT_FILE_NAME = "filejdbctest";
	private final static String DEFAULT_DIRECTORY = XdEnvironment.RESULT_LOCATION +
			"/" + FileJdbcJob.DEFAULT_DIRECTORY + "/";

	private JdbcSink jdbcSink;

	private String tableName;
	
	private String jobName;

	/**
	 * Removes the table created from a previous test.
	 */
	@Before
	public void initialize() {
		jdbcSink = sinks.jdbc();
		tableName = "filejdbctest";
		jdbcSink.tableName(tableName);
		jobName = "fjt"+UUID.randomUUID().toString();
		cleanup();
	}

	/**
	 * Asserts that fileJdbcJob has written the test data from a file to the table.
	 *
	 */
	@Test
	public void testPartitionedFileJdbcJob() {
		String data = UUID.randomUUID().toString();
		jdbcSink.getJdbcTemplate().getDataSource();
		FileJdbcJob job = new FileJdbcJob(DEFAULT_DIRECTORY,
				String.format("/%spartition*", DEFAULT_FILE_NAME),
				FileJdbcJob.DEFAULT_TABLE_NAME,
				FileJdbcJob.DEFAULT_NAMES,
				FileJdbcJob.DEFAULT_DELETE_FILES);

		job(jobName, job.toDSL(), true);

		for (int i = 0; i < 5; i++) {
			// Setup data files to be used for partition test
			assertTrue(setupDataFiles(getContainerHostForJob(jobName), DEFAULT_DIRECTORY,
					DEFAULT_FILE_NAME + "partition" + i + ".out", data));
		}
		jobLaunch(jobName);

		String query = String.format("SELECT data FROM %s", tableName);
		waitForTablePopulation(query, jdbcSink.getJdbcTemplate(), 5);

		List<String> results = jdbcSink.getJdbcTemplate().queryForList(query, String.class);

		assertEquals(5, results.size());

		for (String result : results) {
			assertEquals(data, result);
		}
	}

	/**
	 * Asserts that fileJdbcJob has written the test data from a file to the table.
	 *
	 */
	@Test
	public void testFileJdbcJob() {
		String data = UUID.randomUUID().toString();
		jdbcSink.getJdbcTemplate().getDataSource();
		FileJdbcJob job = new FileJdbcJob(DEFAULT_DIRECTORY, FileJdbcJob.DEFAULT_FILE_NAME,
				FileJdbcJob.DEFAULT_TABLE_NAME, FileJdbcJob.DEFAULT_NAMES,
				FileJdbcJob.DEFAULT_DELETE_FILES);

		job(jobName, job.toDSL(), true);
		// Setup data files to be used for test
		assertTrue(setupDataFiles(getContainerHostForJob(jobName), DEFAULT_DIRECTORY,
				DEFAULT_FILE_NAME + ".out", data));

		jobLaunch(jobName);
		String query = String.format("SELECT data FROM %s", tableName);

		waitForTablePopulation(query, jdbcSink.getJdbcTemplate(), 1);

		List<String> results = jdbcSink.getJdbcTemplate().queryForList(query, String.class);
		assertEquals(1, results.size());

		for (String result : results) {
			assertEquals(data, result);
		}
	}

	/**
	 * Asserts that fileJdbcJob has written the test data from a file to the table.
	 *
	 */
	@Test
	public void testFileJdbcJobMultipleInvocations() {
		List<String> data = new ArrayList<>();
		String curData = UUID.randomUUID().toString();
		data.add(curData);
		final String FILE_NAME = DEFAULT_FILE_NAME+"_MULTIPLE";
		jdbcSink.getJdbcTemplate().getDataSource();
		FileJdbcJob job = new FileJdbcJob(DEFAULT_DIRECTORY, FILE_NAME+"*",
				FileJdbcJob.DEFAULT_TABLE_NAME, FileJdbcJob.DEFAULT_NAMES, true);

		job(jobName, job.toDSL(), true);

		// Setup data files to be used for test
		assertTrue(setupDataFiles(getContainerHostForJob(jobName), DEFAULT_DIRECTORY,
				FILE_NAME + "1.out", curData));

		jobLaunch(jobName);
		String query = String.format("SELECT data FROM %s", tableName);

		waitForTablePopulation(query, jdbcSink.getJdbcTemplate(), 1);

		List<String> results = jdbcSink.getJdbcTemplate().queryForList(query, String.class);

		assertEquals(1, results.size());

		for (String result : results) {
			assertTrue(data.contains(result));
		}

		assertTrue(!fileExistsOnXDInstance(getContainerHostForJob(jobName), DEFAULT_DIRECTORY + File.separator + FILE_NAME + "1.out"));

		curData = UUID.randomUUID().toString();
		data.add(curData);

		assertTrue(setupDataFiles(getContainerHostForJob(jobName), DEFAULT_DIRECTORY,
				FILE_NAME + "2.out", curData));

		jobLaunch(jobName);

		waitForTablePopulation(query, jdbcSink.getJdbcTemplate(), 2);

		results = jdbcSink.getJdbcTemplate().queryForList(query, String.class);

		assertEquals(2, results.size());

		for (String result : results) {
			assertTrue(data.contains(result));
		}

		assertTrue(!fileExistsOnXDInstance(getContainerHostForJob(jobName), DEFAULT_DIRECTORY + File.separator + FILE_NAME + "2.out"));
	}

	/**
	 * Being a good steward of the database remove the result table from the database.
	 */
	@After
	public void cleanup() {
		if (jdbcSink != null) {
			jdbcSink.dropTable(tableName);
		}
	}

}
