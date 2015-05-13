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
 */
public class JdbcHdfsJob extends AbstractModuleFixture<JdbcHdfsJob> {

	public final static String DEFAULT_DIRECTORY = "/xd/jdbchdfstest";

	public final static String DEFAULT_FILE_NAME = "jdbchdfstest";

	public final static String DEFAULT_SQL = "select payload from jdbchdfstest";

	public final static String DEFAULT_COLUMNS = "";

	public final static String DEFAULT_TABLE = "";

	private final String dir;

	private final String fileName;

	private final String sql;

	private final String columns;

	private final String table;

	/**
	 * Construct a new jdbchdfs fixture using the provided dir, file and sql.
	 *
	 * @param dir the directory where the result file will be written
	 * @param fileName The name of the file to be written.
	 * @param sql The sql statement that will extract the data from the database
	 */
	public JdbcHdfsJob(String dir, String fileName, String sql, String columns, String table) {
		Assert.hasText(dir, "dir must not be null or empty");
		Assert.hasText(fileName, "fileName must not be null or empty");
		Assert.state(StringUtils.hasText(sql) || (StringUtils.hasText(columns) && StringUtils.hasText(table)), "Either sql or column/table must be provided");

		this.dir = dir;
		this.fileName = fileName;
		this.sql = sql;
		this.columns = columns;
		this.table = table;
	}

	/**
	 * Creates an instance of the JdbcHdfsJob fixture using defaults.
	 *
	 * @return an instance of the JdbcHdfsJob fixture.
	 */
	public static JdbcHdfsJob withDefaults() {
		return new JdbcHdfsJob(DEFAULT_DIRECTORY, DEFAULT_FILE_NAME, DEFAULT_SQL, DEFAULT_COLUMNS, DEFAULT_TABLE);
	}

	/**
	 * Renders the default DSL for this fixture.
	 */
	@Override
	public String toDSL() {
		if(StringUtils.hasText(sql)) {
			return String.format(
					"jdbchdfs --directory=%s --fileName=%s --sql='%s' ",
					dir, fileName, sql);
		}
		else {
			return String.format("jdbchdfs --directory=%s --fileName=%s --tableName=%s --columns=%s", dir, fileName, table, columns);
		}
	}
}
