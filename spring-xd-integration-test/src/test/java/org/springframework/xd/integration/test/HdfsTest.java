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

import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.apache.hadoop.fs.FileStatus;
import org.junit.Before;
import org.junit.Test;

import org.springframework.util.StringUtils;
import org.springframework.xd.test.fixtures.HdfsSink;


/**
 * Runs a basic suite of hdfs tests on an XD Cluster instance.
 *
 * @author Glenn Renfro
 */
public class HdfsTest extends AbstractIntegrationTest {


	/**
	 * Clears out the test directory
	 *
	 * @throws Exception
	 */
	@Before
	public void initialize() throws Exception {
		if (hadoopUtil.fileExists(HdfsSink.DEFAULT_DIRECTORY)) {
			hadoopUtil.fileRemove(HdfsSink.DEFAULT_DIRECTORY);
		}
	}


	/**
	 * Asserts that data sent to the hdfs sink is stored on a hadoop hdfs.
	 */
	@Test
	public void testHdfsSink() {
		String data = UUID.randomUUID().toString();
		stream("trigger --payload='" + data + "'" + XD_DELIMITER + sinks.hdfs());
		// wait up to 10 seconds for directory to be created
		assertTrue(HdfsSink.DEFAULT_DIRECTORY + " directory is missing from hdfs",
				hadoopUtil.waitForPath(10000, HdfsSink.DEFAULT_DIRECTORY));

		// This forces the hdfs to flush the contents to the file and close. So the tests can be executed.
		undeployStream();

		String path = HdfsSink.DEFAULT_DIRECTORY + "/" + HdfsSink.DEFAULT_FILE_NAME + "*";

		path = getTestFilePath(HdfsSink.DEFAULT_DIRECTORY, hadoopUtil.getFileStatus(path));
		assertValidHdfs(data, path);
	}


	private String getTestFilePath(String directory, FileStatus status) {
		String fileName = status.getPath().getName();
		if (StringUtils.getFilenameExtension(fileName).equals("tmp")) {
			fileName = FilenameUtils.removeExtension(fileName);
		}
		return directory + "/" + fileName;
	}
}
