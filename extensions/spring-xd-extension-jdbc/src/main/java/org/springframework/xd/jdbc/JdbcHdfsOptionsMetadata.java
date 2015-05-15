/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.jdbc;

import static org.springframework.xd.module.options.spi.ModulePlaceholders.XD_JOB_NAME;

import javax.validation.constraints.AssertTrue;

import org.springframework.util.StringUtils;
import org.springframework.xd.module.options.mixins.BatchJobCommitIntervalOptionMixin;
import org.springframework.xd.module.options.mixins.BatchJobFieldDelimiterOptionMixin;
import org.springframework.xd.module.options.mixins.BatchJobSinglestepPartitionSupportOptionMixin;
import org.springframework.xd.module.options.mixins.HadoopConfigurationMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Captures options for the {@code jdbchdfs} job.
 * 
 * @author Eric Bottard
 * @author Luke Taylor
 * @author Ilayaperumal Gopinathan
 * @author Thomas Risberg
 * @author Glenn Renfro
 * @author Michael Minella
 */
@Mixin({ JdbcConnectionMixin.class, JdbcConnectionPoolMixin.class, BatchJobFieldDelimiterOptionMixin.class,
		BatchJobCommitIntervalOptionMixin.class, HadoopConfigurationMixin.class,
		BatchJobSinglestepPartitionSupportOptionMixin.class})
public class JdbcHdfsOptionsMetadata {

	private String tableName = "";

	private String columns = "";

	private String partitionColumn = "";

	private int partitions = 1;

	private String sql = "";

	private String fileName = XD_JOB_NAME;

	private int rollover = 1000000;

	private String directory = "/xd/" + XD_JOB_NAME;

	private String fileExtension = "csv";

	private String checkColumn = "";

	@ModuleOption("the column to be examined when determining which rows to import")
	public void setCheckColumn(String checkColumn) {
		this.checkColumn = checkColumn;
	}

	@ModuleOption("the table to read data from")
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	@ModuleOption("the column names to read from the supplied table")
	public void setColumns(String columns) {
		this.columns = columns;
	}

	@ModuleOption("the column to use for partitioning, should be numeric and uniformly distributed")
	public void setPartitionColumn(String partitionColumn) {
		this.partitionColumn = partitionColumn;
	}

	@ModuleOption("the number of partitions")
	public void setPartitions(int partitions) {
		this.partitions = partitions;
	}

	@ModuleOption("the SQL to use to extract data")
	public void setSql(String sql) {
		this.sql = sql;
	}

	@AssertTrue(message = "Use ('tableName' AND 'columns') OR 'sql' to define the data import")
	boolean isEitherSqlOrTableAndColumns() {
		if (!StringUtils.hasText(sql)) {
			return StringUtils.hasText(tableName) && StringUtils.hasText(columns);
		}
		else {
			return !StringUtils.hasText(tableName) && !StringUtils.hasText(columns);
		}
	}

	@AssertTrue(message = "Use ('tableName' AND 'columns') when using partition column")
	boolean isPartitionedWithTableName() {
		if (StringUtils.hasText(partitionColumn)) {
			return StringUtils.hasText(tableName) && !StringUtils.hasText(sql);
		}
		else {
			return true;
		}
	}

	@ModuleOption("the filename to use in HDFS")
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@ModuleOption("the number of bytes to write before creating a new file in HDFS")
	public void setRollover(int rollover) {
		this.rollover = rollover;
	}

	@ModuleOption("the directory to write the file(s) to in HDFS")
	public void setDirectory(String directory) {
		this.directory = directory;
	}

	@ModuleOption("the file extension to use")
	public void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension;
	}

	public String getTableName() {
		return tableName;
	}

	public String getColumns() {
		return columns;
	}

	public String getSql() {
		return sql;
	}

	public String getPartitionColumn() {
		return partitionColumn;
	}

	public int getPartitions() {
		return partitions;
	}

	public String getFileName() {
		return fileName;
	}

	public int getRollover() {
		return rollover;
	}

	public String getDirectory() {
		return directory;
	}

	public String getFileExtension() {
		return fileExtension;
	}

	public String getCheckColumn() {
		return checkColumn;
	}
}
