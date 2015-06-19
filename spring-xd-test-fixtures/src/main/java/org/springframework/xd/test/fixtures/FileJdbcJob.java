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
import org.springframework.util.StringUtils;


/**
 * A test fixture that represents the FileJdbc Job
 *
 * @author Glenn Renfro
 */
public class FileJdbcJob extends AbstractModuleFixture<FileJdbcJob> {

	public final static String DEFAULT_DIRECTORY = "filejdbctest";

	public final static String DEFAULT_FILE_NAME = "filejdbctest.out";

	public final static String DEFAULT_TABLE_NAME = "filejdbctest";

	public final static String DEFAULT_NAMES = "data";

	public static final boolean DEFAULT_DELETE_FILES = false;


	private final String dir;

	private final String fileName;

	private final String tableName;

	private final String names;

	private final boolean deleteFiles;

	/**
	 * Construct a new FileJdbcJob using the provided directory and file names.
	 *
	 * @param dir the directory where the source file is located
	 * @param fileName The file from which data will be pulled.
	 * @param tableName the table where the data will be written.
	 * @param names a comma delimited list of column names that are contained in the source file.
	 *
	 */
	public FileJdbcJob(String dir, String fileName, String tableName, String names, boolean deleteFiles) {
		Assert.hasText(dir, "dir must not be null or empty");
		Assert.hasText(tableName, "tableName must not be null or empty");
		Assert.hasText(names, "names must not be null nor empty");

		this.dir = dir;
		this.fileName = fileName;
		this.tableName = tableName;
		this.names = names;
		this.deleteFiles = deleteFiles;
	}

	public static FileJdbcJob withDefaults() {
		return new FileJdbcJob(DEFAULT_DIRECTORY, DEFAULT_FILE_NAME, DEFAULT_TABLE_NAME, DEFAULT_NAMES, DEFAULT_DELETE_FILES);
	}

	/**
	 * Renders the default DSL for this fixture.
	 */
	@Override
	public String toDSL() {
		StringBuilder definition = new StringBuilder();

		if(StringUtils.hasText(fileName)) {
			definition.append(String.format(
					"filejdbc --resources=file:%s/%s --names=%s --tableName=%s --initializeDatabase=true ", dir,
					fileName, names, tableName));
		}
		else {
			definition.append(String.format(
					"filejdbc --resources=file:%s --names=%s --tableName=%s --initializeDatabase=true ", dir,
					names, tableName));
		}

		if(deleteFiles) {
			definition.append("--deleteFiles=true ");
		}

		return definition.toString();
	}
}
