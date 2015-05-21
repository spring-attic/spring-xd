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

import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import org.apache.hadoop.fs.FileStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import org.springframework.util.StringUtils;
import org.springframework.xd.test.fixtures.IncrementalJdbcHdfsJob;
import org.springframework.xd.test.fixtures.JdbcHdfsJob;
import org.springframework.xd.test.fixtures.JdbcSink;
import org.springframework.xd.test.fixtures.PartitionedJdbcHdfsJob;


/**
 * Verifies that this job will read the specified table and place the results in the specified directory and file on
 * hdfs.
 *
 * @author Glenn Renfro
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JdbcHdfsTest extends AbstractJobTest {

	private final static String DEFAULT_TABLE_NAME = "jdbchdfstest";

	private JdbcSink jdbcSink;

	private String tableName;

	private String jobName;
	
	/**
	 * Removes the table created from a previous test. Also deletes the result hdfs directory.
	 */
	@Before
	public void initialize() {
		jobName = "jdbchdfs" + UUID.randomUUID().toString();
		jdbcSink = sinks.jdbc();
		tableName = DEFAULT_TABLE_NAME;
		jdbcSink.tableName(tableName);
		cleanup();
		if (hadoopUtil.fileExists(JdbcHdfsJob.DEFAULT_DIRECTORY)) {
			hadoopUtil.fileRemove(JdbcHdfsJob.DEFAULT_DIRECTORY);
		}
	}

	/**
	 * Asserts that jdbcHdfsJob has written the test data from a jdbc source table to a hdfs file system.
	 *
	 */
	@Test
	public void testJdbcHdfsJobWithSql() {
		// Deploy stream and job.
		String data = UUID.randomUUID().toString();
		jdbcSink.getJdbcTemplate().getDataSource();
		JdbcHdfsJob job = jobs.jdbcHdfsJob();
		// Use a trigger to send data to JDBC
		stream("dataSender", sources.http() + XD_DELIMITER
				+ jdbcSink);
		sources.httpSource("dataSender").postData(data);

		job(jobName, job.toDSL(), true);
		jobLaunch(jobName);
		waitForJobToComplete(jobName);
		// Evaluate the results of the test.
		String path = JdbcHdfsJob.DEFAULT_DIRECTORY + "/" + JdbcHdfsJob.DEFAULT_FILE_NAME + "-0.csv";
		assertPathsExists(path);
		Collection<FileStatus> fileStatuses = hadoopUtil.listDir(path);
		assertEquals("The number of files in list result should only be 1. The file itself. ", 1,
				fileStatuses.size());
		Iterator<FileStatus> statuses = fileStatuses.iterator();
		assertEquals("File size should match the data size +1 for the //n", data.length() + 1, statuses.next().getLen());
		assertEquals("The data returned from hadoop was different than was sent.  ", data + "\n",
				hadoopUtil.getFileContentsFromHdfs(path));
	}

	/**
	 * Asserts that jdbcHdfsJob has written the test data from a jdbc source table to a hdfs file system.
	 *
	 */
	@Test
	public void testJdbcHdfsJobWithColumnsAndTable() {
		// Deploy stream and job.
		String data = UUID.randomUUID().toString();
		jdbcSink.getJdbcTemplate().getDataSource();
		JdbcHdfsJob job = new JdbcHdfsJob(JdbcHdfsJob.DEFAULT_DIRECTORY, JdbcHdfsJob.DEFAULT_FILE_NAME, null, "payload", "jdbchdfstest");
		// Use a trigger to send data to JDBC
		stream("dataSender", sources.http() + XD_DELIMITER
				+ jdbcSink);
		sources.httpSource("dataSender").postData(data);

		job(jobName, job.toDSL(), true);
		jobLaunch(jobName);
		waitForJobToComplete(jobName);
		// Evaluate the results of the test.
		String path = JdbcHdfsJob.DEFAULT_DIRECTORY + "/" + JdbcHdfsJob.DEFAULT_FILE_NAME + "-0.csv";
		assertPathsExists(path);
		Collection<FileStatus> fileStatuses = hadoopUtil.listDir(path);
		assertEquals("The number of files in list result should only be 1. The file itself. ", 1,
				fileStatuses.size());
		Iterator<FileStatus> statuses = fileStatuses.iterator();
		assertEquals("File size should match the data size +1 for the //n", data.length() + 1, statuses.next().getLen());
		assertEquals("The data returned from hadoop was different than was sent.  ", data + "\n",
				hadoopUtil.getFileContentsFromHdfs(path));
	}

	/**
	 * Asserts that jdbcHdfsJob has written the test data from a jdbc source table to a hdfs file system.
	 *
	 */
	@Test
	public void testIncrementalJdbcHdfsJobWithColumnsAndTable() {
		// Deploy stream and job.
		jdbcSink.getJdbcTemplate().getDataSource();
		IncrementalJdbcHdfsJob job = jobs.incrementalJdbcHdfsJob();
		// Use a trigger to send data to JDBC
		stream("dataSender", sources.http() + XD_DELIMITER
				+ jdbcSink);

		String file1Contents = StringUtils.arrayToDelimitedString(new String[] {"1", "2", "3"}, "\n");

		sources.httpSource("dataSender").postData("1");
		sources.httpSource("dataSender").postData("2");
		sources.httpSource("dataSender").postData("3");

		job(jobName, job.toDSL(), true);
		jobLaunch(jobName);
		waitForJobToComplete(jobName);

		// Evaluate the results of the test.
		String path = JdbcHdfsJob.DEFAULT_DIRECTORY + "/" + JdbcHdfsJob.DEFAULT_FILE_NAME + "-p0-0.csv";
		assertPathsExists(path);
		Collection<FileStatus> fileStatuses = hadoopUtil.listDir(path);
		assertEquals("The number of files in list result should only be 1. The file itself. ", 1,
				fileStatuses.size());
		Iterator<FileStatus> statuses = fileStatuses.iterator();
		assertEquals("File size should match the data size +1 for the //n", file1Contents.length() + 1, statuses.next().getLen());
		assertEquals("The data returned from hadoop was different than was sent.  ", file1Contents + "\n",
				hadoopUtil.getFileContentsFromHdfs(path));

		String file2Contents = StringUtils.arrayToDelimitedString(new String[] {"4", "5", "6"}, "\n");

		sources.httpSource("dataSender").postData("4");
		sources.httpSource("dataSender").postData("5");
		sources.httpSource("dataSender").postData("6");

		jobLaunch(jobName);
		waitForJobToComplete(jobName, 2);
		
		// Evaluate the results of the test.
		path = JdbcHdfsJob.DEFAULT_DIRECTORY + "/" + JdbcHdfsJob.DEFAULT_FILE_NAME + "-p0-1.csv";
		assertPathsExists(path);
		fileStatuses = hadoopUtil.listDir(path);
		assertEquals("The number of files in list result should only be 1. The file itself. ", 1,
				fileStatuses.size());
		statuses = fileStatuses.iterator();
		assertEquals("File size should match the data size +1 for the //n", file2Contents.length() + 1, statuses.next().getLen());
		assertEquals("The data returned from hadoop was different than was sent.  ", file2Contents + "\n",
				hadoopUtil.getFileContentsFromHdfs(path));
	}

	/**
	 * Asserts that jdbcHdfsJob has written the test data from a jdbc source table to a hdfs file system.
	 *
	 */
	@Test
	public void testIncrementalJdbcHdfsJobWithColumnsAndTableNothingNew() {
		// Deploy stream and job.
		jdbcSink.getJdbcTemplate().getDataSource();
		IncrementalJdbcHdfsJob job = jobs.incrementalJdbcHdfsJob();
		// Use a trigger to send data to JDBC
		stream("dataSender", sources.http() + XD_DELIMITER
				+ jdbcSink);

		String file1Contents = StringUtils.arrayToDelimitedString(new String[] {"1", "2", "3"}, "\n");

		sources.httpSource("dataSender").postData("1");
		sources.httpSource("dataSender").postData("2");
		sources.httpSource("dataSender").postData("3");

		job(jobName, job.toDSL(), true);
		jobLaunch(jobName);
		waitForJobToComplete(jobName);

		// Evaluate the results of the test.
		String path = JdbcHdfsJob.DEFAULT_DIRECTORY + "/" + JdbcHdfsJob.DEFAULT_FILE_NAME + "-p0-0.csv";
		assertPathsExists(path);
		Collection<FileStatus> fileStatuses = hadoopUtil.listDir(path);
		assertEquals("The number of files in list result should only be 1. The file itself. ", 1,
				fileStatuses.size());
		Iterator<FileStatus> statuses = fileStatuses.iterator();
		assertEquals("File size should match the data size +1 for the //n", file1Contents.length() + 1, statuses.next().getLen());
		assertEquals("The data returned from hadoop was different than was sent.  ", file1Contents + "\n",
				hadoopUtil.getFileContentsFromHdfs(path));

		jobLaunch(jobName);
		waitForJobToComplete(jobName, 2);

		// Evaluate the results of the test.
		String dir = JdbcHdfsJob.DEFAULT_DIRECTORY + "/";
		path = JdbcHdfsJob.DEFAULT_DIRECTORY + "/" + JdbcHdfsJob.DEFAULT_FILE_NAME + "-p0-0.csv";
		assertPathsExists(path);
		fileStatuses = hadoopUtil.listDir(dir);
		assertEquals("The number of files in list result should only be 2. The directory and file itself. ", 2,
				fileStatuses.size());
		statuses = fileStatuses.iterator();
		statuses.next();
		assertEquals("File size should match the data size +1 for the //n", file1Contents.length() + 1, statuses.next().getLen());
		assertEquals("The data returned from hadoop was different than was sent.  ", file1Contents + "\n",
				hadoopUtil.getFileContentsFromHdfs(path));
	}

	/**
	 * Asserts that jdbcHdfsJob has written the test data from a jdbc source table to a hdfs file system.
	 *
	 */
	@Test
	public void testIncrementalJdbcHdfsJobWithColumnsAndTablePartitionDifferentFromCheck() {
		// Deploy stream and job.
		jdbcSink.getJdbcTemplate().getDataSource();
		IncrementalJdbcHdfsJob job = new IncrementalJdbcHdfsJob(IncrementalJdbcHdfsJob.DEFAULT_DIRECTORY, IncrementalJdbcHdfsJob.DEFAULT_FILE_NAME, IncrementalJdbcHdfsJob.DEFAULT_TABLE, "payload,checkColumn", "payload", 3, "checkColumn", -1);
		// Use a trigger to send data to JDBC
		jdbcSink.columns("payload,checkColumn");
		stream("dataSender", sources.http() + XD_DELIMITER
				+ jdbcSink);

		sources.httpSource("dataSender").postData("{\"payload\": 1, \"checkColumn\": 1}");
		sources.httpSource("dataSender").postData("{\"payload\": 2, \"checkColumn\": 1}");
		sources.httpSource("dataSender").postData("{\"payload\": 3, \"checkColumn\": 1}");

		job(jobName, job.toDSL(), true);
		jobLaunch(jobName);
		waitForJobToComplete(jobName);

		// Evaluate the results of the test.
		String dir = JdbcHdfsJob.DEFAULT_DIRECTORY + "/";
		String path0 = JdbcHdfsJob.DEFAULT_DIRECTORY + "/" + JdbcHdfsJob.DEFAULT_FILE_NAME + "-p0" + "-0.csv";
		String path1 = JdbcHdfsJob.DEFAULT_DIRECTORY + "/" + JdbcHdfsJob.DEFAULT_FILE_NAME + "-p1" + "-0.csv";
		String path2 = JdbcHdfsJob.DEFAULT_DIRECTORY + "/" + JdbcHdfsJob.DEFAULT_FILE_NAME + "-p2" + "-0.csv";
		assertPathsExists(path0, path1, path2);
		Collection<FileStatus> fileStatuses = hadoopUtil.listDir(dir);
		assertEquals("The number of files in list result should only be 4. The directory itself and 3 files. ", 4,
				fileStatuses.size());
		for (FileStatus fileStatus : fileStatuses) {
			if (!fileStatus.isDirectory()) {
				assertTrue("The file should be of reasonable size", fileStatus.getLen() > 2 && fileStatus.getLen() < 10);
			}
		}

		sources.httpSource("dataSender").postData("{\"payload\": 4, \"checkColumn\": 2}");
		sources.httpSource("dataSender").postData("{\"payload\": 5, \"checkColumn\": 2}");
		sources.httpSource("dataSender").postData("{\"payload\": 6, \"checkColumn\": 2}");

		jobLaunch(jobName);
		waitForJobToComplete(jobName, 2);

		// Evaluate the results of the test.
		String path3 = JdbcHdfsJob.DEFAULT_DIRECTORY + "/" + JdbcHdfsJob.DEFAULT_FILE_NAME + "-p0" + "-1.csv";
		String path4 = JdbcHdfsJob.DEFAULT_DIRECTORY + "/" + JdbcHdfsJob.DEFAULT_FILE_NAME + "-p1" + "-1.csv";
		String path5 = JdbcHdfsJob.DEFAULT_DIRECTORY + "/" + JdbcHdfsJob.DEFAULT_FILE_NAME + "-p2" + "-1.csv";
		assertPathsExists(path3, path4, path5);
		fileStatuses = hadoopUtil.listDir(dir);
		assertEquals("The number of files in list result should only be 7. The directory itself and 5 files. ", 7,
				fileStatuses.size());
		for (FileStatus fileStatus : fileStatuses) {
			if (!fileStatus.isDirectory()) {
				assertTrue("The file should be of reasonable size", fileStatus.getLen() > 2 && fileStatus.getLen() < 10);
			}
		}
	}

	/**
	 * Asserts that jdbcHdfsJob has written the test data from a jdbc source table to a hdfs file system.
	 *
	 */
	@Test
	public void testIncrementalJdbcHdfsJobWithOverride() {
		// Deploy stream and job.
		jdbcSink.getJdbcTemplate().getDataSource();
		IncrementalJdbcHdfsJob job = jobs.incrementalJdbcHdfsJob();
		// Use a trigger to send data to JDBC
		stream("dataSender", sources.http() + XD_DELIMITER
				+ jdbcSink);

		String file1Contents = StringUtils.arrayToDelimitedString(new String[] {"1", "2", "3"}, "\n");

		sources.httpSource("dataSender").postData("1");
		sources.httpSource("dataSender").postData("2");
		sources.httpSource("dataSender").postData("3");

		job(jobName, job.toDSL(), true);
		jobLaunch(jobName);
		waitForJobToComplete(jobName);

		// Evaluate the results of the test.
		String path = JdbcHdfsJob.DEFAULT_DIRECTORY + "/" + JdbcHdfsJob.DEFAULT_FILE_NAME + "-p0-0.csv";
		assertPathsExists(path);
		Collection<FileStatus> fileStatuses = hadoopUtil.listDir(path);
		assertEquals("The number of files in list result should only be 1. The file itself. ", 1,
				fileStatuses.size());
		Iterator<FileStatus> statuses = fileStatuses.iterator();
		assertEquals("File size should match the data size +1 for the //n", file1Contents.length() + 1, statuses.next().getLen());
		assertEquals("The data returned from hadoop was different than was sent.  ", file1Contents + "\n",
				hadoopUtil.getFileContentsFromHdfs(path));

		String file2Contents = StringUtils.arrayToDelimitedString(new String[] {"3", "4", "5", "6"}, "\n");

		sources.httpSource("dataSender").postData("4");
		sources.httpSource("dataSender").postData("5");
		sources.httpSource("dataSender").postData("6");

		launchJob(jobName, "{\"overrideCheckColumnValue\" : 2}");
		waitForJobToComplete(jobName, 2);

		// Evaluate the results of the test.
		path = JdbcHdfsJob.DEFAULT_DIRECTORY + "/" + JdbcHdfsJob.DEFAULT_FILE_NAME + "-p0-1.csv";
		assertPathsExists(path);
		fileStatuses = hadoopUtil.listDir(path);
		assertEquals("The number of files in list result should only be 1. The file itself. ", 1,
				fileStatuses.size());
		statuses = fileStatuses.iterator();
		assertEquals("File size should match the data size +1 for the //n", file2Contents.length() + 1, statuses.next().getLen());
		assertEquals("The data returned from hadoop was different than was sent.  ", file2Contents + "\n",
				hadoopUtil.getFileContentsFromHdfs(path));
	}

	/**
	 * Asserts that jdbcHdfsJob has written the test data from a jdbc source table to a hdfs file system.
	 *
	 */
	@Test
	public void testPartitionedJdbcHdfsJobWithColumnsTable() {
		// Deploy stream and job.
		jdbcSink.columns(PartitionedJdbcHdfsJob.DEFAULT_COLUMN_NAMES);
		String data0 = "{\"id\":1,\"name\":\"Sven\"}";
		String data1 = "{\"id\":2,\"name\":\"Anna\"}";
		String data2 = "{\"id\":3,\"name\":\"Nisse\"}";
		jdbcSink.getJdbcTemplate().getDataSource();
		PartitionedJdbcHdfsJob job = jobs.partitionedJdbcHdfsJob();
		// Use a trigger to send data to JDBC
		stream("dataSender", sources.http() + XD_DELIMITER
				+ jdbcSink);
		sources.httpSource("dataSender").postData(data0);
		sources.httpSource("dataSender").postData(data1);
		sources.httpSource("dataSender").postData(data2);

		job(jobName, job.toDSL(), true);
		jobLaunch(jobName);
		waitForJobToComplete(jobName);
		// Evaluate the results of the test.
		String dir = JdbcHdfsJob.DEFAULT_DIRECTORY + "/";
		String path0 = JdbcHdfsJob.DEFAULT_DIRECTORY + "/" + JdbcHdfsJob.DEFAULT_FILE_NAME + "-p0" + "-0.csv";
		String path1 = JdbcHdfsJob.DEFAULT_DIRECTORY + "/" + JdbcHdfsJob.DEFAULT_FILE_NAME + "-p1" + "-0.csv";
		String path2 = JdbcHdfsJob.DEFAULT_DIRECTORY + "/" + JdbcHdfsJob.DEFAULT_FILE_NAME + "-p2" + "-0.csv";
		assertPathsExists(path0, path1, path2);
		Collection<FileStatus> fileStatuses = hadoopUtil.listDir(dir);
		assertEquals("The number of files should be 4. The directory and 3 files. ", 4, fileStatuses.size());
		for (FileStatus fileStatus : fileStatuses) {
			if (!fileStatus.isDirectory()) {
				assertTrue("The file should be of reasonable size", fileStatus.getLen() > 5 && fileStatus.getLen() < 10);
			}
		}
	}

	private void assertPathsExists(String... paths) {
		for (String path : paths) {
			assertTrue(path + " is missing from hdfs",
					hadoopUtil.waitForPath(WAIT_TIME, path));// wait up to 10 seconds for file to be closed
		}
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
