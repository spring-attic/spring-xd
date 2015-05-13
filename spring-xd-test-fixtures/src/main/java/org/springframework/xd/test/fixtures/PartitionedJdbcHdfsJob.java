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
 * A test fixture that represents a partitioned JdbcHdfs Job
 *
 * @author Glenn Renfro
 * @author Thomas Risberg
 */
public class PartitionedJdbcHdfsJob extends AbstractModuleFixture<PartitionedJdbcHdfsJob> {

	public final static String DEFAULT_TABLE = "jdbchdfstest";

	public final static String DEFAULT_COLUMN_NAMES = "id,name";

	public final static String DEFAULT_PARTITION_COLUMN = "id";

	public final static int DEFAULT_PARTITIONS = 3;

	private final String dir;

	private final String fileName;

	private final String table;

	private final String columnNames;

	private final String partitionColumn;

	private final int partitions;


	/**
	 * Construct a new jdbchdfs partitioned fixture using the provided parameters.
	 *
	 * @param dir the directory where the result file will be written
	 * @param fileName The name of the file to be written.
	 * @param tableName The tableName name to select from
	 * @param columns The column names to select
	 * @param partitionColumn The column used for partitioning
	 * @param partitions The number of partitions
	 */
	public PartitionedJdbcHdfsJob(String dir, String fileName, String tableName, String columns,
	                              String partitionColumn, int partitions) {
		Assert.hasText(dir, "dir must not be null or empty");
		Assert.hasText(fileName, "fileName must not be null or empty");
		Assert.hasText(tableName, "tableName must not be null nor empty");
		Assert.hasText(columns, "columns must not be null nor empty");
		Assert.hasText(partitionColumn, "partitionColumn must not be null nor empty");

		this.dir = dir;
		this.fileName = fileName;
		this.table = tableName;
		this.columnNames = columns;
		this.partitionColumn = partitionColumn;
		this.partitions = partitions;
	}

	/**
	 * Creates an instance of the JdbcHdfsJob fixture using defaults.
	 *
	 * @return an instance of the JdbcHdfsJob fixture.
	 */
	public static PartitionedJdbcHdfsJob withDefaults() {
		return new PartitionedJdbcHdfsJob(JdbcHdfsJob.DEFAULT_DIRECTORY, JdbcHdfsJob.DEFAULT_FILE_NAME,
				DEFAULT_TABLE, DEFAULT_COLUMN_NAMES, DEFAULT_PARTITION_COLUMN, DEFAULT_PARTITIONS);
	}

	/**
	 * Renders the default DSL for this fixture.
	 */
	@Override
	public String toDSL() {
		return String.format(
				"jdbchdfs --directory=%s --fileName=%s --tableName=%s --columns='%s' --partitionColumn=%s --partitions=%d",
				dir, fileName, table, columnNames, partitionColumn, partitions);
	}


}
