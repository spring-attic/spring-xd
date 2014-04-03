
package org.springframework.xd.jdbc;

import javax.validation.constraints.AssertTrue;

import org.springframework.util.StringUtils;
import org.springframework.xd.module.options.mixins.BatchJobRestartableOptionMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * @author Luke Taylor
 * @author Ilayaperumal Gopinathan
 */
@Mixin(BatchJobRestartableOptionMixin.class)
public class JdbcHdfsOptionsMetadata extends AbstractJdbcOptionsMetadata {

	private String tableName = "";

	private String columns = "";

	private String sql = "";

	private String fileName;

	private int rollover = 1000000;

	private String directory;

	private String fileExtension = "csv";

	@ModuleOption("the table to read data from")
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	@ModuleOption("the column names to read from the supplied table")
	public void setColumns(String columns) {
		this.columns = columns;
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

	@ModuleOption("the file extension to use (defaults to 'csv')")
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
}
