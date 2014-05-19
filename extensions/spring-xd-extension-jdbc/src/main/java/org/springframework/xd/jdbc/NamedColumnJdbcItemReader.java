package org.springframework.xd.jdbc;

import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Reader which reads a row from a database as a delimited string from a
 * predefined list of column names.
 *
 * @author Luke Taylor
 */
public class NamedColumnJdbcItemReader extends JdbcCursorItemReader<Tuple> {
	private String names;
	private String tableName;

	/**
	 * The column names in the database, in the order in which they should be read and
	 * assembled into the returned string.
	 */
	public void setColumnNames(String names) {
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
		setRowMapper(new RowMapper<Tuple>() {
			@Override
			public Tuple mapRow(ResultSet rs, int rowNum) throws SQLException {
				TupleBuilder builder = TupleBuilder.tuple();

				for (int i=1; i <= rs.getMetaData().getColumnCount(); i++) {
					String name = JdbcUtils.lookupColumnName(rs.getMetaData(), i);
					builder.put(name, JdbcUtils.getResultSetValue(rs, i, String.class));
				}

				return builder.build();
			}
		});

		super.afterPropertiesSet();
	}
}
