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

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.xd.test.fixtures.HdfsJdbcJob;
import org.springframework.xd.test.fixtures.JdbcSink;


/**
 * Asserts that this job will read the specified file from hdfs and place the results into the database.
 *
 * @author Glenn Renfro
 */
public class HdfsJdbcTest extends AbstractJobTest {

	private final static String DEFAULT_FILE_NAME = "hdfsjdbctest";

	private JdbcSink jdbcSink;

	private String tableName;

	/**
	 * Removes the table created from a previous test.
	 */
	@Before
	public void initialize() {
		jdbcSink = sinks.jdbc();
		tableName = HdfsJdbcJob.DEFAULT_TABLE_NAME;
		jdbcSink.tableName(tableName);
		cleanup();
		jdbcSink.getJdbcTemplate().getDataSource();
	}

	/**
	 * Asserts that hdfsJdbcJob has written the test data from a file on hdfs to the table.
	 *
	 */
	@Test
	public void testHdfsJdbcJob() {
		String data = UUID.randomUUID().toString();
		HdfsJdbcJob job = jobs.hdfsJdbcJob();
		job.fileName(HdfsJdbcJob.DEFAULT_FILE_NAME + "-0.txt");
		// Create a stream that writes to a hdfs file. This file will be used by the job.
		stream("dataSender", sources.http() + XD_DELIMITER + sinks.hdfs()
				.directoryName(HdfsJdbcJob.DEFAULT_DIRECTORY).fileName(DEFAULT_FILE_NAME).toDSL());
		sources.httpSource("dataSender").postData(data);
		job(job.toDSL());
		waitForXD();
		//Undeploy the dataSender stream to force XD to close the file.
		undeployStream("dataSender");
		waitForXD();
		jobLaunch();
		waitForXD();
		String query = String.format("SELECT data FROM %s", tableName);
		assertEquals(data,
				jdbcSink.getJdbcTemplate().queryForObject(query, String.class));
	}

	/**
	 * Being a good steward, remove the result table from the DB and source file from hdfs.
	 */
	@After
	public void cleanup() {
		if (jdbcSink != null) {
			jdbcSink.dropTable(tableName);
		}
		if (hadoopUtil.fileExists(HdfsJdbcJob.DEFAULT_DIRECTORY)) {
			hadoopUtil.fileRemove(HdfsJdbcJob.DEFAULT_DIRECTORY);
		}
	}
}
