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
public class FilePollHdfsTest extends AbstractJobTest {


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
		String jobName = "tfphj" + UUID.randomUUID().toString();
		String sourceFileName = UUID.randomUUID().toString() + ".out";
		String sourceDir = "/tmp/xd/" + FilePollHdfsJob.DEFAULT_FILE_NAME;

		SimpleFileSource fileSource = sources.file(sourceDir, sourceFileName).mode(SimpleFileSource.Mode.REF);
		job(jobName, jobs.filePollHdfsJob(DEFAULT_NAMES).toDSL(), true);
		stream(fileSource + XD_TAP_DELIMITER + " queue:job:" + jobName);
		//Since the job may be on a different container you will have to copy the file to the job's container.
		setupDataFiles(getContainerHostForJob(jobName), sourceDir, sourceFileName, data);
		//Copy file to source container instance.
		setupSourceDataFiles(sourceDir, sourceFileName, data);
		waitForJobToComplete(jobName);
		assertValidHdfs(data, FilePollHdfsJob.DEFAULT_DIRECTORY + "/" + FilePollHdfsJob.DEFAULT_FILE_NAME + "-0.csv");
	}

}
