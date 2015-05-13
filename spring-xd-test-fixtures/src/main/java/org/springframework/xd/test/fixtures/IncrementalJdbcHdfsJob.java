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
 * A test fixture that represents the JdbcHdfs Job
 *
 * @author Glenn Renfro
 * @author Michael Minella
 */
public class IncrementalJdbcHdfsJob extends AbstractModuleFixture<IncrementalJdbcHdfsJob> {

	public final static String DEFAULT_DIRECTORY = "/xd/jdbchdfstest";

	public final static String DEFAULT_FILE_NAME = "jdbchdfstest";

	public final static String DEFAULT_TABLE = "jdbchdfstest";

	public final static String DEFAULT_COLUMN = "payload";

	public final static int DEFAULT_OVERRIDE = -1;

	private static final int DEFAULT_PARTITIONS = 1;

	private final String dir;

	private final String fileName;

	private final String table;

	private final String columns;

	private final String checkColumn;

	private final int override;

	private final String partitionColumn;

	private final int partitions;

	/**
	 * Construct a new jdbchdfs fixture using the provided dir, file and sql.
	 *
	 * @param dir the directory where the result file will be written
	 * @param fileName The name of the file to be written.
	 * @param table The table that contains the data to be extracted from the database
	 * @param columns The columns that contains the data to be extracted from the database
	 */
	public IncrementalJdbcHdfsJob(String dir, String fileName, String table, String columns, String partitionColumn, int partitions, String checkColumn, int override) {
		Assert.hasText(dir, "dir must not be null or empty");
		Assert.hasText(fileName, "fileName must not be null or empty");
		Assert.hasText(table, "table must not be null nor empty");
		Assert.hasText(columns, "column must not be null nor empty");

		this.dir = dir;
		this.fileName = fileName;
		this.checkColumn = checkColumn;
		this.table = table;
		this.columns = columns;
		this.override = override;
		this.partitionColumn = partitionColumn;
		this.partitions = partitions;
	}

	/**
	 * Creates an instance of the JdbcHdfsJob fixture using defaults.
	 *
	 * @return an instance of the JdbcHdfsJob fixture.
	 */
	public static IncrementalJdbcHdfsJob withDefaults() {
		return new IncrementalJdbcHdfsJob(DEFAULT_DIRECTORY, DEFAULT_FILE_NAME, DEFAULT_TABLE, DEFAULT_COLUMN, DEFAULT_COLUMN, DEFAULT_PARTITIONS, DEFAULT_COLUMN, DEFAULT_OVERRIDE);
	}

	/**
	 * Renders the default DSL for this fixture.
	 */
	@Override
	public String toDSL() {
		StringBuilder dsl = new StringBuilder();
		dsl.append(String.format("jdbchdfs --directory=%s --fileName=%s", this.dir, this.fileName));

		if(StringUtils.hasText(this.checkColumn)) {
			dsl.append(String.format(" --tableName=%s --columns=%s --checkColumn=%s", this.table, this.columns, this.checkColumn));
		}

		if(StringUtils.hasText(this.partitionColumn)) {
			dsl.append(String.format(" --partitionColumn=%s --partitions=%s", this.partitionColumn, this.partitions));
		}

		return dsl.toString();
	}
}
