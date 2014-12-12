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
import static org.junit.Assert.assertNotNull;

import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.xd.test.fixtures.HdfsMongoDbJob;


/**
 * Asserts that this job will read the specified file from hdfs and place the results into the database.
 *
 * @author Glenn Renfro
 */
public class HdfsMongoDbTest extends AbstractJobTest {

	private HdfsMongoDbJob job;


	/**
	 * Removes the table created from a previous test.
	 */
	@Before
	public void initialize() {
		job = jobs.hdfsMongoDb();
		cleanup();
	}

	/**
	 * Asserts that hdfsMongoJob has written the test data from a file on hdfs to mongodb.
	 *
	 */
	@Test
	public void testHdfsMongoJob() {
		String data = UUID.randomUUID().toString();
		job.fileName(HdfsMongoDbJob.DEFAULT_FILE_NAME + "-0.txt");
		// Create a stream that writes to a hdfs file. This file will be used by the job.
		stream("dataSender", sources.http() + XD_DELIMITER + sinks.hdfs()
				.directoryName(HdfsMongoDbJob.DEFAULT_DIRECTORY).fileName(HdfsMongoDbJob.DEFAULT_FILE_NAME).toDSL());
		sources.httpSource("dataSender").postData(data);
		job(job.toDSL());
		waitForXD();
		//Undeploy the dataSender stream to force XD to close the file.
		this.undeployStream("dataSender");
		waitForXD();
		jobLaunch();
		waitForXD();
		Map<String, String> result = job.getSingleObject(HdfsMongoDbJob.DEFAULT_COLLECTION_NAME);
		String dataResult = result.get("_id");
		assertNotNull("The attribute " + HdfsMongoDbJob.DEFAULT_ID_FIELD + "not present in result", dataResult);
		assertEquals(data, dataResult);
	}

	/**
	 * Being a good steward, remove the result collection from mongo and source file from hdfs.
	 */
	@After
	public void cleanup() {
		if (job != null) {
			job.dropCollection(HdfsMongoDbJob.DEFAULT_COLLECTION_NAME);
		}
		if (hadoopUtil.fileExists(HdfsMongoDbJob.DEFAULT_DIRECTORY)) {
			hadoopUtil.fileRemove(HdfsMongoDbJob.DEFAULT_DIRECTORY);
		}
	}
}
