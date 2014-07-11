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

package org.springframework.xd.test.fixtures;

import org.springframework.util.Assert;


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


/**
 * Test fixture that creates a File Poll Hdfs Job.
 *
 * @author Glenn Renfro
 */
public class FilePollHdfsJob extends AbstractModuleFixture<FilePollHdfsJob> {


	public static final String DEFAULT_FILE_NAME = "filePollHdfsJob";

	public static final String DEFAULT_DIRECTORY = "/xd/filepollhdfsjob";

	private String names;

	private String fileName;

	private String directory;

	/**
	 * Creates a FilePollHdfsJob instance using defaults.
	 * @param names the column names the job will pull from the csv file 
	 * @return an instance of the FilePollHdfsJob
	 */
	public static FilePollHdfsJob withDefaults(String names) {
		return new FilePollHdfsJob(DEFAULT_DIRECTORY, DEFAULT_FILE_NAME, names);
	}

	/**
	 * Initializes an instance of FilePollHdfsJob
	 * @param directory The hdfs directory where the result file will be written
	 * @param fileName The result file name
	 * @param names the column names the job will pull from the csv file 
	 */
	public FilePollHdfsJob(String directory, String fileName, String names) {
		Assert.hasText("directory must not be empty nor null");
		Assert.hasText("fileName must not be empty nor null");
		Assert.hasText("names must not be empty nor null");
		this.directory = directory;
		this.fileName = fileName;
		this.names = names;
	}

	/**
	 * Renders the default DSL for this fixture.
	 */
	@Override
	public String toDSL() {
		StringBuilder dsl = new StringBuilder("filepollhdfs ");
		if (fileName != null) {
			dsl.append(" --fileName=" + fileName);
		}
		if (directory != null) {
			dsl.append(" --directory=" + directory);
		}
		if (names != null) {
			dsl.append(" --names=" + names);
		}
		return dsl.toString();

	}

	/**
	 * Sets the fileName that the hdfs will write to.
	 *
	 * @param fileName the name of the table.
	 * @return an instance to this FilePollHdfsJob.
	 */
	public FilePollHdfsJob fileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	/**
	 * Sets the directory that the hdfs will write to.
	 *
	 * @param directory the name of the table.
	 * @return an instance to this FilePollHdfsJob.
	 */
	public FilePollHdfsJob directory(String directory) {
		this.directory = directory;
		return this;
	}

	/**
	 * Sets the column names the job will pull from the csv file
	 *
	 * @param names comma delimited list of column names
	 * @return an instance to this FilePollHdfsJob.
	 */
	public FilePollHdfsJob names(String names) {
		this.names = names;
		return this;
	}

}
