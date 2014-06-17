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

package org.springframework.xd.test.fixtures;

import org.springframework.util.Assert;


/**
 * A test fixture that represents the HdfsJdbc Job
 *
 * @author Glenn Renfro
 */
public class HdfsJdbcJob extends AbstractModuleFixture<HdfsJdbcJob> {

	public final static String DEFAULT_DIRECTORY = "/xd/hdfsjdbctest";

	public final static String DEFAULT_FILE_NAME = "hdfsjdbctest";

	public final static String DEFAULT_TABLE_NAME = "hdfsjdbctest";

	public final static String DEFAULT_NAMES = "data";


	private String dir;

	private String fileName;

	private String tableName;

	private String names;

	/**
	 * Construct a new HdfsJdbcJob using the provided directory and file names.
	 *
	 * @param dir the directory where the source file is located on hdfs
	 * @param fileName The file from which data will be pulled.
	 * @param tableName the table where the data will be written.
	 * @param names a comma delimited list of column names that are contained in the source file.
	 */
	public HdfsJdbcJob(String dir, String fileName, String tableName, String names) {
		Assert.hasText(dir, "dir must not be null or empty");
		Assert.hasText(fileName, "fileName must not be null or empty");
		Assert.hasText(tableName, "tableName must not be null or empty");
		Assert.hasText(names, "names must not be null nor empty");

		this.dir = dir;
		this.fileName = fileName;
		this.tableName = tableName;
		this.names = names;
	}

	public static HdfsJdbcJob withDefaults() {
		return new HdfsJdbcJob(DEFAULT_DIRECTORY, DEFAULT_FILE_NAME, DEFAULT_TABLE_NAME, DEFAULT_NAMES);
	}

	/**
	 * Renders the default DSL for this fixture.
	 */
	@Override
	public String toDSL() {
		return String.format(
				"hdfsjdbc --resources=%s/%s --names=%s --tableName=%s --initializeDatabase=true ", dir,
				fileName, names, tableName);
	}

	/**
	 * Sets the directory where the file will be read from the hdfs.
	 * @param dir the directory path.
	 * @return The current instance of the HdfsJdbcJob
	 */
	public HdfsJdbcJob dir(String dir) {
		Assert.hasText(dir, "Dir should not be empty nor null");
		this.dir = dir;
		return this;
	}

	/**
	 * Sets the fileName of the file to be read
	 * @param fileName the name of the file
	 * @return The current instance of the HdfsJdbcJob
	 */
	public HdfsJdbcJob fileName(String fileName) {
		Assert.hasText(fileName, "FileName should not be empty nor null");
		this.fileName = fileName;
		return this;
	}

	/**
	 * Sets the tableName where the data will be written.
	 * @param tableName The name of the table
	 * @return The current instance of the HdfsJdbcJob
	 */
	public HdfsJdbcJob tableName(String tableName) {
		Assert.hasText(tableName, "tableName should not be empty nor null");
		this.tableName = tableName;
		return this;
	}

	/**
	 * Sets the column names that will be written to.
	 * @param names  Comma delimited list of column names.
	 * @return The current instance of the HdfsJdbcJob
	 */
	public HdfsJdbcJob names(String names) {
		Assert.hasText(names, "Names should not be empty nor null");
		this.names = names;
		return this;
	}


}
