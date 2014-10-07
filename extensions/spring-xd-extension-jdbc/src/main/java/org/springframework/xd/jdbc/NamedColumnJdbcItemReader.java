/*
 * Copyright 2014 the original author or authors.
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
 * @author Thomas Risberg
 */
public class NamedColumnJdbcItemReader extends JdbcCursorItemReader<Tuple> {

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
