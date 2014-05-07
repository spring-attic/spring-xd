/*
 * Copyright 2013-2014 the original author or authors.
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

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import org.springframework.xd.test.fixtures.FilePollHdfsJob;
import org.springframework.xd.test.fixtures.SimpleFileSource;


/**
 * Runs a basic suite of FilePollHdfs tests on an XD Cluster instance.
 *
 * @author Glenn Renfro
 */
public class FilePollHdfsTest extends AbstractIntegrationTest {


	public final static String DEFAULT_NAMES = "data";

	/**
	 * Clears out the test directory
	 *
	 * @throws Exception
	 */
	@Before
	public void initialize() throws Exception {
		if (hadoopUtil.fileExists(FilePollHdfsJob.DEFAULT_DIRECTORY)) {
			hadoopUtil.fileRemove(FilePollHdfsJob.DEFAULT_DIRECTORY);
		}
	}

	/**
	 * Verifies that FilePollHdfs Job has written the test data to the hdfs.
	 *
	 */
	@Test
	public void testFilePollHdfsJob() {
		String data = UUID.randomUUID().toString();
		String sourceFileName = UUID.randomUUID().toString();
		SimpleFileSource fileSource = sources.file(FilePollHdfsJob.DEFAULT_FILE_NAME, sourceFileName + ".out").reference(
				true);

		job(jobs.filePollHdfsJob(DEFAULT_NAMES).toDSL());
		waitForXD();
		stream(fileSource + XD_TAP_DELIMETER + " queue:job:" + JOB_NAME);
		waitForXD();
		stream("DataReceiver", sources.http() + XD_DELIMETER
				+ sinks.file(FilePollHdfsJob.DEFAULT_FILE_NAME, sourceFileName).toDSL("REPLACE", "true"), WAIT_TIME);
		waitForXD();
		sources.http().postData(data);
		String path = FilePollHdfsJob.DEFAULT_DIRECTORY + "/" + FilePollHdfsJob.DEFAULT_FILE_NAME + "-0.csv";
		assertTrue(path + " is not present on hdfs file system", hadoopUtil.waitForPath(WAIT_TIME, path));
		assertEquals("File size should match the data size +1 for the //n", data.length() + 1,
				hadoopUtil.getFileStatus(path).getLen());
		waitForXD(); //wait for Hadoop
		assertEquals("The data returned from hadoop was different than was sent.  ", data + "\n",
				hadoopUtil.getFileContentsFromHdfs(path));
	}


}
