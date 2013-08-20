/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.xd.tuple.batch;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.xd.tuple.Tuple;
import org.springframework.xd.tuple.TupleBuilder;

/**
 * A {@link RowMapper} implementation to be used with the {@link Tuple} data structure. The resulting Tuple consists of
 * the column names as the field names and the result of the {@link ResultSet#getObject(int)} as the value. The order of
 * the data will be the order they are returned in the ResultSet (column order).
 * 
 * @author Michael Minella
 * 
 */
public class TupleRowMapper implements RowMapper<Tuple> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.jdbc.core.RowMapper#mapRow(ResultSet rs, int rowNum)
	 */
	@Override
	public Tuple mapRow(ResultSet rs, int rowNum) throws SQLException {
		int columnCount = rs.getMetaData().getColumnCount();

		TupleBuilder builder = TupleBuilder.tuple();

		for (int i = 1; i <= columnCount; i++) {
			builder.put(rs.getMetaData().getColumnName(i), JdbcUtils.getResultSetValue(rs, i));
		}

		return builder.build();
	}
}
