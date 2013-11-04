package org.springframework.xd.jdbc;

import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.util.Assert;

/**
 * JdbcBatchItemWriter which is customized for use with the XD
 * JDBC import job.
 *
 * Sets the SQL insert property based on the column names and table name
 * supplied in the module configuration.
 *
 * @author Luke Taylor
 */
public class NamedColumnJdbcBatchItemWriter<T> extends JdbcBatchItemWriter<T> {
	private String[] names;
	private String tableName;

	public void setColumnNames(String[] names) {
		this.names = names;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notEmpty(names, "columnNames must be set");
		StringBuilder columns = new StringBuilder();
		StringBuilder namedParams = new StringBuilder();
		for (String column : names) {
			if (columns.length() > 0) {
				columns.append(", ");
				namedParams.append(", ");
			}
			columns.append(column);
			namedParams.append(":").append(column.trim());
		}
		setSql("insert into " + tableName + "(" + columns.toString() + ") values (" + namedParams.toString() + ")");
		super.afterPropertiesSet();
	}
}
