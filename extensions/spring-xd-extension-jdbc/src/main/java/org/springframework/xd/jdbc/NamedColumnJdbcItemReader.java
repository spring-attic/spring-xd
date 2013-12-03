package org.springframework.xd.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;
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
		if (!StringUtils.hasText(getSql())) {
			Assert.hasText(tableName, "tableName must be set");
			Assert.hasText(names, "columns must be set");

			String sql = "select " + names + " from " + tableName;
			log.info("Setting SQL to: " + sql);
			setSql(sql);
		}
		else if (StringUtils.hasText(names) || StringUtils.hasText(tableName)) {
			log.warn("You must set either the 'sql' property or 'tableName' and 'columns'.");
		}

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
