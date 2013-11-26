package org.springframework.xd.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * Reader which reads a row from a database as a delimited string from a
 * predefined list of column names.
 *
 * @author Luke Taylor
 */
public class NamedColumnJdbcItemReader extends JdbcCursorItemReader<Tuple> {
	private String[] names;
	private String tableName;

	/**
	 * The column names in the database, in the order in which they should be read and
	 * assembled into the returned string.
	 */
	public void setColumnNames(String[] names) {
		this.names = names;
	}

	/**
	 * The table to read from.
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notEmpty(names, "columnNames must be set");
		Assert.hasText(tableName, "tableName must be set");

		setSql("select " + StringUtils.arrayToCommaDelimitedString(names) + " from " + tableName);
		setRowMapper(new RowMapper<Tuple>() {
			@Override
			public Tuple mapRow(ResultSet rs, int rowNum) throws SQLException {
				TupleBuilder builder = TupleBuilder.tuple();

				for (String name: names) {
					builder.put(name, rs.getString(name));
				}

				return builder.build();
			}
		});

		super.afterPropertiesSet();
	}
}
